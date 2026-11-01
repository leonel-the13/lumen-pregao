package ao.lumen.pregao.ranking.infra;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PracticeDayRepository {
    private final JdbcTemplate jdbc;

    public PracticeDayRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Devolve true se o dia é novo para o utilizador. */
    public boolean record(UUID userId, LocalDate day) {
        return jdbc.update("insert into practice_days (user_id, day) values (?, ?) on conflict do nothing",
                userId, day) == 1;
    }

    /** Dias com prática, do mais recente para o mais antigo (até 400). */
    public List<LocalDate> recentDays(UUID userId) {
        return jdbc.queryForList("select day from practice_days where user_id = ? order by day desc limit 400",
                LocalDate.class, userId);
    }
}
