package ao.lumen.pregao.grading.app;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Resultado de uma correcção.
 * <ul>
 *   <li>CORRECT — resposta certa;</li>
 *   <li>INCORRECT — resposta errada (conta como tentativa falhada);</li>
 *   <li>STEP_OK — tipo E: passos enviados certos, faltam passos (não conta como falha).</li>
 * </ul>
 * `details` nunca contém valores esperados: só estados por linha/rubrica/passo.
 */
public record GradeResult(Outcome outcome, String summary, String hint, JsonNode details) {

    public enum Outcome { CORRECT, INCORRECT, STEP_OK }

    public boolean correct() {
        return outcome == Outcome.CORRECT;
    }
}
