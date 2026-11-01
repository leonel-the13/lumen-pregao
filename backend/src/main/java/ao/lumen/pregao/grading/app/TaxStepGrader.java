package ao.lumen.pregao.grading.app;

import ao.lumen.pregao.grading.domain.Expr;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Tipo E · cálculo fiscal passo a passo: os passos são validados por ordem; um passo só desbloqueia
 * o seguinte quando está certo. Enviar passos certos mas incompletos devolve STEP_OK.
 */
@Component
class TaxStepGrader implements Grader {
    private final ObjectMapper json;

    TaxStepGrader(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public String type() {
        return "E";
    }

    @Override
    public GradeResult grade(ExerciseDefinition def, JsonNode params, JsonNode answer) {
        JsonNode steps = answer.path("steps");
        if (!steps.isObject() || steps.isEmpty()) throw new AnswerFormatException("Preenche o passo actual.");
        Map<String, BigDecimal> vars = ParamGenerator.numericView(params);
        Tolerance tol = Tolerance.of(def.tolerance(), Tolerance.DEFAULT);

        ArrayNode results = json.createArrayNode();
        int completed = 0;
        boolean failed = false, stopped = false;
        int total = def.solutionSpec().path("steps").size();
        for (JsonNode s : def.solutionSpec().path("steps")) {
            String key = s.path("key").asText();
            ObjectNode r = results.addObject().put("key", key);
            if (failed || stopped) {
                r.put("status", "LOCKED");
                continue;
            }
            BigDecimal got = Answers.decimal(steps.path(key));
            if (got == null) {
                r.put("status", "PENDING");
                stopped = true;
                continue;
            }
            BigDecimal expected = Expr.eval(s.path("expr").asText(), vars).setScale(2, RoundingMode.HALF_UP);
            if (tol.accepts(expected, got)) {
                r.put("status", "OK");
                completed++;
            } else {
                r.put("status", "WRONG");
                failed = true;
            }
        }
        ObjectNode details = json.createObjectNode().put("completed", completed).put("total", total);
        details.set("steps", results);
        if (failed) {
            return new GradeResult(GradeResult.Outcome.INCORRECT, "O passo " + (completed + 1) + " está incorrecto.",
                    "Revê a fórmula do passo " + (completed + 1) + " e os dados do exercício.", details);
        }
        if (completed < total) {
            return new GradeResult(GradeResult.Outcome.STEP_OK, "Passo " + completed + " correcto. Avança para o seguinte.",
                    null, details);
        }
        return new GradeResult(GradeResult.Outcome.CORRECT, "Apuramento correcto.", null, details);
    }
}
