package ao.lumen.pregao.learning.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** DTOs da gestão de conteúdo (administração). */
public final class ContentDtos {
    private ContentDtos() {}

    public record TaskAdmin(UUID id, int position, String title, int points, UUID exerciseVersionId, String exerciseCode,
                            String exerciseTitle, String exerciseType, String editorialStatus, long attempts) {}

    public record RoomAdmin(UUID id, int position, String title, String status, int difficulty, int estMinutes,
                            List<UUID> prerequisites, String description, List<String> objectives, String context,
                            List<TaskAdmin> tasks) {}

    public record ModuleAdmin(UUID id, int position, String title, String summary, List<RoomAdmin> rooms) {}

    public record TrackAdmin(UUID id, String slug, String title, String status, int position, String summary, String description,
                             int difficulty, String topic, Integer estHours, List<ModuleAdmin> modules) {}

    public record TrackInput(
            @NotBlank @Pattern(regexp = "[a-z0-9-]{3,80}", message = "Usa minúsculas, números e -.") String slug,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 300) String summary,
            @Size(max = 4000) String description,
            @NotNull @Min(1) @Max(4) Integer difficulty,
            @NotBlank @Size(max = 60) String topic,
            List<@Size(max = 16) String> courses,
            List<@Size(max = 80) String> competencies,
            @Size(max = 300) String completionRule,
            @Min(0) @Max(500) Integer estHours,
            @Pattern(regexp = "DRAFT|PUBLISHED|COMING_SOON") String status) {}

    public record ModuleInput(@NotBlank @Size(max = 200) String title, @Size(max = 300) String summary) {}

    public record RoomInput(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 500) String description,
            List<@Size(max = 200) String> objectives,
            @NotNull @Min(1) @Max(4) Integer difficulty,
            @NotNull @Min(1) @Max(600) Integer estMinutes,
            @Size(max = 8000) String context,
            List<UUID> prerequisites,
            @Pattern(regexp = "DRAFT|PUBLISHED|COMING_SOON") String status) {}

    public record TaskInput(@NotNull UUID exerciseVersionId, @NotBlank @Size(max = 200) String title,
                            @NotNull @Min(0) @Max(1000) Integer points) {}

    public record StatusInput(@NotBlank @Pattern(regexp = "DRAFT|PUBLISHED|COMING_SOON") String status) {}
}
