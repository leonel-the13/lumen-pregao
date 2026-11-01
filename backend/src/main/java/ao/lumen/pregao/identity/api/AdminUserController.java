package ao.lumen.pregao.identity.api;

import ao.lumen.pregao.common.security.SessionUser;
import ao.lumen.pregao.common.web.CursorPage;
import ao.lumen.pregao.identity.api.AdminUserDtos.AdminUserRow;
import ao.lumen.pregao.identity.api.AdminUserDtos.RolesUpdate;
import ao.lumen.pregao.identity.api.AdminUserDtos.StatusUpdate;
import ao.lumen.pregao.identity.app.AdminUserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping
    public CursorPage<AdminUserRow> list(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) String role,
                                         @RequestParam(required = false) String cursor,
                                         @RequestParam(required = false) Integer limit) {
        return service.search(q, role, cursor, limit);
    }

    @org.springframework.web.bind.annotation.PostMapping("/invite")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public AdminUserRow invite(@AuthenticationPrincipal SessionUser me, @Valid @RequestBody AdminUserDtos.InviteRequest in) {
        return service.invite(me.id(), me.name(), in.name(), in.email(), in.roles(), in.courseId(), in.year());
    }

    @org.springframework.web.bind.annotation.PostMapping("/{id}/resend-invite")
    public AdminUserRow resendInvite(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        return service.resendInvite(me.id(), me.name(), id);
    }

    @org.springframework.web.bind.annotation.PostMapping("/{id}/anonymize")
    public AdminUserRow anonymize(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        return service.anonymize(me.id(), id);
    }

    @org.springframework.web.bind.annotation.PostMapping("/{id}/reset-mfa")
    public AdminUserRow resetMfa(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        return service.resetMfa(me.id(), id);
    }

    @PatchMapping("/{id}/roles")
    public AdminUserRow roles(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id,
                              @Valid @RequestBody RolesUpdate body) {
        return service.updateRoles(me.id(), id, body.roles());
    }

    @PatchMapping("/{id}")
    public AdminUserRow status(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id,
                               @Valid @RequestBody StatusUpdate body) {
        return service.updateStatus(me.id(), id, body.status());
    }
}
