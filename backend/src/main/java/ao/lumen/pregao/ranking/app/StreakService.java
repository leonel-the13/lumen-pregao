package ao.lumen.pregao.ranking.app;

import ao.lumen.pregao.common.config.AppProperties;
import ao.lumen.pregao.ranking.domain.PointSource;
import ao.lumen.pregao.ranking.infra.PracticeDayRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sequência de dias: há prática num dia se houver pelo menos uma tarefa submetida nesse dia (Africa/Luanda).
 * A cada 7 dias seguidos atribui o bónus de sequência (spec §7).
 */
@Service
public class StreakService {
    private final PracticeDayRepository days;
    private final PointService points;
    private final BadgeService badges;
    private final AppProperties props;
    private final Clock clock;

    public StreakService(PracticeDayRepository days, PointService points, BadgeService badges, AppProperties props,
                         Clock clock) {
        this.days = days;
        this.points = points;
        this.badges = badges;
        this.props = props;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordPractice(UUID userId, Instant at) {
        LocalDate day = at.atZone(zone()).toLocalDate();
        if (!days.record(userId, day)) return;
        int streak = streakEndingAt(days.recentDays(userId), day);
        int every = props.ranking().streakDays();
        if (streak > 0 && streak % every == 0) {
            points.award(userId, null, PointSource.STREAK, "streak:" + day, props.ranking().streakBonus());
        }
        if (streak >= 10) badges.award(userId, "SEQUENCIA_10");
    }

    @Transactional(readOnly = true)
    public int currentStreak(UUID userId) {
        List<LocalDate> recent = days.recentDays(userId);
        if (recent.isEmpty()) return 0;
        LocalDate today = clock.instant().atZone(zone()).toLocalDate();
        LocalDate last = recent.getFirst();
        if (last.isBefore(today.minusDays(1))) return 0;
        return streakEndingAt(recent, last);
    }

    @Transactional(readOnly = true)
    public boolean practicedToday(UUID userId) {
        List<LocalDate> recent = days.recentDays(userId);
        return !recent.isEmpty() && recent.getFirst().equals(clock.instant().atZone(zone()).toLocalDate());
    }

    static int streakEndingAt(List<LocalDate> desc, LocalDate end) {
        int n = 0;
        LocalDate expected = end;
        for (LocalDate d : desc) {
            if (d.isAfter(end)) continue;
            if (!d.equals(expected)) break;
            n++;
            expected = expected.minusDays(1);
        }
        return n;
    }

    private ZoneId zone() {
        return ZoneId.of(props.ranking().zone());
    }
}
