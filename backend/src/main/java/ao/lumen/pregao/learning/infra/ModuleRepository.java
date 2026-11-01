package ao.lumen.pregao.learning.infra;

import ao.lumen.pregao.learning.domain.TrackModule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleRepository extends JpaRepository<TrackModule, UUID> {
    List<TrackModule> findByTrackIdOrderByPositionAsc(UUID trackId);
}
