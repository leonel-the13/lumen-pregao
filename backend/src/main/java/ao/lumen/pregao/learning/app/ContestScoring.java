package ao.lumen.pregao.learning.app;

import java.time.Instant;
import java.util.UUID;

/**
 * Implementado pelo módulo contest: aplica o resultado de uma submissão de contest ao Scoreboard,
 * na mesma transacção da correcção (CON-04). Devolve os pontos atribuídos.
 */
public interface ContestScoring {
    int onGraded(UUID contestId, UUID problemId, UUID teamId, Instant receivedAt, boolean correct, int wrongBefore);
}
