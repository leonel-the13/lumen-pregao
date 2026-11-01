package ao.lumen.pregao.exam.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "exams")
public class Exam extends BaseEntity {
    private String slug;
    private String title;
    @Column(name = "module_id")
    private UUID moduleId;
    @Column(name = "duration_min")
    private int durationMin;
    /** {"topic": "IVA", "count": 20, "types": ["A","B"]} */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_pool_rule")
    private JsonNode questionPoolRule;
    @Column(name = "pass_mark")
    private BigDecimal passMark;
    private String status;

    protected Exam() {}

    public Exam(String slug, String title, UUID moduleId, int durationMin, JsonNode rule, BigDecimal passMark,
                String status) {
        this.slug = slug;
        this.title = title;
        this.moduleId = moduleId;
        this.durationMin = durationMin;
        this.questionPoolRule = rule;
        this.passMark = passMark;
        this.status = status;
    }

    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public UUID getModuleId() { return moduleId; }
    public int getDurationMin() { return durationMin; }
    public JsonNode getQuestionPoolRule() { return questionPoolRule; }
    public BigDecimal getPassMark() { return passMark; }
    public String getStatus() { return status; }
}
