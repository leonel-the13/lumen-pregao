package ao.lumen.pregao.learning.app;

import ao.lumen.pregao.authoring.app.ExerciseCatalog;
import ao.lumen.pregao.authoring.app.ExerciseCatalog.ReferenceView;
import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.learning.api.LearningDtos.Breadcrumb;
import ao.lumen.pregao.learning.api.LearningDtos.ModuleView;
import ao.lumen.pregao.learning.api.LearningDtos.RoomCard;
import ao.lumen.pregao.learning.api.LearningDtos.RoomDetail;
import ao.lumen.pregao.learning.api.LearningDtos.TaskView;
import ao.lumen.pregao.learning.api.LearningDtos.TrackCard;
import ao.lumen.pregao.learning.api.LearningDtos.TrackDetail;
import ao.lumen.pregao.learning.app.ProgressService.RoomProgress;
import ao.lumen.pregao.learning.app.ProgressService.RoomState;
import ao.lumen.pregao.learning.app.ProgressService.Snapshot;
import ao.lumen.pregao.learning.domain.ContentStatus;
import ao.lumen.pregao.learning.domain.Room;
import ao.lumen.pregao.learning.domain.Task;
import ao.lumen.pregao.learning.domain.Track;
import ao.lumen.pregao.learning.domain.TrackModule;
import ao.lumen.pregao.learning.infra.RoomRepository;
import ao.lumen.pregao.learning.infra.TrackRepository;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final TrackRepository tracks;
    private final RoomRepository rooms;
    private final ProgressService progress;
    private final ExerciseCatalog catalog;

    public CatalogService(TrackRepository tracks, RoomRepository rooms, ProgressService progress,
                          ExerciseCatalog catalog) {
        this.tracks = tracks;
        this.rooms = rooms;
        this.progress = progress;
        this.catalog = catalog;
    }

    public List<TrackCard> tracks(UUID user, String course, String topic, Integer difficulty, String q) {
        Snapshot snap = progress.snapshot(user);
        return tracks.findVisible().stream()
                .filter(t -> course == null || course.isBlank() || t.getCourses().contains(course))
                .filter(t -> topic == null || topic.isBlank() || t.getTopic().equalsIgnoreCase(topic))
                .filter(t -> difficulty == null || t.getDifficulty() == difficulty)
                .filter(t -> matches(q, t.getTitle(), t.getSummary()))
                .map(t -> card(t, snap)).toList();
    }

    public TrackDetail track(UUID user, String slug) {
        Track t = tracks.findBySlug(slug).filter(x -> x.getStatus() != ContentStatus.DRAFT)
                .orElseThrow(() -> DomainException.notFound("TRACK_NOT_FOUND", "A trilha não existe."));
        Snapshot snap = progress.snapshot(user);
        List<ModuleView> modules = new ArrayList<>();
        List<UUID> versionIds = new ArrayList<>();
        for (TrackModule m : t.getModules()) {
            List<RoomCard> cards = new ArrayList<>();
            int ob = 0, pos = 0;
            boolean allDone = !m.getRooms().isEmpty(), anyActive = false, anyAvailable = false;
            for (Room r : m.getRooms()) {
                RoomProgress p = snap.rooms().get(r.getId());
                if (p == null) continue;
                cards.add(roomCard(p, snap));
                ob += p.obtained();
                pos += p.possible();
                allDone &= p.state() == RoomState.COMPLETED;
                anyActive |= p.state() == RoomState.IN_PROGRESS;
                anyAvailable |= p.state() == RoomState.AVAILABLE;
                p.tasks().forEach(x -> versionIds.add(x.getExerciseVersionId()));
            }
            RoomState ms = allDone ? RoomState.COMPLETED
                    : anyActive || (ob > 0) ? RoomState.IN_PROGRESS
                    : anyAvailable ? RoomState.AVAILABLE : RoomState.LOCKED;
            modules.add(new ModuleView(m.getId(), m.getPosition(), m.getTitle(), m.getSummary(), m.getRooms().size(),
                    pos == 0 ? 0 : Math.round(ob * 100f / pos), ms, cards));
        }
        Map<String, ReferenceView> refs = new LinkedHashMap<>();
        catalog.references(versionIds).values().forEach(l -> l.forEach(r -> refs.putIfAbsent(r.title(), r)));
        return new TrackDetail(t.getSlug(), t.getTitle(), t.getSummary(), t.getDescription(), t.getDifficulty(),
                t.getTopic(), t.getCompetencies(), t.getCompletionRule() == null ? null
                : t.getCompletionRule().path("text").asText(null), t.getStatus().name(), snap.trackPercent(t.getId()),
                t.getEstHours(), nextRoom(t.getId(), snap), modules, List.copyOf(refs.values()));
    }

    public List<RoomCard> rooms(UUID user, String q, String topic, Integer difficulty) {
        Snapshot snap = progress.snapshot(user);
        return rooms.findAllVisible().stream()
                .filter(r -> topic == null || topic.isBlank() || r.getModule().getTrack().getTopic().equalsIgnoreCase(topic))
                .filter(r -> difficulty == null || r.getDifficulty() == difficulty)
                .filter(r -> matches(q, r.getTitle(), r.getDescription()))
                .map(r -> snap.rooms().get(r.getId())).filter(Objects::nonNull)
                .map(p -> roomCard(p, snap)).toList();
    }

    public RoomDetail room(UUID user, UUID roomId) {
        Snapshot snap = progress.snapshot(user);
        RoomProgress p = snap.rooms().get(roomId);
        if (p == null) throw DomainException.notFound("ROOM_NOT_FOUND", "A sala não existe.");
        Room r = p.room();
        var defs = catalog.definitions(p.tasks().stream().map(Task::getExerciseVersionId).toList());
        List<TaskView> taskViews = p.tasks().stream().map(t -> {
            var a = snap.attemptByTask().get(t.getId());
            var d = defs.get(t.getExerciseVersionId());
            return new TaskView(t.getId(), t.getPosition(), t.getTitle(), t.getPoints(), snap.taskState(t),
                    a == null ? 0 : a.getScore(), a == null ? 0 : a.getAttemptsUsed(),
                    d == null ? 0 : d.maxAttempts(), d == null ? null : d.type());
        }).toList();
        Map<String, ReferenceView> refs = new LinkedHashMap<>();
        catalog.references(defs.keySet()).values().forEach(l -> l.forEach(x -> refs.putIfAbsent(x.title() + x.identifier(), x)));
        TrackModule m = r.getModule();
        boolean locked = p.state() == RoomState.LOCKED;
        return new RoomDetail(r.getId(), r.getTitle(), r.getDescription(), r.getObjectives(), r.getDifficulty(),
                r.getEstMinutes(), locked ? null : r.getContext(), locked ? null : r.getMaterials(), p.state(),
                p.obtained(), p.possible(),
                new Breadcrumb(m.getTrack().getSlug(), m.getTrack().getTitle(), m.getPosition(), m.getTitle(), r.getPosition()),
                taskViews, List.copyOf(refs.values()), locked ? lockedReason(r, snap) : null);
    }

    /** Salas com tarefas cujos exercícios têm um destes temas (relatório de exame, recomendações). */
    public List<RoomCard> roomsForTopics(UUID user, java.util.Set<String> topics, int limit) {
        if (topics.isEmpty()) return List.of();
        Snapshot snap = progress.snapshot(user);
        var allTasks = snap.rooms().values().stream().flatMap(p -> p.tasks().stream()).toList();
        var defs = catalog.definitions(allTasks.stream().map(Task::getExerciseVersionId).toList());
        return snap.rooms().values().stream()
                .filter(p -> p.state() != RoomState.LOCKED)
                .filter(p -> p.tasks().stream().anyMatch(t -> {
                    var d = defs.get(t.getExerciseVersionId());
                    return d != null && topics.contains(d.topic());
                }))
                .sorted(Comparator.comparingInt((RoomProgress p) -> p.state() == RoomState.COMPLETED ? 1 : 0)
                        .thenComparing(p -> p.room().getModule().getTrack().getPosition())
                        .thenComparing(p -> p.room().getModule().getPosition())
                        .thenComparing(p -> p.room().getPosition()))
                .limit(limit).map(p -> roomCard(p, snap)).toList();
    }

    UUID nextRoom(UUID trackId, Snapshot snap) {
        return snap.rooms().values().stream()
                .filter(p -> p.room().getModule().getTrack().getId().equals(trackId))
                .filter(p -> p.state() == RoomState.IN_PROGRESS || p.state() == RoomState.AVAILABLE)
                .min(Comparator.comparingInt((RoomProgress p) -> p.state() == RoomState.IN_PROGRESS ? 0 : 1)
                        .thenComparingInt(p -> p.room().getModule().getPosition())
                        .thenComparingInt(p -> p.room().getPosition()))
                .map(p -> p.room().getId()).orElse(null);
    }

    TrackCard card(Track t, Snapshot snap) {
        int rc = t.getModules().stream().mapToInt(m -> m.getRooms().size()).sum();
        return new TrackCard(t.getSlug(), t.getTitle(), t.getSummary(), t.getDifficulty(), t.getTopic(), t.getCourses(),
                t.getStatus().name(), t.getModules().size(), rc, t.getEstHours(), snap.trackPercent(t.getId()),
                nextRoom(t.getId(), snap));
    }

    RoomCard roomCard(RoomProgress p, Snapshot snap) {
        Room r = p.room();
        TrackModule m = r.getModule();
        return new RoomCard(r.getId(), r.getTitle(), r.getDescription(), m.getTrack().getSlug(), m.getTrack().getTitle(),
                m.getPosition(), m.getTitle(), r.getPosition(), r.getDifficulty(), r.getEstMinutes(), p.state(),
                p.obtained(), p.possible(), p.state() == RoomState.LOCKED ? lockedReason(r, snap) : null);
    }

    private String lockedReason(Room r, Snapshot snap) {
        if (r.getStatus() == ContentStatus.COMING_SOON) return "Fase posterior";
        for (UUID pre : r.getPrerequisites()) {
            RoomProgress p = snap.rooms().get(pre);
            if (p != null && p.state() != RoomState.COMPLETED) return "Disponível ao concluir «" + p.room().getTitle() + "».";
        }
        return "Requer sala anterior";
    }

    private static boolean matches(String q, String... fields) {
        if (q == null || q.isBlank()) return true;
        String n = norm(q);
        for (String f : fields) if (f != null && norm(f).contains(n)) return true;
        return false;
    }

    private static String norm(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
