package ao.lumen.pregao.learning.api;

import ao.lumen.pregao.common.security.SessionUser;
import ao.lumen.pregao.learning.api.LearningDtos.AttemptView;
import ao.lumen.pregao.learning.api.LearningDtos.HintView;
import ao.lumen.pregao.learning.api.LearningDtos.RoomCard;
import ao.lumen.pregao.learning.api.LearningDtos.RoomDetail;
import ao.lumen.pregao.learning.api.LearningDtos.SubmissionAccepted;
import ao.lumen.pregao.learning.api.LearningDtos.SubmissionView;
import ao.lumen.pregao.learning.api.LearningDtos.TrackCard;
import ao.lumen.pregao.learning.api.LearningDtos.TrackDetail;
import ao.lumen.pregao.learning.app.AttemptService;
import ao.lumen.pregao.learning.app.CatalogService;
import ao.lumen.pregao.learning.app.SubmissionService;
import ao.lumen.pregao.learning.domain.Attempt;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class LearningController {
    private final CatalogService catalog;
    private final AttemptService attempts;
    private final SubmissionService submissions;
    private final List<SubmissionVisibility> visibility;

    public LearningController(CatalogService catalog, AttemptService attempts, SubmissionService submissions,
                              List<SubmissionVisibility> visibility) {
        this.catalog = catalog;
        this.attempts = attempts;
        this.submissions = submissions;
        this.visibility = visibility;
    }

    /** Outros módulos (contest) dizem se um utilizador pode ver submissões de uma tentativa de equipa. */
    public interface SubmissionVisibility {
        boolean canSee(UUID userId, Attempt attempt);
    }

    @GetMapping("/tracks")
    public List<TrackCard> tracks(@AuthenticationPrincipal SessionUser me,
                                  @RequestParam(required = false) String course,
                                  @RequestParam(required = false) String topic,
                                  @RequestParam(required = false) Integer difficulty,
                                  @RequestParam(required = false) String q) {
        return catalog.tracks(me.id(), course, topic, difficulty, q);
    }

    @GetMapping("/tracks/{slug}")
    public TrackDetail track(@AuthenticationPrincipal SessionUser me, @PathVariable String slug) {
        return catalog.track(me.id(), slug);
    }

    @GetMapping("/rooms")
    public List<RoomCard> rooms(@AuthenticationPrincipal SessionUser me,
                                @RequestParam(required = false) String q,
                                @RequestParam(required = false) String topic,
                                @RequestParam(required = false) Integer difficulty) {
        return catalog.rooms(me.id(), q, topic, difficulty);
    }

    @GetMapping("/rooms/{id}")
    public RoomDetail room(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        return catalog.room(me.id(), id);
    }

    @PostMapping("/tasks/{id}/attempts")
    public AttemptView start(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        return attempts.startOrGet(id, me.id());
    }

    @GetMapping("/attempts/{id}")
    public AttemptView attempt(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        return attempts.get(id, me.id());
    }

    @PutMapping("/attempts/{id}/draft")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void draft(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id, @RequestBody JsonNode draft) {
        attempts.saveDraft(id, me.id(), draft);
    }

    @PostMapping("/attempts/{id}/hints")
    public HintView hint(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        return attempts.revealHint(id, me.id());
    }

    @PostMapping("/attempts/{id}/submissions")
    public ResponseEntity<SubmissionAccepted> submit(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id,
                                                     @RequestHeader("Idempotency-Key") String key,
                                                     @RequestBody JsonNode payload) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(submissions.submitTask(id, me.id(), key, payload));
    }

    @GetMapping("/submissions/{id}")
    public SubmissionView submission(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        Predicate<Attempt> canSee = a -> visibility.stream().anyMatch(v -> v.canSee(me.id(), a));
        return submissions.get(id, me.id(), canSee);
    }
}
