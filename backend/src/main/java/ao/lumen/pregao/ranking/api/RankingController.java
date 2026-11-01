package ao.lumen.pregao.ranking.api;

import ao.lumen.pregao.common.config.AppProperties;
import ao.lumen.pregao.common.security.SessionUser;
import ao.lumen.pregao.ranking.app.BadgeService;
import ao.lumen.pregao.ranking.app.RankingProjection;
import ao.lumen.pregao.ranking.app.RankingService;
import ao.lumen.pregao.ranking.app.RankingService.RankingPage;
import ao.lumen.pregao.ranking.app.RankingService.Scope;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RankingController {
    private final RankingService rankings;
    private final RankingProjection projection;
    private final BadgeService badges;
    private final AppProperties props;

    public RankingController(RankingService rankings, RankingProjection projection, BadgeService badges,
                             AppProperties props) {
        this.rankings = rankings;
        this.projection = projection;
        this.badges = badges;
        this.props = props;
    }

    @GetMapping("/rankings")
    public RankingPage read(@AuthenticationPrincipal SessionUser me,
                            @RequestParam(defaultValue = "GLOBAL") Scope scope,
                            @RequestParam(required = false) String course,
                            @RequestParam(required = false) Integer year,
                            @RequestParam(required = false) String cursor,
                            @RequestParam(required = false) Integer limit) {
        return rankings.read(me.id(), scope, course, year, cursor, limit);
    }

    public record Rule(String action, String points) {}

    /** Regras de pontuação públicas (valores iniciais a confirmar, spec §7). */
    @GetMapping("/scoring-rules")
    public Map<String, Object> rules() {
        var l = props.learning();
        var r = props.ranking();
        return Map.of(
                "rules", List.of(
                        new Rule("Tarefa correcta", "pontos da tarefa"),
                        new Rule("Tentativa falhada", "−" + l.failedAttemptPenalty() + " (mínimo da tarefa: 0)"),
                        new Rule("Dica usada", "−" + l.hintPenalty()),
                        new Rule("Exame de módulo", "nota (0–20) × " + props.exam().pointsPerGradeUnit()),
                        new Rule("Sequência de " + r.streakDays() + " dias", "+" + r.streakBonus()),
                        new Rule("Conquista", "conforme a conquista")),
                "badges", badges.catalog(),
                "confirmed", false,
                "note", "Valores iniciais a confirmar pela organização e publicados antes de cada época.");
    }

    @PostMapping("/admin/rankings/rebuild")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Integer> rebuild() {
        return Map.of("users", projection.rebuild());
    }
}
