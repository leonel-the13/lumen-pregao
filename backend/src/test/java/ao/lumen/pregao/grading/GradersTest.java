package ao.lumen.pregao.grading;

import static ao.lumen.pregao.grading.GraderFixtures.def;
import static ao.lumen.pregao.grading.GraderFixtures.j;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ao.lumen.pregao.grading.app.AnswerFormatException;
import ao.lumen.pregao.grading.app.ExerciseDefinition;
import ao.lumen.pregao.grading.app.GradeResult.Outcome;
import ao.lumen.pregao.grading.app.GradingService;
import ao.lumen.pregao.grading.app.PgcAccountService;
import ao.lumen.pregao.grading.app.SolutionPreviewer;
import com.fasterxml.jackson.databind.JsonNode;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Motores de correcção A–F (sem Spring): determinísticos e sem expor valores esperados. */
class GradersTest {
    static GradingService grading;
    static final JsonNode PARAMS = j("{\"valor\":500000,\"taxa\":14,\"vendas\":1200000,\"compras\":500000,\"ac\":1500000,\"pc\":1000000}");

    @BeforeAll
    static void setUp() throws Exception {
        PgcAccountService accounts = mock(PgcAccountService.class);
        when(accounts.codes(any())).thenReturn(Set.of("26", "32", "34.5.x", "43", "45", "61", "31"));
        List<Object> graders = new ArrayList<>();
        for (String n : List.of("ChoiceGrader", "NumericGrader", "StatementGrader", "TaxStepGrader", "RatioGrader")) {
            Constructor<?> c = Class.forName("ao.lumen.pregao.grading.app." + n).getDeclaredConstructor(com.fasterxml.jackson.databind.ObjectMapper.class);
            c.setAccessible(true);
            graders.add(c.newInstance(GraderFixtures.JSON));
        }
        Constructor<?> jc = Class.forName("ao.lumen.pregao.grading.app.JournalGrader")
                .getDeclaredConstructor(com.fasterxml.jackson.databind.ObjectMapper.class, PgcAccountService.class);
        jc.setAccessible(true);
        graders.add(jc.newInstance(GraderFixtures.JSON, accounts));
        Constructor<?> gc = GradingService.class.getDeclaredConstructor(List.class);
        gc.setAccessible(true);
        grading = (GradingService) gc.newInstance(graders);
    }

    static final ExerciseDefinition JOURNAL = def("C", "{}", """
            {"lines":[{"account":"26","side":"D","expr":"valor"},
                      {"account":"34.5.x","side":"D","expr":"valor * taxa / 100"},
                      {"account":"32","side":"C","expr":"valor * (1 + taxa / 100)"}]}""", "{\"type\":\"ABS\",\"value\":0.01}");

    @Test
    void journalAcceptsAnyLineOrderAndTextAmounts() {
        var r = grading.grade(JOURNAL, PARAMS, j("""
                {"lines":[{"account":"32","credit":"570 000,00"},{"account":"26","debit":500000},{"account":"34.5.x","debit":"70000.00"}]}"""));
        assertThat(r.outcome()).isEqualTo(Outcome.CORRECT);
        assertThat(r.details().path("balanced").asBoolean()).isTrue();
    }

    @Test
    void journalFlagsWrongAccountPerLineWithoutRevealingSolution() {
        var r = grading.grade(JOURNAL, PARAMS, j("""
                {"lines":[{"account":"26","debit":500000},{"account":"34.5.x","debit":70000},{"account":"45","credit":570000}]}"""));
        assertThat(r.outcome()).isEqualTo(Outcome.INCORRECT);
        assertThat(r.details().path("lines").get(2).path("status").asText()).isEqualTo("WRONG_ACCOUNT");
        assertThat(r.summary()).isEqualTo("Quase: 1 conta incorrecta");
        assertThat(r.details().toString()).doesNotContain("\"32\"");
    }

    @Test
    void journalDetectsImbalanceAndUnknownAccounts() {
        var r = grading.grade(JOURNAL, PARAMS, j("""
                {"lines":[{"account":"26","debit":500000},{"account":"99","credit":400000}]}"""));
        assertThat(r.details().path("balanced").asBoolean()).isFalse();
        assertThat(r.details().path("lines").get(1).path("status").asText()).isEqualTo("UNKNOWN_ACCOUNT");
    }

    @Test
    void journalRejectsBadFormat() {
        assertThatThrownBy(() -> grading.grade(JOURNAL, PARAMS, j("""
                {"lines":[{"account":"26","debit":1,"credit":1}]}"""))).isInstanceOf(AnswerFormatException.class);
        assertThatThrownBy(() -> grading.grade(JOURNAL, PARAMS, j("{\"lines\":[]}"))).isInstanceOf(AnswerFormatException.class);
    }

    @Test
    void choiceRequiresExactSet() {
        var d = def("A", "{\"options\":[{\"key\":\"A\"},{\"key\":\"B\"},{\"key\":\"C\"}],\"multiple\":true}",
                "{\"correct\":[\"A\",\"C\"]}", "{}");
        assertThat(grading.grade(d, PARAMS, j("{\"selected\":[\"C\",\"A\"]}")).correct()).isTrue();
        assertThat(grading.grade(d, PARAMS, j("{\"selected\":[\"A\"]}")).correct()).isFalse();
        assertThatThrownBy(() -> grading.grade(d, PARAMS, j("{\"selected\":[\"Z\"]}"))).isInstanceOf(AnswerFormatException.class);
    }

    @Test
    void numericRespectsAbsoluteAndRelativeTolerance() {
        var abs = def("B", "{}", "{\"expr\":\"valor * taxa / 100\",\"scale\":2}", "{\"type\":\"ABS\",\"value\":0.01}");
        assertThat(grading.grade(abs, PARAMS, j("{\"value\":\"70 000,01\"}")).correct()).isTrue();
        assertThat(grading.grade(abs, PARAMS, j("{\"value\":70000.02}")).correct()).isFalse();
        var rel = def("B", "{}", "{\"expr\":\"valor\"}", "{\"type\":\"REL\",\"value\":0.001}");
        assertThat(grading.grade(rel, PARAMS, j("{\"value\":500400}")).correct()).isTrue();
    }

    @Test
    void taxStepsUnlockInOrder() {
        var d = def("E", "{}", """
                {"steps":[{"key":"liq","expr":"vendas * taxa / 100"},{"key":"ded","expr":"compras * taxa / 100"},
                          {"key":"apu","expr":"(vendas - compras) * taxa / 100"}]}""", "{}");
        assertThat(grading.grade(d, PARAMS, j("{\"steps\":{\"liq\":168000}}")).outcome()).isEqualTo(Outcome.STEP_OK);
        var wrong = grading.grade(d, PARAMS, j("{\"steps\":{\"liq\":168000,\"ded\":1}}"));
        assertThat(wrong.outcome()).isEqualTo(Outcome.INCORRECT);
        assertThat(wrong.details().path("steps").get(2).path("status").asText()).isEqualTo("LOCKED");
        assertThat(grading.grade(d, PARAMS, j("{\"steps\":{\"liq\":168000,\"ded\":70000,\"apu\":98000}}")).correct()).isTrue();
    }

    @Test
    void statementChecksItemsAndRelations() {
        var d = def("D", "{}", """
                {"items":{"a":"valor","b":"valor * 2","total":"valor * 3"},
                 "relations":[{"label":"total","left":"total","right":"a + b"}]}""", "{}");
        assertThat(grading.grade(d, PARAMS, j("{\"items\":{\"a\":500000,\"b\":1000000,\"total\":1500000}}")).correct()).isTrue();
        var r = grading.grade(d, PARAMS, j("{\"items\":{\"a\":500000,\"b\":930000,\"total\":1500000}}"));
        assertThat(r.details().path("items").path("b").asText()).isEqualTo("WRONG");
        assertThat(r.details().path("relations").get(0).path("ok").asBoolean()).isFalse();
    }

    @Test
    void ratioNeedsValueAndJustification() {
        var d = def("F", "{}", """
                {"expr":"ac / pc","scale":2,"justification":{"minLength":30,"keywordsAny":["liquidez"],"keywordsMin":1}}""",
                "{\"type\":\"ABS\",\"value\":0.01}");
        assertThat(grading.grade(d, PARAMS, j("{\"value\":1.5,\"justification\":\"A liquidez geral é confortável para o curto prazo.\"}")).correct()).isTrue();
        assertThat(grading.grade(d, PARAMS, j("{\"value\":1.5,\"justification\":\"curto\"}")).details().path("justification").asText())
                .isEqualTo("TOO_SHORT");
    }

    @Test
    void previewerProducesAnswersThatGradeCorrect() {
        var previewer = new SolutionPreviewer(GraderFixtures.JSON);
        assertThat(grading.grade(JOURNAL, PARAMS, previewer.expectedAnswer(JOURNAL, PARAMS)).correct()).isTrue();
    }
}
