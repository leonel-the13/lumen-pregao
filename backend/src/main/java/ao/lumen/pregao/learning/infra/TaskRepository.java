package ao.lumen.pregao.learning.infra;

import ao.lumen.pregao.learning.domain.Task;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    @Query("select t from Task t where t.room.id in :roomIds order by t.position")
    List<Task> findByRoomIds(@Param("roomIds") Collection<UUID> roomIds);

    @EntityGraph(attributePaths = {"room", "room.module", "room.module.track"})
    @Query("select t from Task t where t.id = :id")
    Optional<Task> findWithRoom(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"room", "room.module", "room.module.track"})
    List<Task> findByIdIn(Collection<UUID> ids);
}
