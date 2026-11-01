package ao.lumen.pregao.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class StreakTest {
    @Test
    void countsConsecutiveDaysEndingAtDay() throws Exception {
        Method m = Class.forName("ao.lumen.pregao.ranking.app.StreakService")
                .getDeclaredMethod("streakEndingAt", List.class, LocalDate.class);
        m.setAccessible(true);
        LocalDate d = LocalDate.of(2026, 10, 31);
        List<LocalDate> desc = List.of(d, d.minusDays(1), d.minusDays(2), d.minusDays(4));
        assertThat(m.invoke(null, desc, d)).isEqualTo(3);
        assertThat(m.invoke(null, desc, d.minusDays(4))).isEqualTo(1);
    }
}
