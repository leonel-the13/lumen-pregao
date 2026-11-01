package ao.lumen.pregao.contest.infra;

import ao.lumen.pregao.contest.domain.Team;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRepository extends JpaRepository<Team, UUID> {
    List<Team> findByContestId(UUID contestId);

    List<Team> findByIdIn(Collection<UUID> ids);

    /** Incrementa a versão no commit: juntar membros em paralelo dá um sucesso e um 409 (CON-03). */
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select t from Team t where t.id = :id")
    Optional<Team> findForUpdate(@Param("id") UUID id);

    boolean existsByContestIdAndNameIgnoreCase(UUID contestId, String name);
}
