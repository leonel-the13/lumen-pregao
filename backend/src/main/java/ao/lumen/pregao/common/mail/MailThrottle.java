package ao.lumen.pregao.common.mail;

import ao.lumen.pregao.common.config.AppProperties;
import java.util.concurrent.locks.LockSupport;
import org.springframework.stereotype.Component;

/**
 * Espaça os envios de email de todo o processo (o fornecedor limita pedidos por segundo).
 * Cada chamada a {@link #acquire()} espera pela próxima vaga.
 */
@Component
public class MailThrottle {
    private final long intervalNanos;
    private long next = System.nanoTime();

    public MailThrottle(AppProperties props) {
        this.intervalNanos = props.mail().minInterval().toNanos();
    }

    public void acquire() {
        long wait;
        synchronized (this) {
            long now = System.nanoTime();
            long slot = Math.max(now, next);
            next = slot + intervalNanos;
            wait = slot - now;
        }
        long until = System.nanoTime() + wait;
        while (wait > 0) {
            LockSupport.parkNanos(wait);
            wait = until - System.nanoTime();
        }
    }
}
