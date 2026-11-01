package ao.lumen.pregao.contest.app;

import ao.lumen.pregao.common.audit.AuditService;
import ao.lumen.pregao.contest.domain.Contest;
import ao.lumen.pregao.contest.domain.ContestState;
import ao.lumen.pregao.contest.infra.ContestRepository;
import ao.lumen.pregao.contest.infra.ScoreboardRepository;
import ao.lumen.pregao.contest.infra.TeamRepository;
import ao.lumen.pregao.learning.infra.SubmissionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Agendador idempotente das transições temporais (spec §8). Compara com o relógio do servidor;
 * o cliente apenas apresenta. Se o servidor esteve parado, aplica as transições em falta por ordem.
 */
@Component
public class ContestLifecycle {
    private static final Logger log = LoggerFactory.getLogger(ContestLifecycle.class);
    private static final EnumSet<ContestState> TIMED = EnumSet.of(ContestState.REGISTRATION, ContestState.WAITING_ROOM,
            ContestState.RUNNING, ContestState.FROZEN, ContestState.SUBMISSIONS_CLOSED);

    private final ContestRepository contests;
    private final TeamRepository teams;
    private final ScoreboardRepository scoreboards;
    private final SubmissionRepository submissions;
    private final AuditService audit;
    private final ao.lumen.pregao.notification.app.NotificationService notifications;
    private final ao.lumen.pregao.contest.infra.TeamMemberRepository members;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;
    private final Clock clock;

    public ContestLifecycle(ContestRepository contests, TeamRepository teams, ScoreboardRepository scoreboards,
                            SubmissionRepository submissions, AuditService audit, ApplicationEventPublisher events,
                            TransactionTemplate tx, Clock clock, ao.lumen.pregao.notification.app.NotificationService notifications, ao.lumen.pregao.contest.infra.TeamMemberRepository members) {
        this.notifications = notifications;
        this.members = members;
        this.contests = contests;
        this.teams = teams;
        this.scoreboards = scoreboards;
        this.submissions = submissions;
        this.audit = audit;
        this.events = events;
        this.tx = tx;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.contest.scheduler-interval-ms:1000}", initialDelay = 2000)
    public void tick() {
        var ids = tx.execute(s -> contests.findByStateIn(TIMED).stream().map(Contest::getId).toList());
        if (ids == null) return;
        for (UUID id : ids) {
            try {
                while (Boolean.TRUE.equals(tx.execute(s -> step(id)))) {
                    // aplica transições pendentes até estabilizar
                }
            } catch (OptimisticLockingFailureException e) {
                log.debug("Contest {} alterado em paralelo; nova tentativa no próximo ciclo", id);
            } catch (RuntimeException e) {
                log.error("Falha no agendador do contest {}", id, e);
            }
        }
    }

    /** Uma transição (ou bloqueio de equipas). Devolve true se mudou de estado. */
    Boolean step(UUID id) {
        Contest c = contests.findById(id).orElseThrow();
        Instant now = clock.instant();
        if (!now.isBefore(c.lockAt())) {
            teams.findByContestId(id).stream().filter(t -> !t.isLocked()).forEach(t -> t.lock(c.lockAt()));
        }
        ContestState next = switch (c.getState()) {
            case REGISTRATION -> !now.isBefore(c.getRegistrationClosesAt()) ? ContestState.WAITING_ROOM : null;
            case WAITING_ROOM -> !now.isBefore(c.getStartsAt()) ? ContestState.RUNNING : null;
            case RUNNING -> !now.isBefore(c.getFreezeAt()) ? ContestState.FROZEN : null;
            case FROZEN -> !now.isBefore(c.getEndsAt()) ? ContestState.SUBMISSIONS_CLOSED : null;
            case SUBMISSIONS_CLOSED -> c.getFinalPhase() != null && !c.getFinalPhase().isEmpty()
                    && submissions.pendingCount() == 0 ? ContestState.FINAL_EVALUATION : null;
            default -> null;
        };
        if (next == null) return false;
        if (next == ContestState.FROZEN) scoreboards.findByContestId(id).forEach(s -> s.freeze());
        ContestState from = c.getState();
        c.moveTo(next);
        audit.record(null, "CONTEST_" + next, "Contest", id, Map.of("state", from), Map.of("state", next));
        events.publishEvent(new ContestEvents.StateChanged(id, next));
        String msg = switch (next) {
            case WAITING_ROOM -> "A sala de espera abriu. Confirma a tua equipa antes do início.";
            case RUNNING -> "Os enunciados foram libertados. Boa sorte!";
            case FROZEN -> "O ranking congelou. Continuas a ver os teus resultados.";
            case SUBMISSIONS_CLOSED -> "As submissões terminaram. Os resultados são anunciados em breve.";
            default -> null;
        };
        if (msg != null) {
            notifications.notifyAll(members.findByContestId(id).stream().map(m -> m.getUserId()).toList(), "CONTEST",
                    c.getTitle(), msg, "/competicoes/" + id);
        }
        log.info("Contest {}: {} → {}", c.getSlug(), from, next);
        return true;
    }
}
