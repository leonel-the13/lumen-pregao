package ao.lumen.pregao.identity.app;

import ao.lumen.pregao.common.config.AppProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ao.lumen.pregao.common.mail.EmailSender;
import ao.lumen.pregao.common.mail.EmailTemplate;
import java.util.List;
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
        send(e.email(), EmailTemplate.build("Confirma o teu email · Lúmen Lab", "Falta um passo para activares a tua conta.",
                "Olá, " + EmailTemplate.firstName(e.name()) + "!",
                List.of("Obrigado por te juntares ao Lúmen Lab, o laboratório de competências do DCSA: trilhas práticas, "
                        + "exames e a Maratona de casos, com correcção no servidor.",
                        "Para activares a conta, confirma que este email é teu. Abre o link no mesmo dispositivo onde te registaste."),
                "Confirmar o meu email", link,
                "O link é válido durante " + props.identity().verificationTtl().toHours() + " horas. Se não criaste esta conta, ignora este email."));
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onDuplicate(AuthEvents.DuplicateRegistration e) {
        send(e.email(), EmailTemplate.build("Já tens conta · Lúmen Lab", "Alguém tentou criar uma conta com o teu email.",
                "Olá!",
                List.of("Alguém tentou criar uma conta no Lúmen Lab com este email, mas ela já existe.",
                        "Se foste tu, entra com a tua palavra-passe. Se não te lembras dela, usa «Esqueci-me» no ecrã de entrada."),
                "Entrar no Lúmen Lab", props.baseUrl() + "/entrar", "Se não foste tu, podes ignorar este email: a tua conta não foi alterada."));
    }

    @TransactionalEventListener
    public void onReset(AuthEvents.PasswordResetRequested e) {
        String link = props.baseUrl() + "/redefinir?token=" + URLEncoder.encode(e.token(), StandardCharsets.UTF_8);
        send(e.email(), EmailTemplate.build("Redefinir a palavra-passe · Lúmen Lab", "Escolhe uma nova palavra-passe.",
                "Olá, " + EmailTemplate.firstName(e.name()) + "!",
                List.of("Recebemos um pedido para redefinir a palavra-passe da tua conta.",
                        "Ao definires a nova palavra-passe, as sessões abertas noutros dispositivos terminam."),
                "Escolher nova palavra-passe", link,
                "O link é válido durante 1 hora e só pode ser usado uma vez. Se não foste tu, ignora este email: a palavra-passe actual continua válida."));
    }

    @TransactionalEventListener
    public void onInvite(AuthEvents.UserInvited e) {
        String link = props.baseUrl() + "/redefinir?convite=1&token=" + URLEncoder.encode(e.token(), StandardCharsets.UTF_8);
        send(e.email(), EmailTemplate.build("Convite para o Lúmen Lab", e.invitedBy() + " convidou-te para a equipa da plataforma.",
                "Olá, " + EmailTemplate.firstName(e.name()) + "!",
                List.of(e.invitedBy() + " criou uma conta para ti no Lúmen Lab, a plataforma de prática do DCSA da Associação de Estudantes.",
                        "Antes de entrares, define a tua palavra-passe. Se o teu perfil for de administração, revisão, júri ou organização, "
                                + "vais configurar o segundo factor no primeiro login."),
                "Activar a minha conta", link, "O link é válido durante 72 horas e só pode ser usado uma vez."));
    }

    private void send(String to, EmailTemplate.Email email) {
        executor.submit(() -> {
            try {
                mail.send(to, email);
            } catch (RuntimeException ex) {
                log.warn("Falha ao enviar email «{}»: {}", email.subject(), ex.getMessage());
            }
        });
    }
}
