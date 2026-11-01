package ao.lumen.pregao.contest.infra;

import ao.lumen.pregao.contest.domain.Contest;
import ao.lumen.pregao.contest.domain.ContestState;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContestRepository extends JpaRepository<Contest, UUID> {
    List<Contest> findByStateNotOrderByStartsAtDesc(ContestState state);

    List<Contest> findAllByOrderByStartsAtDesc();

    List<Contest> findByStateIn(Collection<ContestState> states);

    boolean existsBySlug(String slug);
}
