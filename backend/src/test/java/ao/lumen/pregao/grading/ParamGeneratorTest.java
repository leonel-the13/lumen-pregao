package ao.lumen.pregao.grading;

import static ao.lumen.pregao.grading.GraderFixtures.j;
import static org.assertj.core.api.Assertions.assertThat;

import ao.lumen.pregao.grading.app.ParamGenerator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class ParamGeneratorTest {
    final ParamGenerator gen = new ParamGenerator(GraderFixtures.JSON);
    final Map<String, List<String>> pools = Map.of("companies", List.of("Empresa A", "Empresa B"));
    final com.fasterxml.jackson.databind.JsonNode spec = j("""
            {"params":[
              {"name":"empresa","type":"text","rule":{"kind":"entity","source":"companies"}},
              {"name":"valor","type":"money","rule":{"kind":"range","min":300000,"max":900000,"step":10000}},
              {"name":"desconto","type":"percent","rule":{"kind":"set","values":[2,5,10]}},
              {"name":"taxa","type":"percent","rule":{"kind":"constant","value":14}},
              {"name":"base","type":"money","rule":{"kind":"expr","expr":"valor * (1 - desconto / 100)","scale":2}}
            ]}""");

    @Test
    void sameSeedSameValues() {
        assertThat(gen.generate(spec, 42L, pools)).isEqualTo(gen.generate(spec, 42L, pools));
    }

    @RepeatedTest(20)
    void valuesRespectRules() {
        var p = gen.generate(spec, System.nanoTime(), pools);
        BigDecimal v = p.path("valor").decimalValue();
        assertThat(v).isBetween(new BigDecimal("300000"), new BigDecimal("900000"));
        assertThat(v.remainder(new BigDecimal("10000"))).isEqualByComparingTo("0");
        assertThat(List.of(2, 5, 10)).contains(p.path("desconto").asInt());
        assertThat(p.path("taxa").asInt()).isEqualTo(14);
        assertThat(pools.get("companies")).contains(p.path("empresa").asText());
    }
}
