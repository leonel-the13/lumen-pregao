package ao.lumen.pregao.messaging.app;

import ao.lumen.pregao.common.audit.AuditService;
import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.messaging.api.AdminEmailDtos.TemplateIn;
import ao.lumen.pregao.messaging.api.AdminEmailDtos.TemplateView;
import ao.lumen.pregao.messaging.domain.MessageContent;
import ao.lumen.pregao.messaging.domain.MessageTemplate;
import ao.lumen.pregao.messaging.infra.MessageTemplateRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Modelos de email da administração. */
@Service
public class MessageTemplateService {
    private final MessageTemplateRepository repo;
    private final AuditService audit;

    public MessageTemplateService(MessageTemplateRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<TemplateView> list() {
        return repo.findAllByOrderByNameAsc().stream().map(MessageTemplateService::view).toList();
    }

    @Transactional
    public TemplateView create(UUID actor, TemplateIn in) {
        String name = in.name().strip();
        if (repo.existsByNameIgnoreCase(name)) {
            throw DomainException.conflict("TEMPLATE_NAME_TAKEN", "Já existe um modelo com este nome. Escolhe outro.");
        }
        MessageContent c = MessageRenderer.validate(in.content().toContent());
        MessageTemplate t = repo.save(new MessageTemplate(name, c, actor));
        audit.record(actor, "EMAIL_TEMPLATE_CREATED", "EmailTemplate", t.getId(), null, Map.of("name", name));
        return view(t);
    }

    @Transactional
    public TemplateView update(UUID actor, UUID id, TemplateIn in) {
        MessageTemplate t = load(id);
        String name = in.name().strip();
        if (repo.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw DomainException.conflict("TEMPLATE_NAME_TAKEN", "Já existe um modelo com este nome. Escolhe outro.");
        }
        String before = t.getName();
        t.update(name, MessageRenderer.validate(in.content().toContent()));
        audit.record(actor, "EMAIL_TEMPLATE_UPDATED", "EmailTemplate", id, Map.of("name", before), Map.of("name", name));
        return view(t);
    }

    @Transactional
    public void delete(UUID actor, UUID id) {
        MessageTemplate t = load(id);
        repo.delete(t);
        audit.record(actor, "EMAIL_TEMPLATE_DELETED", "EmailTemplate", id, Map.of("name", t.getName()), null);
    }

    private MessageTemplate load(UUID id) {
        return repo.findById(id).orElseThrow(() -> DomainException.notFound("TEMPLATE_NOT_FOUND", "O modelo não existe."));
    }

    static TemplateView view(MessageTemplate t) {
        var c = t.content();
        return new TemplateView(t.getId(), t.getName(), c.subject(), c.title(), c.body(), c.ctaLabel(), c.ctaPath(),
                t.getCreatedBy() == null, t.getUpdatedAt());
    }
}
