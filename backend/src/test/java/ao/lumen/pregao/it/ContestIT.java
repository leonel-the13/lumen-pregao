package ao.lumen.pregao.it;

import static org.assertj.core.api.Assertions.assertThat;

import ao.lumen.pregao.authoring.app.ExerciseCatalog;
import ao.lumen.pregao.contest.app.ContestLifecycle;
import ao.lumen.pregao.contest.infra.ContestRepository;
import ao.lumen.pregao.grading.app.SolutionPreviewer;
import ao.lumen.pregao.learning.infra.AttemptRepository;
import ao.lumen.pregao.ranking.app.RankingProjection;
import ao.lumen.pregao.support.Api;
import ao.lumen.pregao.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Treino individual semeado: começa 10 min depois do arranque, congela aos 100 min, termina aos 130 min. */
class ContestIT extends IntegrationTest {
    @Autowired ContestLifecycle lifecycle;
    @Autowired ContestRepository contests;
    @Autowired AttemptRepository attempts;
    @Autowired ExerciseCatalog catalog;
    @Autowired SolutionPreviewer previewer;
    @Autowired RankingProjection projection;

    JsonNode answerFor(JsonNode problem) {
        var at = attempts.findById(UUID.fromString(problem.path("attemptId").asText())).orElseThrow();
        return previewer.expectedAnswer(catalog.definition(at.getExerciseVersionId()), at.getParams());
    }

    @Test
    void fullLifecycleWithFreezeAndServerDeadline() {
        var c = contests.findAll().stream().filter(x -> x.getSlug().equals("treino-ronda-0")).findFirst().orElseThrow();
        String id = c.getId().toString();
        Api kiala = student("kiala.afonso@exemplo.ao");
        Api ana = student("ana.lopes@exemplo.ao");
        Api joao = student("joao.cassoma@exemplo.ao");
        assertThat(kiala.post("/contests/" + id + "/registrations", null).status()).isIn(201, 409);
        assertThat(ana.post("/contests/" + id + "/registrations", null).status()).isIn(201, 409);
        assertThat(kiala.get("/contests/" + id + "/problems").code()).isEqualTo("STATEMENTS_NOT_RELEASED");

        clock.setTo(c.getStartsAt().plusSeconds(60));
        lifecycle.tick();
        assertThat(kiala.get("/contests/" + id).body().path("state").asText()).isEqualTo("RUNNING");
        assertThat(joao.post("/contests/" + id + "/registrations", null).code()).isEqualTo("REGISTRATION_CLOSED");

        var problems = kiala.get("/contests/" + id + "/problems").body();
        var a1 = kiala.post("/contests/" + id + "/problems/A/submissions", answerFor(problems.get(0)),
                Map.of("Idempotency-Key", UUID.randomUUID().toString()));
        assertThat(a1.status()).isEqualTo(202);
        assertThat(waitGraded(kiala, a1.body().path("submissionId").asText()).body().path("correct").asBoolean()).isTrue();
        // Outro participante não vê as submissões de Kiala.
        assertThat(ana.get("/submissions/" + a1.body().path("submissionId").asText()).status()).isEqualTo(404);

        clock.setTo(c.getFreezeAt().plusSeconds(30));
        lifecycle.tick();
        var b = kiala.post("/contests/" + id + "/problems/B/submissions", answerFor(problems.get(1)),
                Map.of("Idempotency-Key", UUID.randomUUID().toString()));
        waitGraded(kiala, b.body().path("submissionId").asText());

        JsonNode mine = kiala.get("/contests/" + id + "/scoreboard").body();
        JsonNode others = ana.get("/contests/" + id + "/scoreboard").body();
        assertThat(others.path("frozen").asBoolean()).isTrue();
        assertThat(row(mine, true).path("points").asInt()).isEqualTo(250);
        assertThat(rowByName(others, "Kiala A.").path("points").asInt()).isEqualTo(100);

        // Depois de endsAt, a submissão é registada como rejeitada, seja qual for a hora do cliente.
        clock.setTo(c.getEndsAt().plusSeconds(5));
        var late = ana.post("/contests/" + id + "/problems/A/submissions", "{\"value\":\"1\"}",
                Map.of("Idempotency-Key", UUID.randomUUID().toString(), "X-Client-Time", "2020-01-01T00:00:00Z"));
        assertThat(late.body().path("status").asText()).isEqualTo("REJECTED");
        assertThat(ana.get("/submissions/" + late.body().path("submissionId").asText()).body().path("errorCode").asText())
                .isEqualTo("SUBMISSION_AFTER_DEADLINE");

        lifecycle.tick();
        Api org = privileged("organizador@exemplo.ao");
        var pub = org.post("/admin/contests/" + id + "/publish-results", null);
        assertThat(pub.status()).isEqualTo(200);
        JsonNode after = ana.get("/contests/" + id + "/scoreboard").body();
        assertThat(after.path("frozen").asBoolean()).isFalse();
        assertThat(rowByName(after, "Kiala A.").path("points").asInt()).isEqualTo(250);
    }

    @Test
    void redisRankingCanBeRebuiltFromPostgres() {
        Api a = student("kiala.afonso@exemplo.ao");
        JsonNode before = a.get("/rankings?scope=GLOBAL&limit=50").body().path("entries");
        redis.delete(redis.keys("rank:*"));
        projection.rebuild();
        JsonNode after = a.get("/rankings?scope=GLOBAL&limit=50").body().path("entries");
        assertThat(after).isEqualTo(before);
    }

    static JsonNode row(JsonNode board, boolean mine) {
        for (JsonNode r : board.path("rows")) if (r.path("mine").asBoolean() == mine) return r;
        throw new IllegalStateException("linha não encontrada");
    }

    static JsonNode rowByName(JsonNode board, String name) {
        for (JsonNode r : board.path("rows")) if (r.path("name").asText().equals(name)) return r;
        throw new IllegalStateException("linha não encontrada: " + name + " em " + board);
    }
}
