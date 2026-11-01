package ao.lumen.pregao.exam.app;

import ao.lumen.pregao.authoring.app.ExerciseCatalog;
import ao.lumen.pregao.common.audit.AuditService;
import ao.lumen.pregao.common.config.AppProperties;
import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.common.text.Formats;
import ao.lumen.pregao.exam.api.ExamDtos.AnswerSaved;
import ao.lumen.pregao.exam.api.ExamDtos.ExamCard;
import ao.lumen.pregao.exam.api.ExamDtos.QuestionView;
import ao.lumen.pregao.exam.api.ExamDtos.Report;
import ao.lumen.pregao.exam.api.ExamDtos.SessionStarted;
import ao.lumen.pregao.exam.api.ExamDtos.SessionView;
import ao.lumen.pregao.exam.api.ExamDtos.TopicResult;
import ao.lumen.pregao.exam.domain.Exam;
import ao.lumen.pregao.exam.domain.ExamSession;
import ao.lumen.pregao.exam.domain.ExamSessionStatus;
import ao.lumen.pregao.exam.infra.ExamRepository;
import ao.lumen.pregao.exam.infra.ExamSessionRepository;
import ao.lumen.pregao.grading.app.AnswerFormatException;
import ao.lumen.pregao.grading.app.ExerciseDefinition;
import ao.lumen.pregao.grading.app.GradingService;
import ao.lumen.pregao.grading.app.ParamGenerator;
import ao.lumen.pregao.grading.app.StatementRenderer;
import ao.lumen.pregao.learning.app.CatalogService;
import ao.lumen.pregao.ranking.app.BadgeService;
import ao.lumen.pregao.ranking.app.PointService;
import ao.lumen.pregao.ranking.domain.PointSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Exames em modo concentração (spec §4.6). O relógio do servidor decide início, fim e submissão automática. */
@Service
public class ExamService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final ExamRepository exams;
    private final ExamSessionRepository sessions;
    private final ExerciseCatalog catalog;
    private final ParamGenerator generator;
    private final StatementRenderer renderer;
    private final GradingService grading;
    private final PointService points;
    private final BadgeService badges;
    private final CatalogService rooms;
    private final AuditService audit;
    private final AppProperties props;
    private final ObjectMapper json;
    private final Clock clock;

    public ExamService(ExamRepository exams, ExamSessionRepository sessions, ExerciseCatalog catalog,
                       ParamGenerator generator, StatementRenderer renderer, GradingService grading,
                       PointService points, BadgeService badges, CatalogService rooms, AuditService audit,
                       AppProperties props, ObjectMapper json, Clock clock) {
        this.exams = exams;
        this.sessions = sessions;
        this.catalog = catalog;
        this.generator = generator;
        this.renderer = renderer;
        this.grading = grading;
        this.points = points;
        this.badges = badges;
        this.rooms = rooms;
        this.audit = audit;
        this.props = props;
        this.json = json;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ExamCard> list(UUID user) {
        var mine = sessions.findByUserIdOrderByStartedAtDesc(user);
        return exams.findByStatusOrderByTitleAsc("PUBLISHED").stream().map(e -> {
            var forExam = mine.stream().filter(s -> s.getExamId().equals(e.getId())).toList();
            UUID open = forExam.stream().filter(s -> s.getStatus() == ExamSessionStatus.IN_PROGRESS)
                    .map(ExamSession::getId).findFirst().orElse(null);
            BigDecimal best = forExam.stream().map(ExamSession::getGrade).filter(g -> g != null)
                    .max(BigDecimal::compareTo).orElse(null);
            return new ExamCard(e.getId(), e.getSlug(), e.getTitle(), e.getDurationMin(),
                    e.getQuestionPoolRule().path("count").asInt(), e.getPassMark(), open, best,
                    forExam.isEmpty() ? null : forExam.getFirst().getStatus().name());
        }).toList();
    }

    public record ExamResult(String title, BigDecimal grade, Instant at) {}

    @Transactional(readOnly = true)
    public List<ExamResult> history(UUID user) {
        var titles = exams.findAll().stream().collect(Collectors.toMap(Exam::getId, Exam::getTitle));
        return sessions.findByUserIdOrderByStartedAtDesc(user).stream()
                .filter(s -> s.getStatus() == ExamSessionStatus.GRADED)
                .map(s -> new ExamResult(titles.getOrDefault(s.getExamId(), "Exame"), s.getGrade(),
                        s.getSubmittedAt() == null ? s.getEndsAt() : s.getSubmittedAt()))
                .toList();
    }

    @Transactional
    public SessionStarted start(UUID examId, UUID user) {
        Exam exam = exams.findById(examId).filter(e -> "PUBLISHED".equals(e.getStatus()))
                .orElseThrow(() -> DomainException.notFound("EXAM_NOT_FOUND", "O exame não existe."));
        Instant now = clock.instant();
        var open = sessions.findByExamIdAndUserIdAndStatus(examId, user, ExamSessionStatus.IN_PROGRESS);
        if (open.isPresent() && !open.get().getEndsAt().isBefore(now)) {
            return new SessionStarted(open.get().getId(), open.get().getEndsAt(), now);
        }
        JsonNode rule = exam.getQuestionPoolRule();
        List<String> types = new ArrayList<>();
        rule.path("types").forEach(t -> types.add(t.asText()));
        if (types.isEmpty()) types.addAll(List.of("A", "B"));
        List<ExerciseDefinition> pool = new ArrayList<>(catalog.publishedByTopic(types, rule.path("topic").asText("")));
        if (pool.isEmpty()) throw DomainException.rule("EXAM_POOL_EMPTY", "Este exame ainda não tem perguntas publicadas.");
        long seed = RANDOM.nextLong();
        Collections.shuffle(pool, new Random(seed));
        int count = Math.min(rule.path("count").asInt(pool.size()), pool.size());

        ArrayNode qs = json.createArrayNode();
        var entities = catalog.entityPools();
        for (int i = 0; i < count; i++) {
            ExerciseDefinition d = pool.get(i);
            long qseed = RANDOM.nextLong();
            qs.addObject().put("n", i + 1).put("versionId", d.versionId().toString()).put("seed", qseed)
                    .set("params", generator.generate(d.paramSpec(), qseed, entities));
        }
        Instant endsAt = now.plus(Duration.ofMinutes(exam.getDurationMin()));
        ExamSession s = sessions.save(new ExamSession(examId, user, now, endsAt, qs, json.createObjectNode(),
                json.createArrayNode()));
        audit.record(user, "EXAM_STARTED", "ExamSession", s.getId(), null, Map.of("exam", exam.getSlug()));
        return new SessionStarted(s.getId(), endsAt, now);
    }

    @Transactional(readOnly = true)
    public SessionView view(UUID sessionId, UUID user) {
        ExamSession s = owned(sessionId, user);
        Exam exam = exams.findById(s.getExamId()).orElseThrow();
        var defs = catalog.definitions(versionIds(s));
        var confirmed = catalog.confirmedSources();
        List<QuestionView> qs = new ArrayList<>();
        for (JsonNode q : s.getQuestions()) {
            ExerciseDefinition d = defs.get(UUID.fromString(q.path("versionId").asText()));
            var r = renderer.render(d.statement(), d.paramSpec(), q.path("params"), confirmed);
            qs.add(new QuestionView(q.path("n").asInt(), d.type(), r.text(), json.valueToTree(r.segments()),
                    renderer.renderPresentation(d.presentation(), q.path("params"))));
        }
        return new SessionView(s.getId(), s.getExamId(), exam.getTitle(), s.getStatus().name(), s.getStartedAt(),
                s.getEndsAt(), clock.instant(), qs, s.getAnswers(), s.getMarked());
    }

    /** Grava a resposta. Recusa depois de endsAt + tolerância de rede, seja qual for o relógio do cliente. */
    @Transactional
    public AnswerSaved saveAnswer(UUID sessionId, UUID user, int n, JsonNode answer, Boolean marked) {
        ExamSession s = owned(sessionId, user);
        Instant now = clock.instant();
        if (s.getStatus() != ExamSessionStatus.IN_PROGRESS || now.isAfter(s.getEndsAt().plus(props.exam().networkGrace()))) {
            throw DomainException.rule("SUBMISSION_AFTER_DEADLINE",
                    "O tempo do exame terminou às " + Formats.time(s.getEndsAt()) + " (hora do servidor). A resposta não foi gravada.");
        }
        if (n < 1 || n > s.getQuestions().size()) throw DomainException.notFound("QUESTION_NOT_FOUND", "A pergunta não existe.");
        if (answer != null && answer.toString().length() > 8000) {
            throw DomainException.badRequest("ANSWER_TOO_LARGE", "A resposta é demasiado grande.");
        }
        if (answer == null || answer.isNull()) s.getAnswers().remove(Integer.toString(n));
        else s.getAnswers().set(Integer.toString(n), answer);
        if (marked != null) {
            ArrayNode m = json.createArrayNode();
            s.getMarked().forEach(x -> { if (x.asInt() != n) m.add(x); });
            if (marked) m.add(n);
            s.getMarked().removeAll();
            s.getMarked().addAll(m);
        }
        return new AnswerSaved(n, now, now);
    }

    @Transactional
    public Report submit(UUID sessionId, UUID user) {
        ExamSession s = owned(sessionId, user);
        Instant now = clock.instant();
        if (s.getStatus() != ExamSessionStatus.IN_PROGRESS) return report(sessionId, user);
        boolean late = now.isAfter(s.getEndsAt().plus(props.exam().networkGrace()));
        s.submit(late ? s.getEndsAt() : now, late);
        grade(s);
        audit.record(user, late ? "EXAM_AUTO_SUBMITTED" : "EXAM_SUBMITTED", "ExamSession", s.getId(), null,
                Map.of("grade", s.getGrade()));
        return buildReport(s, user);
    }

    /** Submissão automática pelo agendador; corrige as respostas guardadas. */
    @Transactional
    public void autoSubmit(UUID sessionId) {
        ExamSession s = sessions.findById(sessionId).orElseThrow();
        if (s.getStatus() == ExamSessionStatus.IN_PROGRESS) {
            s.submit(s.getEndsAt(), true);
            audit.record(null, "EXAM_AUTO_SUBMITTED", "ExamSession", s.getId(), null, null);
        }
        if (s.getStatus() == ExamSessionStatus.SUBMITTED || s.getStatus() == ExamSessionStatus.EXPIRED_AUTO_SUBMITTED) {
            grade(s);
        }
    }

    @Transactional(readOnly = true)
    public Report report(UUID sessionId, UUID user) {
        ExamSession s = owned(sessionId, user);
        if (s.getStatus() != ExamSessionStatus.GRADED) {
            throw DomainException.conflict("EXAM_NOT_GRADED", "O relatório fica disponível depois de submeteres o exame.");
        }
        return buildReport(s, user);
    }

    private void grade(ExamSession s) {
        var defs = catalog.definitions(versionIds(s));
        int correct = 0, total = s.getQuestions().size();
        ArrayNode per = json.createArrayNode();
        Map<String, int[]> byTopic = new LinkedHashMap<>();
        for (JsonNode q : s.getQuestions()) {
            ExerciseDefinition d = defs.get(UUID.fromString(q.path("versionId").asText()));
            JsonNode answer = s.getAnswers().get(q.path("n").asText());
            boolean ok = false;
            if (answer != null) {
                try {
                    ok = grading.grade(d, q.path("params"), answer).correct();
                } catch (AnswerFormatException ignored) {
                    ok = false;
                }
            }
            if (ok) correct++;
            per.add(ok);
            int[] t = byTopic.computeIfAbsent(d.topic(), k -> new int[2]);
            t[1]++;
            if (ok) t[0]++;
        }
        BigDecimal grade = total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(correct * 20L).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        ObjectNode report = json.createObjectNode().put("correct", correct).put("total", total);
        report.set("perQuestion", per);
        ArrayNode topics = report.putArray("topics");
        byTopic.forEach((k, v) -> topics.addObject().put("topic", k).put("correct", v[0]).put("total", v[1]));
        boolean first = sessions.countGraded(s.getExamId(), s.getUserId()) == 0;
        s.graded(grade, report);

        if (first) {
            // Só a primeira correcção de cada exame conta para o ranking (valor a confirmar).
            int delta = grade.multiply(BigDecimal.valueOf(props.exam().pointsPerGradeUnit()))
                    .setScale(0, RoundingMode.HALF_UP).intValue();
            points.award(s.getUserId(), null, PointSource.EXAM, "exam:" + s.getExamId(), delta);
        }
        if (total > 0 && correct == total) badges.award(s.getUserId(), "RIGOR");
    }

    private Report buildReport(ExamSession s, UUID user) {
        Exam exam = exams.findById(s.getExamId()).orElseThrow();
        JsonNode r = s.getReport();
        List<TopicResult> topics = new ArrayList<>();
        r.path("topics").forEach(t -> topics.add(new TopicResult(t.path("topic").asText(), t.path("correct").asInt(),
                t.path("total").asInt())));
        List<String> strengths = topics.stream().filter(t -> t.correct() * 10 >= t.total() * 7).map(TopicResult::topic).toList();
        List<String> weaknesses = topics.stream().filter(t -> t.correct() * 10 < t.total() * 7).map(TopicResult::topic).toList();
        List<Boolean> per = new ArrayList<>();
        r.path("perQuestion").forEach(x -> per.add(x.asBoolean()));
        Instant end = s.getSubmittedAt() == null ? s.getEndsAt() : s.getSubmittedAt();
        boolean passed = s.getGrade().compareTo(exam.getPassMark()) >= 0;
        Set<String> weak = Set.copyOf(weaknesses);
        return new Report(s.getId(), exam.getTitle(), s.getStatus().name(), s.getGrade(), exam.getPassMark(), passed,
                Duration.between(s.getStartedAt(), end).toSeconds(), r.path("correct").asInt(), r.path("total").asInt(),
                topics, strengths, weaknesses, per, rooms.roomsForTopics(user, weak, 3),
                "AUTO".equals(s.getSubmitReason()));
    }

    private ExamSession owned(UUID id, UUID user) {
        return sessions.findById(id).filter(s -> s.getUserId().equals(user))
                .orElseThrow(() -> DomainException.notFound("EXAM_SESSION_NOT_FOUND", "A sessão de exame não existe."));
    }

    private static List<UUID> versionIds(ExamSession s) {
        List<UUID> ids = new ArrayList<>();
        s.getQuestions().forEach(q -> ids.add(UUID.fromString(q.path("versionId").asText())));
        return ids.stream().distinct().collect(Collectors.toList());
    }
}
