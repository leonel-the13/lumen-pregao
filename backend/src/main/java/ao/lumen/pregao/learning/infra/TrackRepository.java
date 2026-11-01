package ao.lumen.pregao.learning.infra;

import ao.lumen.pregao.learning.domain.Track;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TrackRepository extends JpaRepository<Track, UUID> {
    @Query("select t from Track t where t.status <> 'DRAFT' order by t.position")
    List<Track> findVisible();

    Optional<Track> findBySlug(String slug);
}
