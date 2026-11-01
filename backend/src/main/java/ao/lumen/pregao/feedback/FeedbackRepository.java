package ao.lumen.pregao.feedback;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {
    @Query("select f from Feedback f where (:status is null or f.status = :status) and (:cursor is null or f.id < :cursor) order by f.id desc")
    List<Feedback> page(@Param("status") String status, @Param("cursor") UUID cursor, Pageable page);
}
