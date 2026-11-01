package ao.lumen.pregao.ranking.app;

import ao.lumen.pregao.common.privacy.PersonalDataContributor;
import ao.lumen.pregao.ranking.infra.PointEventRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class RankingPersonalData implements PersonalDataContributor {
    private final PointEventRepository events;
    private final BadgeService badges;

    RankingPersonalData(PointEventRepository events, BadgeService badges) {
        this.events = events;
        this.badges = badges;
    }

    @Override
    public String section() {
        return "points";
    }

    @Override
    public Object export(UUID userId) {
        return Map.of(
                "events", events.findByUserIdOrderByAtAsc(userId).stream()
                        .map(e -> Map.of("source", e.getSource(), "sourceId", e.getSourceId(), "delta", e.getDelta(),
                                "at", e.getAt())).toList(),
                "badges", badges.earned(userId));
    }
}
