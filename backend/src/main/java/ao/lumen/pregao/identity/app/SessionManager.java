package ao.lumen.pregao.identity.app;

import ao.lumen.pregao.common.security.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/** Cria, roda e termina sessões. A sessão só fica activa depois de todos os factores. */
@Component
public class SessionManager {
    public static final String PENDING_MFA = "lp.pendingMfa";

    private final SecurityContextRepository contexts;

    public SessionManager(SecurityContextRepository contexts) {
        this.contexts = contexts;
    }

    /** Estado da «sessão parcial» entre a palavra-passe e o 2.º factor. */
    public record PendingMfa(UUID userId, boolean remember, Instant startedAt, String enrollmentSecretEnc)
            implements java.io.Serializable {}

    public void establish(HttpServletRequest req, HttpServletResponse res, UUID id, String email, String name,
                          Set<String> roles, boolean remember, Duration rememberFor, Instant now) {
        HttpSession existing = req.getSession(false);
        if (existing != null) {
            existing.removeAttribute(PENDING_MFA);
            req.changeSessionId(); // rotação no login (SEC-02)
        }
        HttpSession session = req.getSession(true);
        if (remember) session.setMaxInactiveInterval((int) rememberFor.toSeconds());

        SessionUser principal = new SessionUser(id, email, name, Set.copyOf(roles), now);
        var authorities = roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).collect(Collectors.toSet());
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
        SecurityContextHolder.setContext(ctx);
        contexts.saveContext(ctx, req, res);
    }

    public void startPendingMfa(HttpServletRequest req, PendingMfa pending) {
        HttpSession existing = req.getSession(false);
        if (existing != null) req.changeSessionId();
        req.getSession(true).setAttribute(PENDING_MFA, pending);
    }

    public PendingMfa pendingMfa(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return s == null ? null : (PendingMfa) s.getAttribute(PENDING_MFA);
    }

    public void clearPendingMfa(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s != null) s.removeAttribute(PENDING_MFA);
    }

    public void terminate(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s != null) s.invalidate();
        SecurityContextHolder.clearContext();
    }
}
