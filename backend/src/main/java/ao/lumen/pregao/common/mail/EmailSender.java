package ao.lumen.pregao.common.mail;

/** Envio de email (texto + HTML). Implementações: Resend (API HTTP, produção) ou SMTP (desenvolvimento, Mailpit). */
public interface EmailSender {
    void send(String to, EmailTemplate.Email email);
}
