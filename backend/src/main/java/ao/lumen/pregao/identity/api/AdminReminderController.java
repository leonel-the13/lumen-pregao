package ao.lumen.pregao.identity.api;

import ao.lumen.pregao.common.security.SessionUser;
import ao.lumen.pregao.identity.app.AccountReminders;
import ao.lumen.pregao.identity.app.AccountReminders.ReminderResult;
import ao.lumen.pregao.identity.app.AccountReminders.ReminderStats;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lembretes por email: contas por confirmar e estudantes inactivos. */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReminderController {
    private final AccountReminders reminders;

    public AdminReminderController(AccountReminders reminders) {
        this.reminders = reminders;
    }

    @GetMapping("/emails/reminders")
    public ReminderStats stats() {
        return reminders.stats();
    }

    @PostMapping("/emails/reminders/unverified")
    public ReminderResult remindUnverified(@AuthenticationPrincipal SessionUser me) {
        return reminders.remindAllUnverified(me.id());
    }

    @PostMapping("/emails/reminders/inactive")
    public ReminderResult remindInactive(@AuthenticationPrincipal SessionUser me) {
        return reminders.remindAllInactive(me.id());
    }

    @PostMapping("/users/{id}/remind-verification")
    public ReminderResult remindOne(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        return reminders.remindUnverified(me.id(), id);
    }
}
