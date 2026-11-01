package ao.lumen.pregao.ranking.infra;

import ao.lumen.pregao.ranking.domain.PointEvent;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointEventRepository extends JpaRepository<PointEvent, UUID> {

    /** Inserção idempotente: (user, source, sourceId) único. Devolve 1 se inseriu. */
    @Modifying
    @Query(value = """
            insert into point_events (id, user_id, team_id, source, source_id, delta, at)
            values (:id, :userId, :teamId, :source, :sourceId, :delta, :at)
            on conflict (user_id, source, source_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("userId") UUID userId, @Param("teamId") UUID teamId,
                       @Param("source") String source, @Param("sourceId") String sourceId,
                       @Param("delta") int delta, @Param("at") Instant at);

    @Query("select coalesce(sum(p.delta), 0) from PointEvent p where p.userId = :userId")
    long total(@Param("userId") UUID userId);

    @Query("select coalesce(sum(p.delta), 0) from PointEvent p where p.userId = :userId and p.at >= :since")
    long totalSince(@Param("userId") UUID userId, @Param("since") Instant since);

    @Query("""
            select p.userId, coalesce(sum(p.delta), 0) from PointEvent p
            where p.userId in :ids and p.at >= :since group by p.userId
            """)
    List<Object[]> totalsSince(@Param("ids") Collection<UUID> ids, @Param("since") Instant since);

    @Query("select p.userId, sum(p.delta) from PointEvent p group by p.userId")
    List<Object[]> totalsByUser();

    @Query("""
            select p.userId, sum(p.delta) from PointEvent p
            where p.at >= :from and p.at < :to group by p.userId
            """)
    List<Object[]> totalsByUserBetween(@Param("from") Instant from, @Param("to") Instant to);

    List<PointEvent> findByUserIdOrderByAtDesc(UUID userId, Pageable page);

    List<PointEvent> findByUserIdOrderByAtAsc(UUID userId);
}
