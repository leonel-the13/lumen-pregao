package ao.lumen.pregao.authoring.app;

import ao.lumen.pregao.authoring.api.AuthoringDtos.ExerciseDto;
import ao.lumen.pregao.authoring.api.AuthoringDtos.ExerciseSummary;
import ao.lumen.pregao.authoring.api.AuthoringDtos.PreviewResponse;
import ao.lumen.pregao.authoring.api.AuthoringDtos.ReferenceDto;
import ao.lumen.pregao.authoring.api.AuthoringDtos.ReferenceInput;
import ao.lumen.pregao.authoring.api.AuthoringDtos.TransitionRequest;
import ao.lumen.pregao.authoring.api.AuthoringDtos.VersionDto;
import ao.lumen.pregao.authoring.api.AuthoringDtos.VersionInput;
import ao.lumen.pregao.authoring.domain.EditorialStatus;
import ao.lumen.pregao.authoring.domain.Exercise;
import ao.lumen.pregao.authoring.domain.ExerciseReference;
import ao.lumen.pregao.authoring.domain.ExerciseVersion;
import ao.lumen.pregao.authoring.domain.ReviewWorkflow;
import ao.lumen.pregao.authoring.infra.ExerciseReferenceRepository;
import ao.lumen.pregao.authoring.infra.ExerciseRepository;
import ao.lumen.pregao.authoring.infra.ExerciseVersionRepository;
import ao.lumen.pregao.authoring.infra.LegalSourceRepository;
import ao.lumen.pregao.authoring.infra.ReviewWorkflowRepository;
import ao.lumen.pregao.common.audit.AuditService;
import ao.lumen.pregao.common.config.AppProperties;
import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.common.security.Roles;
import ao.lumen.pregao.common.security.SessionUser;
import ao.lumen.pregao.common.text.MarkdownSanitizer;
import ao.lumen.pregao.grading.app.AnswerFormatException;
import ao.lumen.pregao.grading.app.ExerciseDefinition;
import ao.lumen.pregao.grading.app.GradeResult;
import ao.lumen.pregao.grading.app.GradingService;
import ao.lumen.pregao.grading.app.ParamGenerator;
import ao.lumen.pregao.grading.app.PgcAccountService;
import ao.lumen.pregao.grading.app.SolutionPreviewer;
import ao.lumen.pregao.grading.app.StatementRenderer;
import ao.lumen.pregao.grading.domain.Expr;
import ao.lumen.pregao.identity.app.UserDirectory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthoringService {
    private final ExerciseRepository exercises;
    private final ExerciseVersionRepository versions;
    private final ReviewWorkflowRepository workflows;
    private final ExerciseReferenceRepository references;
    private final LegalSourceRepository sources;
    private final ExerciseCatalog catalog;
    private final ParamGenerator generator;
    private final StatementRenderer renderer;
    private final GradingService grading;
    private final SolutionPreviewer previewer;
    private final UserDirectory directory;
    private final AuditService audit;
    private final ao.lumen.pregao.notification.app.NotificationService notifications;
    private final AppProperties props;
    private final ObjectMapper json;
    private final Clock clock;

    public AuthoringService(ExerciseRepository exercises, ExerciseVersionRepository versions,
                            ReviewWorkflowRepository workflows, ExerciseReferenceRepository references,
                            LegalSourceRepository sources, ExerciseCatalog catalog, ParamGenerator generator,
                            StatementRenderer renderer, GradingService grading, SolutionPreviewer previewer,
                            UserDirectory directory, AuditService audit, AppProperties props, ObjectMapper json,
                            Clock clock, ao.lumen.pregao.notification.app.NotificationService notifications) {
        this.notifications = notifications;
        this.exercises = exercises;
        this.versions = versions;
        this.workflows = workflows;
        this.references = references;
        this.sources = sources;
        this.catalog = catalog;
        this.generator = generator;
        this.renderer = renderer;
        this.grading = grading;
        this.previewer = previewer;
        this.directory = directory;
        this.audit = audit;
        this.props = props;
        this.json = json;
        this.clock = clock;
    }

    // ───────────────────────── leitura ─────────────────────────

    @Transactional(readOnly = true)
    public List<ExerciseSummary> list(SessionUser me) {
        boolean all = me.has(Roles.REVIEWER) || me.has(Roles.ADMIN);
        List<Exercise> rows = all ? exercises.findAllByOrderByCreatedAtDesc()
                : exercises.findByAuthorIdOrderByCreatedAtDesc(me.id());
        return summaries(rows);
    }

    @Transactional(readOnly = true)
    public List<ExerciseSummary> reviewQueue() {
        var ws = workflows.findByStatusInOrderBySubmittedAtAsc(
                List.of(EditorialStatus.IN_REVIEW, EditorialStatus.APPROVED, EditorialStatus.NEEDS_REVIEW));
        Set<Exercise> exs = ws.stream().map(w -> w.getExerciseVersion().getExercise()).collect(Collectors.toSet());
        return summaries(exs.stream().toList());
    }

    @Transactional(readOnly = true)
    public ExerciseDto get(SessionUser me, UUID id) {
        Exercise e = exercises.findById(id)
                .orElseThrow(() -> DomainException.notFound("EXERCISE_NOT_FOUND", "O exercício não existe."));
        requireRead(me, e);
        List<ExerciseVersion> vs = versions.findByExerciseIdOrderByNumberDesc(id);
        var ids = vs.stream().map(ExerciseVersion::getId).toList();
        var wf = workflows.findByVersionIds(ids).stream()
                .collect(Collectors.toMap(w -> w.getExerciseVersion().getId(), w -> w));
        var refs = references.findByVersionIds(ids).stream()
                .collect(Collectors.groupingBy(r -> r.getExerciseVersion().getId()));
        return new ExerciseDto(e.getId(), e.getCode(), e.getTopic(), e.getAuthorId(),
                directory.summary(e.getAuthorId()).name(),
                vs.stream().map(v -> toDto(v, wf.get(v.getId()), refs.getOrDefault(v.getId(), List.of()))).toList());
    }

    // ───────────────────────── escrita ─────────────────────────

    @Transactional
    public ExerciseDto create(SessionUser me, VersionInput in) {
        if (in.topic() == null || in.topic().isBlank()) {
            throw DomainException.badRequest("TOPIC_REQUIRED", "Indica o tema do exercício.");
        }
        String code = "EX-%04d".formatted(exercises.nextCode());
        Exercise e = exercises.save(new Exercise(code, me.id(), in.topic().strip()));
        ExerciseVersion v = new ExerciseVersion(e, 1);
        apply(v, in);
        versions.save(v);
        workflows.save(new ReviewWorkflow(v));
        replaceReferences(v, in.references());
        audit.record(me.id(), "EXERCISE_CREATED", "Exercise", e.getId(), null, Map.of("code", code));
        return get(me, e.getId());
    }

    @Transactional
    public ExerciseDto updateDraft(SessionUser me, UUID exerciseId, int number, VersionInput in) {
        Exercise e = owned(me, exerciseId);
        ExerciseVersion v = versions.findByExerciseIdAndNumber(exerciseId, number)
                .orElseThrow(() -> DomainException.notFound("VERSION_NOT_FOUND", "A versão não existe."));
        ReviewWorkflow w = workflow(v.getId());
        if (w.getStatus() != EditorialStatus.DRAFT) {
            throw DomainException.conflict("VERSION_LOCKED",
                    "Só podes editar versões em rascunho. Cria uma nova versão para alterar esta.");
        }
        if (in.topic() != null && !in.topic().isBlank()) e.setTopic(in.topic().strip());
        apply(v, in);
        replaceReferences(v, in.references());
        audit.record(me.id(), "VERSION_EDITED", "ExerciseVersion", v.getId(), null, Map.of("number", number));
        return get(me, exerciseId);
    }

    /** Nova versão a partir da mais recente (as versões publicadas nunca são alteradas). */
    @Transactional
    public ExerciseDto newVersion(SessionUser me, UUID exerciseId) {
        Exercise e = owned(me, exerciseId);
        List<ExerciseVersion> vs = versions.findByExerciseIdOrderByNumberDesc(exerciseId);
        ExerciseVersion latest = vs.getFirst();
        if (workflow(latest.getId()).getStatus() == EditorialStatus.DRAFT) {
            throw DomainException.conflict("DRAFT_EXISTS", "Já existe uma versão em rascunho deste exercício.");
        }
        ExerciseVersion v = new ExerciseVersion(e, latest.getNumber() + 1);
        v.define(latest.getType(), latest.getTitle(), latest.getStatement(), latest.getPresentation(),
                latest.getParamSpec(), latest.getSolutionSpec(), latest.getTolerance(), latest.getHints(),
                latest.getFeedbackPolicy(), latest.getDifficulty(), latest.getPoints(), latest.getMaxAttempts(),
                latest.getPgcVersion(), latest.getValidUntil(), null);
        versions.save(v);
        workflows.save(new ReviewWorkflow(v));
        for (var r : references.findByVersionIds(List.of(latest.getId()))) {
            references.save(new ExerciseReference(v, r.getLegalSource(), r.getArticles(), r.getConsultedAt()));
        }
        audit.record(me.id(), "VERSION_CREATED", "ExerciseVersion", v.getId(), null, Map.of("number", v.getNumber()));
        return get(me, exerciseId);
    }

    @Transactional
    public VersionDto transition(SessionUser me, UUID versionId, TransitionRequest req) {
        ExerciseVersion v = versions.findWithExercise(versionId)
                .orElseThrow(() -> DomainException.notFound("VERSION_NOT_FOUND", "A versão não existe."));
        ReviewWorkflow w = workflow(versionId);
        boolean isAuthor = v.getExercise().getAuthorId().equals(me.id());
        boolean reviewer = me.has(Roles.REVIEWER) || me.has(Roles.ADMIN);
        EditorialStatus from = w.getStatus();
        switch (req.to()) {
            case IN_REVIEW -> {
                if (!isAuthor && !reviewer) throw DomainException.forbidden("NOT_OWNER", "Só o autor envia o exercício para revisão.");
                selfCheck(catalog.definition(versionId));
            }
            case APPROVED -> {
                if (!reviewer) throw DomainException.forbidden("REVIEWER_REQUIRED", "Só um revisor pode aprovar.");
                if (isAuthor) throw DomainException.forbidden("SELF_APPROVAL", "Não podes aprovar um exercício de que és autor.");
                if (!req.referencesChecked()) {
                    throw DomainException.rule("REFERENCES_NOT_CHECKED",
                            "Confirma que validaste a versão em vigor das referências antes de aprovar.");
                }
            }
            case PUBLISHED -> {
                if (!reviewer) throw DomainException.forbidden("REVIEWER_REQUIRED", "Só um revisor pode publicar.");
                if (isAuthor) throw DomainException.forbidden("SELF_APPROVAL", "Não podes publicar um exercício de que és autor.");
                if (references.findByVersionIds(List.of(versionId)).isEmpty()) {
                    throw DomainException.rule("REFERENCE_REQUIRED",
                            "Não é possível publicar sem referência normativa. Acrescenta a fonte, os artigos e a versão.");
                }
            }
            case DRAFT, ARCHIVED -> {
                if (!reviewer) throw DomainException.forbidden("REVIEWER_REQUIRED", "Só um revisor pode fazer esta transição.");
            }
            case NEEDS_REVIEW -> throw DomainException.rule("SYSTEM_ONLY", "Este estado é atribuído pelos alertas de legislação.");
        }
        w.moveTo(req.to(), me.id(), req.notes() == null ? null : MarkdownSanitizer.clean(req.notes()), clock.instant());
        audit.record(me.id(), "EDITORIAL_" + req.to(), "ExerciseVersion", versionId,
                Map.of("status", from), Map.of("status", req.to()));
        String code = v.getExercise().getCode();
        String link = "/autoria/" + v.getExercise().getId();
        switch (req.to()) {
            case IN_REVIEW -> notifications.notifyAll(directory.idsWithRoles(java.util.Set.of("REVIEWER", "ADMIN")).stream()
                    .filter(id -> !id.equals(me.id())).toList(), "EDITORIAL", "Exercício para rever: " + code, v.getTitle(), link);
            case APPROVED -> notifications.notify(v.getExercise().getAuthorId(), "EDITORIAL", code + " aprovado", v.getTitle(), link);
            case PUBLISHED -> notifications.notify(v.getExercise().getAuthorId(), "EDITORIAL", code + " publicado",
                    "Já está visível para os estudantes.", link);
            case DRAFT -> notifications.notify(v.getExercise().getAuthorId(), "EDITORIAL", code + " devolvido para alterações",
                    req.notes() == null ? "Vê as notas do revisor." : req.notes(), link);
            default -> { }
        }
        return toDto(v, w, references.findByVersionIds(List.of(versionId)));
    }

    @Transactional(readOnly = true)
    public PreviewResponse preview(SessionUser me, UUID versionId, Long seed) {
        ExerciseVersion v = versions.findWithExercise(versionId)
                .orElseThrow(() -> DomainException.notFound("VERSION_NOT_FOUND", "A versão não existe."));
        requireRead(me, v.getExercise());
        ExerciseDefinition def = ExerciseCatalog.toDefinition(v);
        long s = seed != null ? seed : new java.util.Random().nextLong();
        try {
            JsonNode params = generator.generate(def.paramSpec(), s, catalog.entityPools());
            var rendered = renderer.render(def.statement(), def.paramSpec(), params, catalog.confirmedSources());
            JsonNode expected = previewer.expectedAnswer(def, params);
            GradeResult check = grading.grade(def, params, expected);
            return new PreviewResponse(params, rendered.text(), json.valueToTree(rendered.segments()),
                    renderer.renderPresentation(def.presentation(), params), expected,
                    check.correct() || check.outcome() == GradeResult.Outcome.STEP_OK ? "OK"
                            : "A solução esperada não é aceite pela correcção: " + check.summary());
        } catch (Expr.ExprException | IllegalStateException | AnswerFormatException ex) {
            throw DomainException.rule("PREVIEW_FAILED", "Não foi possível gerar o exemplo: " + ex.getMessage());
        }
    }

    // ───────────────────────── auxiliares ─────────────────────────

    private void selfCheck(ExerciseDefinition def) {
        try {
            JsonNode params = generator.generate(def.paramSpec(), 42L, catalog.entityPools());
            GradeResult r = grading.grade(def, params, previewer.expectedAnswer(def, params));
            if (!r.correct()) {
                throw DomainException.rule("SELF_CHECK_FAILED",
                        "A solução esperada não passa na correcção automática: " + r.summary());
            }
        } catch (Expr.ExprException | IllegalStateException | AnswerFormatException ex) {
            throw DomainException.rule("SELF_CHECK_FAILED", "Revê os parâmetros e a solução: " + ex.getMessage());
        }
    }

    private void apply(ExerciseVersion v, VersionInput in) {
        v.define(in.type(), in.title().strip(), MarkdownSanitizer.clean(in.statement()), obj(in.presentation()),
                obj(in.paramSpec()), obj(in.solutionSpec()), obj(in.tolerance()),
                in.hints() == null ? json.createArrayNode() : in.hints(),
                in.feedbackPolicy() == null ? "DETAILED" : in.feedbackPolicy(), in.difficulty(), in.points(),
                in.maxAttempts() == null ? props.learning().maxAttemptsDefault() : in.maxAttempts(),
                in.pgcVersion() == null || in.pgcVersion().isBlank() ? PgcAccountService.DEFAULT_VERSION : in.pgcVersion(),
                in.validUntil(), in.changeNote());
    }

    private void replaceReferences(ExerciseVersion v, List<ReferenceInput> refs) {
        if (refs == null) return;
        references.deleteByVersionId(v.getId());
        references.flush();
        for (ReferenceInput r : refs) {
            var src = sources.findById(r.legalSourceId())
                    .orElseThrow(() -> DomainException.badRequest("LEGAL_SOURCE_NOT_FOUND", "A fonte normativa não existe."));
            references.save(new ExerciseReference(v, src, r.articles(), r.consultedAt()));
        }
    }

    private JsonNode obj(JsonNode n) {
        return n == null || n.isNull() ? json.createObjectNode() : n;
    }

    private Exercise owned(SessionUser me, UUID id) {
        Exercise e = exercises.findById(id)
                .orElseThrow(() -> DomainException.notFound("EXERCISE_NOT_FOUND", "O exercício não existe."));
        if (!e.getAuthorId().equals(me.id())) {
            throw DomainException.forbidden("NOT_OWNER", "Só o autor pode editar os seus rascunhos.");
        }
        return e;
    }

    private void requireRead(SessionUser me, Exercise e) {
        if (e.getAuthorId().equals(me.id()) || me.has(Roles.REVIEWER) || me.has(Roles.ADMIN)) return;
        throw DomainException.forbidden("NOT_OWNER", "Não tens acesso a este exercício.");
    }

    private ReviewWorkflow workflow(UUID versionId) {
        return workflows.findByVersionId(versionId).orElseThrow();
    }

    private List<ExerciseSummary> summaries(List<Exercise> rows) {
        if (rows.isEmpty()) return List.of();
        var authors = directory.summaries(new HashSet<>(rows.stream().map(Exercise::getAuthorId).toList()));
        return rows.stream().map(e -> {
            ExerciseVersion latest = versions.findByExerciseIdOrderByNumberDesc(e.getId()).getFirst();
            ReviewWorkflow w = workflow(latest.getId());
            return new ExerciseSummary(e.getId(), e.getCode(), e.getTopic(),
                    authors.containsKey(e.getAuthorId()) ? authors.get(e.getAuthorId()).name() : "—",
                    latest.getNumber(), latest.getId(), latest.getTitle(), latest.getType(), w.getStatus(),
                    w.getLegalAlertAt(), latest.getUpdatedAt());
        }).sorted(Comparator.comparing(ExerciseSummary::updatedAt).reversed()).toList();
    }

    private VersionDto toDto(ExerciseVersion v, ReviewWorkflow w, List<ExerciseReference> refs) {
        return new VersionDto(v.getId(), v.getNumber(), v.getType(), v.getTitle(), v.getStatement(), v.getPresentation(),
                v.getParamSpec(), v.getSolutionSpec(), v.getTolerance(), v.getHints(), v.getFeedbackPolicy(),
                v.getDifficulty(), v.getPoints(), v.getMaxAttempts(), v.getPgcVersion(), v.getValidUntil(),
                v.getChangeNote(), w.getStatus(), w.getNotes(), w.getLegalAlertAt(),
                refs.stream().map(r -> new ReferenceDto(r.getLegalSource().getId(), r.getLegalSource().getTitle(),
                        r.getLegalSource().getIdentifier(), r.getLegalSource().getVersion(), r.getArticles(),
                        r.getConsultedAt(), r.getLegalSource().isConfirmed())).toList(),
                v.getUpdatedAt());
    }
}
