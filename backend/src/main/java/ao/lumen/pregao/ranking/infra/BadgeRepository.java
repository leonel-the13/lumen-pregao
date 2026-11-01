package ao.lumen.pregao.ranking.infra;

import ao.lumen.pregao.ranking.domain.Badge;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeRepository extends JpaRepository<Badge, UUID> {
    Optional<Badge> findByCode(String code);
}
