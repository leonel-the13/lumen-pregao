package ao.lumen.pregao.identity.app;

import java.util.UUID;

public final class AuthEvents {
    private AuthEvents() {}

    /** {@code reminder}: lembrete de uma conta que ainda não foi confirmada (texto diferente, mesmo link). */
    public record VerificationRequested(UUID userId, String email, String name, String token, boolean reminder) {}

    /** Estudante sem actividade há {@code days} dias (lembrete leve, respeita a preferência de emails). */
    public record InactivityReminder(UUID userId, String email, String name, long days) {}

    public record DuplicateRegistration(String email) {}

    public record PasswordResetRequested(String email, String name, String token) {}

    /** Conta anonimizada: os módulos retiram-na das projecções (rankings). */
    public record AccountDeleted(java.util.UUID userId) {}

    public record UserInvited(String email, String name, String invitedBy, String token) {}
}
