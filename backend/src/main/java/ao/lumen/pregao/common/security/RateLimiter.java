package ao.lumen.pregao.common.security;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Janela fixa em Redis (INCR + EXPIRE). Se o Redis falhar, deixa passar e regista (disponibilidade). */
@Component
public class RateLimiter {
    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);
    private final StringRedisTemplate redis;

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
            log.warn("Rate limiter indisponível: {}", e.getClass().getSimpleName());
            return 0;
        }
    }

    public long count(String key) {
        try {
            String v = redis.opsForValue().get("rl:" + key);
            return v == null ? 0 : Long.parseLong(v);
        } catch (RuntimeException e) {
            log.warn("Rate limiter indisponível: {}", e.getClass().getSimpleName());
            return 0;
        }
    }

    public void reset(String key) {
        try {
            redis.delete("rl:" + key);
        } catch (RuntimeException e) {
            log.warn("Rate limiter indisponível: {}", e.getClass().getSimpleName());
        }
    }
}
