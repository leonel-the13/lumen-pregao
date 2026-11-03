package ao.lumen.pregao.messaging.app;

import ao.lumen.pregao.common.config.AppProperties;
import ao.lumen.pregao.common.mail.EmailTemplate;
import ao.lumen.pregao.common.mail.MailQueue;
import ao.lumen.pregao.identity.app.MailingDirectory;
import ao.lumen.pregao.notification.app.NotificationCreated;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * As notificações importantes também chegam por email, depois do commit: segurança (sempre), convites de equipa,
 * contests e resultados, exames corrigidos e fluxo editorial. Conquistas, boas-vindas e saídas de equipa ficam só no sino.
 */
@Component
public class NotificationEmailBridge {
    private static final Logger log = LoggerFactory.getLogger(NotificationEmailBridge.class);
    public static final Set<String> EMAIL_KINDS = Set.of("SECURITY", "TEAM_INVITE", "CONTEST", "EXAM", "EDITORIAL", "LEGAL_ALERT");
    /** Chegam mesmo com os emails desligados. */
    public static final Set<String> ALWAYS = Set.of("SECURITY");

    private final MailingDirectory directory;
    private final MailQueue mail;
    private final AppProperties props;
    private final ExecutorService executor;

    public NotificationEmailBridge(MailingDirectory directory, MailQueue mail, AppProperties props, ExecutorService gradingExecutor) {
        this.directory = directory;
        this.mail = mail;
        this.props = props;
        this.executor = gradingExecutor;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void on(NotificationCreated e) {
        if (!EMAIL_KINDS.contains(e.kind())) return;
        executor.submit(() -> {
            try {
                directory.active(e.userId()).ifPresent(r -> {
                    boolean always = ALWAYS.contains(e.kind());
                    if (!always && !r.emailOptIn()) return;
                    mail.sendNow(r.email(), email(e, r.name(), always));
                });
            } catch (RuntimeException ex) {
                log.warn("Falha ao enviar a notificação {} por email: {}", e.notificationId(), ex.getMessage());
            }
        });
    }

    EmailTemplate.Email email(NotificationCreated e, String name, boolean always) {
        String base = props.baseUrl();
        String url = e.link() != null && e.link().startsWith("/") && !e.link().startsWith("//") ? base + e.link() : base + "/inicio";
        java.util.ArrayList<String> paragraphs = new java.util.ArrayList<>();
        paragraphs.add("Olá, " + EmailTemplate.firstName(name) + "!");
        if (e.body() != null && !e.body().isBlank()) paragraphs.add(e.body());
        paragraphs.add("Também a encontras nas notificações do Lúmen Lab (o sino no topo da página).");
        return EmailTemplate.build(e.title() + " · Lúmen Lab", e.body() == null ? e.title() : e.body(), e.title(),
                paragraphs, "Abrir no Lúmen Lab", url, null, always ? null : base + "/definicoes");
    }
}
