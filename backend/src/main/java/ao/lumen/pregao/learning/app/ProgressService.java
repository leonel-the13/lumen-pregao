package ao.lumen.pregao.learning.app;

import ao.lumen.pregao.authoring.app.ExerciseCatalog;
import ao.lumen.pregao.learning.domain.Attempt;
import ao.lumen.pregao.learning.domain.AttemptStatus;
import ao.lumen.pregao.learning.domain.ContentStatus;
import ao.lumen.pregao.learning.domain.Room;
import ao.lumen.pregao.learning.domain.Task;
import ao.lumen.pregao.learning.infra.AttemptRepository;
import ao.lumen.pregao.learning.infra.RoomRepository;
import ao.lumen.pregao.learning.infra.TaskRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calcula o estado de todas as salas para um utilizador (4 queries, sem N+1).
 * Estados: LOCKED, AVAILABLE, IN_PROGRESS, COMPLETED (spec §4.3).
 */
@Service
public class ProgressService {
    private final RoomRepository rooms;
    private final TaskRepository tasks;
    private final AttemptRepository attempts;
    private final ExerciseCatalog catalog;

    public ProgressService(RoomRepository rooms, TaskRepository tasks, AttemptRepository attempts,
                           ExerciseCatalog catalog) {
        this.rooms = rooms;
        this.tasks = tasks;
        this.attempts = attempts;
        this.catalog = catalog;
    }

    public enum RoomState { LOCKED, AVAILABLE, IN_PROGRESS, COMPLETED }

    public enum TaskState { AVAILABLE, IN_PROGRESS, PASSED, FAILED, UNAVAILABLE }

    public record RoomProgress(Room room, RoomState state, int obtained, int possible, int tasksDone, int tasksTotal,
                               List<Task> tasks) {
        public int percent() {
            return possible == 0 ? 0 : Math.round(obtained * 100f / possible);
        }
    }

    /** Fotografia completa do progresso do utilizador. */
    public record Snapshot(Map<UUID, RoomProgress> rooms, Map<UUID, Attempt> attemptByTask, Set<UUID> published) {

        public TaskState taskState(Task t) {
            if (!published.contains(t.getExerciseVersionId())) return TaskState.UNAVAILABLE;
            Attempt a = attemptByTask.get(t.getId());
            if (a == null) return TaskState.AVAILABLE;
            return switch (a.getStatus()) {
                case OPEN -> TaskState.IN_PROGRESS;
                case PASSED -> TaskState.PASSED;
                case FAILED -> TaskState.FAILED;
            };
        }

        public int trackPercent(UUID trackId) {
            int ob = 0, pos = 0;
            for (RoomProgress p : rooms.values()) {
                if (!p.room().getModule().getTrack().getId().equals(trackId)) continue;
                if (p.room().getStatus() != ContentStatus.PUBLISHED) continue;
                ob += p.obtained();
                pos += p.possible();
            }
            return pos == 0 ? 0 : Math.round(ob * 100f / pos);
        }
    }

    @Transactional(readOnly = true)
    public Snapshot snapshot(UUID userId) {
        List<Room> all = rooms.findAllVisible();
        Map<UUID, Room> byId = all.stream().collect(Collectors.toMap(Room::getId, Function.identity()));
        List<Task> allTasks = all.isEmpty() ? List.of() : tasks.findByRoomIds(byId.keySet());
        Map<UUID, List<Task>> tasksByRoom = allTasks.stream().collect(Collectors.groupingBy(t -> t.getRoom().getId()));
        Map<UUID, Attempt> byTask = allTasks.isEmpty() ? Map.of()
                : attempts.findForUserTasks(userId, allTasks.stream().map(Task::getId).toList()).stream()
                        .collect(Collectors.toMap(Attempt::getTaskId, Function.identity()));
        Set<UUID> published = catalog.published(new HashSet<>(allTasks.stream().map(Task::getExerciseVersionId).toList()));

        Map<UUID, RoomProgress> out = new HashMap<>();
        for (Room r : all) compute(r, byId, tasksByRoom, byTask, published, out, new HashSet<>());
        return new Snapshot(out, byTask, published);
    }

    private RoomProgress compute(Room r, Map<UUID, Room> byId, Map<UUID, List<Task>> tasksByRoom,
                                 Map<UUID, Attempt> byTask, Set<UUID> published, Map<UUID, RoomProgress> memo,
                                 Set<UUID> visiting) {
        RoomProgress cached = memo.get(r.getId());
        if (cached != null) return cached;
        visiting.add(r.getId());

        List<Task> ts = tasksByRoom.getOrDefault(r.getId(), List.of());
        int obtained = 0, possible = 0, done = 0, total = 0;
        boolean anyAttempt = false;
        for (Task t : ts) {
            if (!published.contains(t.getExerciseVersionId())) continue;
            total++;
            possible += t.getPoints();
            Attempt a = byTask.get(t.getId());
            if (a == null) continue;
            anyAttempt = true;
            if (a.getStatus() == AttemptStatus.PASSED) obtained += a.getScore();
            if (a.getStatus() != AttemptStatus.OPEN) done++;
        }

        boolean prereqOk = true;
        for (UUID pre : r.getPrerequisites()) {
            Room p = byId.get(pre);
            if (p == null || visiting.contains(pre)) { prereqOk = false; continue; }
            if (compute(p, byId, tasksByRoom, byTask, published, memo, visiting).state() != RoomState.COMPLETED) {
                prereqOk = false;
            }
        }
        RoomState state;
        if (r.getStatus() != ContentStatus.PUBLISHED || !prereqOk || total == 0) state = RoomState.LOCKED;
        else if (done == total) state = RoomState.COMPLETED;
        else if (anyAttempt) state = RoomState.IN_PROGRESS;
        else state = RoomState.AVAILABLE;
        if (state == RoomState.LOCKED && anyAttempt && prereqOk && r.getStatus() == ContentStatus.PUBLISHED) {
            state = RoomState.IN_PROGRESS;
        }
        RoomProgress p = new RoomProgress(r, state, obtained, possible, done, total, ts);
        memo.put(r.getId(), p);
        visiting.remove(r.getId());
        return p;
    }
}
