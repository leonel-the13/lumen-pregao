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

    public AdminUserService(UserRepository users, RoleRepository roles, AuditService audit,
                            SessionRevocation revocation, Clock clock) {
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
