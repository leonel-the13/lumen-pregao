package ao.lumen.pregao.contest.app;

import ao.lumen.pregao.authoring.app.ExerciseCatalog;
import ao.lumen.pregao.common.audit.AuditService;
import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.common.security.SessionUser;
import ao.lumen.pregao.common.text.MarkdownSanitizer;
import ao.lumen.pregao.contest.api.ContestDtos.AdminContestRow;
import ao.lumen.pregao.contest.api.ContestDtos.ContestInput;
import ao.lumen.pregao.contest.api.ContestDtos.ProblemInput;
import ao.lumen.pregao.contest.domain.Contest;
import ao.lumen.pregao.contest.domain.ContestProblem;
import ao.lumen.pregao.contest.domain.ContestState;
import ao.lumen.pregao.contest.domain.Scoreboard;
import ao.lumen.pregao.contest.domain.TeamMember;
import ao.lumen.pregao.contest.infra.ContestProblemRepository;
import ao.lumen.pregao.contest.infra.ContestRepository;
import ao.lumen.pregao.contest.infra.ScoreboardRepository;
import ao.lumen.pregao.contest.infra.TeamMemberRepository;
import ao.lumen.pregao.contest.infra.TeamRepository;
import ao.lumen.pregao.learning.infra.SubmissionRepository;
import ao.lumen.pregao.ranking.app.BadgeService;
import ao.lumen.pregao.ranking.app.PointService;
import ao.lumen.pregao.ranking.domain.PointSource;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestão de contests pelo organizador (spec §6.4). */
@Service
public class ContestAdminService {
    private final ContestRepository contests;
    private final ContestProblemRepository problems;
    private final TeamRepository teams;
    private final TeamMemberRepository members;
    private final ScoreboardRepository scoreboards;
    private final SubmissionRepository submissions;
    private final ExerciseCatalog catalog;
    private final PointService points;
    private final BadgeService badges;
    private final AuditService audit;
    private final ApplicationEventPublisher events;

    public ContestAdminService(ContestRepository contests, ContestProblemRepository problems, TeamRepository teams,
                               TeamMemberRepository members, ScoreboardRepository scoreboards,
                               SubmissionRepository submissions, ExerciseCatalog catalog, PointService points,
                               BadgeService badges, AuditService audit, ApplicationEventPublisher events) {
        this.contests = contests;
        this.problems = problems;
        this.teams = teams;
        this.members = members;
        this.scoreboards = scoreboards;
        this.submissions = submissions;
        this.catalog = catalog;
        this.points = points;
        this.badges = badges;
        this.audit = audit;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<AdminContestRow> list() {
        long pending = submissions.pendingCount();
        return contests.findAllByOrderByStartsAtDesc().stream().map(c -> new AdminContestRow(c.getId(), c.getSlug(),
                c.getTitle(), c.getMode(), c.getState(), c.getStartsAt(), c.getEndsAt(), teams.findByContestId(c.getId()).size(),
                problems.findByContestIdOrderByPositionAsc(c.getId()).size(), pending)).toList();
    }

    @Transactional
    public AdminContestRow create(SessionUser me, ContestInput in) {
        if (contests.existsBySlug(in.slug())) throw DomainException.conflict("SLUG_TAKEN", "Já existe um contest com este identificador.");
        Contest c = new Contest(in.slug(), me.id());
        define(c, in);
        contests.save(c);
        replaceProblems(c, in.problems());
        audit.record(me.id(), "CONTEST_CREATED", "Contest", c.getId(), null, Map.of("slug", in.slug()));
        return row(c);
    }

    @Transactional
    public AdminContestRow update(SessionUser me, UUID id, ContestInput in) {
        Contest c = load(id);
        define(c, in);
        if (in.problems() != null) replaceProblems(c, in.problems());
        audit.record(me.id(), "CONTEST_EDITED", "Contest", id, null, Map.of("slug", c.getSlug()));
        return row(c);
    }

    /** DRAFT → REGISTRATION: datas válidas e problemas aprovados (publicados). */
    @Transactional
    public AdminContestRow publish(SessionUser me, UUID id) {
        Contest c = load(id);
        if (c.getState() != ContestState.DRAFT) throw DomainException.conflict("INVALID_TRANSITION", "O contest já foi publicado.");
        Contest.validateDates(c.getRegistrationOpensAt(), c.getRegistrationClosesAt(), c.getStartsAt(), c.getFreezeAt(), c.getEndsAt());
        var ps = problems.findByContestIdOrderByPositionAsc(id);
        if (ps.isEmpty()) throw DomainException.rule("NO_PROBLEMS", "Acrescenta pelo menos um problema antes de publicar.");
        var published = catalog.published(ps.stream().map(ContestProblem::getExerciseVersionId).toList());
        if (ps.stream().anyMatch(p -> !published.contains(p.getExerciseVersionId()))) {
            throw DomainException.rule("PROBLEMS_NOT_APPROVED", "Todos os problemas têm de usar exercícios publicados.");
        }
        transition(me, c, ContestState.REGISTRATION);
        return row(c);
    }

    /** Publica os resultados: substitui a fotografia congelada, atribui pontos e conquistas. */
    @Transactional
    public AdminContestRow publishResults(SessionUser me, UUID id) {
        Contest c = load(id);
        boolean noFinal = c.getFinalPhase() == null || c.getFinalPhase().isEmpty();
        boolean allowed = c.getState() == ContestState.FINAL_EVALUATION
                || (c.getState() == ContestState.SUBMISSIONS_CLOSED && noFinal);
        if (!allowed) throw DomainException.conflict("INVALID_TRANSITION", "Os resultados só podem ser publicados depois do fim do contest.");
        if (submissions.pendingCount() > 0) {
            throw DomainException.conflict("GRADING_PENDING", "Ainda há submissões por corrigir. Espera que a fila esvazie.");
        }
        var ranked = scoreboards.findByContestId(id).stream()
                .sorted(Comparator.comparingInt((Scoreboard s) -> -s.getPoints()).thenComparingInt(Scoreboard::getPenaltyMinutes)
                        .thenComparing(Scoreboard::getLastCorrectAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        var byTeam = members.findByTeamIdIn(ranked.stream().map(Scoreboard::getParticipantId).toList()).stream()
                .collect(Collectors.groupingBy(TeamMember::getTeamId));
        for (int i = 0; i < ranked.size(); i++) {
            Scoreboard s = ranked.get(i);
            for (TeamMember m : byTeam.getOrDefault(s.getParticipantId(), List.of())) {
                if (s.getPoints() > 0) {
                    // Pontos de contest no ranking geral = pontos da equipa (valor a confirmar, spec §7).
                    points.award(m.getUserId(), s.getParticipantId(), PointSource.CONTEST, "contest:" + id, s.getPoints());
                    if (i < 3) badges.award(m.getUserId(), "PODIO");
                }
            }
        }
        transition(me, c, ContestState.RESULTS_PUBLISHED);
        return row(c);
    }

    @Transactional
    public AdminContestRow close(SessionUser me, UUID id) {
        Contest c = load(id);
        if (c.getState() != ContestState.RESULTS_PUBLISHED) throw DomainException.conflict("INVALID_TRANSITION", "Publica primeiro os resultados.");
        transition(me, c, ContestState.CLOSED);
        return row(c);
    }

    private void transition(SessionUser me, Contest c, ContestState to) {
        ContestState from = c.getState();
        c.moveTo(to);
        audit.record(me.id(), "CONTEST_" + to, "Contest", c.getId(), Map.of("state", from), Map.of("state", to));
        events.publishEvent(new ContestEvents.StateChanged(c.getId(), to));
    }

    private void define(Contest c, ContestInput in) {
        c.define(in.title(), MarkdownSanitizer.clean(in.description() == null ? "" : in.description()), in.mode(),
                in.minTeam(), in.maxTeam(), in.registrationOpensAt(), in.registrationClosesAt(), in.startsAt(),
                in.freezeAt(), in.endsAt(), in.lockMinutes(), in.wrongPenaltyMinutes(), in.finalPhase());
    }

    private void replaceProblems(Contest c, List<ProblemInput> in) {
        if (in == null) return;
        if (new HashSet<>(in.stream().map(ProblemInput::label).toList()).size() != in.size()) {
            throw DomainException.badRequest("DUPLICATE_LABEL", "Há problemas com a mesma letra.");
        }
        problems.deleteByContestId(c.getId());
        problems.flush();
        int pos = 0;
        for (ProblemInput p : in) {
            catalog.definition(p.exerciseVersionId());
            problems.save(new ContestProblem(c.getId(), p.label(), p.title(), p.companyName(), p.exerciseVersionId(),
                    p.points(), pos++));
        }
    }

    private Contest load(UUID id) {
        return contests.findById(id).orElseThrow(() -> DomainException.notFound("CONTEST_NOT_FOUND", "O contest não existe."));
    }

    private AdminContestRow row(Contest c) {
        return new AdminContestRow(c.getId(), c.getSlug(), c.getTitle(), c.getMode(), c.getState(), c.getStartsAt(),
                c.getEndsAt(), teams.findByContestId(c.getId()).size(),
                problems.findByContestIdOrderByPositionAsc(c.getId()).size(), submissions.pendingCount());
    }
}
