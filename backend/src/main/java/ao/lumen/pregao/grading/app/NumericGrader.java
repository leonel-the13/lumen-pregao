package ao.lumen.pregao.grading.app;

import ao.lumen.pregao.grading.domain.Expr;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/** Tipo B · numérica: valor ± tolerância, com regra de arredondamento e precisão da solução. */
@Component
class NumericGrader implements Grader {
    private final ObjectMapper json;

    NumericGrader(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public String type() {
        return "B";
    }

    @Override
    public GradeResult grade(ExerciseDefinition def, JsonNode params, JsonNode answer) {
        BigDecimal value = Answers.decimal(answer.path("value"));
        if (value == null) throw new AnswerFormatException("Indica um valor.");
        BigDecimal expected = expected(def.solutionSpec(), params);
        boolean ok = Tolerance.of(def.tolerance(), Tolerance.DEFAULT).accepts(expected, value);
        return new GradeResult(ok ? GradeResult.Outcome.CORRECT : GradeResult.Outcome.INCORRECT,
                ok ? "Valor correcto." : "Valor incorrecto.", null,
                json.createObjectNode().put("value", ok ? "OK" : "WRONG"));
    }

    static BigDecimal expected(JsonNode solution, JsonNode params) {
        BigDecimal v = Expr.eval(solution.path("expr").asText(), ParamGenerator.numericView(params));
        if (solution.has("scale")) {
            v = v.setScale(solution.path("scale").asInt(), RoundingMode.valueOf(solution.path("rounding").asText("HALF_UP")));
        }
        return v;
    }
}
