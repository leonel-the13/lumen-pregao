package ao.lumen.pregao.it;

import static org.assertj.core.api.Assertions.assertThat;

import ao.lumen.pregao.exam.app.ExamScheduler;
import ao.lumen.pregao.support.Api;
import ao.lumen.pregao.support.IntegrationTest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ExamIT extends IntegrationTest {
    @Autowired ExamScheduler scheduler;

    @Test
    void serverDeadlineWinsAndExpiredExamIsAutoSubmitted() {
        Api a = student("kiala.afonso@exemplo.ao");
        String examId = a.get("/exams").body().get(0).path("id").asText();
        var start = a.post("/exams/" + examId + "/sessions", null).body();
        String sid = start.path("sessionId").asText();
        var session = a.get("/exam-sessions/" + sid).body();
        assertThat(session.toString()).doesNotContain("solutionSpec").doesNotContain("correct");
        assertThat(a.put("/exam-sessions/" + sid + "/answers/1", Map.of("answer", Map.of("selected", List.of("A")), "marked", true))
                .status()).isEqualTo(200);

        clock.advance(Duration.ofMinutes(25));
        var late = a.put("/exam-sessions/" + sid + "/answers/2", Map.of("answer", Map.of("value", "1")));
        assertThat(late.status()).isEqualTo(422);
        assertThat(late.code()).isEqualTo("SUBMISSION_AFTER_DEADLINE");

        scheduler.expire();
        var report = a.get("/exam-sessions/" + sid + "/report").body();
        assertThat(report.path("status").asText()).isEqualTo("GRADED");
        assertThat(report.path("autoSubmitted").asBoolean()).isTrue();
        assertThat(report.path("total").asInt()).isEqualTo(session.path("questions").size());
    }
}
