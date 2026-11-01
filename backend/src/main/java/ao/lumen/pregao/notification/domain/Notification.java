package ao.lumen.pregao.notification.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {
    @Column(name = "user_id")
    private UUID userId;
    private String kind;
    private String title;
    private String body;
    private String link;
    @Column(name = "read_at")
    private Instant readAt;

    protected Notification() {}

    public Notification(UUID userId, String kind, String title, String body, String link) {
        this.userId = userId;
        this.kind = kind;
        this.title = title;
        this.body = body;
        this.link = link;
    }

    public void markRead(Instant at) { if (readAt == null) readAt = at; }
    public UUID getUserId() { return userId; }
    public String getKind() { return kind; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getLink() { return link; }
    public Instant getReadAt() { return readAt; }
}
