package ao.lumen.pregao.contest.app;

import ao.lumen.pregao.contest.domain.TeamMember;
import ao.lumen.pregao.contest.infra.ContestRepository;
import ao.lumen.pregao.contest.infra.TeamMemberRepository;
import ao.lumen.pregao.contest.infra.TeamRepository;
import ao.lumen.pregao.learning.api.LearningController.SubmissionVisibility;
import ao.lumen.pregao.learning.domain.Attempt;
import ao.lumen.pregao.ranking.app.TeamDirectory;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Serviços que o contest disponibiliza a outros módulos (ranking de equipas, visibilidade de submissões). */
@Component
class ContestIntegrations implements TeamDirectory, SubmissionVisibility {
    private final TeamRepository teams;
    private final TeamMemberRepository members;
    private final ContestRepository contests;

    ContestIntegrations(TeamRepository teams, TeamMemberRepository members, ContestRepository contests) {
        this.teams = teams;
        this.members = members;
        this.contests = contests;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, TeamInfo> teams(Collection<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        var ts = teams.findByIdIn(ids);
        var ms = members.findByTeamIdIn(ids).stream().collect(Collectors.groupingBy(TeamMember::getTeamId,
                Collectors.mapping(TeamMember::getUserId, Collectors.toSet())));
        var titles = contests.findAllById(ts.stream().map(t -> t.getContestId()).distinct().toList()).stream()
                .collect(Collectors.toMap(c -> c.getId(), c -> c.getTitle()));
        return ts.stream().collect(Collectors.toMap(t -> t.getId(), t -> new TeamInfo(t.getId(), t.getName(),
                titles.getOrDefault(t.getContestId(), ""), ms.getOrDefault(t.getId(), java.util.Set.of()))));
    }

    /** Um membro da equipa pode ver as submissões da sua equipa (R-AUTH-2). */
    @Override
    @Transactional(readOnly = true)
    public boolean canSee(UUID userId, Attempt attempt) {
        if (attempt.getTeamId() == null || attempt.getContestId() == null) return false;
        return members.findByContestIdAndUserId(attempt.getContestId(), userId)
                .map(m -> m.getTeamId().equals(attempt.getTeamId())).orElse(false);
    }
}
