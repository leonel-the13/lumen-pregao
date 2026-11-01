package ao.lumen.pregao.learning.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "rooms")
public class Room extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id")
    private TrackModule module;
    private int position;
    private String title;
    private String description;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> objectives = new ArrayList<>();
    private int difficulty;
    @Column(name = "est_minutes")
    private int estMinutes;
    private String context;
    /** Lista de {title, kind, body} (markdown) — materiais de apoio da sala. */
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode materials;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<UUID> prerequisites = new ArrayList<>();
    @Enumerated(EnumType.STRING)
    private ContentStatus status;
    @OneToMany(mappedBy = "room")
    @OrderBy("position")
    private List<Task> tasks = new ArrayList<>();

    protected Room() {}

    public Room(TrackModule module, int position, String title, String description, List<String> objectives,
                int difficulty, int estMinutes, String context, JsonNode materials, List<UUID> prerequisites,
                ContentStatus status) {
        this.module = module;
        this.position = position;
        this.title = title;
        this.description = description;
        this.objectives = objectives;
        this.difficulty = difficulty;
        this.estMinutes = estMinutes;
        this.context = context;
        this.materials = materials;
        this.prerequisites = prerequisites;
        this.status = status;
    }

    public TrackModule getModule() { return module; }
    public int getPosition() { return position; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<String> getObjectives() { return objectives; }
    public int getDifficulty() { return difficulty; }
    public int getEstMinutes() { return estMinutes; }
    public String getContext() { return context; }
    public JsonNode getMaterials() { return materials; }
    public List<UUID> getPrerequisites() { return prerequisites; }
    public ContentStatus getStatus() { return status; }
    public List<Task> getTasks() { return tasks; }
}
