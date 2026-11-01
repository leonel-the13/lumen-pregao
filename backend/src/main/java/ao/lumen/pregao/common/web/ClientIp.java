package ao.lumen.pregao.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class ClientIp {
    private ClientIp() {}

    /** IP do cliente; atrás de proxy usa server.forward-headers-strategy (perfil prod). */
    public static String of(HttpServletRequest req) {
        return req.getRemoteAddr();
    }

    public static String current() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a) {
            return of(a.getRequest());
        }
        return null;
    }
}
