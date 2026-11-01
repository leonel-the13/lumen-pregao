package ao.lumen.pregao.learning.app;

import ao.lumen.pregao.authoring.app.ExerciseCatalog;
import ao.lumen.pregao.common.config.AppProperties;
import ao.lumen.pregao.common.domain.UuidV7;
import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.common.security.RateLimiter;
import ao.lumen.pregao.grading.app.AnswerFormatException;
import ao.lumen.pregao.grading.app.GradingService;
import ao.lumen.pregao.learning.api.LearningDtos.SubmissionAccepted;
import ao.lumen.pregao.learning.api.LearningDtos.SubmissionView;
import ao.lumen.pregao.learning.domain.Attempt;
import ao.lumen.pregao.learning.domain.Submission;
import ao.lumen.pregao.learning.domain.SubmissionStatus;
import ao.lumen.pregao.learning.infra.AttemptRepository;
import ao.lumen.pregao.learning.infra.SubmissionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recebe submissões: valida, persiste (QUEUED) e só depois corrige (CON-04).
 * Reenviar com a mesma Idempotency-Key devolve a submissão original (CON-02).
 */
@Service
public class SubmissionService {
    private static final Pattern KEY = Pattern.compile("^[A-Za-z0-9-]{8,64}$");
    static final int MAX_PAYLOAD_CHARS = 32 * 1024;

    private final AttemptRepository attempts;
    private final SubmissionRepository submissions;
    private final AttemptService attemptService;
    private final ExerciseCatalog catalog;
    private final GradingService grading;
    private final RateLimiter limiter;
    private final ApplicationEventPublisher events;
    private final AppProperties props;
    private final Clock clock;

    public SubmissionService(AttemptRepository attempts, SubmissionRepository submissions,
                             AttemptService attemptService, ExerciseCatalog catalog, GradingService grading,
                             RateLimiter limiter, ApplicationEventPublisher events, AppProperties props, Clock clock) {
        this.attempts = attempts;
        this.submissions = submissions;
        this.attemptService = attemptService;
        this.catalog = catalog;
        this.grading = grading;
        this.limiter = limiter;
        this.events = events;
        this.props = props;
        this.clock = clock;
    }

    /** Submissão numa tarefa de sala (o utilizador tem de ser o dono da tentativa). */
    @Transactional
    public SubmissionAccepted submitTask(UUID attemptId, UUID userId, String key, JsonNode payload) {
        Attempt a = attempts.findById(attemptId)
                .filter(x -> userId.equals(x.getUserId()))
                .orElseThrow(() -> DomainException.notFound("ATTEMPT_NOT_FOUND", "A tentativa não existe."));
        return enqueue(a, userId, key, payload, null);
    }

    /**
     * Ponto comum de entrada (salas e contests). `deadline` não nulo: submissões recebidas depois
     * são persistidas como REJECTED (CON-06), seja qual for a hora enviada pelo cliente.
     */
    @Transactional
    public SubmissionAccepted enqueue(Attempt a, UUID userId, String key, JsonNode payload, Instant deadline) {
        if (key == null || !KEY.matcher(key).matches()) {
            throw DomainException.badRequest("IDEMPOTENCY_KEY_INVALID",
                    "O cabeçalho Idempotency-Key é obrigatório (8 a 64 caracteres: letras, números e -).");
        }
        var existing = submissions.findByAttemptIdAndIdempotencyKey(a.getId(), key);
        if (existing.isPresent()) return accepted(existing.get());

        String body = payload == null ? "null" : payload.toString();
        if (body.length() > MAX_PAYLOAD_CHARS) throw DomainException.badRequest("PAYLOAD_TOO_LARGE", "A resposta é demasiado grande.");

        Instant received = clock.instant();
        boolean late = deadline != null && received.isAfter(deadline);
        if (!late) {
            if (!a.isOpen()) {
                throw DomainException.conflict(a.isContest() ? "ALREADY_SOLVED" : "ATTEMPT_CLOSED",
                        a.isContest() ? "A tua equipa já resolveu este problema." : "Esta tarefa já está terminada.");
            }
            validateFormat(a, payload);
        }
        UUID id = UuidV7.next();
        int inserted = submissions.insertIfAbsent(id, a.getId(), key, userId, body, received,
                late ? SubmissionStatus.REJECTED.name() : SubmissionStatus.QUEUED.name(),
                late ? "SUBMISSION_AFTER_DEADLINE" : null);
        Submission s = submissions.findByAttemptIdAndIdempotencyKey(a.getId(), key).orElseThrow();
        if (inserted == 0) return accepted(s); // pedido repetido em paralelo: devolve o original

        // Só os pedidos novos contam para o limite; uma excepção aqui desfaz a inserção.
        if (!limiter.tryAcquire("submit:" + userId, props.security().rateLimit().submissionsPerMinute(), Duration.ofMinutes(1))) {
            throw DomainException.tooMany("Estás a submeter demasiado depressa. Espera um minuto e tenta de novo.");
        }
        if (!late && submissions.pendingForAttemptExcept(a.getId(), s.getId()) > 0) {
            throw DomainException.conflict("SUBMISSION_IN_PROGRESS",
                    "A tua submissão anterior ainda está a ser corrigida. Espera pelo resultado.");
        }
        if (late) {
            events.publishEvent(new LearningEvents.SubmissionFinished(s.getId(), a.getId(), userId, a.getTeamId(),
                    a.getContestId(), a.getContestProblemId(), SubmissionStatus.REJECTED.name(), false));
        } else {
            events.publishEvent(new LearningEvents.SubmissionQueued(s.getId()));
        }
        return accepted(s);
    }

    @Transactional(readOnly = true)
    public SubmissionView get(UUID submissionId, UUID userId, java.util.function.Predicate<Attempt> canSee) {
        Submission s = submissions.findById(submissionId)
                .orElseThrow(() -> DomainException.notFound("SUBMISSION_NOT_FOUND", "A submissão não existe."));
        Attempt a = attempts.findById(s.getAttemptId()).orElseThrow();
        boolean own = userId.equals(a.getUserId());
        if (!own && !canSee.test(a)) throw DomainException.notFound("SUBMISSION_NOT_FOUND", "A submissão não existe.");
        return attemptService.submissionView(s, catalog.definition(a.getExerciseVersionId()).binaryFeedback());
    }

    private void validateFormat(Attempt a, JsonNode payload) {
        try {
            grading.grade(catalog.definition(a.getExerciseVersionId()), a.getParams(), payload);
        } catch (AnswerFormatException e) {
            throw e;
        } catch (RuntimeException ignored) {
            // erros de correcção são tratados pelo worker; aqui só interessa o formato
        }
    }

    private SubmissionAccepted accepted(Submission s) {
        return new SubmissionAccepted(s.getId(), s.getStatus().name(), s.getReceivedAt(), clock.instant());
    }
}
