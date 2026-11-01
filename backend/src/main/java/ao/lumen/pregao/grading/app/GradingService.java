package ao.lumen.pregao.grading.app;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Ponto de entrada dos motores de correcção (tipos A–F). Determinístico: mesma tentativa, mesmo resultado. */
@Service
public class GradingService {
    private final Map<String, Grader> graders;

    GradingService(List<Grader> graders) {
        this.graders = graders.stream().collect(Collectors.toMap(Grader::type, Function.identity()));
    }

    public GradeResult grade(ExerciseDefinition def, JsonNode params, JsonNode answer) {
        Grader g = graders.get(def.type());
        if (g == null) {
            throw new IllegalStateException("Tipo " + def.type() + " sem correcção automática");
        }
        return g.grade(def, params, answer == null ? com.fasterxml.jackson.databind.node.NullNode.getInstance() : answer);
    }

    /** Validação de formato sem correcção (ex.: guardar rascunho de exame). */
    public boolean supports(String type) {
        return graders.containsKey(type);
    }
}
