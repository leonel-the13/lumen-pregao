package ao.lumen.pregao.common.web;

import ao.lumen.pregao.common.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Atrás do Cloudflare, o backend recebe os pedidos do frontend (Worker). O frontend envia o IP real em X-Client-IP
 * (de CF-Connecting-IP) com um segredo partilhado; só nesse caso o IP é aceite (rate limiting e auditoria).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ClientIpFilter extends OncePerRequestFilter {
    public static final String IP_HEADER = "X-Client-IP";
    public static final String SECRET_HEADER = "X-Proxy-Secret";
    private static final Pattern IP = Pattern.compile("^[0-9a-fA-F:.]{3,45}$");
    private final byte[] secret;

    public ClientIpFilter(AppProperties props) {
        String s = props.proxySecret();
        this.secret = s == null || s.isBlank() ? null : s.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String ip = req.getHeader(IP_HEADER);
        String given = req.getHeader(SECRET_HEADER);
        if (secret != null && ip != null && given != null && IP.matcher(ip).matches()
                && MessageDigest.isEqual(secret, given.getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(new HttpServletRequestWrapper(req) {
                @Override
                public String getRemoteAddr() {
                    return ip;
                }
            }, res);
            return;
        }
        chain.doFilter(req, res);
    }
}
