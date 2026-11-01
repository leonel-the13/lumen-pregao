package ao.lumen.pregao.ranking.domain;

import ao.lumen.pregao.common.domain.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_badges")
public class UserBadge {
    @Id
    private UUID id;
    @Column(name = "user_id")
    private UUID userId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "badge_id")
    private Badge badge;
    @Column(name = "awarded_at")
    private Instant awardedAt;

    protected UserBadge() {}

    public UserBadge(UUID userId, Badge badge, Instant awardedAt) {
        this.id = UuidV7.next();
        this.userId = userId;
        this.badge = badge;
        this.awardedAt = awardedAt;
    }

    public UUID getUserId() { return userId; }
    public Badge getBadge() { return badge; }
    public Instant getAwardedAt() { return awardedAt; }
}
