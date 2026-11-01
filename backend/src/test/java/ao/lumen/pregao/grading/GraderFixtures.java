package ao.lumen.pregao.grading;

import ao.lumen.pregao.grading.app.ExerciseDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;

final class GraderFixtures {
    static final ObjectMapper JSON = new ObjectMapper();

    private GraderFixtures() {}

    static JsonNode j(String s) {
        try {
            return JSON.readTree(s);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static ExerciseDefinition def(String type, String presentation, String solution, String tolerance) {
        return new ExerciseDefinition(UUID.randomUUID(), "EX-0001", type, "t", "s", j(presentation), j("{\"params\":[]}"),
                j(solution), j(tolerance), j("[]"), "DETAILED", 1, 40, 3, "PGC-a-confirmar", "IVA · Compras");
    }
}
