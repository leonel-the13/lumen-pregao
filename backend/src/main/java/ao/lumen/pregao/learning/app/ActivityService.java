package ao.lumen.pregao.learning.app;

import ao.lumen.pregao.authoring.app.ExerciseCatalog;
import ao.lumen.pregao.common.privacy.PersonalDataContributor;
import ao.lumen.pregao.learning.domain.Attempt;
import ao.lumen.pregao.learning.domain.AttemptStatus;
import ao.lumen.pregao.learning.domain.SubmissionStatus;
import ao.lumen.pregao.learning.domain.Task;
import ao.lumen.pregao.learning.infra.AttemptRepository;
import ao.lumen.pregao.learning.infra.SubmissionRepository;
import ao.lumen.pregao.learning.infra.TaskRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Actividade recente e competências (painel inicial e portefólio). */
@Service
@Transactional(readOnly = true)
public class ActivityService implements PersonalDataContributor {
    private final SubmissionRepository submissions;
    private final AttemptRepository attempts;
    private final TaskRepository tasks;
    private final ExerciseCatalog catalog;

    public ActivityService(SubmissionRepository submissions, AttemptRepository attempts, TaskRepository tasks,
                           ExerciseCatalog catalog) {
        this.submissions = submissions;
        this.attempts = attempts;
        this.tasks = tasks;
        this.catalog = catalog;
    }

    public record Activity(Instant at, String kind, String label, Integer points) {}

    public record Competency(String topic, int rooms) {}

    public List<Activity> recent(UUID user, int limit) {
        var subs = submissions.findBySubmittedByOrderByReceivedAtDesc(user, PageRequest.of(0, limit));
        var atts = attempts.findAllById(subs.stream().map(s -> s.getAttemptId()).distinct().toList()).stream()
                .collect(Collectors.toMap(Attempt::getId, a -> a));
        var taskMap = tasks.findByIdIn(atts.values().stream().map(Attempt::getTaskId).filter(Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(Task::getId, t -> t));
        List<Activity> out = new ArrayList<>();
        for (var s : subs) {
            Attempt a = atts.get(s.getAttemptId());
            if (a == null || a.getTaskId() == null) continue;
            Task t = taskMap.get(a.getTaskId());
            if (t == null) continue;
            String label = "Tarefa " + t.getPosition() + " · " + t.getRoom().getTitle();
            String kind = switch (s.getStatus()) {
                case GRADED -> Boolean.TRUE.equals(s.getCorrect()) ? "CORRECT"
                        : "STEP_OK".equals(s.getResult() == null ? null : s.getResult().path("outcome").asText()) ? "STEP" : "INCORRECT";
                case REJECTED -> "REJECTED";
                default -> "PENDING";
            };
            out.add(new Activity(s.getReceivedAt(), kind, label, s.getStatus() == SubmissionStatus.GRADED ? s.getPoints() : null));
        }
        return out;
    }

    /** Competências demonstradas: n.º de salas com tarefas resolvidas, por tema de exercício. */
    public List<Competency> competencies(UUID user) {
        var passed = attempts.findAllTaskAttempts(user).stream().filter(a -> a.getStatus() == AttemptStatus.PASSED).toList();
        if (passed.isEmpty()) return List.of();
        var defs = catalog.definitions(passed.stream().map(Attempt::getExerciseVersionId).distinct().toList());
        var taskMap = tasks.findByIdIn(passed.stream().map(Attempt::getTaskId).toList()).stream()
                .collect(Collectors.toMap(Task::getId, t -> t));
        Map<String, Set<UUID>> roomsByTopic = new java.util.TreeMap<>();
        for (Attempt a : passed) {
            var d = defs.get(a.getExerciseVersionId());
            Task t = taskMap.get(a.getTaskId());
            if (d == null || t == null) continue;
            roomsByTopic.computeIfAbsent(d.topic(), k -> new HashSet<>()).add(t.getRoom().getId());
        }
        return roomsByTopic.entrySet().stream().map(e -> new Competency(e.getKey(), e.getValue().size()))
                .sorted((x, y) -> Integer.compare(y.rooms(), x.rooms())).toList();
    }

    @Override
    public String section() {
        return "learning";
    }

    @Override
    public Object export(UUID userId) {
        var mine = attempts.findByUserId(userId);
        var subs = submissions.findByAttemptIdInOrderByReceivedAtDesc(mine.stream().map(Attempt::getId).toList());
        return Map.of(
                "attempts", mine.stream().map(a -> Map.of("id", a.getId(), "taskId", String.valueOf(a.getTaskId()),
                        "status", a.getStatus(), "score", a.getScore(), "attemptsUsed", a.getAttemptsUsed(),
                        "hintsUsed", a.getHintsUsed(), "params", a.getParams())).toList(),
                "submissions", subs.stream().map(s -> Map.of("id", s.getId(), "attemptId", s.getAttemptId(),
                        "receivedAt", s.getReceivedAt(), "status", s.getStatus(), "payload", s.getPayload())).toList());
    }
}
