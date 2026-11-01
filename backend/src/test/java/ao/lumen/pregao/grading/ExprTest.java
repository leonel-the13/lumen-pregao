package ao.lumen.pregao.grading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ao.lumen.pregao.grading.domain.Expr;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExprTest {
    private static final Map<String, BigDecimal> V = Map.of("valor", new BigDecimal("500000"), "taxa", new BigDecimal("14"));

    @Test
    void respectsPrecedenceAndParentheses() {
        assertThat(Expr.eval("valor * taxa / 100", V)).isEqualByComparingTo("70000");
        assertThat(Expr.eval("valor * (1 + taxa / 100)", V)).isEqualByComparingTo("570000");
        assertThat(Expr.eval("-2 + 3 * 4", Map.of())).isEqualByComparingTo("10");
    }

    @Test
    void supportsFunctions() {
        assertThat(Expr.eval("round(10 / 3, 2)", Map.of())).isEqualByComparingTo("3.33");
        assertThat(Expr.eval("max(1, 5, 3) + min(2, 4) + abs(-1)", Map.of())).isEqualByComparingTo("8");
    }

    @Test
    void rejectsUnknownNamesAndBadSyntax() {
        assertThatThrownBy(() -> Expr.eval("valor + desconhecido", V)).isInstanceOf(Expr.ExprException.class);
        assertThatThrownBy(() -> Expr.eval("valor +", V)).isInstanceOf(Expr.ExprException.class);
        assertThatThrownBy(() -> Expr.eval("1 / 0", V)).isInstanceOf(Expr.ExprException.class);
        assertThatThrownBy(() -> Expr.eval("System.exit(0)", V)).isInstanceOf(Expr.ExprException.class);
    }
}
