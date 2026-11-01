package ao.lumen.pregao.ranking.infra;

import ao.lumen.pregao.ranking.domain.Season;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeasonRepository extends JpaRepository<Season, UUID> {
    @Query("select s from Season s where s.startsAt <= :at and s.endsAt > :at")
    Optional<Season> findAt(@Param("at") Instant at);

    List<Season> findAllByOrderByStartsAtDesc();
}
