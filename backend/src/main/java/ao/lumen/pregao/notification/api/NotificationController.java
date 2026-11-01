package ao.lumen.pregao.notification.api;

import ao.lumen.pregao.common.security.SessionUser;
import ao.lumen.pregao.common.web.CursorPage;
import ao.lumen.pregao.notification.app.NotificationService;
import ao.lumen.pregao.notification.app.NotificationService.NotificationView;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public CursorPage<NotificationView> list(@AuthenticationPrincipal SessionUser me, @RequestParam(required = false) String cursor,
                                             @RequestParam(required = false) Integer limit) {
        return service.page(me.id(), cursor, limit);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(@AuthenticationPrincipal SessionUser me) {
        return Map.of("unread", service.unread(me.id()));
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        service.markRead(me.id(), id);
    }

    @PostMapping("/read-all")
    public Map<String, Integer> readAll(@AuthenticationPrincipal SessionUser me) {
        return Map.of("updated", service.markAllRead(me.id()));
    }
}
