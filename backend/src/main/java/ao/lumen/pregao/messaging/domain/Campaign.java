package ao.lumen.pregao.messaging.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

/** Email enviado pela administração a um segmento. Imutável depois de criado; o estado vive nas entregas. */
@Entity
@Table(name = "email_campaigns")
public class Campaign extends BaseEntity {
    @Column(name = "created_by")
    private UUID createdBy;
    @Column(name = "idempotency_key")
    private String idempotencyKey;
    @Column(name = "template_id")
    private UUID templateId;
    @Enumerated(EnumType.STRING)
    private Audience audience;
    @Column(name = "course_id")
    private UUID courseId;
    private Integer year;
    @Column(name = "inactive_days")
    private Integer inactiveDays;
    @Column(name = "target_user_id")
    private UUID targetUserId;
    @Column(name = "audience_label")
    private String audienceLabel;
    private String subject;
    private String title;
    private String body;
    @Column(name = "cta_label")
    private String ctaLabel;
    @Column(name = "cta_path")
    private String ctaPath;

    protected Campaign() {}

    public Campaign(UUID createdBy, String idempotencyKey, UUID templateId, Audience audience, UUID courseId, Integer year,
                    Integer inactiveDays, UUID targetUserId, String audienceLabel, MessageContent c) {
        this.createdBy = createdBy;
        this.idempotencyKey = idempotencyKey;
        this.templateId = templateId;
        this.audience = audience;
        this.courseId = courseId;
        this.year = year;
        this.inactiveDays = inactiveDays;
        this.targetUserId = targetUserId;
        this.audienceLabel = audienceLabel;
        this.subject = c.subject();
        this.title = c.title();
        this.body = c.body();
        this.ctaLabel = c.ctaLabel();
        this.ctaPath = c.ctaPath();
    }

    public MessageContent content() { return new MessageContent(subject, title, body, ctaLabel, ctaPath); }
    public UUID getCreatedBy() { return createdBy; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Audience getAudience() { return audience; }
    public UUID getTargetUserId() { return targetUserId; }
    public String getAudienceLabel() { return audienceLabel; }
    public String getSubject() { return subject; }
}
