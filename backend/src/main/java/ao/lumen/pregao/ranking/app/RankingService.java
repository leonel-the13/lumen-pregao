package ao.lumen.pregao.ranking.app;

import ao.lumen.pregao.common.config.AppProperties;
import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.identity.app.UserDirectory;
import ao.lumen.pregao.identity.domain.RankingVisibility;
import ao.lumen.pregao.ranking.infra.PointEventRepository;
import ao.lumen.pregao.ranking.infra.SeasonRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Leitura dos rankings a partir da projecção em Redis (spec §4.8). */
@Service
public class RankingService {
    private final StringRedisTemplate redis;
    private final UserDirectory directory;
    private final PointEventRepository events;
    private final SeasonRepository seasons;
    private final List<TeamDirectory> teamDirectories;
    private final AppProperties props;
    private final Clock clock;

    public RankingService(StringRedisTemplate redis, UserDirectory directory, PointEventRepository events,
                          SeasonRepository seasons, List<TeamDirectory> teamDirectories, AppProperties props,
                          Clock clock) {
        this.redis = redis;
        this.directory = directory;
        this.events = events;
        this.seasons = seasons;
        this.teamDirectories = teamDirectories;
        this.props = props;
        this.clock = clock;
    }

    public enum Scope { GLOBAL, COURSE, YEAR, SEASON, TEAMS }

    public record Entry(long position, String name, String initials, String course, Integer year, long points,
                        Long movement, boolean me, String portfolioHandle) {}

    public record MyPosition(Long position, long points, Long movement, Long gapToNext, long weekPoints) {}

    public record RankingPage(Scope scope, String label, List<Entry> entries, MyPosition me, long total,
                              String nextCursor, Instant generatedAt) {}

    @Transactional(readOnly = true)
    public RankingPage read(UUID me, Scope scope, String course, Integer year, String cursor, Integer limit) {
        var mine = directory.summary(me);
        int lim = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);
        long offset = cursor == null || cursor.isBlank() ? 0 : parseCursor(cursor);
        var season = seasons.findAt(clock.instant());
        String key;
        String label;
        switch (scope) {
            case GLOBAL -> { key = RankingKeys.global(); label = "Todos os cursos · todos os anos"; }
            case COURSE -> {
                String c = course != null ? course : mine.courseCode();
                if (c == null) throw DomainException.badRequest("COURSE_REQUIRED", "Indica o curso.");
                key = RankingKeys.course(c);
                label = c;
            }
            case YEAR -> {
                Integer y = year != null ? year : mine.year();
                if (y == null) throw DomainException.badRequest("YEAR_REQUIRED", "Indica o ano.");
                key = RankingKeys.year(y);
                label = y + ".º ano";
            }
            case SEASON -> {
                var s = season.orElseThrow(() -> DomainException.notFound("NO_SEASON", "Não há época activa."));
                key = RankingKeys.season(s.getId());
                label = s.getName();
            }
            case TEAMS -> {
                var s = season.orElseThrow(() -> DomainException.notFound("NO_SEASON", "Não há época activa."));
                return teams(me, RankingKeys.teams(s.getId()), s.getName(), offset, lim);
            }
            default -> throw new IllegalStateException();
        }
        var z = redis.opsForZSet();
        Long total = z.zCard(key);
        var rows = Optional.ofNullable(z.reverseRangeWithScores(key, offset, offset + lim - 1)).orElse(java.util.Set.of());
        List<UUID> ids = new ArrayList<>();
        for (TypedTuple<String> t : rows) ids.add(UUID.fromString(t.getValue()));
        var users = directory.summaries(ids);

        List<Entry> entries = new ArrayList<>();
        long pos = offset;
        for (TypedTuple<String> t : rows) {
            pos++;
            UUID id = UUID.fromString(t.getValue());
            var u = users.get(id);
            if (u == null || (!u.active() && !id.equals(me))) continue;
            boolean isMe = id.equals(me);
            String name = isMe ? "Tu · " + u.shortName() : u.publicName();
            String initials = isMe ? u.initials() : u.publicInitials();
            String handle = u.portfolioPublic() && u.visibility() != RankingVisibility.HIDDEN ? u.portfolioHandle() : null;
            entries.add(new Entry(pos, name, initials, u.courseName(), u.year(), Math.round(t.getScore()),
                    movement(key, t.getValue(), pos), isMe, handle));
        }
        long t = total == null ? 0 : total;
        String next = offset + lim < t ? Long.toString(offset + lim) : null;
        return new RankingPage(scope, label, entries, myPosition(key, me), t, next, clock.instant());
    }

    /** Posição do utilizador no ranking do curso (painel inicial). */
    @Transactional(readOnly = true)
    public MyPosition coursePosition(UUID me) {
        var u = directory.summary(me);
        if (u.courseCode() == null) return myPosition(RankingKeys.global(), me);
        return myPosition(RankingKeys.course(u.courseCode()), me);
    }

    private MyPosition myPosition(String key, UUID me) {
        var z = redis.opsForZSet();
        String m = me.toString();
        Long rank = z.reverseRank(key, m);
        Double score = z.score(key, m);
        long week = events.totalSince(me, startOfWeek());
        if (rank == null) return new MyPosition(null, 0, null, null, week);
        long position = rank + 1;
        Long gap = null;
        if (rank > 0) {
            var above = z.reverseRangeWithScores(key, rank - 1, rank - 1);
            if (above != null && !above.isEmpty()) {
                gap = Math.round(above.iterator().next().getScore() - (score == null ? 0 : score));
            }
        }
        return new MyPosition(position, score == null ? 0 : Math.round(score), movement(key, m, position), gap, week);
    }

    private RankingPage teams(UUID me, String key, String label, long offset, int lim) {
        var z = redis.opsForZSet();
        Long total = z.zCard(key);
        var rows = Optional.ofNullable(z.reverseRangeWithScores(key, offset, offset + lim - 1)).orElse(java.util.Set.of());
        List<UUID> ids = rows.stream().map(t -> UUID.fromString(t.getValue())).toList();
        Map<UUID, TeamDirectory.TeamInfo> infos = new HashMap<>();
        teamDirectories.forEach(d -> infos.putAll(d.teams(new HashSet<>(ids))));
        List<Entry> entries = new ArrayList<>();
        long pos = offset;
        for (TypedTuple<String> t : rows) {
            pos++;
            var info = infos.get(UUID.fromString(t.getValue()));
            if (info == null) continue;
            boolean mine = info.memberIds().contains(me);
            entries.add(new Entry(pos, info.name(), ao.lumen.pregao.identity.domain.DisplayNames.initials(info.name()),
                    info.contestTitle(), null, Math.round(t.getScore()), movement(key, t.getValue(), pos), mine, null));
        }
        long tt = total == null ? 0 : total;
        return new RankingPage(Scope.TEAMS, label, entries, new MyPosition(null, 0, null, null, 0), tt,
                offset + lim < tt ? Long.toString(offset + lim) : null, clock.instant());
    }

    private Long movement(String key, String member, long position) {
        Long before = redis.opsForZSet().reverseRank(RankingKeys.snapshot(key), member);
        return before == null ? null : (before + 1) - position;
    }

    Instant startOfWeek() {
        ZoneId zone = ZoneId.of(props.ranking().zone());
        return clock.instant().atZone(zone).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(zone).toInstant();
    }

    private static long parseCursor(String c) {
        try {
            long v = Long.parseLong(c);
            if (v < 0) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            throw DomainException.badRequest("CURSOR_INVALID", "Cursor inválido.");
        }
    }
}
