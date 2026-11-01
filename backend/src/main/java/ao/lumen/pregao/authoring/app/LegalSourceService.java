package ao.lumen.pregao.authoring.app;

import ao.lumen.pregao.authoring.api.AuthoringDtos.LegalSourceDto;
import ao.lumen.pregao.authoring.api.AuthoringDtos.LegalSourceInput;
import ao.lumen.pregao.authoring.api.AuthoringDtos.LegalSourceUpdate;
import ao.lumen.pregao.authoring.domain.LegalSource;
import ao.lumen.pregao.authoring.infra.LegalSourceRepository;
import ao.lumen.pregao.authoring.infra.ReviewWorkflowRepository;
import ao.lumen.pregao.common.audit.AuditService;
import ao.lumen.pregao.common.config.AppProperties;
import ao.lumen.pregao.common.error.DomainException;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegalSourceService {
    private final LegalSourceRepository sources;
    private final ReviewWorkflowRepository workflows;
    private final AuditService audit;
    private final ao.lumen.pregao.notification.app.NotificationService notifications;
    private final AppProperties props;
    private final Clock clock;

    public LegalSourceService(LegalSourceRepository sources, ReviewWorkflowRepository workflows, AuditService audit,
                              AppProperties props, Clock clock, ao.lumen.pregao.notification.app.NotificationService notifications) {
        this.notifications = notifications;
        this.sources = sources;
        this.workflows = workflows;
        this.audit = audit;
        this.props = props;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<LegalSourceDto> list() {
        return sources.findAllByOrderByTitleAsc().stream().map(LegalSourceService::toDto).toList();
    }

    @Transactional
    public LegalSourceDto create(UUID actor, LegalSourceInput in) {
        LegalSource s = new LegalSource(in.title(), in.identifier(), in.version());
        s.update(in.title(), in.identifier(), in.version(), in.publishedAt(), in.effectiveFrom(), in.effectiveTo(),
                blank(in.url()), in.confirmed());
        sources.save(s);
        audit.record(actor, "LEGAL_SOURCE_CREATED", "LegalSource", s.getId(), null, toDto(s));
        return toDto(s);
    }

    /**
     * Actualiza a fonte. Se a versão mudar, todos os exercícios ligados recebem alerta e, segundo a política,
     * passam a NEEDS_REVIEW e saem de publicação (spec §4.14).
     */
    @Transactional
    public LegalSourceUpdate update(UUID actor, UUID id, LegalSourceInput in) {
        LegalSource s = sources.findById(id)
                .orElseThrow(() -> DomainException.notFound("LEGAL_SOURCE_NOT_FOUND", "A fonte normativa não existe."));
        LegalSourceDto before = toDto(s);
        boolean newVersion = !s.getVersion().equals(in.version());
        s.update(in.title(), in.identifier(), in.version(), in.publishedAt(), in.effectiveFrom(), in.effectiveTo(),
                blank(in.url()), in.confirmed());
        int affected = 0;
        if (newVersion) {
            boolean unpublish = !"WARN".equalsIgnoreCase(props.authoring().legalChangePolicy());
            var now = clock.instant();
            for (var w : workflows.findLinkedToSource(id)) {
                var from = w.getStatus();
                w.legalAlert(now, unpublish);
                affected++;
                notifications.notify(w.getExerciseVersion().getExercise().getAuthorId(), "LEGAL_ALERT",
                        "Norma alterada: " + w.getExerciseVersion().getExercise().getCode(),
                        "«" + s.getTitle() + "» tem nova versão. Revê o exercício e envia-o de novo para revisão.",
                        "/autoria/" + w.getExerciseVersion().getExercise().getId());
                if (from != w.getStatus()) {
                    audit.record(actor, "EDITORIAL_" + w.getStatus(), "ExerciseVersion", w.getExerciseVersion().getId(),
                            Map.of("status", from), Map.of("status", w.getStatus(), "reason", "LEGAL_SOURCE_VERSION"));
                }
            }
        }
        audit.record(actor, newVersion ? "LEGAL_SOURCE_NEW_VERSION" : "LEGAL_SOURCE_UPDATED", "LegalSource", id,
                before, toDto(s));
        LegalSourceDto dto = toDto(s);
        return new LegalSourceUpdate(new LegalSourceInput(dto.title(), dto.identifier(), dto.version(), dto.publishedAt(),
                dto.effectiveFrom(), dto.effectiveTo(), dto.url() == null ? "" : dto.url(), dto.confirmed()), affected);
    }

    @Transactional(readOnly = true)
    public long openAlerts() {
        return workflows.countLegalAlerts();
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    static LegalSourceDto toDto(LegalSource s) {
        return new LegalSourceDto(s.getId(), s.getTitle(), s.getIdentifier(), s.getVersion(), s.getPublishedAt(),
                s.getEffectiveFrom(), s.getEffectiveTo(), s.getUrl(), s.isConfirmed(), s.getUpdatedAt());
    }
}
