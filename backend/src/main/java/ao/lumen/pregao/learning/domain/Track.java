package ao.lumen.pregao.learning.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tracks")
public class Track extends BaseEntity {
    private String slug;
    private String title;
    private String summary;
    private String description;
    private int difficulty;
    private String topic;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> courses = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> competencies = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "completion_rule")
    private JsonNode completionRule;
    @Column(name = "est_hours")
    private Integer estHours;
    private int position;
    @Enumerated(EnumType.STRING)
    private ContentStatus status;
    @OneToMany(mappedBy = "track")
    @OrderBy("position")
    private List<TrackModule> modules = new ArrayList<>();

    protected Track() {}

    public Track(String slug, String title, String summary, String description, int difficulty, String topic,
                 List<String> courses, List<String> competencies, JsonNode completionRule, Integer estHours,
                 int position, ContentStatus status) {
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.description = description;
        this.difficulty = difficulty;
        this.topic = topic;
        this.courses = courses;
        this.competencies = competencies;
        this.completionRule = completionRule;
        this.estHours = estHours;
        this.position = position;
        this.status = status;
    }

    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public int getDifficulty() { return difficulty; }
    public String getTopic() { return topic; }
    public List<String> getCourses() { return courses; }
    public List<String> getCompetencies() { return competencies; }
    public JsonNode getCompletionRule() { return completionRule; }
    public Integer getEstHours() { return estHours; }
    public int getPosition() { return position; }
    public ContentStatus getStatus() { return status; }
    public List<TrackModule> getModules() { return modules; }
}
