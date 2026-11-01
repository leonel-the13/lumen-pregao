package ao.lumen.pregao.authoring.infra;

import ao.lumen.pregao.authoring.domain.LegalSource;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalSourceRepository extends JpaRepository<LegalSource, UUID> {
    List<LegalSource> findAllByOrderByTitleAsc();
}
