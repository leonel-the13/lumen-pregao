package ao.lumen.pregao.it;

import static org.assertj.core.api.Assertions.assertThat;

import ao.lumen.pregao.support.Api;
import ao.lumen.pregao.support.IntegrationTest;
import ao.lumen.pregao.support.TestInfra.TokenCapture;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Notificações, avaliações de salas, feedback, números públicos e eliminação de contas. */
class EngagementIT extends IntegrationTest {
    @Autowired
    TokenCapture tokens;

    Api newStudent(String email) {
        var courses = api().get("/courses").body();
        assertThat(courses.toString()).contains("Outro curso do ISPTEC");
        Api a = api();
        assertThat(a.post("/auth/register", Map.of("name", "Estudante Avaliação", "email", email,
                "courseId", courses.get(0).path("id").asText(), "year", 1, "password", "Mussulo#Balancete-77",
                "consent", true)).status()).isEqualTo(202);
        assertThat(a.post("/auth/verify", Map.of("token", tokens.byEmail.get(email))).status()).isIn(200, 204);
        Api s = api();
        assertThat(s.login(email, "Mussulo#Balancete-77").status()).isEqualTo(204);
        return s;
    }

    String firstOpenRoom(Api a) {
        for (JsonNode m : a.get("/tracks/fiscalidade-iva").body().path("modules")) {
            for (JsonNode r : m.path("rooms")) if (!r.path("state").asText().equals("LOCKED")) return r.path("id").asText();
        }
        throw new IllegalStateException("sem sala aberta");
    }

    @Test
    void welcomeNotificationCanBeReadAndCounted() {
        Api s = newStudent("notificacoes@exemplo.ao");
        assertThat(s.get("/me/notifications/unread-count").body().path("unread").asLong()).isEqualTo(1);
        JsonNode page = s.get("/me/notifications").body();
        assertThat(page.path("items").get(0).path("kind").asText()).isEqualTo("WELCOME");
        String id = page.path("items").get(0).path("id").asText();
        assertThat(student("ana.lopes@exemplo.ao").post("/me/notifications/" + id + "/read", null).status()).isEqualTo(404);
        assertThat(s.post("/me/notifications/" + id + "/read", null).status()).isEqualTo(204);
        assertThat(s.get("/me/notifications/unread-count").body().path("unread").asLong()).isZero();
        assertThat(s.post("/me/notifications/read-all", null).body().path("updated").asInt()).isZero();
    }

    @Test
    void onlyStudentsWhoTriedARoomCanRateIt() {
        Api s = newStudent("avaliacao@exemplo.ao");
        String room = firstOpenRoom(s);
        assertThat(s.put("/rooms/" + room + "/rating", Map.of("stars", 5)).code()).isEqualTo("RATING_NOT_ALLOWED");
        String task = s.get("/rooms/" + room).body().path("tasks").get(0).path("id").asText();
        assertThat(s.post("/tasks/" + task + "/attempts", null).status()).isIn(200, 201);
        assertThat(s.put("/rooms/" + room + "/rating", Map.of("stars", 6)).status()).isEqualTo(400);
        JsonNode r = s.put("/rooms/" + room + "/rating", Map.of("stars", 4, "comment", "Enunciado claro.")).body();
        assertThat(r.path("myStars").asInt()).isEqualTo(4);
        assertThat(r.path("count").asLong()).isGreaterThanOrEqualTo(1);
        // Avaliar de novo actualiza, não duplica.
        long count = r.path("count").asLong();
        assertThat(s.put("/rooms/" + room + "/rating", Map.of("stars", 3)).body().path("count").asLong()).isEqualTo(count);
    }

    @Test
    void feedbackReachesAdminsAndOnlyAdminsManageIt() {
        Api s = student("joao.cassoma@exemplo.ao");
        assertThat(s.post("/feedback", Map.of("category", "XYZ", "message", "Mensagem de teste")).code()).isEqualTo("CATEGORY_INVALID");
        var f = s.post("/feedback", Map.of("category", "SUGGESTION", "rating", 5, "message", "Gostava de mais salas de IVA.", "page", "/trilhas"));
        assertThat(f.status()).isEqualTo(201);
        String id = f.body().path("id").asText();
        assertThat(s.get("/admin/feedback").status()).isEqualTo(403);

        Api admin = privileged("admin@exemplo.ao");
        assertThat(admin.get("/admin/feedback?status=NEW").body().path("items").toString()).contains("Gostava de mais salas de IVA.");
        assertThat(admin.patch("/admin/feedback/" + id, Map.of("status", "RESOLVED")).status()).isEqualTo(200);
        assertThat(admin.get("/me/notifications").body().toString()).contains("Novo feedback recebido");
    }

    @Test
    void teamInviteNotifiesAndMemberCanLeave() {
        Api cap = newStudent("capitao.equipa@exemplo.ao");
        Api mem = newStudent("membro.equipa@exemplo.ao");
        String contest = null;
        for (JsonNode c : cap.get("/contests").body()) if (c.path("slug").asText().equals("maratona-ronda-1")) contest = c.path("id").asText();
        var team = cap.post("/contests/" + contest + "/teams", Map.of("name", "Equipa Saída"));
        assertThat(team.status()).isEqualTo(201);
        String teamId = team.body().path("id").asText();
        assertThat(cap.post("/teams/" + teamId + "/invites", Map.of("email", "membro.equipa@exemplo.ao")).status()).isEqualTo(200);
        assertThat(mem.get("/me/notifications").body().toString()).contains("TEAM_INVITE");
        assertThat(mem.post("/teams/" + teamId + "/join", null).status()).isEqualTo(200);
        assertThat(cap.post("/teams/" + teamId + "/leave", null).code()).isEqualTo("CAPTAIN_CANNOT_LEAVE");
        assertThat(mem.post("/teams/" + teamId + "/leave", null).status()).isEqualTo(204);
        assertThat(mem.post("/teams/" + teamId + "/leave", null).code()).isEqualTo("NOT_MEMBER");
        assertThat(cap.get("/me/notifications").body().toString()).contains("saiu da equipa");
    }

    @Test
    void publicStatsAreAnonymousAggregates() {
        var r = api().get("/public/stats");
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.body().path("tracks").asLong()).isGreaterThan(0);
        assertThat(r.body().has("ratingAverage")).isTrue();
        assertThat(r.body().toString()).doesNotContain("@");
    }

    @Test
    void deletionRequestIsCarriedOutByAnonymizingTheAccount() {
        Api s = newStudent("eliminar@exemplo.ao");
        String id = s.get("/me").body().path("id").asText();
        Api admin = privileged("admin@exemplo.ao");
        assertThat(admin.post("/admin/users/" + id + "/anonymize", null).code()).isEqualTo("NO_DELETION_REQUEST");
        assertThat(s.post("/me/deletion-request", null).status()).isEqualTo(202);
        assertThat(student("ana.lopes@exemplo.ao").post("/admin/users/" + id + "/anonymize", null).status()).isEqualTo(403);
        var row = admin.post("/admin/users/" + id + "/anonymize", null);
        assertThat(row.status()).isEqualTo(200);
        assertThat(row.body().path("status").asText()).isEqualTo("DELETED");
        assertThat(row.body().toString()).doesNotContain("eliminar@exemplo.ao");
        assertThat(api().login("eliminar@exemplo.ao", "Mussulo#Balancete-77").status()).isEqualTo(401);
    }
}
