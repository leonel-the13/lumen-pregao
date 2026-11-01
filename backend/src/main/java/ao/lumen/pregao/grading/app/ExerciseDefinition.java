package ao.lumen.pregao.grading.app;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

/**
 * Definição completa de uma versão de exercício, usada só no servidor.
 * Contém a solução: nunca sai em DTOs de estudante.
 */
public record ExerciseDefinition(
        UUID versionId,
        String code,
        String type,
        String title,
        String statement,
        JsonNode presentation,
        JsonNode paramSpec,
        JsonNode solutionSpec,
        JsonNode tolerance,
        JsonNode hints,
        String feedbackPolicy,
        int difficulty,
        int points,
        int maxAttempts,
        String pgcVersion,
        String topic) {

    public boolean binaryFeedback() {
        return "BINARY".equals(feedbackPolicy);
    }
}
