package ao.lumen.pregao.grading.app;

import ao.lumen.pregao.grading.domain.Expr;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Tipo D · demonstrações: valor por rubrica ± tolerância e relações (ex.: Activo = CP + Passivo). */
@Component
class StatementGrader implements Grader {
    private final ObjectMapper json;

    StatementGrader(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public String type() {
        return "D";
    }

    @Override
    public GradeResult grade(ExerciseDefinition def, JsonNode params, JsonNode answer) {
        JsonNode items = answer.path("items");
        if (!items.isObject()) throw new AnswerFormatException("Preenche as rubricas da demonstração.");
        Map<String, BigDecimal> vars = ParamGenerator.numericView(params);
        Tolerance tol = Tolerance.of(def.tolerance(), Tolerance.DEFAULT);

        Map<String, BigDecimal> submitted = new HashMap<>();
        ObjectNode itemResults = json.createObjectNode();
        int wrong = 0;
        var it = def.solutionSpec().path("items").fields();
        while (it.hasNext()) {
            var e = it.next();
            BigDecimal expected = Expr.eval(e.getValue().asText(), vars).setScale(2, RoundingMode.HALF_UP);
            BigDecimal got = Answers.decimal(items.path(e.getKey()));
            submitted.put(e.getKey(), got == null ? BigDecimal.ZERO : got);
            String status = got == null ? "MISSING" : tol.accepts(expected, got) ? "OK" : "WRONG";
            if (!"OK".equals(status)) wrong++;
            itemResults.put(e.getKey(), status);
        }

        ArrayNode relations = json.createArrayNode();
        boolean relationsOk = true;
        for (JsonNode r : def.solutionSpec().path("relations")) {
            BigDecimal left = Expr.eval(r.path("left").asText(), submitted);
            BigDecimal right = Expr.eval(r.path("right").asText(), submitted);
            boolean ok = left.subtract(right).abs().compareTo(new BigDecimal("0.01")) <= 0;
            relationsOk &= ok;
            ObjectNode rr = relations.addObject().put("label", r.path("label").asText()).put("ok", ok);
            if (!ok) rr.put("difference", left.subtract(right).abs().setScale(2, RoundingMode.HALF_UP));
        }
        boolean ok = wrong == 0;
        ObjectNode details = json.createObjectNode();
        details.set("items", itemResults);
        details.set("relations", relations);
        String summary = ok ? "Demonstração correcta."
                : !relationsOk ? "A demonstração não fecha: revê as relações entre rubricas."
                : wrong == 1 ? "1 rubrica incorrecta." : wrong + " rubricas incorrectas.";
        return new GradeResult(ok ? GradeResult.Outcome.CORRECT : GradeResult.Outcome.INCORRECT, summary, null, details);
    }
}
