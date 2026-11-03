package ao.lumen.pregao.messaging.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** Modelo de email guardado pela administração. */
@Entity
@Table(name = "email_templates")
public class MessageTemplate extends BaseEntity {
    private String name;
    private String subject;
    private String title;
    private String body;
    @Column(name = "cta_label")
    private String ctaLabel;
    @Column(name = "cta_path")
    private String ctaPath;
    @Column(name = "created_by")
    private UUID createdBy;

    protected MessageTemplate() {}

    public MessageTemplate(String name, MessageContent c, UUID createdBy) {
        this.createdBy = createdBy;
        update(name, c);
    }

    public void update(String name, MessageContent c) {
        this.name = name;
        this.subject = c.subject();
        this.title = c.title();
        this.body = c.body();
        this.ctaLabel = c.ctaLabel();
        this.ctaPath = c.ctaPath();
    }

    public MessageContent content() { return new MessageContent(subject, title, body, ctaLabel, ctaPath); }
    public String getName() { return name; }
    public UUID getCreatedBy() { return createdBy; }
}
