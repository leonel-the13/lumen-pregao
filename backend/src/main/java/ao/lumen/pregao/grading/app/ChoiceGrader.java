package ao.lumen.pregao.grading.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Tipo A · escolha múltipla: conjunto exacto de opções correctas (uma ou várias). */
@Component
class ChoiceGrader implements Grader {
    private final ObjectMapper json;

    ChoiceGrader(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public String type() {
        return "A";
    }

    @Override
    public GradeResult grade(ExerciseDefinition def, JsonNode params, JsonNode answer) {
        Set<String> valid = new HashSet<>();
        def.presentation().path("options").forEach(o -> valid.add(o.path("key").asText()));
        Set<String> selected = new HashSet<>();
        JsonNode sel = answer.path("selected");
        if (!sel.isArray() || sel.isEmpty()) throw new AnswerFormatException("Escolhe pelo menos uma opção.");
        sel.forEach(s -> selected.add(s.asText()));
        if (!valid.containsAll(selected)) throw new AnswerFormatException("Há opções que não existem nesta pergunta.");
        if (!def.presentation().path("multiple").asBoolean(false) && selected.size() > 1) {
            throw new AnswerFormatException("Esta pergunta aceita só uma resposta.");
        }
        Set<String> correct = new HashSet<>();
        def.solutionSpec().path("correct").forEach(c -> correct.add(c.asText()));
        boolean ok = correct.equals(selected);
        return new GradeResult(ok ? GradeResult.Outcome.CORRECT : GradeResult.Outcome.INCORRECT,
                ok ? "Resposta correcta." : "Resposta incorrecta.", null, json.createObjectNode());
    }
}
