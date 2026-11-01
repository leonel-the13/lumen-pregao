package ao.lumen.pregao.contest.infra;

import ao.lumen.pregao.contest.domain.TeamMember;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {
    Optional<TeamMember> findByContestIdAndUserId(UUID contestId, UUID userId);

    List<TeamMember> findByTeamIdOrderByCreatedAtAsc(UUID teamId);

    List<TeamMember> findByTeamIdIn(Collection<UUID> teamIds);

    List<TeamMember> findByUserId(UUID userId);

    long countByTeamId(UUID teamId);

    @Query("select count(m) from TeamMember m where m.teamId = :teamId and m.confirmedAt is not null")
    long countConfirmed(@Param("teamId") UUID teamId);

    @Modifying
    @Query("delete from TeamMember m where m.teamId = :teamId")
    void deleteByTeamId(@Param("teamId") UUID teamId);
}
