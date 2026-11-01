package ao.lumen.pregao.it;

import static org.assertj.core.api.Assertions.assertThat;

import ao.lumen.pregao.support.Api;
import ao.lumen.pregao.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuthoringIT extends IntegrationTest {

    Map<String, Object> exercise() {
        return Map.of("type", "B", "title", "Teste de IVA", "statement", "Calcula o IVA de [valor] <script>x</script>.",
                "topic", "IVA · Teste", "difficulty", 1, "points", 10,
                "paramSpec", Map.of("params", List.of(Map.of("name", "valor", "type", "money",
                        "rule", Map.of("kind", "range", "min", 100, "max", 200, "step", 10)))),
                "solutionSpec", Map.of("expr", "valor * 14 / 100", "scale", 2),
                "tolerance", Map.of("type", "ABS", "value", 0.01));
    }

    @Test
    void editorialFlowWithSeparationOfDuties() {
        Api author = student("marta.neto@exemplo.ao");
        JsonNode created = author.post("/authoring/exercises", exercise()).body();
        assertThat(created.path("versions").get(0).path("statement").asText()).doesNotContain("<script>");
        String exId = created.path("id").asText();
        String vid = created.path("versions").get(0).path("id").asText();

        assertThat(student("kiala.afonso@exemplo.ao").get("/authoring/exercises/" + exId).status()).isEqualTo(403);
        assertThat(author.post("/authoring/versions/" + vid + "/transition", Map.of("to", "IN_REVIEW")).status()).isEqualTo(200);
        assertThat(author.patch("/authoring/exercises/" + exId + "/versions/1", exercise()).code()).isEqualTo("VERSION_LOCKED");

        Api reviewer = privileged("docente.p@exemplo.ao");
        assertThat(reviewer.post("/authoring/versions/" + vid + "/preview?seed=1", null).body().path("selfCheck").asText()).isEqualTo("OK");
        assertThat(reviewer.post("/authoring/versions/" + vid + "/transition", Map.of("to", "APPROVED", "referencesChecked", true))
                .status()).isEqualTo(200);
        var noRef = reviewer.post("/authoring/versions/" + vid + "/transition", Map.of("to", "PUBLISHED"));
        assertThat(noRef.code()).isEqualTo("REFERENCE_REQUIRED");
    }

    @Test
    void newLegalVersionSendsLinkedExercisesBackToReview() {
        Api reviewer = privileged("docente.p@exemplo.ao");
        JsonNode pgc = null;
        for (JsonNode s : reviewer.get("/legal-sources").body()) if (s.path("title").asText().startsWith("Plano")) pgc = s;
        var body = Map.of("title", pgc.path("title").asText(), "identifier", pgc.path("identifier").asText(),
                "version", "nova versão (a confirmar)", "url", "", "confirmed", false);
        var r = reviewer.patch("/legal-sources/" + pgc.path("id").asText(), body);
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.body().path("affectedExercises").asInt()).isPositive();
        assertThat(student("marta.neto@exemplo.ao").get("/authoring/exercises").body().toString()).contains("NEEDS_REVIEW");
        assertThat(student("kiala.afonso@exemplo.ao").get("/rooms").body().toString()).isNotEmpty();
    }
}
