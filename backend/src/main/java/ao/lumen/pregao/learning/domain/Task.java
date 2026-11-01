package ao.lumen.pregao.learning.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class Task extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id")
    private Room room;
    private int position;
    private String title;
    @Column(name = "exercise_version_id")
    private UUID exerciseVersionId;
    private int points;

    protected Task() {}

    public Task(Room room, int position, String title, UUID exerciseVersionId, int points) {
        this.room = room;
        this.position = position;
        this.title = title;
        this.exerciseVersionId = exerciseVersionId;
        this.points = points;
    }

    public void edit(String title, int points, UUID exerciseVersionId) {
        this.title = title;
        this.points = points;
        this.exerciseVersionId = exerciseVersionId;
    }

    public Room getRoom() { return room; }
    public int getPosition() { return position; }
    public String getTitle() { return title; }
    public UUID getExerciseVersionId() { return exerciseVersionId; }
    public int getPoints() { return points; }
}
