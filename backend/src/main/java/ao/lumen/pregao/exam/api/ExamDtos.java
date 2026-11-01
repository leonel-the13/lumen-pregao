package ao.lumen.pregao.exam.api;

import ao.lumen.pregao.learning.api.LearningDtos.RoomCard;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** DTOs de exame do estudante: sem solução nem regras de geração. */
public final class ExamDtos {
    private ExamDtos() {}

    public record ExamCard(UUID id, String slug, String title, int durationMin, int questions, BigDecimal passMark,
                           UUID openSessionId, BigDecimal bestGrade, String lastStatus) {}

    public record SessionStarted(UUID sessionId, Instant endsAt, Instant serverNow) {}

    public record QuestionView(int n, String type, String statement, JsonNode segments, JsonNode presentation) {}

    public record SessionView(UUID id, UUID examId, String title, String status, Instant startedAt, Instant endsAt,
                              Instant serverNow, List<QuestionView> questions, JsonNode answers, JsonNode marked) {}

    public record AnswerInput(JsonNode answer, Boolean marked) {}

    public record AnswerSaved(int n, Instant savedAt, Instant serverNow) {}

    public record TopicResult(String topic, int correct, int total) {}

    public record Report(UUID sessionId, String title, String status, BigDecimal grade, BigDecimal passMark,
                         boolean passed, long secondsUsed, int correct, int total, List<TopicResult> topics,
                         List<String> strengths, List<String> weaknesses, List<Boolean> perQuestion,
                         List<RoomCard> recommendedRooms, boolean autoSubmitted) {}
}
