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

    public record StatusUpdate(@NotNull UserStatus status) {}
}
