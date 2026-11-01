package ao.lumen.pregao.grading.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Avaliador seguro de expressões aritméticas sobre parâmetros (sem reflexão nem scripts).
 * Gramática: expr := term (('+'|'-') term)* ; term := unary (('*'|'/') unary)* ;
 * unary := '-' unary | primary ; primary := número | nome | nome '(' args ')' | '(' expr ')'.
 * Funções: round(x, casas), min(...), max(...), abs(x).
 */
public final class Expr {
    public static final MathContext MC = MathContext.DECIMAL64;

    private final String src;
    private final Map<String, BigDecimal> vars;
    private int pos;

    private Expr(String src, Map<String, BigDecimal> vars) {
        this.src = src;
        this.vars = vars;
    }

    public static BigDecimal eval(String expression, Map<String, BigDecimal> vars) {
        if (expression == null || expression.length() > 500) throw new ExprException("Expressão inválida");
        Expr e = new Expr(expression, vars);
        BigDecimal v = e.expr();
        e.skipWs();
        if (e.pos != e.src.length()) throw new ExprException("Símbolo inesperado na posição " + e.pos);
        return v;
    }

    private BigDecimal expr() {
        BigDecimal v = term();
        while (true) {
            skipWs();
            if (eat('+')) v = v.add(term(), MC);
            else if (eat('-')) v = v.subtract(term(), MC);
            else return v;
        }
    }

    private BigDecimal term() {
        BigDecimal v = unary();
        while (true) {
            skipWs();
            if (eat('*')) v = v.multiply(unary(), MC);
            else if (eat('/')) {
                BigDecimal d = unary();
                if (d.signum() == 0) throw new ExprException("Divisão por zero");
                v = v.divide(d, MC);
            } else return v;
        }
    }

    private BigDecimal unary() {
        skipWs();
        if (eat('-')) return unary().negate();
        if (eat('+')) return unary();
        return primary();
    }

    private BigDecimal primary() {
        skipWs();
        if (eat('(')) {
            BigDecimal v = expr();
            skipWs();
            expect(')');
            return v;
        }
        int start = pos;
        if (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
            while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) pos++;
            return new BigDecimal(src.substring(start, pos));
        }
        if (pos < src.length() && (Character.isLetter(src.charAt(pos)) || src.charAt(pos) == '_')) {
            while (pos < src.length() && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) pos++;
            String name = src.substring(start, pos);
            skipWs();
            if (eat('(')) return call(name);
            BigDecimal v = vars.get(name);
            if (v == null) throw new ExprException("Parâmetro desconhecido: " + name);
            return v;
        }
        throw new ExprException("Expressão incompleta na posição " + pos);
    }

    private BigDecimal call(String fn) {
        List<BigDecimal> args = new ArrayList<>();
        skipWs();
        if (!eat(')')) {
            do {
                args.add(expr());
                skipWs();
            } while (eat(','));
            expect(')');
        }
        return switch (fn) {
            case "round" -> {
                if (args.size() != 2) throw new ExprException("round(x, casas)");
                yield args.get(0).setScale(args.get(1).intValueExact(), RoundingMode.HALF_UP);
            }
            case "abs" -> {
                if (args.size() != 1) throw new ExprException("abs(x)");
                yield args.get(0).abs();
            }
            case "min" -> args.stream().min(BigDecimal::compareTo).orElseThrow(() -> new ExprException("min()"));
            case "max" -> args.stream().max(BigDecimal::compareTo).orElseThrow(() -> new ExprException("max()"));
            default -> throw new ExprException("Função desconhecida: " + fn);
        };
    }

    private void skipWs() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }

    private boolean eat(char c) {
        if (pos < src.length() && src.charAt(pos) == c) {
            pos++;
            return true;
        }
        return false;
    }

    private void expect(char c) {
        if (!eat(c)) throw new ExprException("Esperava '" + c + "' na posição " + pos);
    }

    public static final class ExprException extends RuntimeException {
        public ExprException(String msg) {
            super(msg);
        }
    }
}
