package ao.lumen.pregao.it;

import static org.assertj.core.api.Assertions.assertThat;

import ao.lumen.pregao.support.Api;
import ao.lumen.pregao.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AdminUsersIT extends IntegrationTest {

    Map<String, String> statuses(Api admin) {
        JsonNode items = admin.get("/admin/users?limit=100").body().path("items");
        Map<String, String> out = new HashMap<>();
        Set<String> ids = new HashSet<>();
        for (JsonNode u : items) {
            assertThat(ids.add(u.path("id").asText())).as("utilizador repetido na lista: " + u.path("email").asText()).isTrue();
            out.put(u.path("id").asText(), u.path("status").asText());
        }
        return out;
    }

    @Test
    void changingOneAccountStatusChangesOnlyThatAccount() {
        Api admin = privileged("admin@exemplo.ao");
        Map<String, String> before = statuses(admin);
        String target = null;
        for (JsonNode u : admin.get("/admin/users?limit=100").body().path("items")) {
            if (u.path("email").asText().equals("marta.neto@exemplo.ao")) target = u.path("id").asText();
        }
        assertThat(admin.patch("/admin/users/" + target, Map.of("status", "SUSPENDED")).status()).isEqualTo(200);
        Map<String, String> after = statuses(admin);
        assertThat(after.get(target)).isEqualTo("SUSPENDED");
        for (var e : before.entrySet()) if (!e.getKey().equals(target)) assertThat(after.get(e.getKey())).as(e.getKey()).isEqualTo(e.getValue());

        assertThat(admin.patch("/admin/users/" + target, Map.of("status", "ACTIVE")).status()).isEqualTo(200);
        assertThat(statuses(admin)).isEqualTo(before);
    }
}
