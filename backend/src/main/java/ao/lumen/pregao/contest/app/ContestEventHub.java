package ao.lumen.pregao.contest.app;

import ao.lumen.pregao.learning.app.LearningEvents;
import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Difusão SSE por contest (ADR-0001): `clock`, `state`, `scoreboard`, `submission`.
 * Os eventos `submission` só vão para os membros da equipa. O `scoreboard` é um aviso
 * para o cliente voltar a pedir o ranking (as regras de congelamento aplicam-se no GET).
 * Nota: registo em memória de uma instância; com várias instâncias, usar Redis pub/sub.
 */
@Component
public class ContestEventHub {
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;
    private final Map<UUID, Set<Subscriber>> subscribers = new ConcurrentHashMap<>();
    private final Clock clock;

    public ContestEventHub(Clock clock) {
        this.clock = clock;
    }

    record Subscriber(SseEmitter emitter, UUID userId, UUID teamId) {}

    public SseEmitter subscribe(UUID contestId, UUID userId, UUID teamId, String state) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        Subscriber sub = new Subscriber(emitter, userId, teamId);
        Set<Subscriber> set = subscribers.computeIfAbsent(contestId, k -> new CopyOnWriteArraySet<>());
        set.add(sub);
        Runnable remove = () -> set.remove(sub);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        send(sub, set, "clock", Map.of("serverNow", clock.instant().toString(), "state", state));
        return emitter;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onState(ContestEvents.StateChanged e) {
        broadcast(e.contestId(), "state", Map.of("state", e.state().name(), "serverNow", clock.instant().toString()));
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onScoreboard(ContestEvents.ScoreboardChanged e) {
        broadcast(e.contestId(), "scoreboard", Map.of("serverNow", clock.instant().toString()));
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onSubmission(LearningEvents.SubmissionFinished e) {
        if (e.contestId() == null) return;
        Set<Subscriber> set = subscribers.get(e.contestId());
        if (set == null) return;
        var data = new java.util.HashMap<String, Object>();
        data.put("submissionId", e.submissionId().toString());
        data.put("problemId", e.contestProblemId() == null ? "" : e.contestProblemId().toString());
        data.put("status", e.status());
        data.put("correct", Boolean.TRUE.equals(e.correct()));
        for (Subscriber s : set) {
            if (e.teamId() != null && e.teamId().equals(s.teamId())) send(s, set, "submission", data);
        }
    }

    @Scheduled(fixedDelayString = "${app.contest.sse-clock-interval-ms:5000}")
    public void heartbeat() {
        String now = clock.instant().toString();
        subscribers.forEach((id, set) -> set.forEach(s -> send(s, set, "clock", Map.of("serverNow", now))));
    }

    private void broadcast(UUID contestId, String event, Object data) {
        Set<Subscriber> set = subscribers.get(contestId);
        if (set == null) return;
        set.forEach(s -> send(s, set, event, data));
    }

    private void send(Subscriber s, Set<Subscriber> set, String event, Object data) {
        try {
            s.emitter().send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            set.remove(s);
        }
    }
}
