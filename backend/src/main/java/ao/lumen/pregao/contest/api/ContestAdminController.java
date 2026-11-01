package ao.lumen.pregao.contest.api;

import ao.lumen.pregao.common.security.SessionUser;
import ao.lumen.pregao.contest.api.ContestDtos.AdminContestRow;
import ao.lumen.pregao.contest.api.ContestDtos.ContestInput;
import ao.lumen.pregao.contest.app.ContestAdminService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/contests")
@PreAuthorize("hasAnyRole('CONTEST_ORGANIZER','ADMIN')")
public class ContestAdminController {
    private final ContestAdminService service;

    public ContestAdminController(ContestAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminContestRow> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminContestRow create(@AuthenticationPrincipal SessionUser me, @Valid @RequestBody ContestInput in) {
        return service.create(me, in);
    }

    @PatchMapping("/{id}")
    public AdminContestRow update(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id,
                                  @Valid @RequestBody ContestInput in) {
        return service.update(me, id, in);
    }

    @PostMapping("/{id}/publish")
    public AdminContestRow publish(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        return service.publish(me, id);
    }

    @PostMapping("/{id}/publish-results")
    public AdminContestRow publishResults(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        return service.publishResults(me, id);
    }

    @PostMapping("/{id}/close")
    public AdminContestRow close(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        return service.close(me, id);
    }
}
