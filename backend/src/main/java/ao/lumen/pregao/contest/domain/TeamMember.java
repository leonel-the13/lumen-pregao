package ao.lumen.pregao.contest.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "team_members")
public class TeamMember extends BaseEntity {
    @Column(name = "team_id")
    private UUID teamId;
    @Column(name = "contest_id")
    private UUID contestId;
    @Column(name = "user_id")
    private UUID userId;
    private String role;
    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    protected TeamMember() {}

    public TeamMember(UUID teamId, UUID contestId, UUID userId, String role, Instant confirmedAt) {
        this.teamId = teamId;
        this.contestId = contestId;
        this.userId = userId;
        this.role = role;
        this.confirmedAt = confirmedAt;
    }

    public boolean isCaptain() { return "CAPTAIN".equals(role); }
    public UUID getTeamId() { return teamId; }
    public UUID getContestId() { return contestId; }
    public UUID getUserId() { return userId; }
    public String getRole() { return role; }
    public Instant getConfirmedAt() { return confirmedAt; }
}
