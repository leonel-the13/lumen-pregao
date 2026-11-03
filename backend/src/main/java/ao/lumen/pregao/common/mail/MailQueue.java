package ao.lumen.pregao.common.mail;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fila de envio fora das transacções: um envio de cada vez, ao ritmo do {@link MailThrottle}.
 * Falhas ficam registadas sem o endereço (dados pessoais fora dos logs).
 */
@Component
public class MailQueue implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(MailQueue.class);
    private final EmailSender sender;
    private final MailThrottle throttle;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(Thread.ofVirtual().name("mail-", 0).factory());

    public MailQueue(EmailSender sender, MailThrottle throttle) {
        this.sender = sender;
        this.throttle = throttle;
    }

    /** Envia em segundo plano. */
    public void enqueue(String to, EmailTemplate.Email email) {
        worker.submit(() -> {
            try {
                sendNow(to, email);
            } catch (RuntimeException e) {
                log.warn("Falha ao enviar email «{}»: {}", email.subject(), e.getMessage());
            }
        });
    }

    /** Envia já (no fio do chamador), respeitando o ritmo; lança a excepção do fornecedor. */
    public void sendNow(String to, EmailTemplate.Email email) {
        throttle.acquire();
        sender.send(to, email);
    }

    @Override
    public void close() {
        worker.shutdown();
    }
}
