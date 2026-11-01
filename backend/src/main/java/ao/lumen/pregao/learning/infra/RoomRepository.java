package ao.lumen.pregao.learning.infra;

import ao.lumen.pregao.learning.domain.Room;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    /** Todas as salas de trilhas visíveis, com módulo e trilha (uma query). */
    @EntityGraph(attributePaths = {"module", "module.track"})
    @Query("select r from Room r where r.module.track.status <> 'DRAFT' order by r.module.track.position, r.module.position, r.position")
    List<Room> findAllVisible();

    @EntityGraph(attributePaths = {"module", "module.track"})
    @Query("select r from Room r where r.module.track.id = :trackId order by r.module.position, r.position")
    List<Room> findByTrack(@Param("trackId") UUID trackId);

    @EntityGraph(attributePaths = {"module", "module.track"})
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findWithModule(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"module", "module.track"})
    List<Room> findByIdIn(Collection<UUID> ids);
}
