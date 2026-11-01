package ao.lumen.pregao.identity.api;

import ao.lumen.pregao.common.security.SecurityConfig;
import ao.lumen.pregao.common.web.ClientIp;
import ao.lumen.pregao.identity.api.AuthDtos.Accepted;
import ao.lumen.pregao.identity.api.AuthDtos.CourseDto;
import ao.lumen.pregao.identity.api.AuthDtos.CsrfResponse;
import ao.lumen.pregao.identity.api.AuthDtos.LoginRequest;
import ao.lumen.pregao.identity.api.AuthDtos.LoginStep;
import ao.lumen.pregao.identity.api.AuthDtos.MfaRequest;
import ao.lumen.pregao.identity.api.AuthDtos.RegisterRequest;
import ao.lumen.pregao.identity.api.AuthDtos.ResendRequest;
import ao.lumen.pregao.identity.api.AuthDtos.VerifyRequest;
import ao.lumen.pregao.identity.app.AuthService;
import ao.lumen.pregao.identity.app.PasswordService;
import ao.lumen.pregao.identity.app.SessionManager;
import ao.lumen.pregao.identity.infra.CourseRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private final AuthService auth;
    private final PasswordService passwords;
    private final SessionManager sessions;
    private final CourseRepository courses;

    public AuthController(AuthService auth, PasswordService passwords, SessionManager sessions, CourseRepository courses) {
        this.auth = auth;
        this.passwords = passwords;
        this.sessions = sessions;
        this.courses = courses;
    }

    @GetMapping("/auth/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(SecurityConfig.CSRF_HEADER, token.getToken());
    }

    @GetMapping("/courses")
    public List<CourseDto> courses() {
        return courses.findAllByOrderByNameAsc().stream()
                .map(c -> new CourseDto(c.getId(), c.getCode(), c.getName())).toList();
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Accepted register(@Valid @RequestBody RegisterRequest r, HttpServletRequest req) {
        auth.register(r, ClientIp.of(req));
        return new Accepted("verification_sent",
                "Enviámos um link para o teu email institucional. Abre-o neste dispositivo para activar a conta.");
    }

    @PostMapping("/auth/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Accepted resend(@Valid @RequestBody ResendRequest r) {
        auth.resendVerification(r.email());
        return new Accepted("verification_sent", "Se houver uma conta por verificar com este email, enviámos um novo link.");
    }

    @PostMapping("/auth/verify")
    public Map<String, Object> verify(@Valid @RequestBody VerifyRequest r, HttpServletRequest req, HttpServletResponse res) {
        var user = auth.verify(r.token());
        boolean signedIn = auth.loginAfterVerification(user, req, res);
        return Map.of("status", "verified", "signedIn", signedIn);
    }

    @PostMapping("/auth/password/forgot")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Accepted forgot(@Valid @RequestBody AuthDtos.ForgotRequest r, HttpServletRequest req) {
        passwords.forgot(r.email(), ClientIp.of(req));
        return new Accepted("reset_sent", "Se existir uma conta com este email, enviámos um link para redefinir a palavra-passe.");
    }

    @PostMapping("/auth/password/reset")
    public Map<String, Object> reset(@Valid @RequestBody AuthDtos.ResetRequest r, HttpServletRequest req) {
        String email = passwords.reset(r.token(), r.password());
        sessions.terminate(req);
        return Map.of("status", "password_set", "email", email);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<LoginStep> login(@Valid @RequestBody LoginRequest r, HttpServletRequest req, HttpServletResponse res) {
        return auth.login(r, req, res)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** 204 no login normal; 200 com os códigos de recuperação quando o 2.º factor acabou de ser configurado. */
    @PostMapping("/auth/mfa")
    public ResponseEntity<AuthDtos.MfaResult> mfa(@Valid @RequestBody MfaRequest r, HttpServletRequest req, HttpServletResponse res) {
        var codes = auth.mfa(r.code(), req, res);
        return codes.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(new AuthDtos.MfaResult("enrolled", codes));
    }

    @PostMapping("/me/mfa/recovery-codes")
    public AuthDtos.MfaResult regenerate(@org.springframework.security.core.annotation.AuthenticationPrincipal
                                         ao.lumen.pregao.common.security.SessionUser me) {
        return new AuthDtos.MfaResult("regenerated", auth.regenerateRecoveryCodes(me.id()));
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest req) {
        sessions.terminate(req);
    }
}
