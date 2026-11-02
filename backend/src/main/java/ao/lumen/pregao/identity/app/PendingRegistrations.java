package ao.lumen.pregao.identity.app;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Liga o browser onde a pessoa se registou à conta criada, para esse browser entrar sozinho quando o email for
 * confirmado noutro dispositivo (ex.: registo no computador, link aberto no telemóvel).
 * O browser guarda um token aleatório num cookie HttpOnly; no Redis fica só o hash → id da conta, durante 24 h.
 * Quem fez o registo já provou conhecer a palavra-passe, por isso entrar neste browser não dá acesso novo.
 */
@Component
public class PendingRegistrations {
    public static final String COOKIE = "LP_PENDING";
    public static final Duration TTL = Duration.ofHours(24);
    private static final Logger log = LoggerFactory.getLogger(PendingRegistrations.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private final StringRedisTemplate redis;

    public PendingRegistrations(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Devolve sempre um token (mesmo sem conta), para a resposta não revelar se o email já existia. */
    public String issue(Optional<UUID> userId) {
        byte[] b = new byte[32];
        RANDOM.nextBytes(b);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        userId.ifPresent(id -> {
            try {
                redis.opsForValue().set(key(token), id.toString(), TTL);
            } catch (RuntimeException e) {
                log.warn("Não foi possível guardar o registo pendente: {}", e.getClass().getSimpleName());
            }
        });
        return token;
    }

    public Optional<UUID> resolve(String token) {
        if (token == null || token.isBlank() || token.length() > 100) return Optional.empty();
        try {
            return Optional.ofNullable(redis.opsForValue().get(key(token))).map(UUID::fromString);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    public void consume(String token) {
        try {
            redis.delete(key(token));
        } catch (RuntimeException e) {
            log.warn("Não foi possível apagar o registo pendente: {}", e.getClass().getSimpleName());
        }
    }

    private static String key(String token) {
        return "pending-reg:" + AuthService.sha256(token);
    }
}
