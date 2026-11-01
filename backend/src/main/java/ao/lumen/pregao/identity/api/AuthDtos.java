package ao.lumen.pregao.identity.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank(message = "Indica o teu nome completo.") @Size(min = 2, max = 120, message = "O nome tem de ter entre 2 e 120 caracteres.") String name,
            @NotBlank(message = "Indica o teu email institucional.") @Email(message = "Email inválido.") @Size(max = 254, message = "O email é demasiado longo.") String email,
            @NotNull(message = "Escolhe o curso.") UUID courseId,
            @NotNull(message = "Escolhe o ano.") @Min(value = 1, message = "Ano inválido.") @Max(value = 5, message = "Ano inválido.") Integer year,
            @NotBlank(message = "Indica uma palavra-passe.") @Size(min = 12, max = 128, message = "A palavra-passe tem de ter entre 12 e 128 caracteres.") String password,
            @AssertTrue(message = "Tens de aceitar a política de privacidade.") boolean consent) {}

    public record VerifyRequest(@NotBlank @Size(max = 128) String token) {}

    public record ResendRequest(@NotBlank @Email @Size(max = 254) String email) {}

    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 128) String password,
            boolean remember) {}

    /** Código de 6 dígitos da aplicação ou código de recuperação (XXXX-XXXX). */
    public record MfaRequest(@NotBlank @Pattern(regexp = "\\d{6}|[A-Za-z0-9]{4}-?[A-Za-z0-9]{4}",
            message = "Escreve o código de 6 dígitos ou um código de recuperação.") String code) {}

    public record MfaResult(String status, java.util.List<String> recoveryCodes) {}

    /** Resposta de login quando falta o 2.º factor. `otpauthUri`/`secret` só no primeiro registo do factor. */
    public record LoginStep(String status, String otpauthUri, String secret) {}

    public record Accepted(String status, String detail) {}

    public record ForgotRequest(@NotBlank(message = "Indica o teu email.") @Email(message = "Email inválido.") @Size(max = 254) String email) {}

    public record ResetRequest(
            @NotBlank @Size(max = 128) String token,
            @NotBlank(message = "Indica uma palavra-passe.") @Size(min = 12, max = 128, message = "A palavra-passe tem de ter entre 12 e 128 caracteres.") String password) {}

    public record CsrfResponse(String headerName, String token) {}

    public record CourseDto(UUID id, String code, String name) {}
}
