package ao.lumen.pregao.authoring.api;

import ao.lumen.pregao.authoring.api.AuthoringDtos.LegalSourceDto;
import ao.lumen.pregao.authoring.api.AuthoringDtos.LegalSourceInput;
import ao.lumen.pregao.authoring.api.AuthoringDtos.LegalSourceUpdate;
import ao.lumen.pregao.authoring.app.LegalSourceService;
import ao.lumen.pregao.common.security.SessionUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/v1/legal-sources")
public class LegalSourceController {
    private final LegalSourceService service;

    public LegalSourceController(LegalSourceService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AUTHOR','REVIEWER','ADMIN')")
    public List<LegalSourceDto> list() {
        return service.list();
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('AUTHOR','REVIEWER','ADMIN')")
    public Map<String, Long> alerts() {
        return Map.of("affectedExercises", service.openAlerts());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('REVIEWER','ADMIN')")
    public LegalSourceDto create(@AuthenticationPrincipal SessionUser me, @Valid @RequestBody LegalSourceInput in) {
        return service.create(me.id(), in);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('REVIEWER','ADMIN')")
    public LegalSourceUpdate update(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id,
                                    @Valid @RequestBody LegalSourceInput in) {
        return service.update(me.id(), id, in);
    }
}
