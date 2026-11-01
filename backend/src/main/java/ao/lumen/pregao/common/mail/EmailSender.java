package ao.lumen.pregao.common.mail;

/** Envio de email de texto. Implementações: Resend (API HTTP, produção) ou SMTP (desenvolvimento, Mailpit). */
public interface EmailSender {
    void send(String to, String subject, String body);
}
