package ao.lumen.pregao.identity.app;

import ao.lumen.pregao.common.audit.AuditService;
import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.common.security.Roles;
import ao.lumen.pregao.common.security.SessionRevocation;
import ao.lumen.pregao.common.web.CursorPage;
import ao.lumen.pregao.identity.api.AdminUserDtos.AdminUserRow;
import ao.lumen.pregao.identity.domain.DisplayNames;
import ao.lumen.pregao.identity.domain.User;
import ao.lumen.pregao.identity.domain.UserStatus;
import ao.lumen.pregao.identity.infra.RoleRepository;
import ao.lumen.pregao.identity.infra.UserRepository;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final AuditService audit;
    private final SessionRevocation revocation;
    private final Clock clock;

    private final ao.lumen.pregao.identity.infra.CourseRepository courses;
    private final PasswordService passwords;
    private final org.springframework.context.ApplicationEventPublisher events;

    public AdminUserService(UserRepository users, RoleRepository roles, AuditService audit,
                            SessionRevocation revocation, Clock clock,
                            ao.lumen.pregao.identity.infra.CourseRepository courses, PasswordService passwords,
                            org.springframework.context.ApplicationEventPublisher events) {
        this.events = events;
        this.courses = courses;
        this.passwords = passwords;
        this.users = users;
        this.roles = roles;
        this.audit = audit;
        this.revocation = revocation;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CursorPage<AdminUserRow> search(String q, String role, String cursor, Integer limit) {
        int lim = CursorPage.clamp(limit);
        UUID c = cursor == null || cursor.isBlank() ? null : UUID.fromString(cursor);
        var rows = users.search(blankToNull(q), blankToNull(role), c, PageRequest.of(0, lim + 1));
        return CursorPage.of(rows, lim, AdminUserService::row, u -> u.getId().toString());
    }

    @Transactional
    public AdminUserRow updateRoles(UUID actor, UUID userId, Set<String> codes) {
        if (!Roles.ALL.containsAll(codes)) {
            throw DomainException.badRequest("UNKNOWN_ROLE", "Há perfis desconhecidos no pedido.");
        }
        if (codes.contains(Roles.TEAM_CAPTAIN)) {
            throw DomainException.rule("ROLE_CONTEXTUAL", "O perfil de capitão é atribuído dentro de cada equipa.");
        }
        User u = users.findWithRolesById(userId)
                .orElseThrow(() -> DomainException.notFound("USER_NOT_FOUND", "O utilizador não existe."));
        if (actor.equals(userId) && u.roleCodes().contains(Roles.ADMIN) && !codes.contains(Roles.ADMIN)) {
            throw DomainException.rule("SELF_DEMOTION", "Não podes retirar a ti próprio o perfil de administrador.");
        }
        Set<String> before = new TreeSet<>(u.roleCodes());
        u.getRoles().clear();
        u.getRoles().addAll(roles.findByCodeIn(codes));
        audit.record(actor, "ROLES_CHANGED", "User", userId, Map.of("roles", before), Map.of("roles", new TreeSet<>(codes)));
        revocation.revokeAll(userId, clock.instant());
        return row(u);
    }

    /**
     * Cria uma conta por convite. A pessoa recebe um email e só entra depois de definir a palavra-passe;
     * perfis privilegiados configuram o 2.º factor no primeiro login. Não aplica a restrição de domínio do registo.
     */
    @Transactional
    public AdminUserRow invite(UUID actor, String actorName, String name, String rawEmail, Set<String> codes,
                               UUID courseId, Integer year) {
        if (!Roles.ALL.containsAll(codes) || codes.contains(Roles.TEAM_CAPTAIN)) {
            throw DomainException.badRequest("UNKNOWN_ROLE", "Há perfis inválidos no pedido.");
        }
        String email = rawEmail.strip().toLowerCase(java.util.Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw DomainException.conflict("EMAIL_TAKEN", "Já existe uma conta com este email.");
        }
        var course = courseId == null ? null : courses.findById(courseId)
                .orElseThrow(() -> DomainException.badRequest("COURSE_NOT_FOUND", "O curso escolhido não existe."));
        String handle = "convidado-" + UUID.randomUUID().toString().substring(0, 8);
        User u = new User(email, passwords.unusablePasswordHash(), name.strip(), course, year, handle, clock.instant());
        u.setStatus(UserStatus.INVITED);
        u.getRoles().addAll(roles.findByCodeIn(codes));
        users.save(u);
        audit.record(actor, "USER_INVITED", "User", u.getId(), null, Map.of("roles", new TreeSet<>(codes)));
        passwords.sendInvite(u, actorName);
        return row(u);
    }

    @Transactional
    public AdminUserRow resendInvite(UUID actor, String actorName, UUID userId) {
        User u = users.findWithRolesById(userId)
                .orElseThrow(() -> DomainException.notFound("USER_NOT_FOUND", "O utilizador não existe."));
        if (u.getStatus() != UserStatus.INVITED) {
            throw DomainException.conflict("NOT_INVITED", "Esta conta já aceitou o convite.");
        }
        passwords.sendInvite(u, actorName);
        audit.record(actor, "INVITE_RESENT", "User", userId, null, null);
        return row(u);
    }

    /** Executa um pedido de eliminação: anonimiza a conta (irreversível) e termina as sessões. */
    @Transactional
    public AdminUserRow anonymize(UUID actor, UUID userId) {
        User u = users.findWithRolesById(userId)
                .orElseThrow(() -> DomainException.notFound("USER_NOT_FOUND", "O utilizador não existe."));
        if (u.getStatus() != UserStatus.DELETION_REQUESTED) {
            throw DomainException.conflict("NO_DELETION_REQUEST", "Só é possível eliminar contas com pedido de eliminação.");
        }
        u.getRoles().clear();
        u.anonymize(passwords.unusablePasswordHash(), clock.instant());
        audit.record(actor, "ACCOUNT_DELETED", "User", userId, null, null);
        events.publishEvent(new AuthEvents.AccountDeleted(userId));
        revocation.revokeAll(userId, clock.instant());
        return row(u);
    }

    /** Repõe o 2.º factor (telemóvel perdido): no próximo login a pessoa configura-o de novo. */
    @Transactional
    public AdminUserRow resetMfa(UUID actor, UUID userId) {
        User u = users.findWithRolesById(userId)
                .orElseThrow(() -> DomainException.notFound("USER_NOT_FOUND", "O utilizador não existe."));
        u.disableMfa();
        audit.record(actor, "MFA_RESET", "User", userId, null, null);
        revocation.revokeAll(userId, clock.instant());
        return row(u);
    }

    @Transactional
    public AdminUserRow updateStatus(UUID actor, UUID userId, UserStatus status) {
        if (actor.equals(userId)) {
            throw DomainException.rule("SELF_STATUS", "Não podes mudar o estado da tua própria conta.");
        }
        User u = users.findWithRolesById(userId)
                .orElseThrow(() -> DomainException.notFound("USER_NOT_FOUND", "O utilizador não existe."));
        UserStatus before = u.getStatus();
        u.setStatus(status);
        audit.record(actor, "STATUS_CHANGED", "User", userId, Map.of("status", before), Map.of("status", status));
        if (status != UserStatus.ACTIVE) revocation.revokeAll(userId, clock.instant());
        return row(u);
    }

    static AdminUserRow row(User u) {
        return new AdminUserRow(u.getId(), u.getName(), DisplayNames.initials(u.getName()), u.getEmail(),
                u.getCourse() == null ? null : u.getCourse().getName(), u.getYear(), u.roleCodes(),
                u.getStatus().name(), u.isMfaEnabled(), u.getLastLoginAt());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
