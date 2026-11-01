package ao.lumen.pregao.grading.app;

import ao.lumen.pregao.common.text.Formats;
import ao.lumen.pregao.grading.domain.Expr;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Substitui [parâmetros] no enunciado e avalia campos {"$expr": "..."} da apresentação.
 * Só produz dados visíveis ao estudante.
 */
@Component
public class StatementRenderer {
    private static final Pattern PARAM = Pattern.compile("\\[([a-z_][a-z0-9_]*)]");
    private final ObjectMapper json;

    public StatementRenderer(ObjectMapper json) {
        this.json = json;
    }

    public record Segment(String text, String param, boolean unconfirmed) {}

    public record VisibleParam(String name, Object value, String display, String type, boolean unconfirmed) {}

    public record Rendered(String text, List<Segment> segments, List<VisibleParam> params) {}

    /** `confirmed` diz se uma LegalSource está confirmada (constantes ligadas a normas). */
    public Rendered render(String statement, JsonNode paramSpec, JsonNode params, Predicate<UUID> confirmed) {
        Map<String, JsonNode> specs = new HashMap<>();
        paramSpec.path("params").forEach(p -> specs.put(p.path("name").asText(), p));

        List<VisibleParam> visible = new ArrayList<>();
        Map<String, VisibleParam> byName = new HashMap<>();
        for (JsonNode p : paramSpec.path("params")) {
            if (!p.path("visible").asBoolean(true)) continue;
            String name = p.path("name").asText();
            JsonNode v = params.path(name);
            if (v.isMissingNode()) continue;
            boolean unconfirmed = isUnconfirmed(p, confirmed);
            VisibleParam vp = new VisibleParam(name, v.isNumber() ? v.decimalValue() : v.asText(),
                    display(p.path("type").asText("text"), v), p.path("type").asText("text"), unconfirmed);
            visible.add(vp);
            byName.put(name, vp);
        }

        List<Segment> segments = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        Matcher m = PARAM.matcher(statement);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                String t = statement.substring(last, m.start());
                segments.add(new Segment(t, null, false));
                text.append(t);
            }
            VisibleParam vp = byName.get(m.group(1));
            String shown = vp != null ? vp.display() : "[" + m.group(1) + "]";
            segments.add(new Segment(shown, m.group(1), vp != null && vp.unconfirmed()));
            text.append(shown);
            last = m.end();
        }
        if (last < statement.length()) {
            segments.add(new Segment(statement.substring(last), null, false));
            text.append(statement.substring(last));
        }
        return new Rendered(text.toString(), segments, visible);
    }

    /** Avalia recursivamente {"$expr": "...", "format": "money"} sobre os parâmetros. */
    public JsonNode renderPresentation(JsonNode presentation, JsonNode params) {
        Map<String, BigDecimal> vars = ParamGenerator.numericView(params);
        return walk(presentation, vars, params);
    }

    private JsonNode walk(JsonNode node, Map<String, BigDecimal> vars, JsonNode params) {
        if (node == null) return json.nullNode();
        if (node.isObject()) {
            if (node.has("$expr")) {
                BigDecimal v = Expr.eval(node.path("$expr").asText(), vars);
                ObjectNode o = json.createObjectNode();
                o.put("value", v.setScale(2, java.math.RoundingMode.HALF_UP));
                o.put("display", display(node.path("format").asText("money"), json.getNodeFactory().numberNode(v)));
                return o;
            }
            if (node.has("$param")) {
                return params.path(node.path("$param").asText());
            }
            ObjectNode o = json.createObjectNode();
            node.fields().forEachRemaining(e -> o.set(e.getKey(), walk(e.getValue(), vars, params)));
            return o;
        }
        if (node.isArray()) {
            ArrayNode a = json.createArrayNode();
            node.forEach(n -> a.add(walk(n, vars, params)));
            return a;
        }
        return node;
    }

    static boolean isUnconfirmed(JsonNode spec, Predicate<UUID> confirmed) {
        JsonNode rule = spec.path("rule");
        if (!"constant".equals(rule.path("kind").asText())) return false;
        if (!rule.hasNonNull("legalSourceId")) return true;
        try {
            return !confirmed.test(UUID.fromString(rule.path("legalSourceId").asText()));
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    public static String display(String type, JsonNode v) {
        if (!v.isNumber()) return v.asText();
        BigDecimal d = v.decimalValue();
        return switch (type) {
            case "money" -> Formats.kz(d);
            case "percent" -> Formats.percent(d);
            default -> Formats.plain(d);
        };
    }
}
