package ao.lumen.pregao.contest.infra;

import ao.lumen.pregao.contest.domain.ContestProblem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContestProblemRepository extends JpaRepository<ContestProblem, UUID> {
    List<ContestProblem> findByContestIdOrderByPositionAsc(UUID contestId);

    Optional<ContestProblem> findByContestIdAndLabel(UUID contestId, String label);

    @Modifying
    @Query("delete from ContestProblem p where p.contestId = :contestId")
    void deleteByContestId(@Param("contestId") UUID contestId);
}
