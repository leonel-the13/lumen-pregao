package ao.lumen.pregao.grading.app;

import ao.lumen.pregao.grading.domain.Expr;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Tipo C · lançamentos: contas existentes no PGC da versão do exercício, conta certa por linha,
 * débito = crédito, valores ± tolerância; a ordem das linhas é irrelevante.
 */
@Component
class JournalGrader implements Grader {
    static final int MAX_LINES = 30;
    private final ObjectMapper json;
    private final PgcAccountService accounts;

    JournalGrader(ObjectMapper json, PgcAccountService accounts) {
        this.json = json;
        this.accounts = accounts;
    }

    @Override
    public String type() {
        return "C";
    }

    record Line(String account, String side, BigDecimal amount) {}

    private record Expected(String account, String side, BigDecimal amount, Tolerance tol) {}

    @Override
    public GradeResult grade(ExerciseDefinition def, JsonNode params, JsonNode answer) {
        List<Line> lines = parse(answer);
        Map<String, BigDecimal> vars = ParamGenerator.numericView(params);
        Tolerance base = Tolerance.of(def.tolerance(), Tolerance.DEFAULT);
        List<Expected> expected = new ArrayList<>();
        for (JsonNode e : def.solutionSpec().path("lines")) {
            expected.add(new Expected(e.path("account").asText(), e.path("side").asText(),
                    Expr.eval(e.path("expr").asText(), vars).setScale(2, RoundingMode.HALF_UP),
                    Tolerance.of(e.path("tolerance"), base)));
        }
        Set<String> valid = accounts.codes(def.pgcVersion());

        BigDecimal debit = BigDecimal.ZERO, credit = BigDecimal.ZERO;
        for (Line l : lines) {
            if ("D".equals(l.side())) debit = debit.add(l.amount());
            else credit = credit.add(l.amount());
        }
        boolean balanced = debit.compareTo(credit) == 0;

        boolean[] used = new boolean[expected.size()];
        ArrayNode lineResults = json.createArrayNode();
        int wrongAccounts = 0, wrongAmounts = 0, wrongSides = 0;
        String firstHint = null;
        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            String status;
            int exact = -1, sameAccount = -1, otherSide = -1;
            for (int j = 0; j < expected.size(); j++) {
                if (used[j]) continue;
                Expected e = expected.get(j);
                if (!e.account().equals(l.account())) continue;
                if (e.side().equals(l.side())) {
                    if (e.tol().accepts(e.amount(), l.amount())) { exact = j; break; }
                    if (sameAccount < 0) sameAccount = j;
                } else if (otherSide < 0) {
                    otherSide = j;
                }
            }
            if (exact >= 0) {
                used[exact] = true;
                status = "OK";
            } else if (sameAccount >= 0) {
                used[sameAccount] = true;
                status = "WRONG_AMOUNT";
                wrongAmounts++;
            } else if (otherSide >= 0) {
                status = "WRONG_SIDE";
                wrongSides++;
            } else if (!valid.contains(l.account())) {
                status = "UNKNOWN_ACCOUNT";
                wrongAccounts++;
            } else {
                status = "WRONG_ACCOUNT";
                wrongAccounts++;
            }
            if (firstHint == null && !"OK".equals(status)) firstHint = hintFor(status, l, i + 1);
            ObjectNode r = json.createObjectNode().put("index", i + 1).put("account", l.account()).put("status", status);
            lineResults.add(r);
        }
        int missing = 0;
        for (boolean u : used) if (!u) missing++;
        boolean ok = balanced && missing == 0 && wrongAccounts == 0 && wrongAmounts == 0 && wrongSides == 0;

        ObjectNode details = json.createObjectNode();
        details.put("balanced", balanced);
        details.putObject("totals").put("debit", debit.setScale(2, RoundingMode.HALF_UP))
                .put("credit", credit.setScale(2, RoundingMode.HALF_UP));
        details.set("lines", lineResults);
        details.put("missingLines", Math.max(0, missing - wrongAmounts));

        String summary;
        if (ok) summary = "Lançamento correcto. Débitos e créditos equilibrados.";
        else if (!balanced) summary = "O lançamento não está equilibrado.";
        else if (wrongAccounts > 0) summary = "Quase: " + wrongAccounts + (wrongAccounts == 1 ? " conta incorrecta" : " contas incorrectas");
        else if (wrongSides > 0) summary = "Há contas movimentadas do lado errado.";
        else if (wrongAmounts > 0) summary = "Há valores incorrectos.";
        else summary = "Falta registar pelo menos uma linha.";
        if (firstHint == null && !ok && missing > 0) firstHint = "Falta pelo menos uma conta no lançamento.";
        return new GradeResult(ok ? GradeResult.Outcome.CORRECT : GradeResult.Outcome.INCORRECT, summary, firstHint, details);
    }

    private static String hintFor(String status, Line l, int index) {
        String lado = "D".equals(l.side()) ? "a débito" : "a crédito";
        return switch (status) {
            case "WRONG_AMOUNT" -> "Revê o valor da linha " + index + ".";
            case "WRONG_SIDE" -> "A conta da linha " + index + " está do lado errado.";
            case "UNKNOWN_ACCOUNT" -> "A conta da linha " + index + " não existe no PGC deste exercício.";
            default -> "Revê a conta " + lado + " da linha " + index + ".";
        };
    }

    static List<Line> parse(JsonNode answer) {
        JsonNode arr = answer.path("lines");
        if (!arr.isArray() || arr.isEmpty()) throw new AnswerFormatException("Adiciona pelo menos uma linha ao lançamento.");
        if (arr.size() > MAX_LINES) throw new AnswerFormatException("O lançamento tem demasiadas linhas.");
        List<Line> out = new ArrayList<>();
        for (JsonNode n : arr) {
            String account = n.path("account").asText("").strip();
            if (account.isEmpty() || account.length() > 16) throw new AnswerFormatException("Há linhas sem conta.");
            BigDecimal d = Answers.decimal(n.path("debit"));
            BigDecimal c = Answers.decimal(n.path("credit"));
            boolean hasD = d != null && d.signum() != 0, hasC = c != null && c.signum() != 0;
            if (hasD == hasC) throw new AnswerFormatException("Cada linha tem um valor a débito ou a crédito, não os dois.");
            BigDecimal amount = hasD ? d : c;
            if (amount.signum() < 0) throw new AnswerFormatException("Os valores não podem ser negativos.");
            out.add(new Line(account, hasD ? "D" : "C", amount.setScale(2, RoundingMode.HALF_UP)));
        }
        return out;
    }
}
