package ao.lumen.pregao.contest.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/** Equipa de um contest. Num contest individual, cada participante tem uma «equipa» de 1 (individual=true). */
@Entity
@Table(name = "teams")
public class Team extends BaseEntity {
    @Column(name = "contest_id")
    private UUID contestId;
    private String name;
    private boolean individual;
    @Column(name = "locked_at")
    private Instant lockedAt;
    @Version
    private Long version;

    protected Team() {}

    public Team(UUID contestId, String name, boolean individual) {
        this.contestId = contestId;
        this.name = name;
        this.individual = individual;
    }

    public void lock(Instant at) { if (lockedAt == null) lockedAt = at; }
    public boolean isLocked() { return lockedAt != null; }

    public UUID getContestId() { return contestId; }
    public String getName() { return name; }
    public boolean isIndividual() { return individual; }
    public Instant getLockedAt() { return lockedAt; }
}
