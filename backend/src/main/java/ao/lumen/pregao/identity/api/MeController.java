package ao.lumen.pregao.identity.api;

import ao.lumen.pregao.common.security.SessionUser;
import ao.lumen.pregao.identity.api.MeDtos.MeResponse;
import ao.lumen.pregao.identity.api.MeDtos.PatchMe;
import ao.lumen.pregao.identity.app.MeService;
import ao.lumen.pregao.identity.app.SessionManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {
    private final MeService me;
    private final SessionManager sessions;
    private final ao.lumen.pregao.identity.app.PasswordService passwords;

    public MeController(MeService me, SessionManager sessions, ao.lumen.pregao.identity.app.PasswordService passwords) {
        this.passwords = passwords;
        this.me = me;
        this.sessions = sessions;
    }

    @GetMapping
    public MeResponse get(@AuthenticationPrincipal SessionUser user) {
        return me.get(user.id());
    }

    @PatchMapping
    public MeResponse patch(@AuthenticationPrincipal SessionUser user, @Valid @RequestBody PatchMe body) {
        return me.patch(user.id(), body);
    }

    /** Muda a palavra-passe; a sessão actual também termina. */
    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void password(@AuthenticationPrincipal SessionUser user, @Valid @RequestBody MeDtos.ChangePassword body,
                         HttpServletRequest req) {
        passwords.change(user.id(), body.currentPassword(), body.newPassword());
        sessions.terminate(req);
    }

    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> export(@AuthenticationPrincipal SessionUser user) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"lumen-pregao-dados.json\"")
                .body(me.export(user.id()));
    }

    @PostMapping("/deletion-request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> deletion(@AuthenticationPrincipal SessionUser user, HttpServletRequest req) {
        me.requestDeletion(user.id());
        sessions.terminate(req);
        return Map.of("status", "deletion_requested",
                "detail", "Recebemos o teu pedido. A administração trata a eliminação e confirma por email.");
    }
}
