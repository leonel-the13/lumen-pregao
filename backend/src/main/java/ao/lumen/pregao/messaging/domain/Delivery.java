package ao.lumen.pregao.messaging.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Envio de uma campanha a uma pessoa: persistido antes do envio (PENDING), actualizado pelo processo de envio. */
@Entity
@Table(name = "email_deliveries")
public class Delivery extends BaseEntity {
    @Column(name = "campaign_id")
    private UUID campaignId;
    @Column(name = "user_id")
    private UUID userId;
    @Enumerated(EnumType.STRING)
    private DeliveryStatus status = DeliveryStatus.PENDING;
    private int attempts;
    @Column(name = "last_error")
    private String lastError;
    @Column(name = "sent_at")
    private Instant sentAt;

    protected Delivery() {}

    public Delivery(UUID campaignId, UUID userId) {
        this.campaignId = campaignId;
        this.userId = userId;
    }

    public void sent(Instant at) {
        attempts++;
        status = DeliveryStatus.SENT;
        sentAt = at;
        lastError = null;
    }

    public void failed(String error) {
        attempts++;
        status = DeliveryStatus.FAILED;
        lastError = error == null ? null : error.substring(0, Math.min(300, error.length()));
    }

    public void skipped() { status = DeliveryStatus.SKIPPED; }


    public UUID getCampaignId() { return campaignId; }
    public UUID getUserId() { return userId; }
    public DeliveryStatus getStatus() { return status; }
}
