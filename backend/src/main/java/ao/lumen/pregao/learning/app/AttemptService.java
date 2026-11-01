package ao.lumen.pregao.learning.app;

import ao.lumen.pregao.authoring.app.ExerciseCatalog;
import ao.lumen.pregao.common.config.AppProperties;
import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.grading.app.ExerciseDefinition;
import ao.lumen.pregao.grading.app.ParamGenerator;
import ao.lumen.pregao.grading.app.StatementRenderer;
import ao.lumen.pregao.learning.api.LearningDtos.AttemptView;
import ao.lumen.pregao.learning.api.LearningDtos.HintView;
import ao.lumen.pregao.learning.api.LearningDtos.SubmissionView;
import ao.lumen.pregao.learning.app.ProgressService.RoomState;
import ao.lumen.pregao.learning.domain.Attempt;
import ao.lumen.pregao.learning.domain.Submission;
import ao.lumen.pregao.learning.domain.Task;
import ao.lumen.pregao.learning.infra.AttemptRepository;
import ao.lumen.pregao.learning.infra.SubmissionRepository;
import ao.lumen.pregao.learning.infra.TaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AttemptService {
    private static final SecureRandom RANDOM = new SecureRandom();
    static final int MAX_DRAFT_BYTES = 64 * 1024;

    private final TaskRepository tasks;
    private final AttemptRepository attempts;
    private final SubmissionRepository submissions;
    private final ProgressService progress;
    private final ExerciseCatalog catalog;
    private final ParamGenerator generator;
    private final StatementRenderer renderer;
    private final AppProperties props;
    private final ObjectMapper json;
    private final Clock clock;
    private final TransactionTemplate tx;

    public AttemptService(TaskRepository tasks, AttemptRepository attempts, SubmissionRepository submissions,
                          ProgressService progress, ExerciseCatalog catalog, ParamGenerator generator,
                          StatementRenderer renderer, AppProperties props, ObjectMapper json, Clock clock,
                          TransactionTemplate tx) {
        this.tasks = tasks;
        this.attempts = attempts;
        this.submissions = submissions;
        this.progress = progress;
        this.catalog = catalog;
        this.generator = generator;
        this.renderer = renderer;
        this.props = props;
        this.json = json;
        this.clock = clock;
        this.tx = tx;
    }

    /** Cria ou obtém a tentativa de uma tarefa. 403 se a sala estiver bloqueada (spec §4.3, CA 1). */
    public AttemptView startOrGet(UUID taskId, UUID userId) {
        try {
            return tx.execute(s -> doStart(taskId, userId));
        } catch (DataIntegrityViolationException race) {
            // Dois pedidos em paralelo: o índice único (task_id, user_id) garante uma só tentativa.
            return tx.execute(s -> view(attempts.findByTaskIdAndUserId(taskId, userId).orElseThrow()));
        }
    }

    private AttemptView doStart(UUID taskId, UUID userId) {
        Task task = tasks.findWithRoom(taskId)
                .orElseThrow(() -> DomainException.notFound("TASK_NOT_FOUND", "A tarefa não existe."));
        var snap = progress.snapshot(userId);
        var room = snap.rooms().get(task.getRoom().getId());
        if (room == null || room.state() == RoomState.LOCKED) {
            throw DomainException.forbidden("ROOM_LOCKED", "Esta sala ainda está bloqueada. Conclui as salas anteriores.");
        }
        var existing = attempts.findByTaskIdAndUserId(taskId, userId);
        if (existing.isPresent()) return view(existing.get());
        if (!catalog.isPublished(task.getExerciseVersionId())) {
            throw DomainException.forbidden("CONTENT_UNAVAILABLE", "Esta tarefa está em revisão e não está disponível.");
        }
        ExerciseDefinition def = catalog.definition(task.getExerciseVersionId());
        long seed = RANDOM.nextLong();
        JsonNode params = generator.generate(def.paramSpec(), seed, catalog.entityPools());
        Attempt a = attempts.saveAndFlush(Attempt.forTask(taskId, userId, def.versionId(), def.type(), seed, params));
        return view(a);
    }

    /** Usado pelo módulo contest: tentativa por equipa (variante própria). */
    @Transactional
    public Attempt startForContest(UUID problemId, UUID contestId, UUID teamId, UUID versionId) {
        return attempts.findByContestProblemIdAndTeamId(problemId, teamId).orElseGet(() -> {
            ExerciseDefinition def = catalog.definition(versionId);
            long seed = RANDOM.nextLong();
            JsonNode params = generator.generate(def.paramSpec(), seed, catalog.entityPools());
            return attempts.saveAndFlush(Attempt.forContest(problemId, contestId, teamId, versionId, def.type(), seed, params));
        });
    }

    @Transactional(readOnly = true)
    public AttemptView get(UUID attemptId, UUID userId) {
        return view(owned(attemptId, userId));
    }

    @Transactional
    public void saveDraft(UUID attemptId, UUID userId, JsonNode draft) {
        Attempt a = owned(attemptId, userId);
        if (draft != null && draft.toString().length() > MAX_DRAFT_BYTES) {
            throw DomainException.badRequest("DRAFT_TOO_LARGE", "O rascunho é demasiado grande.");
        }
        a.saveDraft(draft);
    }

    @Transactional
    public HintView revealHint(UUID attemptId, UUID userId) {
        Attempt a = owned(attemptId, userId);
        if (!a.isOpen()) throw DomainException.conflict("ATTEMPT_CLOSED", "Esta tarefa já está terminada.");
        ExerciseDefinition def = catalog.definition(a.getExerciseVersionId());
        if (a.getHintsUsed() >= def.hints().size()) {
            throw DomainException.rule("NO_MORE_HINTS", "Já viste todas as dicas desta tarefa.");
        }
        int idx = a.getHintsUsed();
        a.useHint();
        return new HintView(idx + 1, def.hints().get(idx).asText(), props.learning().hintPenalty(), a.getHintsUsed());
    }

    /** Vista do estudante: parâmetros visíveis e enunciado renderizado, sem solução. */
    @Transactional(readOnly = true)
    public AttemptView view(Attempt a) {
        ExerciseDefinition def = catalog.definition(a.getExerciseVersionId());
        var rendered = renderer.render(def.statement(), def.paramSpec(), a.getParams(), catalog.confirmedSources());
        List<String> revealed = new ArrayList<>();
        for (int i = 0; i < a.getHintsUsed() && i < def.hints().size(); i++) revealed.add(def.hints().get(i).asText());
        var last = submissions.findByAttemptIdOrderByReceivedAtDesc(a.getId(), PageRequest.of(0, 1));
        int points = a.getTaskId() != null ? tasks.findById(a.getTaskId()).map(Task::getPoints).orElse(def.points())
                : def.points();
        var refs = catalog.references(List.of(def.versionId())).getOrDefault(def.versionId(), List.of());
        return new AttemptView(a.getId(), a.getTaskId(), def.type(), def.title(), rendered.text(),
                json.valueToTree(rendered.segments()), json.valueToTree(rendered.params()),
                renderer.renderPresentation(def.presentation(), a.getParams()), a.getStatus().name(),
                a.getAttemptsUsed(), a.isContest() ? 0 : def.maxAttempts(), a.getHintsUsed(), def.hints().size(),
                revealed, a.getDraft(), a.getScore(), points, def.feedbackPolicy(), def.pgcVersion(), refs,
                last.isEmpty() ? null : submissionView(last.getFirst(), def.binaryFeedback()), clock.instant());
    }

    public SubmissionView submissionView(Submission s, boolean binary) {
        JsonNode r = s.getResult();
        String outcome = r == null ? null : r.path("outcome").asText(null);
        String summary = r == null ? null : r.path("summary").asText(null);
        String hint = r == null || binary ? null : r.path("hint").asText(null);
        JsonNode details = r == null || binary ? null : r.path("details");
        Long position = null;
        if (s.getStatus().name().equals("QUEUED") || s.getStatus().name().equals("PROCESSING")) {
            position = submissions.queuePositionBefore(s.getReceivedAt()) + 1;
        }
        if (binary && summary != null) summary = Boolean.TRUE.equals(s.getCorrect()) ? "Resposta correcta." : "Resposta incorrecta.";
        return new SubmissionView(s.getId(), s.getStatus().name(), outcome, s.getCorrect(), s.getPoints(), summary, hint,
                details, s.getErrorCode(), s.getReceivedAt(), s.getGradedAt(), position);
    }

    private Attempt owned(UUID attemptId, UUID userId) {
        Attempt a = attempts.findById(attemptId)
                .orElseThrow(() -> DomainException.notFound("ATTEMPT_NOT_FOUND", "A tentativa não existe."));
        if (a.getUserId() == null || !a.getUserId().equals(userId)) {
            throw DomainException.notFound("ATTEMPT_NOT_FOUND", "A tentativa não existe.");
        }
        return a;
    }
}
