package ao.lumen.pregao.identity.app;

import ao.lumen.pregao.common.config.AppProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ao.lumen.pregao.common.mail.EmailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** Envia emails depois do commit (nenhuma chamada externa dentro de transacções). */
@Component
public class IdentityMailer {
    private static final Logger log = LoggerFactory.getLogger(IdentityMailer.class);
    private final EmailSender mail;
    private final AppProperties props;
    private final ExecutorService executor;

    public IdentityMailer(EmailSender mail, AppProperties props, ExecutorService gradingExecutor) {
        this.mail = mail;
        this.props = props;
        this.executor = gradingExecutor;
    }

    @TransactionalEventListener
    public void onVerification(AuthEvents.VerificationRequested e) {
        String link = props.baseUrl() + "/verificar?token=" + URLEncoder.encode(e.token(), StandardCharsets.UTF_8);
        send(e.email(), "Confirma o teu email · Lúmen Pregão", """
                Olá, %s.

                Para activar a tua conta no Lúmen Pregão, abre este link no mesmo dispositivo:
                %s

                O link é válido durante %d horas. Se não criaste esta conta, ignora este email.

                Iniciativa da Associação de Estudantes · Não é um canal oficial do ISPTEC
                """.formatted(e.name(), link, props.identity().verificationTtl().toHours()));
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onDuplicate(AuthEvents.DuplicateRegistration e) {
        send(e.email(), "Já tens conta · Lúmen Pregão", """
                Olá.

                Alguém tentou criar uma conta com este email, mas ela já existe.
                Se foste tu, entra em %s/entrar. Se não foste, podes ignorar este email.

                Iniciativa da Associação de Estudantes · Não é um canal oficial do ISPTEC
                """.formatted(props.baseUrl()));
    }

    @TransactionalEventListener
    public void onReset(AuthEvents.PasswordResetRequested e) {
        String link = props.baseUrl() + "/redefinir?token=" + URLEncoder.encode(e.token(), StandardCharsets.UTF_8);
        send(e.email(), "Redefinir a palavra-passe · Lúmen Pregão", """
                Olá, %s.

                Recebemos um pedido para redefinir a tua palavra-passe. Abre este link para escolher uma nova:
                %s

                O link é válido durante 1 hora e só pode ser usado uma vez. Se não foste tu, ignora este email:
                a tua palavra-passe actual continua válida.

                Iniciativa da Associação de Estudantes · Não é um canal oficial do ISPTEC
                """.formatted(e.name(), link));
    }

    @TransactionalEventListener
    public void onInvite(AuthEvents.UserInvited e) {
        String link = props.baseUrl() + "/redefinir?convite=1&token=" + URLEncoder.encode(e.token(), StandardCharsets.UTF_8);
        send(e.email(), "Convite para o Lúmen Pregão", """
                Olá, %s.

                %s criou uma conta para ti no Lúmen Pregão. Antes de entrares, define a tua palavra-passe neste link:
                %s

                O link é válido durante 72 horas e só pode ser usado uma vez. Se o teu perfil exigir segundo factor,
                vais configurá-lo no primeiro login.

                Iniciativa da Associação de Estudantes · Não é um canal oficial do ISPTEC
                """.formatted(e.name(), e.invitedBy(), link));
    }

    private void send(String to, String subject, String body) {
        executor.submit(() -> {
            try {
                mail.send(to, subject, body);
            } catch (RuntimeException ex) {
                log.warn("Falha ao enviar email «{}»: {}", subject, ex.getMessage());
            }
        });
    }
}
