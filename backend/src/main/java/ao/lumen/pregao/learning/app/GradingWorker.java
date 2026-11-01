package ao.lumen.pregao.learning.app;

import ao.lumen.pregao.authoring.app.ExerciseCatalog;
import ao.lumen.pregao.common.config.AppProperties;
import ao.lumen.pregao.grading.app.AnswerFormatException;
import ao.lumen.pregao.grading.app.ExerciseDefinition;
import ao.lumen.pregao.grading.app.GradeResult;
import ao.lumen.pregao.grading.app.GradingService;
import ao.lumen.pregao.learning.app.ProgressService.RoomState;
import ao.lumen.pregao.learning.domain.Attempt;
import ao.lumen.pregao.learning.domain.Submission;
import ao.lumen.pregao.learning.domain.SubmissionStatus;
import ao.lumen.pregao.learning.infra.AttemptRepository;
import ao.lumen.pregao.learning.infra.SubmissionRepository;
import ao.lumen.pregao.learning.infra.TaskRepository;
import ao.lumen.pregao.ranking.app.BadgeService;
import ao.lumen.pregao.ranking.app.PointService;
import ao.lumen.pregao.ranking.app.StreakService;
import ao.lumen.pregao.ranking.domain.PointSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Worker de correcção: QUEUED → PROCESSING → GRADED/REJECTED. Pontos (PointEvent) e Scoreboard são escritos
 * na mesma transacção da correcção. Se o worker morrer a meio, o varrimento devolve a submissão à fila (CON-04).
 */
@Component
public class GradingWorker {
    private static final Logger log = LoggerFactory.getLogger(GradingWorker.class);
    private static final Duration STALE = Duration.ofSeconds(60);
    private static final Duration RETRY_AFTER = Duration.ofSeconds(15);
    private static final int MAX_RETRIES = 5;

    private final SubmissionRepository submissions;
    private final AttemptRepository attempts;
    private final TaskRepository tasks;
    private final ExerciseCatalog catalog;
    private final GradingService grading;
    private final PointService points;
    private final StreakService streaks;
    private final BadgeService badges;
    private final ProgressService progress;
    private final List<ContestScoring> contestScoring;
    private final ApplicationEventPublisher events;
    private final ExecutorService executor;
    private final TransactionTemplate tx;
    private final AppProperties props;
    private final ObjectMapper json;
    private final Clock clock;

    public GradingWorker(SubmissionRepository submissions, AttemptRepository attempts, TaskRepository tasks,
                         ExerciseCatalog catalog, GradingService grading, PointService points, StreakService streaks,
                         BadgeService badges, ProgressService progress, List<ContestScoring> contestScoring,
                         ApplicationEventPublisher events, ExecutorService gradingExecutor, TransactionTemplate tx,
                         AppProperties props, ObjectMapper json, Clock clock) {
        this.submissions = submissions;
        this.attempts = attempts;
        this.tasks = tasks;
        this.catalog = catalog;
        this.grading = grading;
        this.points = points;
        this.streaks = streaks;
        this.badges = badges;
        this.progress = progress;
        this.contestScoring = contestScoring;
        this.events = events;
        this.executor = gradingExecutor;
        this.tx = tx;
        this.props = props;
        this.json = json;
        this.clock = clock;
    }

    @TransactionalEventListener
    public void onQueued(LearningEvents.SubmissionQueued e) {
        executor.submit(() -> process(e.submissionId()));
    }

    /** Varrimento periódico: submissões presas, falhadas ou sem worker. */
    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void sweep() {
        Instant now = clock.instant();
        Integer requeued = tx.execute(s -> submissions.requeueStale(now, now.minus(STALE), now.minus(RETRY_AFTER), MAX_RETRIES));
        if (requeued != null && requeued > 0) log.info("Submissões devolvidas à fila: {}", requeued);
        List<UUID> queued = tx.execute(s -> submissions.findQueuedBefore(now.minusSeconds(2), PageRequest.of(0, 200)));
        if (queued != null) queued.forEach(id -> executor.submit(() -> process(id)));
    }

    public void process(UUID id) {
        Integer claimed = tx.execute(s -> submissions.claim(id, clock.instant()));
        if (claimed == null || claimed == 0) return;
        for (int attempt = 1; ; attempt++) {
            try {
                tx.executeWithoutResult(s -> gradeAndApply(id));
                return;
            } catch (OptimisticLockingFailureException conflict) {
                if (attempt >= 3) {
                    fail(id, "VERSION_CONFLICT");
                    return;
                }
            } catch (RuntimeException ex) {
                log.error("Falha ao corrigir a submissão {}", id, ex);
                fail(id, "GRADING_ERROR");
                return;
            }
        }
    }

    private void fail(UUID id, String code) {
        tx.executeWithoutResult(s -> submissions.markFailed(id, code, clock.instant()));
    }

    void gradeAndApply(UUID id) {
        Submission s = submissions.findById(id).orElseThrow();
        if (s.getStatus() != SubmissionStatus.PROCESSING) return;
        Attempt a = attempts.findById(s.getAttemptId()).orElseThrow();
        ExerciseDefinition def = catalog.definition(a.getExerciseVersionId());
        Instant now = clock.instant();

        if (!a.isOpen()) {
            s.rejected(a.isContest() ? "ALREADY_SOLVED" : "ATTEMPT_CLOSED", resultJson("REJECTED",
                    "Esta tarefa já estava terminada quando a submissão foi corrigida.", null, null), now);
            finished(s, a);
            return;
        }
        GradeResult r;
        try {
            r = grading.grade(def, a.getParams(), s.getPayload());
        } catch (AnswerFormatException e) {
            s.rejected("ANSWER_FORMAT_INVALID", resultJson("REJECTED", e.getMessage(), null, null), now);
            finished(s, a);
            return;
        }
        ObjectNode result = resultJson(r.outcome().name(), r.summary(), r.hint(), r.details());
        var learning = props.learning();
        switch (r.outcome()) {
            case CORRECT -> {
                if (a.isContest()) {
                    int pts = contest().onGraded(a.getContestId(), a.getContestProblemId(), a.getTeamId(),
                            s.getReceivedAt(), true, a.getAttemptsUsed());
                    a.pass(pts);
                } else {
                    int taskPoints = tasks.findById(a.getTaskId()).orElseThrow().getPoints();
                    a.pass(taskPoints - learning.failedAttemptPenalty() * a.getAttemptsUsed()
                            - learning.hintPenalty() * a.getHintsUsed());
                    points.award(a.getUserId(), null, PointSource.TASK, "attempt:" + a.getId(), a.getScore());
                }
                s.graded(true, a.getScore(), result, now);
            }
            case INCORRECT -> {
                if (a.isContest()) {
                    contest().onGraded(a.getContestId(), a.getContestProblemId(), a.getTeamId(), s.getReceivedAt(),
                            false, a.getAttemptsUsed());
                    a.failedOnce(0);
                } else {
                    a.failedOnce(def.maxAttempts());
                }
                result.put("attemptsLeft", a.isContest() ? -1 : Math.max(0, def.maxAttempts() - a.getAttemptsUsed()));
                s.graded(false, 0, result, now);
            }
            case STEP_OK -> s.graded(false, 0, result, now);
        }
        if (!a.isContest()) afterTaskGraded(a, def, r);
        finished(s, a);
    }

    private void afterTaskGraded(Attempt a, ExerciseDefinition def, GradeResult r) {
        UUID user = a.getUserId();
        streaks.recordPractice(user, clock.instant());
        if (!r.correct()) return;
        if ("C".equals(def.type()) && submissions.countCorrectByType(user, "C") >= 10) badges.award(user, "PARTIDAS_DOBRADAS");
        if ("F".equals(def.type()) && submissions.countCorrectByType(user, "F") >= 20) badges.award(user, "ANALISTA");
        var snap = progress.snapshot(user);
        boolean ivaDone = snap.rooms().values().stream()
                .filter(p -> "fiscalidade-iva".equals(p.room().getModule().getTrack().getSlug()))
                .allMatch(p -> p.state() == RoomState.COMPLETED);
        boolean anyIva = snap.rooms().values().stream()
                .anyMatch(p -> "fiscalidade-iva".equals(p.room().getModule().getTrack().getSlug()));
        if (anyIva && ivaDone) badges.award(user, "FISCALISTA");
    }

    private ContestScoring contest() {
        if (contestScoring.isEmpty()) throw new IllegalStateException("Módulo de contests indisponível");
        return contestScoring.getFirst();
    }

    private void finished(Submission s, Attempt a) {
        events.publishEvent(new LearningEvents.SubmissionFinished(s.getId(), a.getId(), s.getSubmittedBy(),
                a.getTeamId(), a.getContestId(), a.getContestProblemId(), s.getStatus().name(), s.getCorrect()));
    }

    private ObjectNode resultJson(String outcome, String summary, String hint, com.fasterxml.jackson.databind.JsonNode details) {
        ObjectNode o = json.createObjectNode().put("outcome", outcome).put("summary", summary);
        if (hint != null) o.put("hint", hint);
        if (details != null) o.set("details", details);
        return o;
    }
}
