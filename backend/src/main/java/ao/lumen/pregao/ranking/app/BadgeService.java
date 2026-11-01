package ao.lumen.pregao.ranking.app;

import ao.lumen.pregao.common.domain.UuidV7;
import ao.lumen.pregao.ranking.domain.PointSource;
import ao.lumen.pregao.ranking.infra.BadgeRepository;
import ao.lumen.pregao.ranking.infra.UserBadgeRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Conquistas. Os outros módulos avaliam as condições que conhecem e pedem a atribuição aqui. */
@Service
public class BadgeService {
    private final BadgeRepository badges;
    private final UserBadgeRepository userBadges;
    private final PointService points;
    private final ao.lumen.pregao.notification.app.NotificationService notifications;

    public BadgeService(BadgeRepository badges, UserBadgeRepository userBadges, @Lazy PointService points,
                        ao.lumen.pregao.notification.app.NotificationService notifications) {
        this.notifications = notifications;
        this.badges = badges;
        this.userBadges = userBadges;
        this.points = points;
    }

    public record BadgeView(String code, String title, String description, int points, Instant awardedAt) {}

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean award(UUID userId, String code) {
        var badge = badges.findByCode(code).orElse(null);
        if (badge == null) return false;
        boolean inserted = userBadges.insertIfAbsent(UuidV7.next(), userId, badge.getId()) == 1;
        if (inserted && badge.getPoints() > 0) {
            points.award(userId, null, PointSource.BADGE, code, badge.getPoints());
        }
        if (inserted) {
            notifications.notify(userId, "BADGE", "Nova conquista: " + badge.getTitle(),
                    badge.getDescription() + (badge.getPoints() > 0 ? " · +" + badge.getPoints() + " pts" : ""), "/portefolio");
        }
        return inserted;
    }

    @Transactional(readOnly = true)
    public List<BadgeView> earned(UUID userId) {
        return userBadges.findByUserIdOrderByAwardedAtDesc(userId).stream()
                .map(ub -> new BadgeView(ub.getBadge().getCode(), ub.getBadge().getTitle(),
                        ub.getBadge().getDescription(), ub.getBadge().getPoints(), ub.getAwardedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BadgeView> catalog() {
        return badges.findAll().stream()
                .map(b -> new BadgeView(b.getCode(), b.getTitle(), b.getDescription(), b.getPoints(), null)).toList();
    }
}
