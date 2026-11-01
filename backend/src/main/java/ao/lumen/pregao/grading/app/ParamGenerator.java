package ao.lumen.pregao.grading.app;

import ao.lumen.pregao.grading.domain.Expr;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import org.springframework.stereotype.Component;

/**
 * Gera os parâmetros de uma tentativa a partir do paramSpec e de uma semente (spec §4.5).
 * Regras: range {min,max,step} · set {values} · entity {source} · constant {value, legalSourceId} · expr {expr}.
 * A mesma semente gera sempre os mesmos valores.
 */
@Component
public class ParamGenerator {
    private final ObjectMapper json;

    public ParamGenerator(ObjectMapper json) {
        this.json = json;
    }

    /** `entities` é a lista ordenada de entidades aprovadas por fonte (ex.: «companies»). */
    public ObjectNode generate(JsonNode paramSpec, long seed, Map<String, List<String>> entities) {
        SplittableRandom rnd = new SplittableRandom(seed);
        ObjectNode out = json.createObjectNode();
        Map<String, BigDecimal> numeric = new HashMap<>();
        for (JsonNode p : paramSpec.path("params")) {
            String name = p.path("name").asText();
            JsonNode rule = p.path("rule");
            switch (rule.path("kind").asText()) {
                case "range" -> {
                    BigDecimal min = rule.path("min").decimalValue();
                    BigDecimal max = rule.path("max").decimalValue();
                    BigDecimal step = rule.has("step") ? rule.path("step").decimalValue() : BigDecimal.ONE;
                    int n = max.subtract(min).divide(step, Expr.MC).intValue();
                    BigDecimal v = min.add(step.multiply(BigDecimal.valueOf(rnd.nextInt(n + 1))));
                    out.put(name, v);
                    numeric.put(name, v);
                }
                case "set" -> {
                    JsonNode values = rule.path("values");
                    JsonNode v = values.get(rnd.nextInt(values.size()));
                    out.set(name, v);
                    if (v.isNumber()) numeric.put(name, v.decimalValue());
                }
                case "entity" -> {
                    List<String> pool = entities.getOrDefault(rule.path("source").asText(), List.of());
                    if (pool.isEmpty()) throw new IllegalStateException("Sem entidades aprovadas para " + name);
                    out.put(name, pool.get(rnd.nextInt(pool.size())));
                }
                case "constant" -> {
                    JsonNode v = rule.path("value");
                    out.set(name, v);
                    if (v.isNumber()) numeric.put(name, v.decimalValue());
                }
                case "expr" -> {
                    BigDecimal v = Expr.eval(rule.path("expr").asText(), numeric);
                    if (rule.has("scale")) v = v.setScale(rule.path("scale").asInt(), java.math.RoundingMode.HALF_UP);
                    out.put(name, v);
                    numeric.put(name, v);
                }
                default -> throw new IllegalStateException("Regra de parâmetro desconhecida em " + name);
            }
        }
        return out;
    }

    public static Map<String, BigDecimal> numericView(JsonNode params) {
        Map<String, BigDecimal> m = new HashMap<>();
        params.fields().forEachRemaining(e -> {
            if (e.getValue().isNumber()) m.put(e.getKey(), e.getValue().decimalValue());
        });
        return m;
    }

    public static List<String> names(JsonNode paramSpec) {
        List<String> names = new ArrayList<>();
        paramSpec.path("params").forEach(p -> names.add(p.path("name").asText()));
        return names;
    }
}
