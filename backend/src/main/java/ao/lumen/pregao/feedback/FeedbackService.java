package ao.lumen.pregao.feedback;

import ao.lumen.pregao.common.audit.AuditService;
import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.common.security.RateLimiter;
import ao.lumen.pregao.common.text.MarkdownSanitizer;
import ao.lumen.pregao.common.web.CursorPage;
import ao.lumen.pregao.identity.app.UserDirectory;
import ao.lumen.pregao.notification.app.NotificationService;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Feedback sobre a plataforma: os estudantes enviam, a administração lê e resolve. */
@Service
public class FeedbackService {
    static final Set<String> CATEGORIES = Set.of("SUGGESTION", "BUG", "CONTENT", "PRAISE", "OTHER");
    private final FeedbackRepository repo;
    private final RateLimiter limiter;
    private final UserDirectory directory;
    private final NotificationService notifications;
    private final AuditService audit;

    public FeedbackService(FeedbackRepository repo, RateLimiter limiter, UserDirectory directory,
                           NotificationService notifications, AuditService audit) {
        this.repo = repo;
        this.limiter = limiter;
        this.directory = directory;
        this.notifications = notifications;
        this.audit = audit;
    }

    public record FeedbackView(UUID id, String category, Integer rating, String message, String page, String status,
                               String author, Instant at) {}

    @Transactional
    public UUID submit(UUID user, String category, Integer rating, String message, String page) {
        if (!CATEGORIES.contains(category)) throw DomainException.badRequest("CATEGORY_INVALID", "Escolhe o tipo de feedback.");
        if (rating != null && (rating < 1 || rating > 5)) throw DomainException.badRequest("RATING_INVALID", "A nota vai de 1 a 5.");
        if (!limiter.tryAcquire("feedback:" + user, 5, Duration.ofHours(1))) {
            throw DomainException.tooMany("Já enviaste vários comentários na última hora. Obrigado! Tenta de novo mais tarde.");
        }
        Feedback f = repo.save(new Feedback(user, category, rating, MarkdownSanitizer.clean(message), page));
        notifications.notifyAll(directory.idsWithRoles(Set.of("ADMIN")), "FEEDBACK", "Novo feedback recebido",
                message.length() > 120 ? message.substring(0, 119) + "…" : message, "/admin?tab=feedback");
        return f.getId();
    }

    @Transactional(readOnly = true)
    public CursorPage<FeedbackView> page(String status, String cursor, Integer limit) {
        int lim = CursorPage.clamp(limit);
        var rows = repo.page(status == null || status.isBlank() ? null : status,
                cursor == null || cursor.isBlank() ? null : UUID.fromString(cursor), PageRequest.of(0, lim + 1));
        var users = directory.summaries(new HashSet<>(rows.stream().map(Feedback::getUserId).filter(java.util.Objects::nonNull).toList()));
        return CursorPage.of(rows, lim, f -> new FeedbackView(f.getId(), f.getCategory(), f.getRating(), f.getMessage(), f.getPage(),
                f.getStatus(), f.getUserId() != null && users.containsKey(f.getUserId()) ? users.get(f.getUserId()).name() : "—",
                f.getCreatedAt()), f -> f.getId().toString());
    }

    @Transactional
    public void setStatus(UUID actor, UUID id, String status) {
        if (!Set.of("NEW", "READ", "RESOLVED").contains(status)) throw DomainException.badRequest("STATUS_INVALID", "Estado inválido.");
        Feedback f = repo.findById(id).orElseThrow(() -> DomainException.notFound("FEEDBACK_NOT_FOUND", "O feedback não existe."));
        String before = f.getStatus();
        f.setStatus(status);
        audit.record(actor, "FEEDBACK_STATUS", "Feedback", id, Map.of("status", before), Map.of("status", status));
    }
}
