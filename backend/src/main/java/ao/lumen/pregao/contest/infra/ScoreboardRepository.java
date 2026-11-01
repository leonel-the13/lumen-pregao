package ao.lumen.pregao.contest.infra;

import ao.lumen.pregao.contest.domain.Scoreboard;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScoreboardRepository extends JpaRepository<Scoreboard, UUID> {
    List<Scoreboard> findByContestId(UUID contestId);

    Optional<Scoreboard> findByContestIdAndParticipantTypeAndParticipantId(UUID contestId, String type, UUID id);

    @Modifying
    @Query("delete from Scoreboard s where s.contestId = :contestId and s.participantId = :teamId")
    void deleteForTeam(@Param("contestId") UUID contestId, @Param("teamId") UUID teamId);
}
