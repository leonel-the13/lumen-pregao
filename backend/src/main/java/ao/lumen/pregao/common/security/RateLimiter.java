package ao.lumen.pregao.common.security;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Janela fixa em Redis (INCR + EXPIRE). Se o Redis falhar, os limites continuam a valer com contadores em memória
 * desta instância (os limites nunca desaparecem: login, 2.º factor, registos e emails continuam protegidos).
 */
@Component
public class RateLimiter {
    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);
    private final StringRedisTemplate redis;
    private final Map<String, Window> local = new ConcurrentHashMap<>();

    private record Window(long count, long expiresAtMillis) {
        boolean expired() { return System.currentTimeMillis() >= expiresAtMillis; }
    }

    public RateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Incrementa e devolve true se ainda está dentro do limite. */
    public boolean tryAcquire(String key, int max, Duration window) {
        return increment(key, window) <= max;
    }

    public long increment(String key, Duration window) {
        try {
            String k = "rl:" + key;
            Long n = redis.opsForValue().increment(k);
            if (n != null && n == 1L) redis.expire(k, window);
            return n == null ? 0 : n;
        } catch (RuntimeException e) {
            log.warn("Rate limiter em Redis indisponível ({}); a usar contadores em memória", e.getClass().getSimpleName());
            return local.compute(key, (k, w) -> w == null || w.expired()
                    ? new Window(1, System.currentTimeMillis() + window.toMillis())
                    : new Window(w.count() + 1, w.expiresAtMillis())).count();
        }
    }

    public long count(String key) {
        try {
            String v = redis.opsForValue().get("rl:" + key);
            return v == null ? 0 : Long.parseLong(v);
        } catch (RuntimeException e) {
            Window w = local.get(key);
            return w == null || w.expired() ? 0 : w.count();
        }
    }

    /** Segundos até o limite expirar (0 se não houver limite activo). */
    public long ttlSeconds(String key) {
        try {
            Long t = redis.getExpire("rl:" + key);
            return t == null || t < 0 ? 0 : t;
        } catch (RuntimeException e) {
            Window w = local.get(key);
            return w == null || w.expired() ? 0 : Math.max(0, (w.expiresAtMillis() - System.currentTimeMillis()) / 1000);
        }
    }

    public void reset(String key) {
        local.remove(key);
        try {
            redis.delete("rl:" + key);
        } catch (RuntimeException e) {
            log.warn("Rate limiter em Redis indisponível: {}", e.getClass().getSimpleName());
        }
    }
}
