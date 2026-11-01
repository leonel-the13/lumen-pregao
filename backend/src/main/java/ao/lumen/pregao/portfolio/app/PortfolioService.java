package ao.lumen.pregao.portfolio.app;

import ao.lumen.pregao.common.config.AppProperties;
import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.contest.api.ContestDtos.ContestCard;
import ao.lumen.pregao.contest.app.ContestService;
import ao.lumen.pregao.exam.app.ExamService;
import ao.lumen.pregao.identity.app.UserDirectory;
import ao.lumen.pregao.identity.app.UserDirectory.UserSummary;
import ao.lumen.pregao.learning.api.LearningDtos.RoomCard;
import ao.lumen.pregao.learning.api.LearningDtos.TrackCard;
import ao.lumen.pregao.learning.app.ActivityService;
import ao.lumen.pregao.learning.app.ActivityService.Activity;
import ao.lumen.pregao.learning.app.ActivityService.Competency;
import ao.lumen.pregao.learning.app.CatalogService;
import ao.lumen.pregao.learning.app.ProgressService;
import ao.lumen.pregao.learning.app.ProgressService.RoomState;
import ao.lumen.pregao.ranking.app.BadgeService;
import ao.lumen.pregao.ranking.app.BadgeService.BadgeView;
import ao.lumen.pregao.ranking.app.PointService;
import ao.lumen.pregao.ranking.app.RankingService;
import ao.lumen.pregao.ranking.app.RankingService.MyPosition;
import ao.lumen.pregao.ranking.app.StreakService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Painel inicial e portefólio: agregam os serviços públicos dos outros módulos. */
@Service
@Transactional(readOnly = true)
public class PortfolioService {
    private final UserDirectory directory;
    private final CatalogService catalog;
    private final ProgressService progress;
    private final ActivityService activity;
    private final PointService points;
    private final RankingService rankings;
    private final StreakService streaks;
    private final BadgeService badges;
    private final ContestService contests;
    private final ExamService exams;
    private final AppProperties props;
    private final Clock clock;

    public PortfolioService(UserDirectory directory, CatalogService catalog, ProgressService progress,
                            ActivityService activity, PointService points, RankingService rankings,
                            StreakService streaks, BadgeService badges, ContestService contests, ExamService exams,
                            AppProperties props, Clock clock) {
        this.directory = directory;
        this.catalog = catalog;
        this.progress = progress;
        this.activity = activity;
        this.points = points;
        this.rankings = rankings;
        this.streaks = streaks;
        this.badges = badges;
        this.contests = contests;
        this.exams = exams;
        this.props = props;
        this.clock = clock;
    }

    public record Dashboard(String firstName, LocalDate today, long points, long weekPoints, int streak,
                            boolean practicedToday, MyPosition coursePosition, String courseName, int roomsCompleted,
                            int roomsInActiveTrack, List<TrackCard> tracksInProgress, ContestCard nextContest,
                            List<RoomCard> recommended, List<Activity> activity, UUID continueRoomId,
                            String continueRoomTitle, Instant serverNow) {}

    public record HistoryItem(Instant at, String label, String result) {}

    public record Portfolio(String name, String initials, String course, Integer year, boolean isPublic, String handle,
                            int tracksCompleted, int roomsCompleted, BigDecimal examAverage, Long bestPosition,
                            String bestPositionLabel, List<Competency> competencies, List<BadgeView> badges,
                            List<BadgeView> badgeCatalog, List<HistoryItem> history) {}

    public record Summary(long points, int streak, boolean liveContest) {}

    /** Resumo leve para a barra superior (pontos, sequência, contest a decorrer). */
    public Summary summary(UUID user) {
        boolean live = contests.list(user).stream().anyMatch(c -> c.state() == ao.lumen.pregao.contest.domain.ContestState.RUNNING
                || c.state() == ao.lumen.pregao.contest.domain.ContestState.FROZEN);
        return new Summary(points.total(user), streaks.currentStreak(user), live);
    }

    public Dashboard dashboard(UUID user) {
        UserSummary u = directory.summary(user);
        var snap = progress.snapshot(user);
        List<TrackCard> all = catalog.tracks(user, null, null, null, null);
        List<TrackCard> inProgress = all.stream().filter(t -> t.percent() > 0 && t.percent() < 100)
                .sorted(Comparator.comparingInt(TrackCard::percent).reversed()).limit(2).toList();
        TrackCard active = inProgress.isEmpty() ? null : inProgress.getFirst();
        int completed = (int) snap.rooms().values().stream().filter(p -> p.state() == RoomState.COMPLETED).count();
        int inActive = active == null ? 0 : (int) snap.rooms().values().stream()
                .filter(p -> p.room().getModule().getTrack().getSlug().equals(active.slug())).count();

        List<RoomCard> recommended = new ArrayList<>();
        UUID continueId = active != null ? active.nextRoomId()
                : all.stream().map(TrackCard::nextRoomId).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        String continueTitle = null;
        for (RoomCard r : catalog.rooms(user, null, null, null)) {
            if (r.id().equals(continueId)) continueTitle = r.title();
            if (r.state() == RoomState.AVAILABLE && !r.id().equals(continueId) && recommended.size() < 3) recommended.add(r);
        }
        ZoneId zone = ZoneId.of(props.ranking().zone());
        MyPosition pos = rankings.coursePosition(user);
        return new Dashboard(u.name().split("\\s+")[0], clock.instant().atZone(zone).toLocalDate(), points.total(user),
                pos.weekPoints(), streaks.currentStreak(user), streaks.practicedToday(user), pos, u.courseName(), completed,
                inActive, inProgress, contests.next(user).orElse(null), recommended, activity.recent(user, 6),
                continueId, continueTitle, clock.instant());
    }

    public Portfolio mine(UUID user) {
        return build(directory.summary(user), true);
    }

    public Portfolio publicPortfolio(String handle) {
        UserSummary u = directory.byPublicHandle(handle)
                .orElseThrow(() -> DomainException.notFound("PORTFOLIO_NOT_FOUND", "Este portefólio não existe ou não é público."));
        return build(u, false);
    }

    private Portfolio build(UserSummary u, boolean owner) {
        var snap = progress.snapshot(u.id());
        int rooms = (int) snap.rooms().values().stream().filter(p -> p.state() == RoomState.COMPLETED).count();
        int tracksDone = (int) catalog.tracks(u.id(), null, null, null, null).stream().filter(t -> t.percent() >= 100).count();
        var examHistory = exams.history(u.id());
        BigDecimal avg = examHistory.isEmpty() ? null : examHistory.stream().map(ExamService.ExamResult::grade)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(examHistory.size()), 1, RoundingMode.HALF_UP);
        var contestHistory = contests.history(u.id());
        var best = contestHistory.stream().filter(c -> c.position() != null)
                .min(Comparator.comparingLong(ContestService.ContestResult::position)).orElse(null);

        List<HistoryItem> history = new ArrayList<>();
        contestHistory.stream().filter(c -> c.position() != null).forEach(c -> history.add(new HistoryItem(c.at(),
                c.title() + (c.teamName() != null ? " · " + c.teamName() : ""), "#" + c.position())));
        examHistory.forEach(e -> history.add(new HistoryItem(e.at(), "Exame · " + e.title(),
                e.grade().stripTrailingZeros().toPlainString().replace('.', ',') + "/20")));
        history.sort(Comparator.comparing(HistoryItem::at).reversed());

        return new Portfolio(owner ? u.name() : u.name(), u.initials(), u.courseName(), u.year(), u.portfolioPublic(),
                u.portfolioHandle(), tracksDone, rooms, avg, best == null ? null : best.position(),
                best == null ? null : best.title() + (best.individual() ? "" : " · equipa"),
                activity.competencies(u.id()), badges.earned(u.id()), badges.catalog(), history);
    }
}
