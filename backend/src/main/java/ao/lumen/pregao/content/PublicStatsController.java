package ao.lumen.pregao.content;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Números públicos para a landing. Só dados reais e agregados (nunca dados pessoais); avaliações só aparecem
 * a partir de 5 respostas, para não expor opiniões individuais.
 */
@RestController
@RequestMapping("/api/v1/public")
public class PublicStatsController {
    private final JdbcTemplate jdbc;

    public PublicStatsController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        long tracks = count("select count(*) from tracks where status = 'PUBLISHED'");
        long rooms = count("select count(*) from rooms r join modules m on m.id = r.module_id join tracks t on t.id = m.track_id "
                + "where r.status = 'PUBLISHED' and t.status = 'PUBLISHED'");
        long exercises = count("select count(*) from review_workflows where status = 'PUBLISHED'");
        long students = count("select count(distinct u.id) from users u join user_roles ur on ur.user_id = u.id "
                + "join roles r on r.id = ur.role_id where r.code = 'STUDENT' and u.status = 'ACTIVE'");
        long submissions = count("select count(*) from submissions where status = 'GRADED'");
        Map<String, Object> rating = jdbc.queryForMap("select coalesce(avg(stars), 0) as avg, count(*) as n from ("
                + "select stars from room_ratings union all select rating as stars from feedback where rating is not null) x");
        long n = ((Number) rating.get("n")).longValue();
        BigDecimal avg = new BigDecimal(rating.get("avg").toString()).setScale(1, RoundingMode.HALF_UP);
        return Map.of("tracks", tracks, "rooms", rooms, "exercises", exercises, "students", students,
                "gradedSubmissions", submissions, "ratingCount", n, "ratingAverage", n >= 5 ? avg : BigDecimal.ZERO);
    }

    private long count(String sql) {
        Long v = jdbc.queryForObject(sql, Long.class);
        return v == null ? 0 : v;
    }
}
