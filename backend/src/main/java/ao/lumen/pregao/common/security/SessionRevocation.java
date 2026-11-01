package ao.lumen.pregao.common.security;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Invalidação de sessões por utilizador (suspensão, mudança de perfis, pedido de eliminação):
 * sessões autenticadas antes do instante registado deixam de ser aceites.
 */
@Component
public class SessionRevocation {
    private static final Logger log = LoggerFactory.getLogger(SessionRevocation.class);
    private final StringRedisTemplate redis;

    public SessionRevocation(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void revokeAll(UUID userId, Instant at) {
        try {
            redis.opsForValue().set("revoked:" + userId, Long.toString(at.toEpochMilli()));
        } catch (RuntimeException e) {
            log.warn("Não foi possível registar a revogação de sessões: {}", e.getClass().getSimpleName());
        }
    }

    public boolean isRevoked(SessionUser user) {
        try {
            String v = redis.opsForValue().get("revoked:" + user.id());
            return v != null && user.authenticatedAt().toEpochMilli() < Long.parseLong(v);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
