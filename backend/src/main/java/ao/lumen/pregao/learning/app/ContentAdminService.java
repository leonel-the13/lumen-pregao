package ao.lumen.pregao.learning.app;

import ao.lumen.pregao.authoring.app.ExerciseCatalog;
import ao.lumen.pregao.common.audit.AuditService;
import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.common.text.MarkdownSanitizer;
import ao.lumen.pregao.learning.api.ContentDtos.ModuleAdmin;
import ao.lumen.pregao.learning.api.ContentDtos.ModuleInput;
import ao.lumen.pregao.learning.api.ContentDtos.RoomAdmin;
import ao.lumen.pregao.learning.api.ContentDtos.RoomInput;
import ao.lumen.pregao.learning.api.ContentDtos.TaskAdmin;
import ao.lumen.pregao.learning.api.ContentDtos.TaskInput;
import ao.lumen.pregao.learning.api.ContentDtos.TrackAdmin;
import ao.lumen.pregao.learning.api.ContentDtos.TrackInput;
import ao.lumen.pregao.learning.domain.ContentStatus;
import ao.lumen.pregao.learning.domain.Room;
import ao.lumen.pregao.learning.domain.Task;
import ao.lumen.pregao.learning.domain.Track;
import ao.lumen.pregao.learning.domain.TrackModule;
import ao.lumen.pregao.learning.infra.AttemptRepository;
import ao.lumen.pregao.learning.infra.ModuleRepository;
import ao.lumen.pregao.learning.infra.RoomRepository;
import ao.lumen.pregao.learning.infra.TaskRepository;
import ao.lumen.pregao.learning.infra.TrackRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestão de trilhas, módulos, salas e tarefas (administração). Cada alteração fica em auditoria. */
@Service
public class ContentAdminService {
    private final TrackRepository tracks;
    private final ModuleRepository modules;
    private final RoomRepository rooms;
    private final TaskRepository tasks;
    private final AttemptRepository attempts;
    private final ExerciseCatalog catalog;
    private final AuditService audit;
    private final EntityManager em;
    private final ObjectMapper json;

    public ContentAdminService(TrackRepository tracks, ModuleRepository modules, RoomRepository rooms, TaskRepository tasks,
                               AttemptRepository attempts, ExerciseCatalog catalog, AuditService audit, EntityManager em,
                               ObjectMapper json) {
        this.tracks = tracks;
        this.modules = modules;
        this.rooms = rooms;
        this.tasks = tasks;
        this.attempts = attempts;
        this.catalog = catalog;
        this.audit = audit;
        this.em = em;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public List<TrackAdmin> tree() {
        List<Track> all = tracks.findAll().stream().sorted(Comparator.comparingInt(Track::getPosition)).toList();
        List<Room> allRooms = rooms.findAll();
        List<Task> allTasks = allRooms.isEmpty() ? List.of() : tasks.findByRoomIds(allRooms.stream().map(Room::getId).toList());
        var defs = catalog.definitions(allTasks.stream().map(Task::getExerciseVersionId).distinct().toList());
        var statuses = catalog.statuses(defs.keySet());
        Map<UUID, Long> attemptCounts = allTasks.isEmpty() ? Map.of() : attempts.countByTasks(allTasks.stream().map(Task::getId).toList()).stream()
                .collect(Collectors.toMap(r -> (UUID) r[0], r -> ((Number) r[1]).longValue()));
        Map<UUID, List<Task>> tasksByRoom = allTasks.stream().collect(Collectors.groupingBy(t -> t.getRoom().getId()));
        Map<UUID, List<Room>> roomsByModule = allRooms.stream().collect(Collectors.groupingBy(r -> r.getModule().getId()));
        List<TrackAdmin> out = new ArrayList<>();
        for (Track t : all) {
            List<ModuleAdmin> ms = new ArrayList<>();
            for (TrackModule m : modules.findByTrackIdOrderByPositionAsc(t.getId())) {
                List<RoomAdmin> rs = roomsByModule.getOrDefault(m.getId(), List.of()).stream()
                        .sorted(Comparator.comparingInt(Room::getPosition))
                        .map(r -> new RoomAdmin(r.getId(), r.getPosition(), r.getTitle(), r.getStatus().name(), r.getDifficulty(),
                                r.getEstMinutes(), r.getPrerequisites(), tasksByRoom.getOrDefault(r.getId(), List.of()).stream()
                                .sorted(Comparator.comparingInt(Task::getPosition))
                                .map(k -> {
                                    var d = defs.get(k.getExerciseVersionId());
                                    var st = statuses.get(k.getExerciseVersionId());
                                    return new TaskAdmin(k.getId(), k.getPosition(), k.getTitle(), k.getPoints(), k.getExerciseVersionId(),
                                            d == null ? null : d.code(), d == null ? null : d.title(), d == null ? null : d.type(),
                                            st == null ? null : st.name(), attemptCounts.getOrDefault(k.getId(), 0L));
                                }).toList()))
                        .toList();
                ms.add(new ModuleAdmin(m.getId(), m.getPosition(), m.getTitle(), m.getSummary(), rs));
            }
            out.add(new TrackAdmin(t.getId(), t.getSlug(), t.getTitle(), t.getStatus().name(), t.getPosition(), ms));
        }
        return out;
    }

    @Transactional
    public UUID createTrack(UUID actor, TrackInput in) {
        if (tracks.findBySlug(in.slug()).isPresent()) throw DomainException.conflict("SLUG_TAKEN", "Já existe uma trilha com este identificador.");
        Track t = new Track(in.slug(), in.title(), in.summary(), MarkdownSanitizer.clean(in.description() == null ? "" : in.description()),
                in.difficulty(), in.topic(), new ArrayList<>(in.courses() == null ? List.of() : in.courses()),
                new ArrayList<>(in.competencies() == null ? List.of() : in.competencies()),
                in.completionRule() == null ? json.createObjectNode() : json.createObjectNode().put("text", in.completionRule()),
                in.estHours(), (int) tracks.count(), status(in.status(), ContentStatus.DRAFT));
        em.persist(t);
        audit.record(actor, "TRACK_CREATED", "Track", t.getId(), null, Map.of("slug", in.slug()));
        return t.getId();
    }

    @Transactional
    public void setTrackStatus(UUID actor, UUID id, String status) {
        Track t = tracks.findById(id).orElseThrow(() -> DomainException.notFound("TRACK_NOT_FOUND", "A trilha não existe."));
        String before = t.getStatus().name();
        t.setStatus(ContentStatus.valueOf(status));
        audit.record(actor, "TRACK_STATUS", "Track", id, Map.of("status", before), Map.of("status", status));
    }

    @Transactional
    public UUID addModule(UUID actor, UUID trackId, ModuleInput in) {
        Track t = tracks.findById(trackId).orElseThrow(() -> DomainException.notFound("TRACK_NOT_FOUND", "A trilha não existe."));
        int pos = modules.findByTrackIdOrderByPositionAsc(trackId).size() + 1;
        TrackModule m = new TrackModule(t, pos, in.title(), in.summary());
        em.persist(m);
        audit.record(actor, "MODULE_CREATED", "Module", m.getId(), null, Map.of("track", t.getSlug(), "position", pos));
        return m.getId();
    }

    @Transactional
    public UUID addRoom(UUID actor, UUID moduleId, RoomInput in) {
        TrackModule m = modules.findById(moduleId).orElseThrow(() -> DomainException.notFound("MODULE_NOT_FOUND", "O módulo não existe."));
        int pos = (int) rooms.findByTrack(m.getTrack().getId()).stream().filter(r -> r.getModule().getId().equals(moduleId)).count() + 1;
        Room r = new Room(m, pos, in.title(), in.description(), new ArrayList<>(in.objectives() == null ? List.of() : in.objectives()),
                in.difficulty(), in.estMinutes(), MarkdownSanitizer.clean(in.context() == null ? "" : in.context()),
                json.createArrayNode(), new ArrayList<>(in.prerequisites() == null ? List.of() : in.prerequisites()),
                status(in.status(), ContentStatus.DRAFT));
        em.persist(r);
        audit.record(actor, "ROOM_CREATED", "Room", r.getId(), null, Map.of("module", moduleId, "position", pos));
        return r.getId();
    }

    @Transactional
    public void setRoomStatus(UUID actor, UUID id, String status) {
        Room r = rooms.findById(id).orElseThrow(() -> DomainException.notFound("ROOM_NOT_FOUND", "A sala não existe."));
        String before = r.getStatus().name();
        r.setStatus(ContentStatus.valueOf(status));
        audit.record(actor, "ROOM_STATUS", "Room", id, Map.of("status", before), Map.of("status", status));
    }

    @Transactional
    public UUID addTask(UUID actor, UUID roomId, TaskInput in) {
        Room r = rooms.findById(roomId).orElseThrow(() -> DomainException.notFound("ROOM_NOT_FOUND", "A sala não existe."));
        catalog.definition(in.exerciseVersionId());
        int pos = tasks.findByRoomIds(List.of(roomId)).size() + 1;
        Task t = new Task(r, pos, in.title(), in.exerciseVersionId(), in.points());
        em.persist(t);
        audit.record(actor, "TASK_CREATED", "Task", t.getId(), null, Map.of("room", roomId, "exerciseVersion", in.exerciseVersionId()));
        return t.getId();
    }

    /** Só remove tarefas sem tentativas (o histórico dos estudantes nunca se perde). */
    @Transactional
    public void removeTask(UUID actor, UUID taskId) {
        Task t = tasks.findById(taskId).orElseThrow(() -> DomainException.notFound("TASK_NOT_FOUND", "A tarefa não existe."));
        long n = attempts.countByTasks(List.of(taskId)).stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        if (n > 0) throw DomainException.conflict("TASK_HAS_ATTEMPTS", "Esta tarefa já tem tentativas de estudantes e não pode ser removida.");
        tasks.delete(t);
        audit.record(actor, "TASK_REMOVED", "Task", taskId, Map.of("title", t.getTitle()), null);
    }

    private static ContentStatus status(String s, ContentStatus fallback) {
        return s == null ? fallback : ContentStatus.valueOf(s);
    }
}
