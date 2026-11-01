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
 * Constrói a resposta esperada a partir da solução e dos parâmetros. Só para autores e revisores
 * (pré-visualização e auto-verificação antes de enviar para revisão). Nunca usado em respostas a estudantes.
 */
@Component
public class SolutionPreviewer {
    private final ObjectMapper json;

    public SolutionPreviewer(ObjectMapper json) {
        this.json = json;
    }

    public JsonNode expectedAnswer(ExerciseDefinition def, JsonNode params) {
        Map<String, BigDecimal> vars = ParamGenerator.numericView(params);
        JsonNode sol = def.solutionSpec();
        ObjectNode out = json.createObjectNode();
        switch (def.type()) {
            case "A" -> out.set("selected", sol.path("correct").deepCopy());
            case "B" -> out.put("value", NumericGrader.expected(sol, params));
            case "C" -> {
                ArrayNode lines = out.putArray("lines");
                for (JsonNode l : sol.path("lines")) {
                    BigDecimal v = Expr.eval(l.path("expr").asText(), vars).setScale(2, RoundingMode.HALF_UP);
                    ObjectNode line = lines.addObject().put("account", l.path("account").asText());
                    if ("D".equals(l.path("side").asText())) line.put("debit", v); else line.put("credit", v);
                }
            }
            case "D" -> {
                ObjectNode items = out.putObject("items");
                sol.path("items").fields().forEachRemaining(e ->
                        items.put(e.getKey(), Expr.eval(e.getValue().asText(), vars).setScale(2, RoundingMode.HALF_UP)));
            }
            case "E" -> {
                ObjectNode steps = out.putObject("steps");
                for (JsonNode s : sol.path("steps")) {
                    steps.put(s.path("key").asText(), Expr.eval(s.path("expr").asText(), vars).setScale(2, RoundingMode.HALF_UP));
                }
            }
            case "F" -> {
                out.put("value", NumericGrader.expected(sol, params));
                StringBuilder text = new StringBuilder("Justificação de exemplo:");
                sol.path("justification").path("keywordsAny").forEach(k -> text.append(' ').append(k.asText()));
                int min = sol.path("justification").path("minLength").asInt(0);
                while (text.length() < min) text.append(" (texto do estudante)");
                out.put("justification", text.toString());
            }
            default -> throw new IllegalStateException("Tipo sem pré-visualização automática: " + def.type());
        }
        return out;
    }
}
