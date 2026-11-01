package ao.lumen.pregao.authoring.app;

import ao.lumen.pregao.authoring.domain.Company;
import ao.lumen.pregao.authoring.domain.EditorialStatus;
import ao.lumen.pregao.authoring.domain.ExerciseVersion;
import ao.lumen.pregao.authoring.domain.LegalSource;
import ao.lumen.pregao.authoring.domain.ReviewWorkflow;
import ao.lumen.pregao.authoring.infra.CompanyRepository;
import ao.lumen.pregao.authoring.infra.ExerciseReferenceRepository;
import ao.lumen.pregao.authoring.infra.ExerciseVersionRepository;
import ao.lumen.pregao.authoring.infra.LegalSourceRepository;
import ao.lumen.pregao.authoring.infra.ReviewWorkflowRepository;
import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.grading.app.ExerciseDefinition;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** API pública de leitura do módulo authoring para learning, exam e contest. */
@Service
@Transactional(readOnly = true)
public class ExerciseCatalog {
    private final ExerciseVersionRepository versions;
    private final ReviewWorkflowRepository workflows;
    private final ExerciseReferenceRepository references;
    private final LegalSourceRepository sources;
    private final CompanyRepository companies;

    public ExerciseCatalog(ExerciseVersionRepository versions, ReviewWorkflowRepository workflows,
                           ExerciseReferenceRepository references, LegalSourceRepository sources,
                           CompanyRepository companies) {
        this.versions = versions;
        this.workflows = workflows;
        this.references = references;
        this.sources = sources;
        this.companies = companies;
    }

    public record ReferenceView(String title, String identifier, String version, String articles, boolean confirmed) {}

    public ExerciseDefinition definition(UUID versionId) {
        return versions.findWithExercise(versionId).map(ExerciseCatalog::toDefinition)
                .orElseThrow(() -> DomainException.notFound("EXERCISE_NOT_FOUND", "O exercício não existe."));
    }

    public Map<UUID, ExerciseDefinition> definitions(Collection<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return versions.findByIdIn(ids).stream().map(ExerciseCatalog::toDefinition)
                .collect(Collectors.toMap(ExerciseDefinition::versionId, Function.identity()));
    }

    public Map<UUID, EditorialStatus> statuses(Collection<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return workflows.findByVersionIds(ids).stream()
                .collect(Collectors.toMap(w -> w.getExerciseVersion().getId(), ReviewWorkflow::getStatus));
    }

    /** Só conteúdo publicado é servido a estudantes (spec §4.14). */
    public boolean isPublished(UUID versionId) {
        return statuses(List.of(versionId)).get(versionId) == EditorialStatus.PUBLISHED;
    }

    public Set<UUID> published(Collection<UUID> ids) {
        return statuses(ids).entrySet().stream().filter(e -> e.getValue() == EditorialStatus.PUBLISHED)
                .map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    public Map<UUID, List<ReferenceView>> references(Collection<UUID> versionIds) {
        if (versionIds.isEmpty()) return Map.of();
        return references.findByVersionIds(versionIds).stream().collect(Collectors.groupingBy(
                r -> r.getExerciseVersion().getId(),
                Collectors.mapping(r -> view(r.getLegalSource(), r.getArticles()), Collectors.toList())));
    }

    public Map<String, List<String>> entityPools() {
        return Map.of("companies", companies.findAllByOrderByNameAsc().stream().map(Company::getName).toList());
    }

    public Predicate<UUID> confirmedSources() {
        Set<UUID> confirmed = sources.findAll().stream().filter(LegalSource::isConfirmed)
                .map(LegalSource::getId).collect(Collectors.toSet());
        return confirmed::contains;
    }

    public List<ExerciseDefinition> publishedByTopic(Collection<String> types, String topicPrefix) {
        return versions.publishedByTopic(types, topicPrefix == null ? "" : topicPrefix).stream()
                .map(ExerciseCatalog::toDefinition).toList();
    }

    static ReferenceView view(LegalSource s, String articles) {
        return new ReferenceView(s.getTitle(), s.getIdentifier(), s.getVersion(), articles, s.isConfirmed());
    }

    static ExerciseDefinition toDefinition(ExerciseVersion v) {
        return new ExerciseDefinition(v.getId(), v.getExercise().getCode(), v.getType(), v.getTitle(), v.getStatement(),
                v.getPresentation(), v.getParamSpec(), v.getSolutionSpec(), v.getTolerance(), v.getHints(),
                v.getFeedbackPolicy(), v.getDifficulty(), v.getPoints(), v.getMaxAttempts(), v.getPgcVersion(),
                v.getExercise().getTopic());
    }
}
