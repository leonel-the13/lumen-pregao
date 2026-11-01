package ao.lumen.pregao.it;

import static org.assertj.core.api.Assertions.assertThat;

import ao.lumen.pregao.authoring.app.ExerciseCatalog;
import ao.lumen.pregao.grading.app.GradingService;
import ao.lumen.pregao.grading.app.SolutionPreviewer;
import ao.lumen.pregao.learning.infra.AttemptRepository;
import ao.lumen.pregao.learning.infra.SubmissionRepository;
import ao.lumen.pregao.support.Api;
import ao.lumen.pregao.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LearningIT extends IntegrationTest {
    @Autowired AttemptRepository attempts;
    @Autowired SubmissionRepository submissions;
    @Autowired ExerciseCatalog catalog;
    @Autowired SolutionPreviewer previewer;
    @Autowired GradingService grading;

    JsonNode room(Api a, String trackSlug, String titlePrefix) {
        for (JsonNode m : a.get("/tracks/" + trackSlug).body().path("modules")) {
            for (JsonNode r : m.path("rooms")) {
                if (r.path("title").asText().startsWith(titlePrefix)) return a.get("/rooms/" + r.path("id").asText()).body();
            }
        }
        throw new IllegalStateException("sala não encontrada");
    }

    JsonNode expected(String attemptId) {
        var at = attempts.findById(UUID.fromString(attemptId)).orElseThrow();
        return previewer.expectedAnswer(catalog.definition(at.getExerciseVersionId()), at.getParams());
    }

    @Test
    void lockedRoomRefusesAttemptsOnTheServer() {
        Api a = student("ana.lopes@exemplo.ao");
        JsonNode r = room(a, "contabilidade-pgc", "Balanço");
        assertThat(r.path("state").asText()).isEqualTo("LOCKED");
        var res = a.post("/tasks/" + r.path("tasks").get(0).path("id").asText() + "/attempts", null);
        assertThat(res.status()).isEqualTo(403);
        assertThat(res.code()).isEqualTo("ROOM_LOCKED");
    }

    @Test
    void attemptNeverExposesSolutionOrGenerationRules() {
        Api a = student("ana.lopes@exemplo.ao");
        JsonNode r = room(a, "fiscalidade-iva", "Apuramento");
        String body = a.post("/tasks/" + r.path("tasks").get(0).path("id").asText() + "/attempts", null).body().toString();
        assertThat(body).doesNotContain("solutionSpec").doesNotContain("paramSpec").doesNotContain("\"expr\"")
                .doesNotContain("tolerance").doesNotContain("legalSourceId").doesNotContain("Se o IVA dedutível");
        assertThat(body).contains("statementSegments").contains("unconfirmed");
    }

    @Test
    void hundredParallelIdenticalSubmissionsCreateOnlyOne() throws Exception {
        Api a = student("ana.lopes@exemplo.ao");
        JsonNode r = room(a, "fiscalidade-iva", "Natureza");
        String attemptId = a.post("/tasks/" + r.path("tasks").get(1).path("id").asText() + "/attempts", null)
                .body().path("attemptId").asText();
        String key = UUID.randomUUID().toString();
        Map<String, Object> answer = Map.of("value", "1");
        List<Callable<Api.Res>> calls = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            calls.add(() -> a.send("POST", "/attempts/" + attemptId + "/submissions", answer, Map.of("Idempotency-Key", key), true));
        }
        try (var pool = Executors.newFixedThreadPool(20)) {
            var results = pool.invokeAll(calls).stream().map(f -> { try { return f.get(); } catch (Exception e) { throw new RuntimeException(e); } }).toList();
            assertThat(results).allMatch(x -> x.status() == 202);
            assertThat(results.stream().map(x -> x.body().path("submissionId").asText()).distinct()).hasSize(1);
        }
        assertThat(submissions.findByAttemptIdOrderByReceivedAtDesc(UUID.fromString(attemptId),
                org.springframework.data.domain.PageRequest.of(0, 200))).hasSize(1);
    }

    @Test
    void failedAttemptsCostPointsAndGradingIsReproducible() {
        Api a = student("joao.cassoma@exemplo.ao");
        JsonNode r = room(a, "contabilidade-pgc", "Compra de mercadorias");
        JsonNode task = r.path("tasks").get(2);
        String attemptId = a.post("/tasks/" + task.path("id").asText() + "/attempts", null).body().path("attemptId").asText();
        String wrong = "{\"lines\":[{\"account\":\"26\",\"debit\":1},{\"account\":\"45\",\"credit\":1}]}";
        var w = a.post("/attempts/" + attemptId + "/submissions", wrong, Map.of("Idempotency-Key", UUID.randomUUID().toString()));
        var wr = waitGraded(a, w.body().path("submissionId").asText());
        assertThat(wr.body().path("correct").asBoolean()).isFalse();
        assertThat(wr.body().path("details").path("lines").get(1).path("status").asText()).isEqualTo("WRONG_ACCOUNT");

        JsonNode answer = expected(attemptId);
        var ok = a.post("/attempts/" + attemptId + "/submissions", answer, Map.of("Idempotency-Key", UUID.randomUUID().toString()));
        var okr = waitGraded(a, ok.body().path("submissionId").asText());
        assertThat(okr.body().path("correct").asBoolean()).isTrue();
        assertThat(okr.body().path("points").asInt()).isEqualTo(task.path("points").asInt() - 5);

        var at = attempts.findById(UUID.fromString(attemptId)).orElseThrow();
        var def = catalog.definition(at.getExerciseVersionId());
        assertThat(grading.grade(def, at.getParams(), answer)).isEqualTo(grading.grade(def, at.getParams(), answer));
        assertThat(a.post("/attempts/" + attemptId + "/submissions", answer, Map.of("Idempotency-Key", UUID.randomUUID().toString())).code())
                .isEqualTo("ATTEMPT_CLOSED");
        assertThat(a.post("/attempts/" + attemptId + "/submissions", answer, Map.of()).status()).isEqualTo(400);
    }

    @Test
    void otherUsersCannotReadMyAttempts() {
        Api a = student("ana.lopes@exemplo.ao");
        JsonNode r = room(a, "analise-demonstracoes", "Liquidez");
        String attemptId = a.post("/tasks/" + r.path("tasks").get(0).path("id").asText() + "/attempts", null).body().path("attemptId").asText();
        Api other = student("kiala.afonso@exemplo.ao");
        assertThat(other.get("/attempts/" + attemptId).status()).isEqualTo(404);
        assertThat(other.put("/attempts/" + attemptId + "/draft", Map.of("x", 1)).status()).isEqualTo(404);
    }
}
