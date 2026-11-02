package ao.lumen.pregao.identity.app;

import ao.lumen.pregao.common.audit.AuditService;
import ao.lumen.pregao.common.config.AppProperties;
import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.common.security.RateLimiter;
import ao.lumen.pregao.common.security.Roles;
import ao.lumen.pregao.common.web.ClientIp;
import ao.lumen.pregao.identity.api.AuthDtos.LoginRequest;
import ao.lumen.pregao.identity.api.AuthDtos.LoginStep;
import ao.lumen.pregao.identity.api.AuthDtos.RegisterRequest;
import ao.lumen.pregao.identity.domain.Course;
import ao.lumen.pregao.identity.domain.EmailVerificationToken;
import ao.lumen.pregao.identity.domain.User;
import ao.lumen.pregao.identity.domain.UserStatus;
import ao.lumen.pregao.identity.infra.CourseRepository;
import ao.lumen.pregao.identity.infra.EmailVerificationTokenRepository;
import ao.lumen.pregao.identity.infra.RoleRepository;
import ao.lumen.pregao.identity.infra.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration MFA_PENDING_TTL = Duration.ofMinutes(5);

    private final UserRepository users;
    private final RoleRepository roles;
    private final CourseRepository courses;
    private final EmailVerificationTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final RateLimiter limiter;
    private final AuditService audit;
    private final ao.lumen.pregao.notification.app.NotificationService notifications;
    private final SessionManager sessions;
    private final SecretCipher cipher;
    private final RecoveryCodes recoveryCodes;
    private final ApplicationEventPublisher events;
    private final AppProperties props;
    private final Clock clock;
    private final String dummyHash;

    public AuthService(UserRepository users, RoleRepository roles, CourseRepository courses,
                       EmailVerificationTokenRepository tokens, PasswordEncoder encoder, RateLimiter limiter,
                       AuditService audit, SessionManager sessions, SecretCipher cipher,
                       ApplicationEventPublisher events, AppProperties props, Clock clock, ao.lumen.pregao.notification.app.NotificationService notifications, RecoveryCodes recoveryCodes) {
        this.recoveryCodes = recoveryCodes;
        this.notifications = notifications;
        this.users = users;
        this.roles = roles;
        this.courses = courses;
        this.tokens = tokens;
        this.encoder = encoder;
        this.limiter = limiter;
        this.audit = audit;
        this.sessions = sessions;
        this.cipher = cipher;
        this.events = events;
        this.props = props;
        this.clock = clock;
        this.dummyHash = encoder.encode("timing-equalizer-" + RANDOM.nextLong());
    }

    // ───────────────────────── registo e verificação ─────────────────────────

    @Transactional
    public void register(RegisterRequest r, String ip) {
        if (!limiter.tryAcquire("register:ip:" + ip, props.security().rateLimit().registerPerHour(), Duration.ofHours(1))) {
            throw DomainException.tooMany("Demasiados registos a partir desta rede. Tenta de novo mais tarde.");
        }
        String email = r.email().strip().toLowerCase(Locale.ROOT);
        if (!domainAllowed(email)) {
            throw DomainException.rule("EMAIL_DOMAIN_NOT_ALLOWED",
                    "Só aceitamos o domínio de email institucional do ISPTEC.");
        }
        if (!localPartAllowed(props.identity().emailLocalPattern(), email)) {
            throw DomainException.rule("EMAIL_NOT_STUDENT",
                    "Usa o teu email institucional de estudante, que começa pelo número de estudante (ex.: 20221987@isptec.co.ao).");
        }
        PasswordPolicy.check(r.password(), email, r.name());
        Course course = courses.findById(r.courseId())
                .orElseThrow(() -> DomainException.badRequest("COURSE_NOT_FOUND", "O curso escolhido não existe."));

        Optional<User> existing = users.findByEmail(email);
        if (existing.isPresent()) {
            // Não revela a existência da conta: responde igual. Conta por confirmar → novo link de verificação;
            // conta já activa → aviso ao titular com o link para entrar.
            if (existing.get().getStatus() == UserStatus.PENDING_VERIFICATION) {
                if (limiter.tryAcquire("resend:" + email, 1, Duration.ofSeconds(45))) issueVerification(existing.get(), clock.instant());
            } else {
                events.publishEvent(new AuthEvents.DuplicateRegistration(email));
            }
            return;
        }
        Instant now = clock.instant();
        User u = new User(email, encoder.encode(r.password()), r.name().strip(), course, r.year(),
                newHandle(r.name()), now);
        u.getRoles().add(roles.findByCode(Roles.STUDENT).orElseThrow());
        users.save(u);
        audit.record(u.getId(), "USER_REGISTERED", "User", u.getId(), null, Map.of("course", course.getCode()));
        issueVerification(u, now);
    }

    @Transactional
    public void resendVerification(String rawEmail) {
        String email = rawEmail.strip().toLowerCase(Locale.ROOT);
        if (!limiter.tryAcquire("resend:" + email, 1, Duration.ofSeconds(45))) {
            throw DomainException.tooMany("Espera alguns segundos antes de pedir outro email.");
        }
        users.findByEmail(email).filter(u -> u.getStatus() == UserStatus.PENDING_VERIFICATION)
                .ifPresent(u -> issueVerification(u, clock.instant()));
    }

    /** Verifica o email e devolve o utilizador activado. */
    @Transactional
    public User verify(String token) {
        EmailVerificationToken t = tokens.findByTokenHash(sha256(token))
                .orElseThrow(() -> DomainException.rule("TOKEN_INVALID", "O link de verificação é inválido. Pede um novo."));
        Instant now = clock.instant();
        if (t.getUsedAt() != null) {
            throw DomainException.rule("TOKEN_USED", "Este link já foi usado. Entra com o teu email e palavra-passe.");
        }
        if (!t.usable(now)) {
            throw DomainException.rule("TOKEN_EXPIRED", "O link expirou. Pede um novo.");
        }
        t.use(now);
        User u = t.getUser();
        if (u.getStatus() == UserStatus.PENDING_VERIFICATION) u.activate();
        audit.record(u.getId(), "EMAIL_VERIFIED", "User", u.getId(), null, null);
        notifications.notify(u.getId(), "WELCOME", "Bem-vindo ao Lúmen Pregão",
                "Começa pela trilha recomendada no teu painel. Cada tarefa é corrigida no servidor.", "/inicio");
        return u;
    }

    private void issueVerification(User u, Instant now) {
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        tokens.save(new EmailVerificationToken(u, sha256(token), now.plus(props.identity().verificationTtl())));
        events.publishEvent(new AuthEvents.VerificationRequested(u.getId(), u.getEmail(), u.getName(), token));
    }

    // ───────────────────────── login e 2.º factor ─────────────────────────

    /** Devolve vazio quando a sessão fica activa; ou o passo de 2.º factor pedido. */
    @Transactional
    public Optional<LoginStep> login(LoginRequest r, HttpServletRequest req, HttpServletResponse res) {
        String email = r.email().strip().toLowerCase(Locale.ROOT);
        String ip = ClientIp.of(req);
        var rl = props.security().rateLimit();
        if (limiter.count("login:acct:" + email) >= rl.loginMaxFailures()
                || limiter.count("login:ip:" + ip) >= rl.loginMaxFailuresPerIp()) {
            throw DomainException.tooMany("Demasiadas tentativas. Espera 15 minutos antes de tentar de novo.");
        }
        Optional<User> found = users.findByEmail(email);
        boolean ok = found.isPresent()
                ? encoder.matches(r.password(), found.get().getPasswordHash())
                : rejectAfterDummyCheck(r.password());
        if (!ok) {
            limiter.increment("login:acct:" + email, rl.loginWindow());
            limiter.increment("login:ip:" + ip, rl.loginWindow());
            audit.recordStandalone(found.map(User::getId).orElse(null), "LOGIN_FAILED", "User",
                    found.map(User::getId).orElse(null), null);
            throw new DomainException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Dados incorrectos",
                    "Verifica o email e a palavra-passe. Após 5 tentativas, espera 15 min.");
        }
        User u = found.get();
        checkStatus(u);
        limiter.reset("login:acct:" + email);

        boolean privileged = u.roleCodes().stream().anyMatch(Roles.PRIVILEGED::contains);
        Instant now = clock.instant();
        if (privileged) {
            if (u.isMfaEnabled()) {
                sessions.startPendingMfa(req, new SessionManager.PendingMfa(u.getId(), r.remember(), now, null));
                return Optional.of(new LoginStep("mfa_required", null, null));
            }
            String secret = Totp.newSecret();
            sessions.startPendingMfa(req, new SessionManager.PendingMfa(u.getId(), r.remember(), now, cipher.encrypt(secret)));
            return Optional.of(new LoginStep("mfa_enrollment_required",
                    Totp.otpauthUri("Lúmen Pregão", u.getEmail(), secret), secret));
        }
        complete(u, r.remember(), req, res, "LOGIN");
        return Optional.empty();
    }

    /** Devolve os códigos de recuperação quando o 2.º factor acabou de ser configurado (vazio nos outros casos). */
    @Transactional
    public java.util.List<String> mfa(String code, HttpServletRequest req, HttpServletResponse res) {
        SessionManager.PendingMfa p = sessions.pendingMfa(req);
        Instant now = clock.instant();
        if (p == null || p.startedAt().plus(MFA_PENDING_TTL).isBefore(now)) {
            sessions.clearPendingMfa(req);
            throw new DomainException(HttpStatus.UNAUTHORIZED, "MFA_SESSION_EXPIRED", "Sessão expirada",
                    "O tempo para o segundo factor terminou. Entra de novo.");
        }
        String key = "mfa:" + p.userId();
        if (limiter.count(key) >= props.security().rateLimit().loginMaxFailures()) {
            sessions.terminate(req);
            throw DomainException.tooMany("Demasiados códigos errados. Espera 15 minutos antes de tentar de novo.");
        }
        User u = users.findWithRolesById(p.userId()).orElseThrow();
        checkStatus(u);
        String secret = p.enrollmentSecretEnc() != null
                ? cipher.decrypt(p.enrollmentSecretEnc()) : cipher.decrypt(u.getMfaSecretEnc());
        boolean recovery = p.enrollmentSecretEnc() == null && RecoveryCodes.looksLikeRecovery(code);
        boolean valid = recovery ? recoveryCodes.consume(u.getId(), code, now) : Totp.verify(secret, code, now);
        if (!valid) {
            limiter.increment(key, props.security().rateLimit().loginWindow());
            audit.recordStandalone(u.getId(), "MFA_FAILED", "User", u.getId(), null);
            throw new DomainException(HttpStatus.UNAUTHORIZED, "MFA_INVALID", "Código inválido",
                    "O código não está correcto. Confirma a hora do teu telemóvel e tenta de novo.");
        }
        limiter.reset(key);
        java.util.List<String> codes = java.util.List.of();
        if (p.enrollmentSecretEnc() != null) {
            u.enableMfa(p.enrollmentSecretEnc());
            codes = recoveryCodes.regenerate(u.getId());
            audit.record(u.getId(), "MFA_ENROLLED", "User", u.getId(), null, null);
        }
        if (recovery) {
            audit.record(u.getId(), "MFA_RECOVERY_CODE_USED", "User", u.getId(), null,
                    java.util.Map.of("remaining", recoveryCodes.remaining(u.getId())));
            notifications.notify(u.getId(), "SECURITY", "Código de recuperação usado",
                    "Entraste com um código de recuperação. Restam " + recoveryCodes.remaining(u.getId())
                            + ". Podes gerar novos em Perfil e definições.", "/definicoes");
        }
        complete(u, p.remember(), req, res, recovery ? "LOGIN_MFA_RECOVERY" : "LOGIN_MFA");
        return codes;
    }

    /** Depois de verificar o email, entra directamente se a conta não exigir 2.º factor. */
    @Transactional
    public boolean loginAfterVerification(User u, HttpServletRequest req, HttpServletResponse res) {
        User fresh = users.findWithRolesById(u.getId()).orElseThrow();
        if (fresh.getStatus() != UserStatus.ACTIVE) return false;
        if (fresh.roleCodes().stream().anyMatch(Roles.PRIVILEGED::contains)) return false;
        complete(fresh, false, req, res, "LOGIN_AFTER_VERIFY");
        return true;
    }

    /** Gera novos códigos de recuperação (os anteriores deixam de valer). */
    @Transactional
    public java.util.List<String> regenerateRecoveryCodes(java.util.UUID userId) {
        User u = users.findWithRolesById(userId).orElseThrow();
        if (!u.isMfaEnabled()) throw DomainException.rule("MFA_NOT_ENABLED", "O 2.º factor não está configurado nesta conta.");
        audit.record(userId, "MFA_RECOVERY_CODES_REGENERATED", "User", userId, null, null);
        return recoveryCodes.regenerate(userId);
    }

    /** Mesmo custo de Argon2 quando o email não existe (não revela a existência da conta). */
    private boolean rejectAfterDummyCheck(String password) {
        encoder.matches(password, dummyHash);
        return false;
    }

    private void complete(User u, boolean remember, HttpServletRequest req, HttpServletResponse res, String action) {
        Instant now = clock.instant();
        u.loggedIn(now);
        sessions.establish(req, res, u.getId(), u.getEmail(), u.getName(), u.roleCodes(), remember,
                Duration.ofDays(props.identity().rememberMeDays()), now);
        audit.record(u.getId(), action, "User", u.getId(), null, null);
    }

    private void checkStatus(User u) {
        switch (u.getStatus()) {
            case PENDING_VERIFICATION -> throw DomainException.forbidden("EMAIL_NOT_VERIFIED",
                    "Ainda não confirmaste o teu email. Abre o link que te enviámos ou pede um novo.");
            case INVITED -> throw DomainException.forbidden("INVITE_PENDING",
                    "Antes de entrares, define a tua palavra-passe pelo link do convite que recebeste por email.");
            case SUSPENDED -> throw DomainException.forbidden("ACCOUNT_SUSPENDED",
                    "A tua conta está suspensa. Contacta a administração da Associação de Estudantes.");
            case DELETED -> throw new DomainException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Dados incorrectos",
                    "Verifica o email e a palavra-passe. Após 5 tentativas, espera 15 min.");
            case DELETION_REQUESTED -> throw DomainException.forbidden("ACCOUNT_DELETION_REQUESTED",
                    "Pediste a eliminação desta conta. Contacta a administração se quiseres cancelar o pedido.");
            case ACTIVE -> { }
        }
    }

    private boolean domainAllowed(String email) {
        return domainAllowed(props.identity().allowedEmailDomain(), email);
    }

    /** Aceita «isptec.co.ao», «@isptec.co.ao», aspas, espaços e vários domínios separados por vírgula. */
    static boolean domainAllowed(String allowed, String email) {
        if (allowed == null || allowed.isBlank()) return false;
        int at = email.lastIndexOf('@');
        if (at <= 0) return false;
        String domain = email.substring(at + 1).strip().toLowerCase(Locale.ROOT);
        for (String d : allowed.split(",")) {
            String clean = d.strip().replace("\"", "").replace("'", "").toLowerCase(Locale.ROOT);
            if (clean.startsWith("@")) clean = clean.substring(1);
            if (!clean.isEmpty() && clean.equals(domain)) return true;
        }
        return false;
    }

    /** A parte local do email (antes de @) tem de seguir o padrão; padrão vazio = sem regra. */
    static boolean localPartAllowed(String pattern, String email) {
        if (pattern == null || pattern.isBlank()) return true;
        return java.util.regex.Pattern.compile(pattern).matcher(email.substring(0, email.lastIndexOf('@'))).find();
    }

    private String newHandle(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.length() > 24) base = base.substring(0, 24);
        if (base.isEmpty()) base = "estudante";
        String handle;
        do {
            handle = base + "-" + HexFormat.of().toHexDigits((byte) RANDOM.nextInt(256))
                    + HexFormat.of().toHexDigits((byte) RANDOM.nextInt(256));
        } while (users.existsByPortfolioHandle(handle));
        return handle;
    }

    public static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
