package ao.lumen.pregao.contest.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Pontuação persistida por participante: fonte de verdade do contest. */
@Entity
@Table(name = "scoreboards")
public class Scoreboard extends BaseEntity {
    @Column(name = "contest_id")
    private UUID contestId;
    @Column(name = "participant_type")
    private String participantType;
    @Column(name = "participant_id")
    private UUID participantId;
    private int points;
    @Column(name = "penalty_minutes")
    private int penaltyMinutes;
    @Column(name = "solved_count")
    private int solvedCount;
    @Column(name = "last_correct_at")
    private Instant lastCorrectAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "frozen_snapshot")
    private JsonNode frozenSnapshot;
    @Version
    private Long version;

    protected Scoreboard() {}

    public Scoreboard(UUID contestId, UUID teamId) {
        this.contestId = contestId;
        this.participantType = "TEAM";
        this.participantId = teamId;
    }

    public void solved(int pts, int penalty, Instant at) {
        points += pts;
        penaltyMinutes += penalty;
        solvedCount++;
        lastCorrectAt = at;
    }

    public void freeze() {
        var o = JsonNodeFactory.instance.objectNode();
        o.put("points", points).put("penaltyMinutes", penaltyMinutes).put("solvedCount", solvedCount);
        if (lastCorrectAt != null) o.put("lastCorrectAt", lastCorrectAt.toString());
        frozenSnapshot = o;
    }

    public UUID getContestId() { return contestId; }
    public String getParticipantType() { return participantType; }
    public UUID getParticipantId() { return participantId; }
    public int getPoints() { return points; }
    public int getPenaltyMinutes() { return penaltyMinutes; }
    public int getSolvedCount() { return solvedCount; }
    public Instant getLastCorrectAt() { return lastCorrectAt; }
    public JsonNode getFrozenSnapshot() { return frozenSnapshot; }
}
