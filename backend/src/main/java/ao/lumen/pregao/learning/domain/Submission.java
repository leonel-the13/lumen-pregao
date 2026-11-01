package ao.lumen.pregao.learning.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Resposta enviada. `receivedAt` é sempre a hora do servidor. */
@Entity
@Table(name = "submissions")
public class Submission extends BaseEntity {
    @Column(name = "attempt_id")
    private UUID attemptId;
    @Column(name = "idempotency_key")
    private String idempotencyKey;
    @Column(name = "submitted_by")
    private UUID submittedBy;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode payload;
    @Column(name = "received_at")
    private Instant receivedAt;
    @Enumerated(EnumType.STRING)
    private SubmissionStatus status;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode result;
    private Boolean correct;
    private Integer points;
    @Column(name = "error_code")
    private String errorCode;
    private int retries;
    @Column(name = "processing_started_at")
    private Instant processingStartedAt;
    @Column(name = "graded_at")
    private Instant gradedAt;

    protected Submission() {}

    public void graded(boolean correct, int points, JsonNode result, Instant at) {
        this.status = SubmissionStatus.GRADED;
        this.correct = correct;
        this.points = points;
        this.result = result;
        this.gradedAt = at;
        this.errorCode = null;
    }

    public void rejected(String code, JsonNode result, Instant at) {
        this.status = SubmissionStatus.REJECTED;
        this.correct = false;
        this.points = 0;
        this.errorCode = code;
        this.result = result;
        this.gradedAt = at;
    }

    public UUID getAttemptId() { return attemptId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public UUID getSubmittedBy() { return submittedBy; }
    public JsonNode getPayload() { return payload; }
    public Instant getReceivedAt() { return receivedAt; }
    public SubmissionStatus getStatus() { return status; }
    public JsonNode getResult() { return result; }
    public Boolean getCorrect() { return correct; }
    public Integer getPoints() { return points; }
    public String getErrorCode() { return errorCode; }
    public int getRetries() { return retries; }
    public Instant getGradedAt() { return gradedAt; }
}
