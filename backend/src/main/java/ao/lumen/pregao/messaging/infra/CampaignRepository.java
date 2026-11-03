package ao.lumen.pregao.messaging.infra;

import ao.lumen.pregao.messaging.domain.Campaign;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {
    Optional<Campaign> findByIdempotencyKey(String key);

    @Query("select c from Campaign c where (:cursor is null or c.id < :cursor) order by c.id desc")
    List<Campaign> page(@Param("cursor") UUID cursor, Pageable page);
}
