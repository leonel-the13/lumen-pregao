package ao.lumen.pregao.ranking.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "badges")
public class Badge extends BaseEntity {
    private String code;
    private String title;
    private String description;
    private int points;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode rule;

    protected Badge() {}

    public String getCode() { return code; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getPoints() { return points; }
    public JsonNode getRule() { return rule; }
}
