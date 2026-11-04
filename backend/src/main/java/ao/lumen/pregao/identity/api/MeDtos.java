package ao.lumen.pregao.identity.api;

import ao.lumen.pregao.identity.domain.RankingVisibility;
import ao.lumen.pregao.identity.domain.Theme;
import jakarta.validation.constraints.Pattern;
import java.util.Set;
import java.util.UUID;

public final class MeDtos {
    private MeDtos() {}

    public record CourseRef(UUID id, String code, String name) {}

    public record MeResponse(UUID id, String name, String shortName, String initials, String email, CourseRef course,
                             Integer year, Set<String> roles, String pseudonym, RankingVisibility rankingVisibility,
                             Theme theme, boolean portfolioPublic, String portfolioHandle, boolean mfaEnabled,
                             String status, boolean emailOptIn) {}

    /** Campos nulos não mudam. Pseudónimo vazio remove-o. */
    public record PatchMe(
            @jakarta.validation.constraints.Size(min = 2, max = 120, message = "O nome tem de ter entre 2 e 120 caracteres.")
            @Pattern(regexp = "^(?!\\s*$).+", message = "Indica o nome.") String name,
            UUID courseId,
            @jakarta.validation.constraints.Min(value = 1, message = "Ano inválido.") @jakarta.validation.constraints.Max(value = 5, message = "Ano inválido.") Integer year,
            @Pattern(regexp = "^$|^[a-z0-9_]{3,24}$",
                    message = "O pseudónimo tem 3 a 24 caracteres: letras minúsculas, números e _.") String pseudonym,
            RankingVisibility rankingVisibility,
            Theme theme,
            Boolean portfolioPublic,
            /** Emails de notificações, lembretes e da administração (os de segurança chegam sempre). */
            Boolean emailOptIn) {}

    public record ChangePassword(
            @jakarta.validation.constraints.NotBlank(message = "Indica a palavra-passe actual.") String currentPassword,
            @jakarta.validation.constraints.NotBlank(message = "Indica a nova palavra-passe.") @jakarta.validation.constraints.Size(max = 128, message = "A palavra-passe é demasiado longa.") String newPassword) {}
}
