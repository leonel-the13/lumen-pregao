package ao.lumen.pregao.authoring.api;

import ao.lumen.pregao.authoring.api.AuthoringDtos.ExerciseDto;
import ao.lumen.pregao.authoring.api.AuthoringDtos.ExerciseSummary;
import ao.lumen.pregao.authoring.api.AuthoringDtos.PreviewResponse;
import ao.lumen.pregao.authoring.api.AuthoringDtos.TransitionRequest;
import ao.lumen.pregao.authoring.api.AuthoringDtos.VersionDto;
import ao.lumen.pregao.authoring.api.AuthoringDtos.VersionInput;
import ao.lumen.pregao.authoring.app.AuthoringService;
import ao.lumen.pregao.common.security.SessionUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/authoring")
@PreAuthorize("hasAnyRole('AUTHOR','REVIEWER','ADMIN')")
public class AuthoringController {
    private final AuthoringService service;

    public AuthoringController(AuthoringService service) {
        this.service = service;
    }

    @GetMapping("/exercises")
    public List<ExerciseSummary> list(@AuthenticationPrincipal SessionUser me) {
        return service.list(me);
    }

    @GetMapping("/review-queue")
    @PreAuthorize("hasAnyRole('REVIEWER','ADMIN')")
    public List<ExerciseSummary> queue() {
        return service.reviewQueue();
    }

    @GetMapping("/exercises/{id}")
    public ExerciseDto get(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        return service.get(me, id);
    }

    @PostMapping("/exercises")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('AUTHOR')")
    public ExerciseDto create(@AuthenticationPrincipal SessionUser me, @Valid @RequestBody VersionInput in) {
        return service.create(me, in);
    }

    @PostMapping("/exercises/{id}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('AUTHOR')")
    public ExerciseDto newVersion(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        return service.newVersion(me, id);
    }

    @PatchMapping("/exercises/{id}/versions/{n}")
    @PreAuthorize("hasRole('AUTHOR')")
    public ExerciseDto update(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id, @PathVariable int n,
                              @Valid @RequestBody VersionInput in) {
        return service.updateDraft(me, id, n, in);
    }

    @PostMapping("/versions/{id}/preview")
    public PreviewResponse preview(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id,
                                   @RequestParam(required = false) Long seed) {
        return service.preview(me, id, seed);
    }

    @PostMapping("/versions/{id}/transition")
    public VersionDto transition(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id,
                                 @Valid @RequestBody TransitionRequest req) {
        return service.transition(me, id, req);
    }
}
