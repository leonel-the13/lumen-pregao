package ao.lumen.pregao.ranking.app;

import ao.lumen.pregao.identity.app.UserDirectory;
import ao.lumen.pregao.ranking.infra.PointEventRepository;
import ao.lumen.pregao.ranking.infra.SeasonRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Projecção dos rankings em Redis (Sorted Sets). A verdade está em PostgreSQL (PointEvent):
 * apagar o Redis e reconstruir dá o mesmo ranking (CON-05).
 */
@Component
public class RankingProjection {
    private static final Logger log = LoggerFactory.getLogger(RankingProjection.class);
    private final StringRedisTemplate redis;
    private final PointEventRepository events;
    private final SeasonRepository seasons;
    private final UserDirectory directory;

    public RankingProjection(StringRedisTemplate redis, PointEventRepository events, SeasonRepository seasons,
                             UserDirectory directory) {
        this.redis = redis;
        this.events = events;
        this.seasons = seasons;
        this.directory = directory;
    }

    @TransactionalEventListener
    public void onPoints(PointService.PointsAwarded e) {
        try {
            String member = e.userId().toString();
            increment(RankingKeys.global(), member, e.delta());
            if (e.courseCode() != null) increment(RankingKeys.course(e.courseCode()), member, e.delta());
            if (e.year() != null) increment(RankingKeys.year(e.year()), member, e.delta());
            if (e.seasonId() != null) increment(RankingKeys.season(e.seasonId()), member, e.delta());
        } catch (RuntimeException ex) {
            log.warn("Projecção do ranking falhou; será corrigida na próxima reconstrução: {}", ex.getClass().getSimpleName());
        }
    }

    /** Regra única com a reconstrução: quem tem 0 pontos não aparece no ranking. */
    private void increment(String key, String member, int delta) {
        Double score = redis.opsForZSet().incrementScore(key, member, delta);
        if (score != null && score == 0d) redis.opsForZSet().remove(key, member);
    }

    /** Pontuação de equipas (fonte: Scoreboard persistido do contest). */
    public void setTeamScore(UUID seasonId, UUID teamId, long points) {
        if (seasonId == null) return;
        try {
            redis.opsForZSet().add(RankingKeys.teams(seasonId), teamId.toString(), points);
        } catch (RuntimeException ex) {
            log.warn("Projecção do ranking de equipas falhou: {}", ex.getClass().getSimpleName());
        }
    }

    @Transactional(readOnly = true)
    public void setTeamScoreAt(java.time.Instant at, UUID teamId, long points) {
        seasons.findAt(at).ifPresent(s -> setTeamScore(s.getId(), teamId, points));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void rebuildIfMissing() {
        try {
            if (Boolean.FALSE.equals(redis.hasKey(RankingKeys.global()))) rebuild();
        } catch (RuntimeException ex) {
            log.warn("Redis indisponível no arranque; os rankings serão reconstruídos mais tarde");
        }
    }

    /** Reconstrói todas as chaves de utilizadores a partir dos PointEvent. Devolve o n.º de utilizadores. */
    @Transactional(readOnly = true)
    public int rebuild() {
        Map<UUID, Long> totals = toMap(events.totalsByUser());
        var users = directory.summaries(totals.keySet());
        Set<String> old = redis.keys("rank:*");
        if (old != null) old.stream().filter(k -> !k.startsWith("rank:teams:")).forEach(redis::delete);

        var z = redis.opsForZSet();
        Map<String, Map<String, Long>> byKey = new HashMap<>();
        totals.forEach((id, pts) -> {
            var u = users.get(id);
            put(byKey, RankingKeys.global(), id, pts);
            if (u != null && u.courseCode() != null) put(byKey, RankingKeys.course(u.courseCode()), id, pts);
            if (u != null && u.year() != null) put(byKey, RankingKeys.year(u.year()), id, pts);
        });
        for (var s : seasons.findAll()) {
            toMap(events.totalsByUserBetween(s.getStartsAt(), s.getEndsAt()))
                    .forEach((id, pts) -> put(byKey, RankingKeys.season(s.getId()), id, pts));
        }
        byKey.forEach((key, members) -> members.forEach((m, pts) -> z.add(key, m, pts)));
        log.info("Rankings reconstruídos: {} utilizadores", totals.size());
        return totals.size();
    }

    /** Fotografia semanal para o movimento ▲/▼ (segunda-feira, 00:00 em Luanda). */
    @Scheduled(cron = "0 0 0 * * MON", zone = "Africa/Luanda")
    public void weeklySnapshot() {
        try {
            Set<String> keys = redis.keys("rank:*");
            if (keys == null) return;
            for (String k : keys) {
                redis.delete(RankingKeys.snapshot(k));
                redis.opsForZSet().unionAndStore(k, List.of(), RankingKeys.snapshot(k));
            }
        } catch (RuntimeException ex) {
            log.warn("Fotografia semanal do ranking falhou: {}", ex.getClass().getSimpleName());
        }
    }

    private static void put(Map<String, Map<String, Long>> m, String key, UUID id, long pts) {
        if (pts == 0) return;
        m.computeIfAbsent(key, k -> new HashMap<>()).put(id.toString(), pts);
    }

    private static Map<UUID, Long> toMap(List<Object[]> rows) {
        Map<UUID, Long> m = new HashMap<>();
        for (Object[] r : rows) m.put((UUID) r[0], ((Number) r[1]).longValue());
        return m;
    }
}
