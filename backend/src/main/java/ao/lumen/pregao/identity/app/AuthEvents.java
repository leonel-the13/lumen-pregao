package ao.lumen.pregao.identity.app;

import java.util.UUID;

public final class AuthEvents {
    private AuthEvents() {}

    public record VerificationRequested(UUID userId, String email, String name, String token) {}

    public record DuplicateRegistration(String email) {}
}
