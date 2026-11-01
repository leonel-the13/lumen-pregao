package ao.lumen.pregao.grading.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Tipo F · rácios: valor ± tolerância e justificação avaliada por regra
 * (tamanho mínimo e termos esperados). A avaliação por revisor fica para fase posterior.
 */
@Component
class RatioGrader implements Grader {
    static final int MAX_JUSTIFICATION = 4000;
    private final ObjectMapper json;

    RatioGrader(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public String type() {
        return "F";
    }

    @Override
    public GradeResult grade(ExerciseDefinition def, JsonNode params, JsonNode answer) {
        BigDecimal value = Answers.decimal(answer.path("value"));
        if (value == null) throw new AnswerFormatException("Indica o valor do rácio.");
        String text = answer.path("justification").asText("").strip();
        if (text.length() > MAX_JUSTIFICATION) throw new AnswerFormatException("A justificação é demasiado longa.");

        boolean valueOk = Tolerance.of(def.tolerance(), Tolerance.DEFAULT)
                .accepts(NumericGrader.expected(def.solutionSpec(), params), value);
        JsonNode rule = def.solutionSpec().path("justification");
        String justStatus = "OK";
        if (text.length() < rule.path("minLength").asInt(0)) {
            justStatus = "TOO_SHORT";
        } else if (rule.has("keywordsAny")) {
            String norm = normalize(text);
            int needed = rule.path("keywordsMin").asInt(1);
            int hits = 0;
            for (JsonNode k : rule.path("keywordsAny")) if (norm.contains(normalize(k.asText()))) hits++;
            if (hits < needed) justStatus = "MISSING_TERMS";
        }
        boolean ok = valueOk && "OK".equals(justStatus);
        String summary = ok ? "Rácio e justificação correctos."
                : !valueOk ? "O valor do rácio está incorrecto."
                : "TOO_SHORT".equals(justStatus) ? "A justificação é demasiado curta."
                : "A justificação não interpreta o rácio.";
        return new GradeResult(ok ? GradeResult.Outcome.CORRECT : GradeResult.Outcome.INCORRECT, summary, null,
                json.createObjectNode().put("value", valueOk ? "OK" : "WRONG").put("justification", justStatus));
    }

    private static String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
