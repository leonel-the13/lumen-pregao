package ao.lumen.pregao.learning.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** Avaliação de uma sala por um estudante (1–5 estrelas e comentário opcional). */
@Entity
@Table(name = "room_ratings")
public class RoomRating extends BaseEntity {
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "room_id")
    private UUID roomId;
    private int stars;
    private String comment;

    protected RoomRating() {}

    public RoomRating(UUID userId, UUID roomId) {
        this.userId = userId;
        this.roomId = roomId;
    }

    public void rate(int stars, String comment) {
        this.stars = stars;
        this.comment = comment;
    }

    public UUID getUserId() { return userId; }
    public UUID getRoomId() { return roomId; }
    public int getStars() { return stars; }
    public String getComment() { return comment; }
}
