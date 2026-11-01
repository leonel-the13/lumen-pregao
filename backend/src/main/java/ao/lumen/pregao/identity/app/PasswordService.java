package ao.lumen.pregao.identity.app;

import ao.lumen.pregao.common.audit.AuditService;
import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.common.security.RateLimiter;
import ao.lumen.pregao.common.security.SessionRevocation;
import ao.lumen.pregao.identity.domain.PasswordToken;
import ao.lumen.pregao.identity.domain.User;
import ao.lumen.pregao.identity.domain.UserStatus;
import ao.lumen.pregao.identity.infra.PasswordTokenRepository;
import ao.lumen.pregao.identity.infra.UserRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Convites e redefinição de palavra-passe por link de uso único (hash SHA-256 na base de dados). */
@Service
public class PasswordService {
    static final Duration RESET_TTL = Duration.ofHours(1);
    static final Duration INVITE_TTL = Duration.ofHours(72);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final PasswordTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final RateLimiter limiter;
    private final SessionRevocation revocation;
    private final AuditService audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public PasswordService(UserRepository users, PasswordTokenRepository tokens, PasswordEncoder encoder,
                           RateLimiter limiter, SessionRevocation revocation, AuditService audit,
                           ApplicationEventPublisher events, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.limiter = limiter;
        this.revocation = revocation;
        this.audit = audit;
        this.events = events;
        this.clock = clock;
    }

    /** «Esqueci-me»: resposta sempre igual, exista ou não a conta. */
    @Transactional
    public void forgot(String rawEmail, String ip) {
        String email = rawEmail.strip().toLowerCase(Locale.ROOT);
        if (!limiter.tryAcquire("forgot:" + email, 3, Duration.ofHours(1))
                || !limiter.tryAcquire("forgot:ip:" + ip, 20, Duration.ofHours(1))) {
            throw DomainException.tooMany("Pediste demasiados links. Tenta de novo dentro de uma hora.");
        }
        users.findByEmail(email)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE || u.getStatus() == UserStatus.PENDING_VERIFICATION)
                .ifPresent(u -> {
                    String token = issue(u, PasswordToken.RESET, RESET_TTL);
                    audit.record(u.getId(), "PASSWORD_RESET_REQUESTED", "User", u.getId(), null, null);
                    events.publishEvent(new AuthEvents.PasswordResetRequested(u.getEmail(), u.getName(), token));
                });
    }

    /** Define a palavra-passe pelo link. Um convite activa a conta; todas as sessões anteriores terminam. */
    @Transactional
    public String reset(String token, String password) {
        PasswordToken t = tokens.findByTokenHash(AuthService.sha256(token))
                .orElseThrow(() -> DomainException.rule("TOKEN_INVALID", "O link é inválido. Pede um novo."));
        Instant now = clock.instant();
        if (t.getUsedAt() != null) throw DomainException.rule("TOKEN_USED", "Este link já foi usado. Pede um novo se precisares.");
        if (!t.usable(now)) throw DomainException.rule("TOKEN_EXPIRED", "O link expirou. Pede um novo.");
        User u = t.getUser();
        if (u.getStatus() == UserStatus.SUSPENDED || u.getStatus() == UserStatus.DELETION_REQUESTED) {
            throw DomainException.forbidden("ACCOUNT_SUSPENDED", "Esta conta não está activa. Contacta a administração.");
        }
        PasswordPolicy.check(password, u.getEmail(), u.getName());
        u.setPasswordHash(encoder.encode(password));
        boolean invite = PasswordToken.INVITE.equals(t.getPurpose());
        if (u.getStatus() == UserStatus.INVITED || u.getStatus() == UserStatus.PENDING_VERIFICATION) u.activate();
        tokens.invalidateAll(u.getId(), now);
        t.use(now);
        audit.record(u.getId(), invite ? "INVITE_ACCEPTED" : "PASSWORD_RESET", "User", u.getId(), null, null);
        revocation.revokeAll(u.getId(), now);
        return u.getEmail();
    }

    /** Usado pela administração: cria o link do convite e envia o email. */
    @Transactional
    public void sendInvite(User u, String invitedBy) {
        tokens.invalidateAll(u.getId(), clock.instant());
        String token = issue(u, PasswordToken.INVITE, INVITE_TTL);
        events.publishEvent(new AuthEvents.UserInvited(u.getEmail(), u.getName(), invitedBy, token));
        audit.record(null, "INVITE_SENT", "User", u.getId(), null, Map.of("by", invitedBy));
    }

    private String issue(User u, String purpose, Duration ttl) {
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        tokens.save(new PasswordToken(u, purpose, AuthService.sha256(token), clock.instant().plus(ttl)));
        return token;
    }

    /** Palavra-passe aleatória e inutilizável para contas convidadas (substituída ao aceitar o convite). */
    String unusablePasswordHash() {
        byte[] raw = new byte[48];
        RANDOM.nextBytes(raw);
        return encoder.encode(Base64.getEncoder().encodeToString(raw));
    }
}
