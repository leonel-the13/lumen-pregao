package ao.lumen.pregao.it;

import static org.assertj.core.api.Assertions.assertThat;

import ao.lumen.pregao.support.Api;
import ao.lumen.pregao.support.IntegrationTest;
import ao.lumen.pregao.support.TestInfra.TokenCapture;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InviteAndResetIT extends IntegrationTest {
    @Autowired TokenCapture tokens;

    @Test
    void invitedUserMustSetPasswordBeforeLogin() {
        Api admin = privileged("admin@exemplo.ao");
        var r = admin.post("/admin/users/invite", Map.of("name", "Nova Revisora", "email", "nova.revisora@outro.ao",
                "roles", List.of("REVIEWER")));
        assertThat(r.status()).isEqualTo(201);
        assertThat(r.body().path("status").asText()).isEqualTo("INVITED");
        assertThat(admin.post("/admin/users/invite", Map.of("name", "Outra", "email", "nova.revisora@outro.ao",
                "roles", List.of("AUTHOR"))).code()).isEqualTo("EMAIL_TAKEN");

        assertThat(api().login("nova.revisora@outro.ao", "qualquer-coisa-123").status()).isIn(401, 403);
        String token = tokens.passwordLinks.get("nova.revisora@outro.ao");
        assertThat(api().post("/auth/password/reset", Map.of("token", token, "password", "curta")).status()).isEqualTo(400);
        assertThat(api().post("/auth/password/reset", Map.of("token", token, "password", "Revisao#Contas-2026")).status())
                .isEqualTo(200);
        assertThat(api().post("/auth/password/reset", Map.of("token", token, "password", "Revisao#Contas-2027")).code())
                .isEqualTo("TOKEN_USED");
        var login = api().login("nova.revisora@outro.ao", "Revisao#Contas-2026");
        assertThat(login.body().path("status").asText()).isEqualTo("mfa_enrollment_required");
        assertThat(student("kiala.afonso@exemplo.ao").post("/admin/users/invite", Map.of("name", "X Y", "email", "x@y.ao",
                "roles", List.of("ADMIN"))).status()).isEqualTo(403);
    }

    @Test
    void adminSwapsProvisionalEmailForInstitutionalOne() {
        Api admin = privileged("admin@exemplo.ao");
        var invited = admin.post("/admin/users/invite", Map.of("name", "Caloira Gmail", "email", "caloira.lumen@gmail.com",
                "roles", List.of("STUDENT")));
        assertThat(invited.status()).isEqualTo(201);
        String id = invited.body().path("id").asText();
        assertThat(api().post("/auth/password/reset", Map.of("token", tokens.passwordLinks.get("caloira.lumen@gmail.com"),
                "password", "Balanco#Ativo-2026")).status()).isEqualTo(200);
        assertThat(api().login("caloira.lumen@gmail.com", "Balanco#Ativo-2026").status()).isEqualTo(204);

        assertThat(admin.patch("/admin/users/" + id + "/email", Map.of("email", "ana.lopes@exemplo.ao")).code()).isEqualTo("EMAIL_TAKEN");
        var changed = admin.patch("/admin/users/" + id + "/email", Map.of("email", "20261234@ISPTEC.co.ao"));
        assertThat(changed.status()).isEqualTo(200);
        assertThat(changed.body().path("email").asText()).isEqualTo("20261234@isptec.co.ao");
        assertThat(api().login("caloira.lumen@gmail.com", "Balanco#Ativo-2026").status()).isEqualTo(401);
        assertThat(api().login("20261234@isptec.co.ao", "Balanco#Ativo-2026").status()).isEqualTo(204);
        assertThat(student("kiala.afonso@exemplo.ao").patch("/admin/users/" + id + "/email", Map.of("email", "x@y.ao")).status())
                .isEqualTo(403);
    }

    @Test
    void forgotPasswordIsGenericAndResetWorks() {
        var unknown = api().post("/auth/password/forgot", Map.of("email", "nao.existe@exemplo.ao"));
        var known = api().post("/auth/password/forgot", Map.of("email", "ana.lopes@exemplo.ao"));
        assertThat(unknown.status()).isEqualTo(202);
        assertThat(known.status()).isEqualTo(202);
        assertThat(unknown.body().path("detail")).isEqualTo(known.body().path("detail"));
        String token = tokens.passwordLinks.get("ana.lopes@exemplo.ao");
        assertThat(api().post("/auth/password/reset", Map.of("token", token, "password", "Nova#Senha-Ana-2026")).status()).isEqualTo(200);
        assertThat(api().login("ana.lopes@exemplo.ao", "Nova#Senha-Ana-2026").status()).isEqualTo(204);
        assertThat(api().login("ana.lopes@exemplo.ao", PASSWORD).status()).isEqualTo(401);
        // repõe a senha original para os outros testes
        api().post("/auth/password/forgot", Map.of("email", "ana.lopes@exemplo.ao"));
        api().post("/auth/password/reset", Map.of("token", tokens.passwordLinks.get("ana.lopes@exemplo.ao"), "password", PASSWORD));
    }
}
