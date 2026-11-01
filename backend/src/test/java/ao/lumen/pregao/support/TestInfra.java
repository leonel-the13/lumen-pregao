package ao.lumen.pregao.support;

import ao.lumen.pregao.identity.app.AuthEvents;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestInfra {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redis() {
        return new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
    }

    @Bean
    @Primary
    MutableClock testClock() {
        return new MutableClock();
    }

    @Bean
    TokenCapture tokenCapture() {
        return new TokenCapture();
    }

    /** Guarda os tokens de verificação enviados por email (o email real não é lido nos testes). */
    public static class TokenCapture {
        public final ConcurrentHashMap<String, String> byEmail = new ConcurrentHashMap<>();

        public final ConcurrentHashMap<String, String> passwordLinks = new ConcurrentHashMap<>();

        @EventListener
        public void on(AuthEvents.VerificationRequested e) {
            byEmail.put(e.email(), e.token());
        }

        @EventListener
        public void on(AuthEvents.PasswordResetRequested e) {
            passwordLinks.put(e.email(), e.token());
        }

        @EventListener
        public void on(AuthEvents.UserInvited e) {
            passwordLinks.put(e.email(), e.token());
        }
    }
}
