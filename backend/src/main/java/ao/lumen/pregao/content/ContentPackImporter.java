package ao.lumen.pregao.content;

import ao.lumen.pregao.authoring.app.ExerciseCatalog;
import ao.lumen.pregao.authoring.domain.EditorialStatus;
import ao.lumen.pregao.authoring.domain.Exercise;
import ao.lumen.pregao.authoring.domain.ExerciseReference;
import ao.lumen.pregao.authoring.domain.ExerciseVersion;
import ao.lumen.pregao.authoring.domain.LegalSource;
import ao.lumen.pregao.authoring.domain.ReviewWorkflow;
import ao.lumen.pregao.authoring.infra.ExerciseReferenceRepository;
import ao.lumen.pregao.authoring.infra.ExerciseRepository;
import ao.lumen.pregao.authoring.infra.ExerciseVersionRepository;
import ao.lumen.pregao.authoring.infra.LegalSourceRepository;
import ao.lumen.pregao.authoring.infra.ReviewWorkflowRepository;
import ao.lumen.pregao.common.audit.AuditService;
import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.exam.domain.Exam;
import ao.lumen.pregao.exam.infra.ExamRepository;
import ao.lumen.pregao.grading.app.ExerciseDefinition;
import ao.lumen.pregao.grading.app.GradingService;
import ao.lumen.pregao.grading.app.ParamGenerator;
import ao.lumen.pregao.grading.app.PgcAccountService;
import ao.lumen.pregao.grading.app.SolutionPreviewer;
import ao.lumen.pregao.identity.domain.User;
import ao.lumen.pregao.identity.domain.UserStatus;
import ao.lumen.pregao.identity.infra.RoleRepository;
import ao.lumen.pregao.identity.infra.UserRepository;
import ao.lumen.pregao.learning.domain.ContentStatus;
import ao.lumen.pregao.learning.domain.Room;
import ao.lumen.pregao.learning.domain.Task;
import ao.lumen.pregao.learning.domain.Track;
import ao.lumen.pregao.learning.domain.TrackModule;
import ao.lumen.pregao.learning.infra.ModuleRepository;
import ao.lumen.pregao.learning.infra.RoomRepository;
import ao.lumen.pregao.learning.infra.TaskRepository;
import ao.lumen.pregao.learning.infra.TrackRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Importa o pacote de conteúdo (content/pack-v1.json): exercícios, trilhas, módulos, salas, tarefas e exames.
 * <ul>
 *   <li>Idempotente: exercícios pela chave estável, trilhas pelo slug, salas pelo título no módulo, exames pelo slug.</li>
 *   <li>Cada exercício é verificado com a correcção automática antes de ser importado.</li>
 *   <li>Os exercícios entram EM REVISÃO, com autoria da «Equipa editorial» (conta sem acesso): um revisor ou
 *       administrador aprova e publica (nada é publicado sem revisão; o autor nunca aprova o próprio exercício).</li>
 * </ul>
 * Excepção documentada à regra de módulos: o importador escreve nos repositórios de authoring, learning e exam.
 */
@Service
public class ContentPackImporter {
    public static final String EDITORIAL_EMAIL = "equipa.editorial@lumen-pregao.invalid";
    private static final String PACK = "content/pack-v1.json";

    private final ExerciseRepository exercises;
    private final ExerciseVersionRepository versions;
    private final ReviewWorkflowRepository workflows;
    private final ExerciseReferenceRepository references;
    private final LegalSourceRepository sources;
    private final TrackRepository tracks;
    private final ModuleRepository modules;
    private final RoomRepository rooms;
    private final TaskRepository tasks;
    private final ExamRepository exams;
    private final UserRepository users;
    private final RoleRepository roles;
    private final ExerciseCatalog catalog;
    private final ParamGenerator generator;
    private final GradingService grading;
    private final SolutionPreviewer previewer;
    private final AuditService audit;
    private final EntityManager em;
    private final ObjectMapper json;
    private final Clock clock;

    public ContentPackImporter(ExerciseRepository exercises, ExerciseVersionRepository versions, ReviewWorkflowRepository workflows,
                               ExerciseReferenceRepository references, LegalSourceRepository sources, TrackRepository tracks,
                               ModuleRepository modules, RoomRepository rooms, TaskRepository tasks, ExamRepository exams,
                               UserRepository users, RoleRepository roles, ExerciseCatalog catalog, ParamGenerator generator,
                               GradingService grading, SolutionPreviewer previewer, AuditService audit, EntityManager em,
                               ObjectMapper json, Clock clock) {
        this.exercises = exercises;
        this.versions = versions;
        this.workflows = workflows;
        this.references = references;
        this.sources = sources;
        this.tracks = tracks;
        this.modules = modules;
        this.rooms = rooms;
        this.tasks = tasks;
        this.exams = exams;
        this.users = users;
        this.roles = roles;
        this.catalog = catalog;
        this.generator = generator;
        this.grading = grading;
        this.previewer = previewer;
        this.audit = audit;
        this.em = em;
        this.json = json;
        this.clock = clock;
    }

    public record ImportResult(int exercisesCreated, int exercisesExisting, int tracksCreated, int modulesCreated,
                               int roomsCreated, int tasksCreated, int examsCreated) {}

    public JsonNode pack() {
        try (InputStream in = new ClassPathResource(PACK).getInputStream()) {
            return json.readTree(in);
        } catch (Exception e) {
            throw new IllegalStateException("Pacote de conteúdo em falta: " + PACK, e);
        }
    }

    @Transactional
    public ImportResult importPack(UUID actor) {
        JsonNode pack = pack();
        Map<String, UUID> sourceIds = new HashMap<>();
        for (LegalSource s : sources.findAll()) {
            if (s.getTitle().startsWith("Plano Geral")) sourceIds.put("PGC", s.getId());
            if (s.getTitle().startsWith("Código do IVA")) sourceIds.put("IVA", s.getId());
        }
        User editorial = editorialUser();
        Map<String, UUID> versionByKey = new HashMap<>();
        int created = 0, existing = 0;
        for (JsonNode e : pack.path("exercises")) {
            String key = e.path("key").asText();
            var found = exercises.findByExternalKey(key);
            if (found.isPresent()) {
                versionByKey.put(key, versions.findByExerciseIdOrderByNumberDesc(found.get().getId()).getFirst().getId());
                existing++;
                continue;
            }
            versionByKey.put(key, createExercise(e, editorial, sourceIds));
            created++;
        }

        int tc = 0, mc = 0, rc = 0, kc = 0;
        Map<String, UUID> roomByKey = new HashMap<>();
        Map<String, List<String>> prereqs = new HashMap<>();
        for (JsonNode t : pack.path("tracks")) {
            Track track = tracks.findBySlug(t.path("slug").asText()).orElse(null);
            if (track == null) {
                ObjectNode rule = json.createObjectNode();
                if (t.hasNonNull("completionRule")) rule.put("text", t.path("completionRule").asText());
                track = new Track(t.path("slug").asText(), t.path("title").asText(), t.path("summary").asText(),
                        t.path("description").asText(), t.path("difficulty").asInt(), t.path("topic").asText(),
                        strings(t.path("courses")), strings(t.path("competencies")), rule,
                        t.hasNonNull("estHours") ? t.path("estHours").asInt() : null, t.path("position").asInt(),
                        ContentStatus.valueOf(t.path("status").asText("DRAFT")));
                em.persist(track);
                tc++;
            }
            List<TrackModule> existingModules = modules.findByTrackIdOrderByPositionAsc(track.getId());
            int pos = 0;
            for (JsonNode m : t.path("modules")) {
                pos++;
                final int position = pos;
                TrackModule module = existingModules.stream().filter(x -> x.getPosition() == position).findFirst().orElse(null);
                if (module == null) {
                    module = new TrackModule(track, position, m.path("title").asText(), m.path("summary").asText(null));
                    em.persist(module);
                    mc++;
                }
                final TrackModule mod = module;
                List<Room> moduleRooms = rooms.findByTrack(track.getId()).stream()
                        .filter(r -> r.getModule().getId().equals(mod.getId())).toList();
                int rpos = moduleRooms.size();
                for (JsonNode r : m.path("rooms")) {
                    Room room = moduleRooms.stream().filter(x -> x.getTitle().equals(r.path("title").asText())).findFirst().orElse(null);
                    if (room == null) {
                        room = new Room(mod, ++rpos, r.path("title").asText(), r.path("description").asText(), strings(r.path("objectives")),
                                r.path("difficulty").asInt(), r.path("estMinutes").asInt(), r.path("context").asText(""),
                                json.createArrayNode(),
                                new ArrayList<>(), ContentStatus.PUBLISHED);
                        em.persist(room);
                        rc++;
                        int kpos = 0;
                        for (JsonNode k : r.path("tasks")) {
                            UUID vid = versionByKey.get(k.path("exercise").asText());
                            if (vid == null) throw new IllegalStateException("Exercício desconhecido no pacote: " + k.path("exercise").asText());
                            em.persist(new Task(room, ++kpos, k.path("title").asText(), vid, k.path("points").asInt()));
                            kc++;
                        }
                        prereqs.put(r.path("key").asText(), strings(r.path("prerequisites")));
                    }
                    roomByKey.put(r.path("key").asText(), room.getId());
                }
            }
        }
        em.flush();
        prereqs.forEach((key, pre) -> {
            if (pre.isEmpty()) return;
            Room room = rooms.findById(roomByKey.get(key)).orElseThrow();
            room.setPrerequisites(new ArrayList<>(pre.stream().map(roomByKey::get).toList()));
        });

        int ec = 0;
        for (JsonNode x : pack.path("exams")) {
            String slug = x.path("slug").asText();
            if (exams.findAll().stream().anyMatch(e -> e.getSlug().equals(slug))) continue;
            UUID moduleId = tracks.findBySlug(x.path("track").asText())
                    .flatMap(t -> modules.findByTrackIdOrderByPositionAsc(t.getId()).stream()
                            .filter(m -> m.getPosition() == x.path("module").asInt()).findFirst())
                    .map(TrackModule::getId).orElse(null);
            ObjectNode rule = json.createObjectNode().put("topic", x.path("topic").asText()).put("count", x.path("count").asInt());
            rule.putArray("types").add("A").add("B");
            exams.save(new Exam(slug, x.path("title").asText(), moduleId, x.path("durationMin").asInt(), rule,
                    new BigDecimal(x.path("passMark").asText("10")), "PUBLISHED"));
            ec++;
        }
        ImportResult result = new ImportResult(created, existing, tc, mc, rc, kc, ec);
        audit.record(actor, "CONTENT_PACK_IMPORTED", "ContentPack", "pack-v1", null, result);
        return result;
    }

    private UUID createExercise(JsonNode e, User editorial, Map<String, UUID> sourceIds) {
        ObjectNode paramSpec = (ObjectNode) e.path("paramSpec").deepCopy();
        for (JsonNode p : paramSpec.path("params")) {
            JsonNode rule = p.path("rule");
            if (rule.has("legalSource")) {
                UUID id = sourceIds.get(rule.path("legalSource").asText());
                ((ObjectNode) rule).remove("legalSource");
                if (id != null) ((ObjectNode) rule).put("legalSourceId", id.toString());
            }
        }
        String key = e.path("key").asText();
        ExerciseDefinition probe = new ExerciseDefinition(UUID.randomUUID(), key, e.path("type").asText(), e.path("title").asText(),
                e.path("statement").asText(), e.path("presentation"), paramSpec, e.path("solution"), e.path("tolerance"),
                e.path("hints"), e.path("feedbackPolicy").asText("DETAILED"), e.path("difficulty").asInt(), e.path("points").asInt(),
                e.path("maxAttempts").asInt(3), PgcAccountService.DEFAULT_VERSION, e.path("topic").asText());
        selfCheck(probe);

        Exercise ex = new Exercise("EX-%04d".formatted(exercises.nextCode()), editorial.getId(), e.path("topic").asText());
        ex.setExternalKey(key);
        exercises.save(ex);
        ExerciseVersion v = new ExerciseVersion(ex, 1);
        v.define(probe.type(), probe.title(), probe.statement(), probe.presentation(), paramSpec, probe.solutionSpec(),
                probe.tolerance(), probe.hints(), probe.feedbackPolicy(), probe.difficulty(), probe.points(), probe.maxAttempts(),
                probe.pgcVersion(), null, "Importado do pacote de conteúdo v1");
        versions.save(v);
        ReviewWorkflow w = new ReviewWorkflow(v);
        w.moveTo(EditorialStatus.IN_REVIEW, editorial.getId(), null, clock.instant());
        workflows.save(w);
        for (JsonNode ref : e.path("refs")) {
            UUID sid = sourceIds.get(ref.asText());
            if (sid != null) references.save(new ExerciseReference(v, sources.findById(sid).orElseThrow(),
                    "a indicar pelo revisor", LocalDate.now(clock)));
        }
        return v.getId();
    }

    /** A solução esperada tem de passar na correcção para várias sementes; senão o pacote está errado. */
    void selfCheck(ExerciseDefinition def) {
        var pools = catalog.entityPools();
        for (long seed : new long[] {1, 42, 2026, 7_777, 123_456}) {
            JsonNode params = generator.generate(def.paramSpec(), seed, pools);
            var r = grading.grade(def, params, previewer.expectedAnswer(def, params));
            if (!r.correct()) {
                throw DomainException.rule("PACK_INVALID", "O exercício «" + def.code() + "» não passa na auto-verificação: " + r.summary());
            }
        }
    }

    private User editorialUser() {
        return users.findByEmail(EDITORIAL_EMAIL).orElseGet(() -> {
            User u = new User(EDITORIAL_EMAIL, "!", "Equipa editorial Lúmen", null, null, "equipa-editorial", clock.instant());
            u.setStatus(UserStatus.SUSPENDED); // conta técnica: nunca inicia sessão
            u.getRoles().addAll(roles.findByCodeIn(List.of("AUTHOR")));
            return users.save(u);
        });
    }

    private static List<String> strings(JsonNode arr) {
        List<String> out = new ArrayList<>();
        arr.forEach(n -> out.add(n.asText()));
        return out;
    }
}
