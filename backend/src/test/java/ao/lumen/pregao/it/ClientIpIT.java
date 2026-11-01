package ao.lumen.pregao.it;

import static org.assertj.core.api.Assertions.assertThat;

import ao.lumen.pregao.common.audit.AuditLogRepository;
import ao.lumen.pregao.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.proxy-secret=segredo-de-teste")
class ClientIpIT extends IntegrationTest {
    @Autowired AuditLogRepository audit;

    @Test
    void trustsForwardedIpOnlyWithSharedSecret() {
        var a = api();
        a.get("/auth/csrf");
        a.send("POST", "/auth/login", Map.of("email", "nobody1@exemplo.ao", "password", "qualquer-coisa-1"),
                Map.of("X-Client-IP", "203.0.113.7", "X-Proxy-Secret", "segredo-de-teste"), true);
        a.send("POST", "/auth/login", Map.of("email", "nobody2@exemplo.ao", "password", "qualquer-coisa-1"),
                Map.of("X-Client-IP", "198.51.100.9", "X-Proxy-Secret", "errado"), true);
        var ips = audit.findAll().stream().filter(l -> "LOGIN_FAILED".equals(l.getAction())).map(l -> l.getIp()).toList();
        assertThat(ips).contains("203.0.113.7").doesNotContain("198.51.100.9");
    }
}
