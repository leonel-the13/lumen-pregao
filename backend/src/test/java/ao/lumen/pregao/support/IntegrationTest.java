package ao.lumen.pregao.support;

import ao.lumen.pregao.identity.app.Totp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base dos testes de integração: PostgreSQL e Redis em Testcontainers, dados de desenvolvimento
 * (perfil dev) e perfil test por cima (cookie Secure, chaves de teste, agendador de contests manual).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev", "test"})
@Import(TestInfra.class)
public abstract class IntegrationTest {
    public static final String PASSWORD = "lumen-dev-2026!";

    @LocalServerPort
    protected int port;
    @Autowired
    protected MutableClock clock;
    @Autowired
    protected StringRedisTemplate redis;

    @BeforeEach
    void resetLimits() {
        clock.reset();
        Set<String> keys = redis.keys("rl:*");
        if (keys != null && !keys.isEmpty()) redis.delete(keys);
    }

    protected Api api() {
        return new Api(port);
    }

    protected Api student(String email) {
        Api a = api();
        var r = a.login(email, PASSWORD);
        if (r.status() != 204) throw new IllegalStateException("login falhou: " + r);
        return a;
    }

    /** Entra com 2.º factor (regista-o no primeiro login e guarda o segredo para logins seguintes). */
    protected Api privileged(String email) {
        Api a = api();
        var r = a.login(email, PASSWORD);
        String secret = r.body().path("secret").asText(null);
        if (secret != null) SECRETS.put(email, secret);
        secret = SECRETS.get(email);
        var m = a.post("/auth/mfa", Map.of("code", Totp.generate(secret, clock.instant().getEpochSecond() / 30)));
        if (m.status() != 204 && m.status() != 200) throw new IllegalStateException("mfa falhou: " + m);
        return a;
    }

    protected static void rememberSecret(String email, String secret) {
        SECRETS.put(email, secret);
    }

    private static final Map<String, String> SECRETS = new java.util.concurrent.ConcurrentHashMap<>();

    protected Api.Res waitGraded(Api a, String submissionId) {
        Instant end = Instant.now().plus(Duration.ofSeconds(15));
        Api.Res r;
        do {
            r = a.get("/submissions/" + submissionId);
            String s = r.body().path("status").asText();
            if (s.equals("GRADED") || s.equals("REJECTED")) return r;
            try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        } while (Instant.now().isBefore(end));
        return r;
    }
}
