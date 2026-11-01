package ao.lumen.pregao.contest.app;

import ao.lumen.pregao.common.audit.AuditService;
import ao.lumen.pregao.contest.domain.Contest;
import ao.lumen.pregao.contest.domain.ContestProblem;
import ao.lumen.pregao.contest.domain.Scoreboard;
import ao.lumen.pregao.contest.domain.Team;
import ao.lumen.pregao.contest.infra.ContestProblemRepository;
import ao.lumen.pregao.contest.infra.ContestRepository;
import ao.lumen.pregao.contest.infra.ScoreboardRepository;
import ao.lumen.pregao.contest.infra.TeamRepository;
import ao.lumen.pregao.learning.app.ContestScoring;
import ao.lumen.pregao.ranking.app.RankingProjection;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Actualiza o Scoreboard (com @Version) na transacção da correcção.
 * Penalização: minutos desde startsAt até à resposta certa + N min por submissão errada anterior (spec §7).
 */
@Component
public class ContestScoringImpl implements ContestScoring {
    private final ContestRepository contests;
    private final ContestProblemRepository problems;
    private final ScoreboardRepository scoreboards;
    private final TeamRepository teams;
    private final RankingProjection projection;
    private final AuditService audit;
    private final ApplicationEventPublisher events;

    public ContestScoringImpl(ContestRepository contests, ContestProblemRepository problems,
                              ScoreboardRepository scoreboards, TeamRepository teams, RankingProjection projection,
                              AuditService audit, ApplicationEventPublisher events) {
        this.contests = contests;
        this.problems = problems;
        this.scoreboards = scoreboards;
        this.teams = teams;
        this.projection = projection;
        this.audit = audit;
        this.events = events;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int onGraded(UUID contestId, UUID problemId, UUID teamId, Instant receivedAt, boolean correct, int wrongBefore) {
        if (!correct) return 0;
        Contest c = contests.findById(contestId).orElseThrow();
        ContestProblem p = problems.findById(problemId).orElseThrow();
        Scoreboard s = scoreboards.findByContestIdAndParticipantTypeAndParticipantId(contestId, "TEAM", teamId)
                .orElseGet(() -> scoreboards.save(new Scoreboard(contestId, teamId)));
        int minutes = (int) Math.max(0, Duration.between(c.getStartsAt(), receivedAt).toMinutes());
        int penalty = minutes + wrongBefore * c.getWrongPenaltyMinutes();
        var before = Map.of("points", s.getPoints(), "penaltyMinutes", s.getPenaltyMinutes());
        s.solved(p.getPoints(), penalty, receivedAt);
        audit.record(null, "SCOREBOARD_UPDATED", "Scoreboard", s.getId(), before,
                Map.of("points", s.getPoints(), "penaltyMinutes", s.getPenaltyMinutes(), "problem", p.getLabel()));
        events.publishEvent(new ContestEvents.ScoreboardChanged(contestId, teamId, s.getPoints()));
        return p.getPoints();
    }

    /** Projecção do ranking de equipas no Redis (depois do commit). */
    @TransactionalEventListener
    public void project(ContestEvents.ScoreboardChanged e) {
        Contest c = contests.findById(e.contestId()).orElse(null);
        Team t = teams.findById(e.teamId()).orElse(null);
        if (c == null || t == null || t.isIndividual()) return;
        projection.setTeamScoreAt(c.getStartsAt(), t.getId(), e.points());
    }
}
