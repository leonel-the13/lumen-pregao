package ao.lumen.pregao.learning.api;

import ao.lumen.pregao.authoring.app.ExerciseCatalog.ReferenceView;
import ao.lumen.pregao.learning.app.ProgressService.RoomState;
import ao.lumen.pregao.learning.app.ProgressService.TaskState;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** DTOs do estudante. Nunca incluem solutionSpec, paramSpec, tolerâncias nem dicas por revelar (SEC-06). */
public final class LearningDtos {
    private LearningDtos() {}

    public record TrackCard(String slug, String title, String summary, int difficulty, String topic,
                            List<String> courses, String status, int modulesCount, int roomsCount, Integer estHours,
                            int percent, UUID nextRoomId) {}

    public record RoomCard(UUID id, String title, String description, String trackSlug, String trackTitle,
                           int modulePosition, String moduleTitle, int position, int difficulty, int estMinutes,
                           RoomState state, int obtained, int possible, String lockedReason) {}

    public record ModuleView(UUID id, int position, String title, String summary, int roomsCount, int percent,
                             RoomState state, List<RoomCard> rooms) {}

    public record TrackDetail(String slug, String title, String summary, String description, int difficulty,
                              String topic, List<String> competencies, String completionRule, String status,
                              int percent, Integer estHours, UUID nextRoomId, List<ModuleView> modules,
                              List<ReferenceView> references) {}

    public record Breadcrumb(String trackSlug, String trackTitle, int modulePosition, String moduleTitle,
                             int roomPosition) {}

    public record TaskView(UUID id, int position, String title, int points, TaskState state, int score,
                           int attemptsUsed, int maxAttempts, String type) {}

    public record RoomDetail(UUID id, String title, String description, List<String> objectives, int difficulty,
                             int estMinutes, String context, JsonNode materials, RoomState state, int obtained,
                             int possible, Breadcrumb breadcrumb, List<TaskView> tasks, List<ReferenceView> references,
                             String lockedReason) {}

    public record SubmissionView(UUID id, String status, String outcome, Boolean correct, Integer points,
                                 String summary, String hint, JsonNode details, String errorCode,
                                 Instant receivedAt, Instant gradedAt, Long queuePosition) {}

    public record AttemptView(UUID attemptId, UUID taskId, String type, String title, String statementRendered,
                              JsonNode statementSegments, JsonNode params, JsonNode presentation, String status,
                              int attemptsUsed, int maxAttempts, int hintsUsed, int hintsAvailable,
                              List<String> revealedHints, JsonNode draft, int score, int points, String feedbackPolicy,
                              String pgcVersion, List<ReferenceView> references, SubmissionView lastSubmission,
                              Instant serverNow) {}

    public record SubmissionAccepted(UUID submissionId, String status, Instant receivedAt, Instant serverNow) {}

    public record HintView(int index, String text, int penalty, int hintsUsed) {}
}
