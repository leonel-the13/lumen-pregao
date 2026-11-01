package ao.lumen.pregao.exam.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Exame de um utilizador. `endsAt` é fixado pelo servidor no início e nunca muda. */
@Entity
@Table(name = "exam_sessions")
public class ExamSession extends BaseEntity {
    @Column(name = "exam_id")
    private UUID examId;
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "ends_at")
    private Instant endsAt;
    @Column(name = "submitted_at")
    private Instant submittedAt;
    @Enumerated(EnumType.STRING)
    private ExamSessionStatus status;
    @Column(name = "submit_reason")
    private String submitReason;
    /** [{n, versionId, seed, params}] */
    @JdbcTypeCode(SqlTypes.JSON)
    private ArrayNode questions;
    @JdbcTypeCode(SqlTypes.JSON)
    private ObjectNode answers;
    @JdbcTypeCode(SqlTypes.JSON)
    private ArrayNode marked;
    private BigDecimal grade;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode report;
    @Version
    private Long version;

    protected ExamSession() {}

    public ExamSession(UUID examId, UUID userId, Instant startedAt, Instant endsAt, ArrayNode questions,
                       ObjectNode answers, ArrayNode marked) {
        this.examId = examId;
        this.userId = userId;
        this.startedAt = startedAt;
        this.endsAt = endsAt;
        this.questions = questions;
        this.answers = answers;
        this.marked = marked;
        this.status = ExamSessionStatus.IN_PROGRESS;
    }

    public void submit(Instant at, boolean auto) {
        this.status = auto ? ExamSessionStatus.EXPIRED_AUTO_SUBMITTED : ExamSessionStatus.SUBMITTED;
        this.submittedAt = at;
        this.submitReason = auto ? "AUTO" : "USER";
    }

    public void graded(BigDecimal grade, JsonNode report) {
        this.grade = grade;
        this.report = report;
        this.status = ExamSessionStatus.GRADED;
    }

    public UUID getExamId() { return examId; }
    public UUID getUserId() { return userId; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndsAt() { return endsAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public ExamSessionStatus getStatus() { return status; }
    public String getSubmitReason() { return submitReason; }
    public ArrayNode getQuestions() { return questions; }
    public ObjectNode getAnswers() { return answers; }
    public ArrayNode getMarked() { return marked; }
    public BigDecimal getGrade() { return grade; }
    public JsonNode getReport() { return report; }
}
