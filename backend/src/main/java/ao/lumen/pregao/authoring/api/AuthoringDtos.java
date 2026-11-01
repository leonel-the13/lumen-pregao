package ao.lumen.pregao.authoring.api;

import ao.lumen.pregao.authoring.domain.EditorialStatus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** DTOs de autoria: incluem solução e regras de geração. Só para autor/revisor/admin. */
public final class AuthoringDtos {
    private AuthoringDtos() {}

    public record ReferenceInput(@NotNull UUID legalSourceId, @Size(max = 200) String articles, LocalDate consultedAt) {}

    public record VersionInput(
            @NotBlank @Pattern(regexp = "[A-F]", message = "Tipo de tarefa inválido.") String type,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 8000) String statement,
            @Size(max = 120) String topic,
            JsonNode presentation,
            JsonNode paramSpec,
            JsonNode solutionSpec,
            JsonNode tolerance,
            JsonNode hints,
            @Pattern(regexp = "BINARY|DETAILED") String feedbackPolicy,
            @NotNull @Min(1) @Max(4) Integer difficulty,
            @NotNull @Min(0) @Max(1000) Integer points,
            @Min(1) @Max(10) Integer maxAttempts,
            @Size(max = 32) String pgcVersion,
            LocalDate validUntil,
            @Size(max = 200) String changeNote,
            @Size(max = 10) List<@Valid ReferenceInput> references) {}

    public record ReferenceDto(UUID legalSourceId, String title, String identifier, String version, String articles,
                               LocalDate consultedAt, boolean confirmed) {}

    public record VersionDto(UUID id, int number, String type, String title, String statement, JsonNode presentation,
                             JsonNode paramSpec, JsonNode solutionSpec, JsonNode tolerance, JsonNode hints,
                             String feedbackPolicy, int difficulty, int points, int maxAttempts, String pgcVersion,
                             LocalDate validUntil, String changeNote, EditorialStatus status, String reviewNotes,
                             Instant legalAlertAt, List<ReferenceDto> references, Instant updatedAt) {}

    public record ExerciseDto(UUID id, String code, String topic, UUID authorId, String authorName,
                              List<VersionDto> versions) {}

    public record ExerciseSummary(UUID id, String code, String topic, String authorName, int latestVersion,
                                  UUID latestVersionId, String title, String type, EditorialStatus status,
                                  Instant legalAlertAt, Instant updatedAt) {}

    public record TransitionRequest(@NotNull EditorialStatus to, @Size(max = 2000) String notes,
                                    boolean referencesChecked) {}

    public record PreviewResponse(JsonNode params, String statementRendered, JsonNode statementSegments,
                                  JsonNode presentation, JsonNode expectedAnswer, String selfCheck) {}

    public record LegalSourceInput(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 120) String identifier,
            @NotBlank @Size(max = 60) String version,
            LocalDate publishedAt, LocalDate effectiveFrom, LocalDate effectiveTo,
            @Size(max = 500) @Pattern(regexp = "^$|^https://.*", message = "Usa um endereço https.") String url,
            boolean confirmed) {}

    public record LegalSourceDto(UUID id, String title, String identifier, String version, LocalDate publishedAt,
                                 LocalDate effectiveFrom, LocalDate effectiveTo, String url, boolean confirmed,
                                 Instant updatedAt) {}

    public record LegalSourceUpdate(@Valid @NotNull LegalSourceInput source, int affectedExercises) {}
}
