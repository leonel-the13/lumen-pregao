package ao.lumen.pregao.common.security;

import ao.lumen.pregao.common.error.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    public static final String CSRF_HEADER = "X-CSRF-Token";
    public static final String CSRF_COOKIE = "XSRF-TOKEN";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain api(HttpSecurity http, ObjectMapper json, SecurityContextRepository contexts,
                                   SessionRevocation revocation,
                                   @Value("${server.servlet.session.cookie.secure:true}") boolean secureCookie)
            throws Exception {
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepo.setHeaderName(CSRF_HEADER);
        csrfRepo.setCookieName(CSRF_COOKIE);
        csrfRepo.setCookieCustomizer(c -> c.secure(secureCookie).sameSite("Lax").path("/"));

        http
            .csrf(c -> c.csrfTokenRepository(csrfRepo)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .securityContext(c -> c.securityContextRepository(contexts))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .sessionFixation(f -> f.changeSessionId()))
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .logout(l -> l.disable())
            .requestCache(r -> r.disable())
            .headers(h -> h
                    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                    .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    .frameOptions(f -> f.deny()))
            .authorizeHttpRequests(a -> a
                    .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                    .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                    .requestMatchers("/api/v1/openapi", "/api/v1/openapi/**", "/api/v1/docs", "/api/v1/docs/**",
                            "/api/v1/swagger-ui/**", "/swagger-ui/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf", "/api/v1/courses", "/api/v1/public/**",
                            "/api/v1/scoring-rules").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/verify",
                            "/api/v1/auth/resend-verification", "/api/v1/auth/login", "/api/v1/auth/mfa",
                            "/api/v1/auth/password/forgot", "/api/v1/auth/password/reset").permitAll()
                    .requestMatchers("/api/v1/**").authenticated()
                    .anyRequest().denyAll())
            .exceptionHandling(e -> e
                    .authenticationEntryPoint((req, res, ex) -> write(res, json, GlobalExceptionHandler.problem(
                            HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Sessão necessária",
                            "A tua sessão expirou ou não iniciaste sessão. Entra de novo para continuar.")))
                    .accessDeniedHandler((req, res, ex) -> write(res, json, ex instanceof CsrfException
                            ? GlobalExceptionHandler.problem(HttpStatus.FORBIDDEN, "CSRF_INVALID", "Pedido recusado",
                                    "O token de segurança é inválido ou expirou. Actualiza a página e tenta de novo.")
                            : GlobalExceptionHandler.problem(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Sem permissão",
                                    "Não tens permissão para esta operação."))))
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
            .addFilterAfter(new RevokedSessionFilter(revocation, json), BasicAuthenticationFilter.class);
        return http.build();
    }

    static void write(HttpServletResponse res, ObjectMapper json, ProblemDetail pd) throws IOException {
        res.setStatus(pd.getStatus());
        res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        json.writeValue(res.getOutputStream(), pd);
    }

    /** Garante que o cookie XSRF-TOKEN é emitido em cada resposta (tokens diferidos do Spring Security 6). */
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            CsrfToken token = (CsrfToken) req.getAttribute(CsrfToken.class.getName());
            if (token != null) token.getToken();
            chain.doFilter(req, res);
        }
    }

    /** Recusa sessões autenticadas antes de uma revogação (suspensão, mudança de perfis). */
    static final class RevokedSessionFilter extends OncePerRequestFilter {
        private final SessionRevocation revocation;
        private final ObjectMapper json;

        RevokedSessionFilter(SessionRevocation revocation, ObjectMapper json) {
            this.revocation = revocation;
            this.json = json;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof SessionUser u && revocation.isRevoked(u)) {
                var session = req.getSession(false);
                if (session != null) session.invalidate();
                SecurityContextHolder.clearContext();
                write(res, json, GlobalExceptionHandler.problem(HttpStatus.UNAUTHORIZED, "SESSION_REVOKED",
                        "Sessão terminada", "A tua sessão foi terminada. Entra de novo para continuar."));
                return;
            }
            chain.doFilter(req, res);
        }
    }
}
