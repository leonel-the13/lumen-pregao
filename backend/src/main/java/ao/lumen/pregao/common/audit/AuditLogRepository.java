package ao.lumen.pregao.common.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
            select a from AuditLog a
            where (:actor is null or a.actorId = :actor)
              and (:entity is null or a.entityType = :entity)
              and (cast(:from as timestamp) is null or a.at >= :from)
              and (cast(:to as timestamp) is null or a.at < :to)
              and (:cursor is null or a.id < :cursor)
            order by a.id desc
            """)
    List<AuditLog> search(@Param("actor") UUID actor, @Param("entity") String entity,
                          @Param("from") Instant from, @Param("to") Instant to,
                          @Param("cursor") UUID cursor, Pageable page);
}
