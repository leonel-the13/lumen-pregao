package ao.lumen.pregao.learning.infra;

import ao.lumen.pregao.learning.domain.Attempt;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttemptRepository extends JpaRepository<Attempt, UUID> {
    Optional<Attempt> findByTaskIdAndUserId(UUID taskId, UUID userId);

    @Query("select a from Attempt a where a.userId = :userId and a.taskId in :taskIds")
    List<Attempt> findForUserTasks(@Param("userId") UUID userId, @Param("taskIds") Collection<UUID> taskIds);

    @Query("select a from Attempt a where a.userId = :userId and a.taskId is not null")
    List<Attempt> findAllTaskAttempts(@Param("userId") UUID userId);

    Optional<Attempt> findByContestProblemIdAndTeamId(UUID problemId, UUID teamId);

    List<Attempt> findByContestIdAndTeamId(UUID contestId, UUID teamId);

    List<Attempt> findByUserId(UUID userId);

    @Query("select a.taskId, count(a) from Attempt a where a.taskId in :taskIds group by a.taskId")
    List<Object[]> countByTasks(@Param("taskIds") Collection<UUID> taskIds);
}
