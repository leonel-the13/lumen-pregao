package ao.lumen.pregao.learning.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/** Módulo de uma trilha (tabela `modules`; o nome evita conflito com java.lang.Module). */
@Entity
@Table(name = "modules")
public class TrackModule extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "track_id")
    private Track track;
    private int position;
    private String title;
    private String summary;
    @OneToMany(mappedBy = "module")
    @OrderBy("position")
    private List<Room> rooms = new ArrayList<>();

    protected TrackModule() {}

    public TrackModule(Track track, int position, String title, String summary) {
        this.track = track;
        this.position = position;
        this.title = title;
        this.summary = summary;
    }

    public Track getTrack() { return track; }
    public int getPosition() { return position; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public List<Room> getRooms() { return rooms; }
}
