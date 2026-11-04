package ao.lumen.pregao.identity.api;

import ao.lumen.pregao.identity.domain.UserStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class AdminUserDtos {
    private AdminUserDtos() {}

    public record AdminUserRow(UUID id, String name, String initials, String email, String course, Integer year,
                               Set<String> roles, String status, boolean mfaEnabled, Instant lastLoginAt) {}

    public record RolesUpdate(@NotNull @Size(min = 1, max = 7) Set<String> roles) {}

    public record InviteRequest(
            @jakarta.validation.constraints.NotBlank(message = "Indica o nome.") @Size(min = 2, max = 120, message = "O nome tem de ter entre 2 e 120 caracteres.") String name,
            @jakarta.validation.constraints.NotBlank(message = "Indica o email.") @jakarta.validation.constraints.Email(message = "Email inválido.") @Size(max = 254) String email,
            @NotNull(message = "Escolhe pelo menos um perfil.") @Size(min = 1, max = 7, message = "Escolhe pelo menos um perfil.") Set<String> roles,
            UUID courseId,
            @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(5) Integer year) {}

    public record EmailUpdate(
            @jakarta.validation.constraints.NotBlank(message = "Indica o email.") @jakarta.validation.constraints.Email(message = "Email inválido.") @Size(max = 254, message = "O email é demasiado longo.") String email) {}

    public record StatusUpdate(@NotNull UserStatus status) {}
}
