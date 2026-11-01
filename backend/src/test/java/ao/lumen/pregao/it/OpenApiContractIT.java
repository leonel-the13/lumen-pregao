package ao.lumen.pregao.it;

import static org.assertj.core.api.Assertions.assertThat;

import ao.lumen.pregao.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Contrato OpenAPI verificado no CI: o gerado tem de ser igual a docs/openapi.json.
 * Para actualizar depois de uma mudança intencional: ./mvnw test -Dtest=OpenApiContractIT -DupdateOpenApi=true
 * (e actualizar docs/spec.md §6).
 */
class OpenApiContractIT extends IntegrationTest {
    private static final Path CONTRACT = Path.of("..", "docs", "openapi.json");
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void contractMatchesAndStudentSchemasNeverExposeSolutions() throws Exception {
        String body = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/openapi")).build(),
                HttpResponse.BodyHandlers.ofString()).body();
        JsonNode generated = JSON.readTree(body);
        ((com.fasterxml.jackson.databind.node.ObjectNode) generated).remove("servers");
        String normalized = JSON.writeValueAsString(JSON.treeToValue(generated, Object.class)) + "\n";

        JsonNode schemas = generated.path("components").path("schemas");
        for (String s : List.of("AttemptView", "SubmissionView", "ProblemView", "SessionView", "QuestionView", "RoomDetail", "TaskView")) {
            String schema = schemas.path(s).toString();
            assertThat(schemas.has(s)).as(s).isTrue();
            assertThat(schema).as(s).doesNotContain("solutionSpec").doesNotContain("paramSpec").doesNotContain("tolerance").doesNotContain("\"hints\"");
        }

        if (Boolean.getBoolean("updateOpenApi") || !Files.exists(CONTRACT)) {
            Files.writeString(CONTRACT, normalized);
        }
        assertThat(normalized).as("O contrato mudou: corre com -DupdateOpenApi=true e actualiza docs/spec.md §6")
                .isEqualTo(Files.readString(CONTRACT));
    }
}
