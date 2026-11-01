package ao.lumen.pregao.learning.infra;

import ao.lumen.pregao.learning.domain.RoomRating;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRatingRepository extends JpaRepository<RoomRating, UUID> {
    Optional<RoomRating> findByUserIdAndRoomId(UUID userId, UUID roomId);

    @Query("select coalesce(avg(r.stars), 0), count(r) from RoomRating r where r.roomId = :roomId")
    List<Object[]> summary(@Param("roomId") UUID roomId);

    @Query("select r from RoomRating r where r.roomId = :roomId and r.comment is not null order by r.updatedAt desc")
    List<RoomRating> recentComments(@Param("roomId") UUID roomId, Pageable page);
}
