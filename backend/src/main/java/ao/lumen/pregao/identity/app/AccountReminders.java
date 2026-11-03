package ao.lumen.pregao.identity.app;

import ao.lumen.pregao.common.audit.AuditService;
import ao.lumen.pregao.common.config.AppProperties;
import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.identity.domain.User;
import ao.lumen.pregao.identity.domain.UserStatus;
import ao.lumen.pregao.identity.infra.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lembretes por email: contas por confirmar (novo link de verificação) e estudantes sem actividade.
 * Automáticos (agendador diário) e manuais (administração). Os emails saem depois do commit.
 */
@Service
public class AccountReminders {
    /** Intervalo mínimo entre lembretes manuais à mesma pessoa. */
    static final Duration MANUAL_VERIFY_GAP = Duration.ofHours(1);
    static final Duration MANUAL_INACTIVE_GAP = Duration.ofDays(7);

    private final UserRepository users;
    private final AuthService auth;
    private final AuditService audit;
    private final ApplicationEventPublisher events;
    private final AppProperties.Mail cfg;
    private final Clock clock;

    public AccountReminders(UserRepository users, AuthService auth, AuditService audit, ApplicationEventPublisher events,
                            AppProperties props, Clock clock) {
        this.users = users;
        this.auth = auth;
        this.audit = audit;
        this.events = events;
        this.cfg = props.mail();
        this.clock = clock;
    }

    public record ReminderStats(long unverified, Instant oldestUnverifiedAt, long dueVerification, long inactive,
                                long inactiveDays, boolean automatic) {}

    public record ReminderResult(int sent) {}

    @Transactional(readOnly = true)
    public ReminderStats stats() {
        Instant now = clock.instant();
        Object[] row = users.pendingVerificationStats().get(0);
        long due = users.dueVerificationReminders(now.minus(cfg.verifyAfter()), cfg.verifyMax(), now.minus(cfg.verifyEvery())).size();
        long inactive = users.dueInactivityReminders(now.minus(cfg.inactiveAfter()), now.minus(MANUAL_INACTIVE_GAP)).size();
        return new ReminderStats(((Number) row[0]).longValue(), (Instant) row[1], due, inactive,
                cfg.inactiveAfter().toDays(), cfg.remindersEnabled());
    }

    /** Volta diária: até {@code verifyMax} lembretes por conta por confirmar e um lembrete por período aos inactivos. */
    @Transactional
    public ReminderResult runAutomatic() {
        Instant now = clock.instant();
        int sent = 0;
        for (User u : users.dueVerificationReminders(now.minus(cfg.verifyAfter()), cfg.verifyMax(), now.minus(cfg.verifyEvery()))) {
            remindVerification(u, now);
            sent++;
        }
        for (User u : users.dueInactivityReminders(now.minus(cfg.inactiveAfter()), now.minus(cfg.inactiveEvery()))) {
            remindInactive(u, now);
            sent++;
        }
        if (sent > 0) audit.record(null, "REMINDERS_AUTOMATIC", "User", null, null, Map.of("sent", sent));
        return new ReminderResult(sent);
    }

    /** Lembra todas as contas por confirmar (excepto as lembradas na última hora). */
    @Transactional
    public ReminderResult remindAllUnverified(UUID actor) {
        Instant now = clock.instant();
        List<User> due = users.dueVerificationReminders(now.minus(MANUAL_VERIFY_GAP), Integer.MAX_VALUE, now.minus(MANUAL_VERIFY_GAP));
        due.forEach(u -> remindVerification(u, now));
        audit.record(actor, "VERIFY_REMINDERS_SENT", "User", null, null, Map.of("sent", due.size()));
        return new ReminderResult(due.size());
    }

    @Transactional
    public ReminderResult remindUnverified(UUID actor, UUID userId) {
        User u = users.findById(userId)
                .orElseThrow(() -> DomainException.notFound("USER_NOT_FOUND", "O utilizador não existe."));
        if (u.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw DomainException.conflict("NOT_PENDING_VERIFICATION", "Esta conta já não está por confirmar.");
        }
        Instant now = clock.instant();
        if (u.getLastVerifyReminderAt() != null && u.getLastVerifyReminderAt().isAfter(now.minus(MANUAL_VERIFY_GAP))) {
            throw DomainException.conflict("REMINDER_TOO_SOON", "Esta pessoa recebeu um lembrete há menos de uma hora. Tenta mais tarde.");
        }
        remindVerification(u, now);
        audit.record(actor, "VERIFY_REMINDER_SENT", "User", userId, null, null);
        return new ReminderResult(1);
    }

    /** Lembra os estudantes inactivos com emails ligados (no máximo um lembrete por semana a cada pessoa). */
    @Transactional
    public ReminderResult remindAllInactive(UUID actor) {
        Instant now = clock.instant();
        List<User> due = users.dueInactivityReminders(now.minus(cfg.inactiveAfter()), now.minus(MANUAL_INACTIVE_GAP));
        due.forEach(u -> remindInactive(u, now));
        audit.record(actor, "INACTIVE_REMINDERS_SENT", "User", null, null, Map.of("sent", due.size()));
        return new ReminderResult(due.size());
    }

    private void remindVerification(User u, Instant now) {
        u.verificationReminded(now);
        auth.issueVerification(u, now, true);
    }

    private void remindInactive(User u, Instant now) {
        u.inactivityReminded(now);
        long days = Math.max(1, Duration.between(u.lastActivity(), now).toDays());
        events.publishEvent(new AuthEvents.InactivityReminder(u.getId(), u.getEmail(), u.getName(), days));
    }
}
