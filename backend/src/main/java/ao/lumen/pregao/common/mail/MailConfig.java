package ao.lumen.pregao.common.mail;

import ao.lumen.pregao.common.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Com RESEND_API_KEY definida, os emails seguem pela API HTTP do Resend (funciona em alojamentos que bloqueiam SMTP).
 * Sem a chave, usa SMTP (Mailpit em desenvolvimento).
 */
@Configuration
public class MailConfig {

    @Bean
    EmailSender emailSender(AppProperties props, JavaMailSender smtp, ObjectMapper json) {
        String key = props.resendApiKey();
        if (key != null && !key.isBlank()) return new ResendEmailSender(key, props.mailFrom(), json);
        return (to, subject, body) -> {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(props.mailFrom());
            m.setTo(to);
            m.setSubject(subject);
            m.setText(body);
            smtp.send(m);
        };
    }

    static final class ResendEmailSender implements EmailSender {
        private static final URI ENDPOINT = URI.create("https://api.resend.com/emails");
        private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        private final String key;
        private final String from;
        private final ObjectMapper json;

        ResendEmailSender(String key, String from, ObjectMapper json) {
            this.key = key;
            this.from = from;
            this.json = json;
        }

        @Override
        public void send(String to, String subject, String body) {
            try {
                String payload = json.writeValueAsString(Map.of("from", from, "to", List.of(to), "subject", subject, "text", body));
                HttpResponse<String> r = http.send(HttpRequest.newBuilder(ENDPOINT).timeout(Duration.ofSeconds(15))
                        .header("Authorization", "Bearer " + key).header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload)).build(), HttpResponse.BodyHandlers.ofString());
                if (r.statusCode() >= 300) throw new IllegalStateException("Resend respondeu " + r.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            } catch (Exception e) {
                throw new IllegalStateException("Falha ao enviar email pelo Resend", e);
            }
        }
    }
}
