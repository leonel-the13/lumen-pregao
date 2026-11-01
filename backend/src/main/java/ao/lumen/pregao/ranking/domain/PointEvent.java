package ao.lumen.pregao.ranking.domain;

import ao.lumen.pregao.common.domain.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Registo imutável de pontos: base dos rankings. Nunca é actualizado, só compensado. */
@Entity
@Immutable
@Table(name = "point_events")
public class PointEvent {
    @Id
    private UUID id;
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "team_id")
    private UUID teamId;
    @Enumerated(EnumType.STRING)
    private PointSource source;
    @Column(name = "source_id")
    private String sourceId;
    private int delta;
    private Instant at;

    protected PointEvent() {}

    public PointEvent(UUID userId, UUID teamId, PointSource source, String sourceId, int delta, Instant at) {
        this.id = UuidV7.next();
        this.userId = userId;
        this.teamId = teamId;
        this.source = source;
        this.sourceId = sourceId;
        this.delta = delta;
        this.at = at;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getTeamId() { return teamId; }
    public PointSource getSource() { return source; }
    public String getSourceId() { return sourceId; }
    public int getDelta() { return delta; }
    public Instant getAt() { return at; }
}
