package ao.lumen.pregao.ranking.app;

import ao.lumen.pregao.common.domain.UuidV7;
import ao.lumen.pregao.identity.app.UserDirectory;
import ao.lumen.pregao.ranking.domain.PointSource;
import ao.lumen.pregao.ranking.infra.PointEventRepository;
import ao.lumen.pregao.ranking.infra.SeasonRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Atribui pontos (PointEvent) na transacção do chamador. A projecção no Redis é feita depois do commit. */
@Service
public class PointService {
    private final PointEventRepository events;
    private final SeasonRepository seasons;
    private final UserDirectory directory;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    public PointService(PointEventRepository events, SeasonRepository seasons, UserDirectory directory,
                        ApplicationEventPublisher publisher, Clock clock) {
        this.events = events;
        this.seasons = seasons;
        this.directory = directory;
        this.publisher = publisher;
        this.clock = clock;
    }

    public record PointsAwarded(UUID userId, UUID teamId, String courseCode, Integer year, UUID seasonId,
                                PointSource source, int delta, Instant at) {}

    /** Idempotente por (utilizador, origem, id da origem). Devolve true se registou pontos novos. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean award(UUID userId, UUID teamId, PointSource source, String sourceId, int delta) {
        Instant at = clock.instant();
        boolean inserted = events.insertIfAbsent(UuidV7.next(), userId, teamId, source.name(), sourceId, delta, at) == 1;
        if (inserted && delta != 0) {
            var u = directory.summary(userId);
            UUID seasonId = seasons.findAt(at).map(s -> s.getId()).orElse(null);
            publisher.publishEvent(new PointsAwarded(userId, teamId, u.courseCode(), u.year(), seasonId, source, delta, at));
        }
        return inserted;
    }

    @Transactional(readOnly = true)
    public long total(UUID userId) {
        return events.total(userId);
    }
}
