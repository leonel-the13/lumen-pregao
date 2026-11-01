package ao.lumen.pregao.contest.api;

import ao.lumen.pregao.common.security.SessionUser;
import ao.lumen.pregao.contest.api.ContestDtos.ContestCard;
import ao.lumen.pregao.contest.api.ContestDtos.ContestDetail;
import ao.lumen.pregao.contest.api.ContestDtos.CreateTeam;
import ao.lumen.pregao.contest.api.ContestDtos.DeleteTeam;
import ao.lumen.pregao.contest.api.ContestDtos.Invite;
import ao.lumen.pregao.contest.api.ContestDtos.ProblemView;
import ao.lumen.pregao.contest.api.ContestDtos.ScoreboardView;
import ao.lumen.pregao.contest.api.ContestDtos.TeamView;
import ao.lumen.pregao.contest.app.ContestEventHub;
import ao.lumen.pregao.contest.app.ContestService;
import ao.lumen.pregao.learning.api.LearningDtos.SubmissionAccepted;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1")
public class ContestController {
    private final ContestService contests;
    private final ContestEventHub hub;

    public ContestController(ContestService contests, ContestEventHub hub) {
        this.contests = contests;
        this.hub = hub;
    }

    @GetMapping("/contests")
    public List<ContestCard> list(@AuthenticationPrincipal SessionUser me) {
        return contests.list(me.id());
    }

    @GetMapping("/contests/{id}")
    public ContestDetail detail(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        return contests.detail(id, me);
    }

    @PostMapping("/contests/{id}/teams")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamView createTeam(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id,
                               @Valid @RequestBody CreateTeam body) {
        return contests.createTeam(id, me, body.name());
    }

    @PostMapping("/contests/{id}/registrations")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamView registerIndividual(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        return contests.registerIndividual(id, me);
    }

    @PostMapping("/teams/{id}/invites")
    public TeamView invite(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id, @Valid @RequestBody Invite body) {
        return contests.invite(id, me, body.email());
    }

    @PostMapping("/teams/{id}/join")
    public TeamView join(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        return contests.join(id, me);
    }

    @PostMapping("/teams/{id}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decline(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        contests.decline(id, me);
    }

    @DeleteMapping("/teams/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeam(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id,
                           @Valid @RequestBody DeleteTeam body) {
        contests.deleteTeam(id, me, body.confirmation());
    }

    @GetMapping("/contests/{id}/problems")
    public List<ProblemView> problems(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        return contests.problems(id, me);
    }

    @PostMapping("/contests/{id}/problems/{label}/submissions")
    public ResponseEntity<SubmissionAccepted> submit(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id,
                                                     @PathVariable String label,
                                                     @RequestHeader("Idempotency-Key") String key,
                                                     @RequestBody JsonNode payload) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(contests.submit(id, label, me, key, payload));
    }

    @GetMapping("/contests/{id}/scoreboard")
    public ScoreboardView scoreboard(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        return contests.scoreboard(id, me);
    }

    @GetMapping(value = "/contests/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id, HttpServletResponse res) {
        contests.detail(id, me); // valida a visibilidade do contest
        res.setHeader("Cache-Control", "no-cache");
        res.setHeader("X-Accel-Buffering", "no");
        return hub.subscribe(id, me.id(), contests.teamOf(id, me.id()).orElse(null), contests.stateOf(id));
    }
}
