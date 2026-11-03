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
    @Primary
    SentMail sentMail() {
        return new SentMail();
    }

    /** Emails «enviados» durante os testes (nenhum sai para a rede). */
    public static class SentMail implements ao.lumen.pregao.common.mail.EmailSender {
        public record Sent(String to, ao.lumen.pregao.common.mail.EmailTemplate.Email email) {}

        public final java.util.concurrent.CopyOnWriteArrayList<Sent> all = new java.util.concurrent.CopyOnWriteArrayList<>();

        @Override
        public void send(String to, ao.lumen.pregao.common.mail.EmailTemplate.Email email) {
            all.add(new Sent(to, email));
        }

        public java.util.List<Sent> to(String email) {
            return all.stream().filter(s -> s.to().equals(email)).toList();
        }
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
