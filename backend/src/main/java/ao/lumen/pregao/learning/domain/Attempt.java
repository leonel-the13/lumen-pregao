package ao.lumen.pregao.learning.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Instância de um exercício para um utilizador ou equipa, com a semente e os parâmetros guardados. */
@Entity
@Table(name = "attempts")
public class Attempt extends BaseEntity {
    @Column(name = "exercise_version_id")
    private UUID exerciseVersionId;
    @Column(name = "exercise_type")
    private String exerciseType;
    @Column(name = "task_id")
    private UUID taskId;
    @Column(name = "contest_problem_id")
    private UUID contestProblemId;
    @Column(name = "contest_id")
    private UUID contestId;
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "team_id")
    private UUID teamId;
    private long seed;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode params;
    @Enumerated(EnumType.STRING)
    private AttemptStatus status = AttemptStatus.OPEN;
    @Column(name = "attempts_used")
    private int attemptsUsed;
    @Column(name = "hints_used")
    private int hintsUsed;
    private int score;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode draft;
    @Version
    private Long version;

    protected Attempt() {}

    public static Attempt forTask(UUID taskId, UUID userId, UUID versionId, String type, long seed, JsonNode params) {
        Attempt a = new Attempt();
        a.taskId = taskId;
        a.userId = userId;
        a.exerciseVersionId = versionId;
        a.exerciseType = type;
        a.seed = seed;
        a.params = params;
        return a;
    }

    public static Attempt forContest(UUID problemId, UUID contestId, UUID teamId, UUID versionId, String type,
                                     long seed, JsonNode params) {
        Attempt a = new Attempt();
        a.contestProblemId = problemId;
        a.contestId = contestId;
        a.teamId = teamId;
        a.exerciseVersionId = versionId;
        a.exerciseType = type;
        a.seed = seed;
        a.params = params;
        return a;
    }

    public void failedOnce(int maxAttempts) {
        attemptsUsed++;
        if (maxAttempts > 0 && attemptsUsed >= maxAttempts) {
            status = AttemptStatus.FAILED;
            score = 0;
        }
    }

    public void pass(int score) {
        this.status = AttemptStatus.PASSED;
        this.score = Math.max(0, score);
    }

    public void useHint() { hintsUsed++; }
    public void saveDraft(JsonNode draft) { this.draft = draft; }
    public boolean isOpen() { return status == AttemptStatus.OPEN; }
    public boolean isContest() { return contestProblemId != null; }

    public UUID getExerciseVersionId() { return exerciseVersionId; }
    public String getExerciseType() { return exerciseType; }
    public UUID getTaskId() { return taskId; }
    public UUID getContestProblemId() { return contestProblemId; }
    public UUID getContestId() { return contestId; }
    public UUID getUserId() { return userId; }
    public UUID getTeamId() { return teamId; }
    public long getSeed() { return seed; }
    public JsonNode getParams() { return params; }
    public AttemptStatus getStatus() { return status; }
    public int getAttemptsUsed() { return attemptsUsed; }
    public int getHintsUsed() { return hintsUsed; }
    public int getScore() { return score; }
    public JsonNode getDraft() { return draft; }
}
