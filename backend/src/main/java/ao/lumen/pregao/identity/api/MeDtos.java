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
                             String status) {}

    /** Campos nulos não mudam. Pseudónimo vazio remove-o. */
    public record PatchMe(
            @Pattern(regexp = "^$|^[a-z0-9_]{3,24}$",
                    message = "O pseudónimo tem 3 a 24 caracteres: letras minúsculas, números e _.") String pseudonym,
            RankingVisibility rankingVisibility,
            Theme theme,
            Boolean portfolioPublic) {}
}
