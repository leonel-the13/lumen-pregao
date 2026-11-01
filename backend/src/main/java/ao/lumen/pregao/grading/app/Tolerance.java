package ao.lumen.pregao.grading.app;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Tolerância absoluta ou relativa (tipo B, C, D, E, F). */
public record Tolerance(String type, BigDecimal value) {
    public static final Tolerance DEFAULT = new Tolerance("ABS", new BigDecimal("0.01"));

    public static Tolerance of(JsonNode node, Tolerance fallback) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.has("value")) return fallback;
        return new Tolerance(node.path("type").asText("ABS"), node.path("value").decimalValue());
    }

    public boolean accepts(BigDecimal expected, BigDecimal actual) {
        if (actual == null) return false;
        BigDecimal diff = expected.subtract(actual).abs();
        if ("REL".equals(type)) {
            if (expected.signum() == 0) return diff.signum() == 0;
            return diff.divide(expected.abs(), 10, RoundingMode.HALF_UP).compareTo(value) <= 0;
        }
        return diff.compareTo(value) <= 0;
    }
}
