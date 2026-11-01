package ao.lumen.pregao.grading.app;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;

final class Answers {
    private Answers() {}

    /** Aceita número JSON ou texto («500 000,00», «500000.00»). Vazio → null. */
    static BigDecimal decimal(JsonNode n) {
        if (n == null || n.isNull() || n.isMissingNode()) return null;
        if (n.isNumber()) return n.decimalValue();
        String s = n.asText().replace(" ", "").replace(" ", "").replace(" ", "").replace("−", "-");
        if (s.isEmpty()) return null;
        if (s.contains(",")) s = s.replace(".", "").replace(',', '.');
        try {
            BigDecimal v = new BigDecimal(s);
            if (v.abs().compareTo(new BigDecimal("1e15")) > 0) throw new AnswerFormatException("Valor fora dos limites.");
            return v;
        } catch (NumberFormatException e) {
            throw new AnswerFormatException("Há valores que não são números válidos.");
        }
    }
}
