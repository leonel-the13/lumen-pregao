package ao.lumen.pregao.ranking.infra;

import ao.lumen.pregao.ranking.domain.UserBadge;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {
    @EntityGraph(attributePaths = "badge")
    List<UserBadge> findByUserIdOrderByAwardedAtDesc(UUID userId);

    @Modifying
    @Query(value = """
            insert into user_badges (id, user_id, badge_id, awarded_at) values (:id, :userId, :badgeId, now())
            on conflict (user_id, badge_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("userId") UUID userId, @Param("badgeId") UUID badgeId);
}
