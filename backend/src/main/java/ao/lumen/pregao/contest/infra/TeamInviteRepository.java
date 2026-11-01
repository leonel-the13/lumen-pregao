package ao.lumen.pregao.contest.infra;

import ao.lumen.pregao.contest.domain.TeamInvite;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamInviteRepository extends JpaRepository<TeamInvite, UUID> {
    List<TeamInvite> findByTeamIdAndStatus(UUID teamId, String status);

    List<TeamInvite> findByInviteeUserIdAndStatus(UUID userId, String status);

    Optional<TeamInvite> findFirstByTeamIdAndInviteeUserIdAndStatus(UUID teamId, UUID userId, String status);

    @Modifying
    @Query("delete from TeamInvite i where i.teamId = :teamId")
    void deleteByTeamId(@Param("teamId") UUID teamId);
}
