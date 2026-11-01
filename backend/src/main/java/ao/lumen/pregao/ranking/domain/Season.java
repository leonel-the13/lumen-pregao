package ao.lumen.pregao.ranking.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "seasons")
public class Season extends BaseEntity {
    private String name;
    @Column(name = "starts_at")
    private Instant startsAt;
    @Column(name = "ends_at")
    private Instant endsAt;

    protected Season() {}

    public boolean contains(Instant at) {
        return !at.isBefore(startsAt) && at.isBefore(endsAt);
    }

    public String getName() { return name; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
}
