package ao.lumen.pregao.identity.app;

import ao.lumen.pregao.common.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Lembretes automáticos uma vez por dia, de manhã em Luanda (fora do horário nocturno). */
@Component
public class AccountReminderScheduler {
    private static final Logger log = LoggerFactory.getLogger(AccountReminderScheduler.class);
    private final AccountReminders reminders;
    private final boolean enabled;

    public AccountReminderScheduler(AccountReminders reminders, AppProperties props) {
        this.reminders = reminders;
        this.enabled = props.mail().remindersEnabled();
    }

    @Scheduled(cron = "${app.mail.reminders-cron:0 0 10 * * *}", zone = "Africa/Luanda")
    public void daily() {
        if (!enabled) return;
        try {
            int sent = reminders.runAutomatic().sent();
            if (sent > 0) log.info("Lembretes automáticos enviados: {}", sent);
        } catch (RuntimeException e) {
            log.error("Falha nos lembretes automáticos", e);
        }
    }
}
