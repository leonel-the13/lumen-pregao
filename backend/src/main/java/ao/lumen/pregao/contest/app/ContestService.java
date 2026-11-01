package ao.lumen.pregao.contest.app;

import ao.lumen.pregao.authoring.app.ExerciseCatalog;
import ao.lumen.pregao.common.audit.AuditService;
import ao.lumen.pregao.common.config.AppProperties;
import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.common.security.RateLimiter;
import ao.lumen.pregao.common.security.Roles;
import ao.lumen.pregao.common.security.SessionUser;
import ao.lumen.pregao.common.text.OffensiveFilter;
import ao.lumen.pregao.contest.api.ContestDtos.ContestCard;
import ao.lumen.pregao.contest.api.ContestDtos.ContestDetail;
import ao.lumen.pregao.contest.api.ContestDtos.InviteView;
import ao.lumen.pregao.contest.api.ContestDtos.MemberView;
import ao.lumen.pregao.contest.api.ContestDtos.ProblemStatus;
import ao.lumen.pregao.contest.api.ContestDtos.ProblemView;
import ao.lumen.pregao.contest.api.ContestDtos.ScoreRow;
import ao.lumen.pregao.contest.api.ContestDtos.ScoreboardView;
import ao.lumen.pregao.contest.api.ContestDtos.TeamView;
import ao.lumen.pregao.contest.domain.Contest;
import ao.lumen.pregao.contest.domain.ContestProblem;
import ao.lumen.pregao.contest.domain.ContestState;
import ao.lumen.pregao.contest.domain.Scoreboard;
import ao.lumen.pregao.contest.domain.Team;
import ao.lumen.pregao.contest.domain.TeamInvite;
import ao.lumen.pregao.contest.domain.TeamMember;
import ao.lumen.pregao.contest.infra.ContestProblemRepository;
import ao.lumen.pregao.contest.infra.ContestRepository;
import ao.lumen.pregao.contest.infra.ScoreboardRepository;
import ao.lumen.pregao.contest.infra.TeamInviteRepository;
import ao.lumen.pregao.contest.infra.TeamMemberRepository;
import ao.lumen.pregao.contest.infra.TeamRepository;
import ao.lumen.pregao.grading.app.StatementRenderer;
import ao.lumen.pregao.identity.app.UserDirectory;
import ao.lumen.pregao.identity.domain.DisplayNames;
import ao.lumen.pregao.learning.api.LearningDtos.SubmissionAccepted;
import ao.lumen.pregao.learning.app.AttemptService;
import ao.lumen.pregao.learning.app.SubmissionService;
import ao.lumen.pregao.learning.domain.Attempt;
import ao.lumen.pregao.learning.domain.AttemptStatus;
import ao.lumen.pregao.learning.infra.AttemptRepository;
import ao.lumen.pregao.learning.infra.SubmissionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Contests do ponto de vista do participante: inscrição, equipas, problemas, submissões e ranking. */
@Service
public class ContestService {
    private final ContestRepository contests;
    private final ContestProblemRepository problems;
    private final TeamRepository teams;
    private final TeamMemberRepository members;
    private final TeamInviteRepository invites;
    private final ScoreboardRepository scoreboards;
    private final AttemptService attemptService;
    private final AttemptRepository attempts;
    private final SubmissionRepository submissionRepo;
    private final SubmissionService submissions;
    private final ExerciseCatalog catalog;
    private final StatementRenderer renderer;
    private final UserDirectory directory;
    private final OffensiveFilter offensive;
    private final RateLimiter limiter;
    private final AuditService audit;
    private final ao.lumen.pregao.notification.app.NotificationService notifications;
    private final AppProperties props;
    private final ObjectMapper json;
    private final Clock clock;

    public ContestService(ContestRepository contests, ContestProblemRepository problems, TeamRepository teams,
                          TeamMemberRepository members, TeamInviteRepository invites, ScoreboardRepository scoreboards,
                          AttemptService attemptService, AttemptRepository attempts, SubmissionRepository submissionRepo,
                          SubmissionService submissions, ExerciseCatalog catalog, StatementRenderer renderer,
                          UserDirectory directory, OffensiveFilter offensive, RateLimiter limiter, AuditService audit,
                          AppProperties props, ObjectMapper json, Clock clock, ao.lumen.pregao.notification.app.NotificationService notifications) {
        this.notifications = notifications;
        this.contests = contests;
        this.problems = problems;
        this.teams = teams;
        this.members = members;
        this.invites = invites;
        this.scoreboards = scoreboards;
        this.attemptService = attemptService;
        this.attempts = attempts;
        this.submissionRepo = submissionRepo;
        this.submissions = submissions;
        this.catalog = catalog;
        this.renderer = renderer;
        this.directory = directory;
        this.offensive = offensive;
        this.limiter = limiter;
        this.audit = audit;
        this.props = props;
        this.json = json;
        this.clock = clock;
    }

    // ───────────────────────── leitura ─────────────────────────

    @Transactional(readOnly = true)
    public List<ContestCard> list(UUID user) {
        var mine = members.findByUserId(user).stream().collect(Collectors.toMap(TeamMember::getContestId, m -> m));
        return contests.findByStateNotOrderByStartsAtDesc(ContestState.DRAFT).stream().map(c -> {
            TeamMember m = mine.get(c.getId());
            Team t = m == null ? null : teams.findById(m.getTeamId()).orElse(null);
            return new ContestCard(c.getId(), c.getSlug(), c.getTitle(), c.getMode(), c.getMinTeam(), c.getMaxTeam(),
                    c.getState(), c.getRegistrationOpensAt(), c.getRegistrationClosesAt(), c.getStartsAt(),
                    c.getFreezeAt(), c.getEndsAt(), t == null ? null : t.getName(),
                    t == null ? null : (int) members.countConfirmed(t.getId()));
        }).toList();
    }

    @Transactional(readOnly = true)
    public ContestDetail detail(UUID contestId, SessionUser me) {
        Contest c = visible(contestId, me);
        TeamView team = members.findByContestIdAndUserId(contestId, me.id())
                .map(m -> teamView(teams.findById(m.getTeamId()).orElseThrow(), c, me.id())).orElse(null);
        List<InviteView> myInvites = team != null ? List.of() : invites.findByInviteeUserIdAndStatus(me.id(), "PENDING").stream()
                .map(i -> teams.findById(i.getTeamId()).filter(t -> t.getContestId().equals(contestId))
                        .map(t -> new InviteView(t.getId(), t.getName(), c.getId(), c.getTitle(),
                                directory.summary(i.getInvitedBy()).shortName())).orElse(null))
                .filter(java.util.Objects::nonNull).toList();
        return new ContestDetail(c.getId(), c.getSlug(), c.getTitle(), c.getDescription(), c.getMode(), c.getMinTeam(),
                c.getMaxTeam(), c.getState(), c.getRegistrationOpensAt(), c.getRegistrationClosesAt(), c.getStartsAt(),
                c.getFreezeAt(), c.getEndsAt(), c.lockAt(), c.getLockMinutes(), c.getWrongPenaltyMinutes(),
                c.getFinalPhase(), problems.findByContestIdOrderByPositionAsc(contestId).size(), team, myInvites,
                clock.instant());
    }

    /** Problemas e variantes da equipa: só para membros inscritos e depois de startsAt (spec §6.4). */
    @Transactional
    public List<ProblemView> problems(UUID contestId, SessionUser me) {
        Contest c = visible(contestId, me);
        if (!c.getState().statementsReleased() || clock.instant().isBefore(c.getStartsAt())) {
            throw DomainException.forbidden("STATEMENTS_NOT_RELEASED", "Os enunciados são libertados pelo servidor à hora de início.");
        }
        Team team = myTeam(contestId, me.id());
        var confirmed = catalog.confirmedSources();
        List<ProblemView> out = new ArrayList<>();
        for (ContestProblem p : problems.findByContestIdOrderByPositionAsc(contestId)) {
            Attempt a = attemptService.startForContest(p.getId(), contestId, team.getId(), p.getExerciseVersionId());
            var def = catalog.definition(p.getExerciseVersionId());
            var r = renderer.render(def.statement(), def.paramSpec(), a.getParams(), confirmed);
            out.add(new ProblemView(p.getLabel(), p.getTitle(), p.getCompanyName(), def.type(), p.getPoints(), a.getId(),
                    r.text(), json.valueToTree(r.segments()), renderer.renderPresentation(def.presentation(), a.getParams()),
                    status(a)));
        }
        return out;
    }

    @Transactional
    public SubmissionAccepted submit(UUID contestId, String label, SessionUser me, String key, JsonNode payload) {
        Contest c = visible(contestId, me);
        if (c.getState().ordinal() < ContestState.RUNNING.ordinal() || clock.instant().isBefore(c.getStartsAt())) {
            throw DomainException.conflict("CONTEST_NOT_RUNNING", "O contest ainda não começou.");
        }
        Team team = myTeam(contestId, me.id());
        if (members.countConfirmed(team.getId()) < c.getMinTeam()) {
            throw DomainException.rule("TEAM_INCOMPLETE",
                    "A tua equipa não tem o número mínimo de membros confirmados para participar.");
        }
        ContestProblem p = problems.findByContestIdAndLabel(contestId, label)
                .orElseThrow(() -> DomainException.notFound("PROBLEM_NOT_FOUND", "O problema não existe."));
        Attempt a = attemptService.startForContest(p.getId(), contestId, team.getId(), p.getExerciseVersionId());
        return submissions.enqueue(a, me.id(), key, payload, c.getEndsAt());
    }

    /** Ranking do contest. Durante o congelamento, terceiros vêem a fotografia; cada equipa vê a sua pontuação real. */
    @Transactional(readOnly = true)
    public ScoreboardView scoreboard(UUID contestId, SessionUser me) {
        Contest c = visible(contestId, me);
        Optional<TeamMember> mine = members.findByContestIdAndUserId(contestId, me.id());
        UUID myTeam = mine.map(TeamMember::getTeamId).orElse(null);
        boolean staff = me.has(Roles.CONTEST_ORGANIZER) || me.has(Roles.ADMIN);
        boolean frozen = c.getState().frozenForOthers() && !staff;

        var boards = scoreboards.findByContestId(contestId);
        var teamMap = teams.findByIdIn(boards.stream().map(Scoreboard::getParticipantId).toList()).stream()
                .collect(Collectors.toMap(Team::getId, t -> t));
        Map<UUID, String> individualNames = individualNames(teamMap.values());

        record Row(UUID teamId, int points, int penalty, int solved, Instant last) {}
        List<Row> rows = new ArrayList<>();
        for (Scoreboard s : boards) {
            boolean useSnapshot = frozen && !s.getParticipantId().equals(myTeam);
            JsonNode snap = s.getFrozenSnapshot();
            if (useSnapshot && snap != null) {
                rows.add(new Row(s.getParticipantId(), snap.path("points").asInt(), snap.path("penaltyMinutes").asInt(),
                        snap.path("solvedCount").asInt(),
                        snap.hasNonNull("lastCorrectAt") ? Instant.parse(snap.path("lastCorrectAt").asText()) : null));
            } else {
                rows.add(new Row(s.getParticipantId(), s.getPoints(), s.getPenaltyMinutes(), s.getSolvedCount(),
                        s.getLastCorrectAt()));
            }
        }
        // Desempate: mais pontos, menor penalização, lastCorrectAt mais cedo (spec §7).
        rows.sort(Comparator.comparingInt((Row r) -> -r.points()).thenComparingInt(Row::penalty)
                .thenComparing(Row::last, Comparator.nullsLast(Comparator.naturalOrder())));
        List<ScoreRow> out = new ArrayList<>();
        long pos = 0;
        for (Row r : rows) {
            pos++;
            Team t = teamMap.get(r.teamId());
            if (t == null) continue;
            String name = t.isIndividual() ? individualNames.getOrDefault(t.getId(), "—") : t.getName();
            out.add(new ScoreRow(pos, name, DisplayNames.initials(name), r.points(), r.penalty(), r.solved(), r.last(),
                    r.teamId().equals(myTeam)));
        }
        return new ScoreboardView(c.getState(), frozen, c.getFreezeAt(), out, clock.instant());
    }

    // ───────────────────────── equipas ─────────────────────────

    @Transactional
    public TeamView createTeam(UUID contestId, SessionUser me, String rawName) {
        Contest c = registrationOpen(contestId);
        if (!c.isTeamMode()) throw DomainException.rule("INDIVIDUAL_CONTEST", "Este contest é individual: usa a inscrição individual.");
        String name = rawName.strip().replaceAll("\\s+", " ");
        if (offensive.isOffensive(name)) throw DomainException.rule("TEAM_NAME_REJECTED", "Este nome de equipa não é permitido.");
        if (members.findByContestIdAndUserId(contestId, me.id()).isPresent()) {
            throw DomainException.conflict("ALREADY_REGISTERED", "Já estás inscrito numa equipa deste contest.");
        }
        if (teams.existsByContestIdAndNameIgnoreCase(contestId, name)) {
            throw DomainException.conflict("TEAM_NAME_TAKEN", "Já existe uma equipa com este nome.");
        }
        Team t = register(c, me.id(), name, false);
        audit.record(me.id(), "TEAM_CREATED", "Team", t.getId(), null, Map.of("contest", c.getSlug(), "name", name));
        return teamView(t, c, me.id());
    }

    @Transactional
    public TeamView registerIndividual(UUID contestId, SessionUser me) {
        Contest c = registrationOpen(contestId);
        if (c.isTeamMode()) throw DomainException.rule("TEAM_CONTEST", "Este contest é por equipas: cria ou junta-te a uma equipa.");
        if (members.findByContestIdAndUserId(contestId, me.id()).isPresent()) {
            throw DomainException.conflict("ALREADY_REGISTERED", "Já estás inscrito neste contest.");
        }
        Team t = register(c, me.id(), "ind-" + me.id(), true);
        audit.record(me.id(), "CONTEST_REGISTERED", "Contest", c.getId(), null, null);
        return teamView(t, c, me.id());
    }

    private Team register(Contest c, UUID userId, String name, boolean individual) {
        try {
            Team t = teams.saveAndFlush(new Team(c.getId(), name, individual));
            members.saveAndFlush(new TeamMember(t.getId(), c.getId(), userId, "CAPTAIN", clock.instant()));
            scoreboards.save(new Scoreboard(c.getId(), t.getId()));
            return t;
        } catch (DataIntegrityViolationException e) {
            throw DomainException.conflict("ALREADY_REGISTERED", "Já estás inscrito ou o nome da equipa está ocupado.");
        }
    }

    @Transactional
    public TeamView invite(UUID teamId, SessionUser me, String email) {
        Team t = teams.findForUpdate(teamId).orElseThrow(() -> DomainException.notFound("TEAM_NOT_FOUND", "A equipa não existe."));
        Contest c = contests.findById(t.getContestId()).orElseThrow();
        TeamMember captain = members.findByContestIdAndUserId(c.getId(), me.id())
                .filter(m -> m.getTeamId().equals(teamId) && m.isCaptain())
                .orElseThrow(() -> DomainException.forbidden("NOT_CAPTAIN", "Só o capitão pode convidar membros."));
        ensureEditable(c, t);
        if (!limiter.tryAcquire("invite:" + me.id(), props.security().rateLimit().invitesPerHour(), Duration.ofHours(1))) {
            throw DomainException.tooMany("Enviaste demasiados convites. Tenta de novo mais tarde.");
        }
        var invitee = directory.byEmail(email).filter(UserDirectory.UserSummary::active)
                .orElseThrow(() -> DomainException.notFound("USER_NOT_FOUND",
                        "Não encontrámos uma conta activa com este email. Pede ao colega que crie conta primeiro."));
        if (members.findByContestIdAndUserId(c.getId(), invitee.id()).isPresent()) {
            throw DomainException.conflict("ALREADY_REGISTERED", "Este estudante já está numa equipa deste contest.");
        }
        long taken = members.countByTeamId(teamId) + invites.findByTeamIdAndStatus(teamId, "PENDING").size();
        if (taken >= c.getMaxTeam()) throw DomainException.rule("TEAM_FULL", "A equipa já tem " + c.getMaxTeam() + " lugares ocupados ou convidados.");
        if (invites.findFirstByTeamIdAndInviteeUserIdAndStatus(teamId, invitee.id(), "PENDING").isEmpty()) {
            invites.save(new TeamInvite(teamId, invitee.id(), captain.getUserId()));
            notifications.notify(invitee.id(), "TEAM_INVITE", "Convite para a equipa " + t.getName(),
                    directory.summary(captain.getUserId()).shortName() + " convidou-te para o contest «" + c.getTitle() + "».",
                    "/competicoes/" + c.getId());
            audit.record(me.id(), "TEAM_INVITE_SENT", "Team", teamId, null, Map.of("invitee", invitee.id()));
        }
        return teamView(t, c, me.id());
    }

    @Transactional
    public TeamView join(UUID teamId, SessionUser me) {
        Team t = teams.findForUpdate(teamId).orElseThrow(() -> DomainException.notFound("TEAM_NOT_FOUND", "A equipa não existe."));
        Contest c = contests.findById(t.getContestId()).orElseThrow();
        ensureEditable(c, t);
        TeamInvite inv = invites.findFirstByTeamIdAndInviteeUserIdAndStatus(teamId, me.id(), "PENDING")
                .orElseThrow(() -> DomainException.forbidden("NO_INVITE", "Não tens convite para esta equipa."));
        if (members.countByTeamId(teamId) >= c.getMaxTeam()) throw DomainException.rule("TEAM_FULL", "A equipa está completa.");
        try {
            members.saveAndFlush(new TeamMember(teamId, c.getId(), me.id(), "MEMBER", clock.instant()));
        } catch (DataIntegrityViolationException e) {
            throw DomainException.conflict("ALREADY_REGISTERED", "Já estás numa equipa deste contest.");
        }
        inv.respond("ACCEPTED", clock.instant());
        audit.record(me.id(), "TEAM_JOINED", "Team", teamId, null, null);
        return teamView(t, c, me.id());
    }

    /** Um membro (não capitão) sai da equipa antes do bloqueio. */
    @Transactional
    public void leave(UUID teamId, SessionUser me) {
        Team t = teams.findForUpdate(teamId).orElseThrow(() -> DomainException.notFound("TEAM_NOT_FOUND", "A equipa não existe."));
        Contest c = contests.findById(t.getContestId()).orElseThrow();
        TeamMember m = members.findByContestIdAndUserId(c.getId(), me.id()).filter(x -> x.getTeamId().equals(teamId))
                .orElseThrow(() -> DomainException.forbidden("NOT_MEMBER", "Não fazes parte desta equipa."));
        if (m.isCaptain()) {
            throw DomainException.rule("CAPTAIN_CANNOT_LEAVE", "O capitão não pode sair: elimina a equipa ou passa-a a outro membro.");
        }
        ensureEditable(c, t);
        members.delete(m);
        audit.record(me.id(), "TEAM_LEFT", "Team", teamId, null, null);
        members.findByTeamIdOrderByCreatedAtAsc(teamId).stream().filter(TeamMember::isCaptain).findFirst()
                .ifPresent(cap -> notifications.notify(cap.getUserId(), "TEAM", "Um membro saiu da equipa " + t.getName(),
                        directory.summary(me.id()).shortName() + " saiu da equipa.", "/competicoes/" + c.getId()));
    }

    @Transactional
    public void decline(UUID teamId, SessionUser me) {
        invites.findFirstByTeamIdAndInviteeUserIdAndStatus(teamId, me.id(), "PENDING")
                .ifPresent(i -> i.respond("DECLINED", clock.instant()));
    }

    /** Eliminar a equipa: só o capitão, antes do bloqueio, com o texto ELIMINAR. */
    @Transactional
    public void deleteTeam(UUID teamId, SessionUser me, String confirmation) {
        if (!"ELIMINAR".equals(confirmation)) {
            throw DomainException.badRequest("CONFIRMATION_REQUIRED", "Escreve ELIMINAR para confirmar.");
        }
        Team t = teams.findById(teamId).orElseThrow(() -> DomainException.notFound("TEAM_NOT_FOUND", "A equipa não existe."));
        Contest c = contests.findById(t.getContestId()).orElseThrow();
        members.findByContestIdAndUserId(c.getId(), me.id()).filter(m -> m.getTeamId().equals(teamId) && m.isCaptain())
                .orElseThrow(() -> DomainException.forbidden("NOT_CAPTAIN", "Só o capitão pode eliminar a equipa."));
        ensureEditable(c, t);
        invites.deleteByTeamId(teamId);
        members.deleteByTeamId(teamId);
        scoreboards.deleteForTeam(c.getId(), teamId);
        teams.delete(t);
        audit.record(me.id(), "TEAM_DELETED", "Team", teamId, Map.of("name", t.getName()), null);
    }

    // ───────────────────────── auxiliares ─────────────────────────

    private Contest visible(UUID id, SessionUser me) {
        Contest c = contests.findById(id).orElseThrow(() -> DomainException.notFound("CONTEST_NOT_FOUND", "O contest não existe."));
        if (c.getState() == ContestState.DRAFT && !(me.has(Roles.CONTEST_ORGANIZER) || me.has(Roles.ADMIN))) {
            throw DomainException.notFound("CONTEST_NOT_FOUND", "O contest não existe.");
        }
        return c;
    }

    private Contest registrationOpen(UUID id) {
        Contest c = contests.findById(id).orElseThrow(() -> DomainException.notFound("CONTEST_NOT_FOUND", "O contest não existe."));
        Instant now = clock.instant();
        if (c.getState() != ContestState.REGISTRATION || now.isBefore(c.getRegistrationOpensAt())
                || !now.isBefore(c.getRegistrationClosesAt())) {
            throw DomainException.conflict("REGISTRATION_CLOSED", "As inscrições deste contest não estão abertas.");
        }
        return c;
    }

    private void ensureEditable(Contest c, Team t) {
        Instant now = clock.instant();
        if (t.isLocked() || !now.isBefore(c.lockAt())
                || !(c.getState() == ContestState.REGISTRATION || c.getState() == ContestState.WAITING_ROOM)) {
            throw DomainException.conflict("TEAM_LOCKED",
                    "Os membros ficam bloqueados " + c.getLockMinutes() + " min antes do início. Já não é possível alterar a equipa.");
        }
    }

    Team myTeam(UUID contestId, UUID user) {
        TeamMember m = members.findByContestIdAndUserId(contestId, user)
                .filter(x -> x.getConfirmedAt() != null)
                .orElseThrow(() -> DomainException.forbidden("NOT_REGISTERED", "Não estás inscrito neste contest."));
        return teams.findById(m.getTeamId()).orElseThrow();
    }

    private ProblemStatus status(Attempt a) {
        var last = submissionRepo.findByAttemptIdOrderByReceivedAtDesc(a.getId(), PageRequest.of(0, 1));
        String lastStatus = last.isEmpty() ? null : last.getFirst().getStatus().name();
        String state;
        if (a.getStatus() == AttemptStatus.PASSED) state = "SOLVED";
        else if ("QUEUED".equals(lastStatus) || "PROCESSING".equals(lastStatus)) state = lastStatus;
        else if ("FAILED".equals(lastStatus)) state = "FAILED";
        else if (last.isEmpty()) state = "UNSOLVED";
        else state = "REJECTED";
        return new ProblemStatus(state, a.getAttemptsUsed(), last.isEmpty() ? null : last.getFirst().getId().toString(),
                lastStatus, a.getStatus() == AttemptStatus.PASSED ? a.getScore() : null);
    }

    TeamView teamView(Team t, Contest c, UUID me) {
        var ms = members.findByTeamIdOrderByCreatedAtAsc(t.getId());
        var users = directory.summaries(ms.stream().map(TeamMember::getUserId).toList());
        List<MemberView> mv = ms.stream().map(m -> {
            var u = users.get(m.getUserId());
            return new MemberView(u == null ? "—" : u.shortName(), u == null ? "—" : u.initials(),
                    u == null ? null : u.courseName(), u == null ? null : u.year(), m.getRole(), m.getConfirmedAt() != null,
                    m.getUserId().equals(me));
        }).toList();
        boolean captain = ms.stream().anyMatch(m -> m.getUserId().equals(me) && m.isCaptain());
        List<String> pending = captain ? invites.findByTeamIdAndStatus(t.getId(), "PENDING").stream()
                .map(i -> directory.summary(i.getInviteeUserId()).shortName()).toList() : List.of();
        String name = t.isIndividual() && !mv.isEmpty() ? mv.getFirst().name() : t.getName();
        return new TeamView(t.getId(), name, t.isIndividual(), t.isLocked() || !clock.instant().isBefore(c.lockAt()),
                t.getLockedAt(), (int) ms.stream().filter(m -> m.getConfirmedAt() != null).count(), c.getMaxTeam(),
                c.getMinTeam(), captain, mv, pending);
    }

    Map<UUID, String> individualNames(java.util.Collection<Team> ts) {
        var ind = ts.stream().filter(Team::isIndividual).map(Team::getId).toList();
        if (ind.isEmpty()) return Map.of();
        Map<UUID, UUID> teamToUser = new HashMap<>();
        members.findByTeamIdIn(ind).forEach(m -> teamToUser.put(m.getTeamId(), m.getUserId()));
        var users = directory.summaries(teamToUser.values());
        Map<UUID, String> out = new HashMap<>();
        teamToUser.forEach((team, user) -> out.put(team, users.containsKey(user) ? users.get(user).publicName() : "—"));
        return out;
    }

    public record ContestResult(UUID contestId, String title, String teamName, boolean individual, Long position,
                                int points, Instant at) {}

    /** Participações do utilizador com a posição final (só depois de publicados os resultados). */
    @Transactional(readOnly = true)
    public List<ContestResult> history(UUID user) {
        List<ContestResult> out = new ArrayList<>();
        for (TeamMember m : members.findByUserId(user)) {
            Contest c = contests.findById(m.getContestId()).orElse(null);
            Team t = teams.findById(m.getTeamId()).orElse(null);
            if (c == null || t == null) continue;
            boolean published = c.getState() == ContestState.RESULTS_PUBLISHED || c.getState() == ContestState.CLOSED;
            Long position = null;
            int pts = 0;
            if (published) {
                var ranked = scoreboards.findByContestId(c.getId()).stream()
                        .sorted(Comparator.comparingInt((Scoreboard x) -> -x.getPoints()).thenComparingInt(Scoreboard::getPenaltyMinutes)
                                .thenComparing(Scoreboard::getLastCorrectAt, Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList();
                for (int i = 0; i < ranked.size(); i++) {
                    if (ranked.get(i).getParticipantId().equals(t.getId())) {
                        position = (long) i + 1;
                        pts = ranked.get(i).getPoints();
                    }
                }
            }
            out.add(new ContestResult(c.getId(), c.getTitle(), t.isIndividual() ? null : t.getName(), t.isIndividual(),
                    position, pts, c.getResultsPublishedAt() != null ? c.getResultsPublishedAt() : c.getEndsAt()));
        }
        return out;
    }

    /** Próximo contest relevante para o painel inicial. */
    @Transactional(readOnly = true)
    public Optional<ContestCard> next(UUID user) {
        return list(user).stream()
                .filter(c -> c.state().ordinal() <= ContestState.FROZEN.ordinal())
                .min(Comparator.comparing(ContestCard::startsAt));
    }

    @Transactional(readOnly = true)
    public Optional<UUID> teamOf(UUID contestId, UUID user) {
        return members.findByContestIdAndUserId(contestId, user).map(TeamMember::getTeamId);
    }

    public String stateOf(UUID contestId) {
        return contests.findById(contestId).map(c -> c.getState().name()).orElse("UNKNOWN");
    }

    long pending() {
        return submissionRepo.pendingCount();
    }

    List<Attempt> teamAttempts(UUID contestId, UUID teamId) {
        return attempts.findByContestIdAndTeamId(contestId, teamId);
    }
}
