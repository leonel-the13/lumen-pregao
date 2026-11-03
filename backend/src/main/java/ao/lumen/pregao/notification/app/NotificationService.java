package ao.lumen.pregao.notification.app;

import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.common.web.CursorPage;
import ao.lumen.pregao.notification.domain.Notification;
import ao.lumen.pregao.notification.infra.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Notificações dentro da plataforma (sino na barra superior). API pública para os outros módulos. */
@Service
public class NotificationService {
    private final NotificationRepository repo;
    private final Clock clock;
    private final org.springframework.context.ApplicationEventPublisher events;

    public NotificationService(NotificationRepository repo, Clock clock, org.springframework.context.ApplicationEventPublisher events) {
        this.repo = repo;
        this.clock = clock;
        this.events = events;
    }

    public record NotificationView(UUID id, String kind, String title, String body, String link, boolean read, Instant at) {}

    /** Cria a notificação na transacção do chamador. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void notify(UUID userId, String kind, String title, String body, String link) {
        if (userId == null) return;
        Notification n = repo.save(new Notification(userId, kind, cut(title, 160), cut(body, 500), link));
        events.publishEvent(new NotificationCreated(n.getId(), userId, kind, n.getTitle(), n.getBody(), link));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void notifyAll(Collection<UUID> userIds, String kind, String title, String body, String link) {
        userIds.forEach(id -> notify(id, kind, title, body, link));
    }

    @Transactional(readOnly = true)
    public CursorPage<NotificationView> page(UUID userId, String cursor, Integer limit) {
        int lim = CursorPage.clamp(limit);
        UUID c = cursor == null || cursor.isBlank() ? null : UUID.fromString(cursor);
        return CursorPage.of(repo.page(userId, c, PageRequest.of(0, lim + 1)), lim, NotificationService::view, n -> n.getId().toString());
    }

    @Transactional(readOnly = true)
    public long unread(UUID userId) {
        return repo.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markRead(UUID userId, UUID id) {
        Notification n = repo.findById(id).filter(x -> x.getUserId().equals(userId))
                .orElseThrow(() -> DomainException.notFound("NOTIFICATION_NOT_FOUND", "A notificação não existe."));
        n.markRead(clock.instant());
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return repo.markAllRead(userId, clock.instant());
    }

    static NotificationView view(Notification n) {
        return new NotificationView(n.getId(), n.getKind(), n.getTitle(), n.getBody(), n.getLink(), n.getReadAt() != null, n.getCreatedAt());
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
