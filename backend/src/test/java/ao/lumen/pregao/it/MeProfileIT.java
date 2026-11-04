package ao.lumen.pregao.it;

import static org.assertj.core.api.Assertions.assertThat;

import ao.lumen.pregao.support.Api;
import ao.lumen.pregao.support.IntegrationTest;
import ao.lumen.pregao.support.TestInfra.TokenCapture;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MeProfileIT extends IntegrationTest {
    @Autowired TokenCapture tokens;

    private Api freshStudent(String email, String password) {
        privileged("admin@exemplo.ao").post("/admin/users/invite", Map.of("name", "Perfil Editavel", "email", email,
                "roles", List.of("STUDENT")));
        api().post("/auth/password/reset", Map.of("token", tokens.passwordLinks.get(email), "password", password));
        Api s = api();
        assertThat(s.login(email, password).status()).isEqualTo(204);
        return s;
    }

    @Test
    void studentEditsNameCourseAndYear() {
        Api s = freshStudent("perfil.editavel@gmail.com", "Balanco#Ativo-2026");
        String courseId = api().get("/courses").body().get(0).path("id").asText();
        var r = s.patch("/me", Map.of("name", "  Nome Novo da Silva ", "courseId", courseId, "year", 2));
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.body().path("name").asText()).isEqualTo("Nome Novo da Silva");
        assertThat(r.body().path("course").path("id").asText()).isEqualTo(courseId);
        assertThat(r.body().path("year").asInt()).isEqualTo(2);
        assertThat(s.patch("/me", Map.of("name", "   ")).status()).isEqualTo(400);
        assertThat(s.patch("/me", Map.of("year", 9)).status()).isEqualTo(400);
    }

    @Test
    void changingPasswordRequiresTheCurrentOneAndEndsSessions() {
        Api s = freshStudent("muda.senha@gmail.com", "Balanco#Ativo-2026");
        assertThat(s.post("/me/password", Map.of("currentPassword", "errada-123", "newPassword", "Passivo#Corrente-2027")).code())
                .isEqualTo("WRONG_PASSWORD");
        assertThat(s.post("/me/password", Map.of("currentPassword", "Balanco#Ativo-2026", "newPassword", "Passivo#Corrente-2027")).status())
                .isEqualTo(204);
        assertThat(s.get("/me").status()).isEqualTo(401);
        assertThat(api().login("muda.senha@gmail.com", "Balanco#Ativo-2026").status()).isEqualTo(401);
        assertThat(api().login("muda.senha@gmail.com", "Passivo#Corrente-2027").status()).isEqualTo(204);
    }
}
