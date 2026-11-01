package ao.lumen.pregao.contest.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "team_invites")
public class TeamInvite extends BaseEntity {
    @Column(name = "team_id")
    private UUID teamId;
    @Column(name = "invitee_user_id")
    private UUID inviteeUserId;
    @Column(name = "invited_by")
    private UUID invitedBy;
    private String status = "PENDING";
    @Column(name = "responded_at")
    private Instant respondedAt;

    protected TeamInvite() {}

    public TeamInvite(UUID teamId, UUID inviteeUserId, UUID invitedBy) {
        this.teamId = teamId;
        this.inviteeUserId = inviteeUserId;
        this.invitedBy = invitedBy;
    }

    public void respond(String status, Instant at) {
        this.status = status;
        this.respondedAt = at;
    }

    public UUID getTeamId() { return teamId; }
    public UUID getInviteeUserId() { return inviteeUserId; }
    public UUID getInvitedBy() { return invitedBy; }
    public String getStatus() { return status; }
}
