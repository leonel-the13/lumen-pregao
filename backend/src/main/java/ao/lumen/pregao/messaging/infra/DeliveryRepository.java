package ao.lumen.pregao.messaging.infra;

import ao.lumen.pregao.messaging.domain.Delivery;
import ao.lumen.pregao.messaging.domain.DeliveryStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    @Query("select d.id from Delivery d where d.status = 'PENDING' order by d.createdAt, d.id")
    List<UUID> pendingIds(Pageable page);

    @Query("select d.campaignId, d.status, count(d) from Delivery d where d.campaignId in :ids group by d.campaignId, d.status")
    List<Object[]> countsByCampaign(@Param("ids") Collection<UUID> ids);

    @Modifying
    @Query("update Delivery d set d.status = 'PENDING' where d.campaignId = :id and d.status = 'FAILED'")
    int retryFailed(@Param("id") UUID campaignId);

    long countByCampaignIdAndStatus(UUID campaignId, DeliveryStatus status);
}
