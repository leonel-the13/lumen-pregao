package ao.lumen.pregao.it;

import static org.assertj.core.api.Assertions.assertThat;

import ao.lumen.pregao.identity.app.AccountReminders;
import ao.lumen.pregao.notification.app.NotificationService;
import ao.lumen.pregao.support.Api;
import ao.lumen.pregao.support.IntegrationTest;
import ao.lumen.pregao.support.TestInfra.SentMail;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EmailsIT extends IntegrationTest {
    @Autowired
    SentMail sent;
    @Autowired
    AccountReminders reminders;
    @Autowired
    NotificationService notifications;

    static Map<String, Object> content(String subject, String body) {
        return Map.of("subject", subject, "title", "Olá, {nome}!", "body", body, "ctaLabel", "Voltar às trilhas", "ctaPath", "/trilhas");
    }

    String userId(Api admin, String email) {
        for (JsonNode u : admin.get("/admin/users?limit=100&q=" + email).body().path("items")) {
            if (u.path("email").asText().equals(email)) return u.path("id").asText();
        }
        throw new IllegalStateException("sem utilizador " + email);
    }

    static <T> T await(Supplier<T> s, java.util.function.Predicate<T> ok) {
        Instant end = Instant.now().plus(Duration.ofSeconds(20));
        T v = s.get();
        while (!ok.test(v) && Instant.now().isBefore(end)) {
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            v = s.get();
        }
        return v;
    }

    @Test
    void onlyAdminsUseTheEmailArea() {
        Api student = student("joao.cassoma@exemplo.ao");
        assertThat(student.get("/admin/email-templates").status()).isEqualTo(403);
        assertThat(student.post("/admin/emails/campaigns", Map.of("audience", Map.of("audience", "ALL_STUDENTS"),
                "content", content("x", "y")), Map.of("Idempotency-Key", "k-" + UUID.randomUUID())).status()).isEqualTo(403);
        assertThat(student.get("/admin/emails/reminders").status()).isEqualTo(403);
        assertThat(privileged("docente.p@exemplo.ao").get("/admin/email-templates").status()).isEqualTo(403);
        assertThat(api().get("/admin/email-templates").status()).isEqualTo(401);
    }

    @Test
    void previewFillsVariablesEscapesTextAndRejectsUnknownOnes() {
        Api admin = privileged("admin@exemplo.ao");
        var p = admin.post("/admin/emails/preview", Map.of("content", content("{nome}, novidades", "Linha <b>um</b>\nLinha dois")));
        assertThat(p.status()).isEqualTo(200);
        assertThat(p.body().path("subject").asText()).isEqualTo("Admin, novidades");
        assertThat(p.body().path("html").asText()).contains("Olá, Admin!").contains("&lt;b&gt;um&lt;/b&gt;").doesNotContain("<b>um</b>")
                .contains("/definicoes");

        var bad = admin.post("/admin/emails/preview", Map.of("content", content("Olá {nme}", "x")));
        assertThat(bad.status()).isEqualTo(400);
        assertThat(bad.code()).isEqualTo("UNKNOWN_VARIABLE");

        var external = admin.post("/admin/emails/preview", Map.of("content",
                Map.of("subject", "x", "body", "y", "ctaLabel", "Clica", "ctaPath", "https://exemplo.com")));
        assertThat(external.code()).isEqualTo("CTA_PATH_INVALID");
        assertThat(admin.get("/admin/email-templates").body().size()).isGreaterThanOrEqualTo(5);
    }

    @Test
    void individualCampaignIsIdempotentAndDelivered() {
        Api admin = privileged("admin@exemplo.ao");
        String ana = userId(admin, "ana.lopes@exemplo.ao");
        String key = "camp-" + UUID.randomUUID();
        Map<String, Object> body = Map.of("audience", Map.of("audience", "USER", "userId", ana),
                "content", content("{nome}, uma ajuda", "A sala 3 tem uma dica para ti."));

        assertThat(admin.post("/admin/emails/campaigns", body).code()).isEqualTo("IDEMPOTENCY_KEY_REQUIRED");
        var r1 = admin.post("/admin/emails/campaigns", body, Map.of("Idempotency-Key", key));
        assertThat(r1.status()).isEqualTo(202);
        var r2 = admin.post("/admin/emails/campaigns", body, Map.of("Idempotency-Key", key));
        assertThat(r2.body().path("id").asText()).isEqualTo(r1.body().path("id").asText());
        assertThat(r1.body().path("total").asLong()).isEqualTo(1);

        List<SentMail.Sent> mails = await(() -> sent.to("ana.lopes@exemplo.ao").stream()
                .filter(s -> s.email().subject().equals("Ana, uma ajuda")).toList(), l -> !l.isEmpty());
        assertThat(mails).hasSize(1);
        assertThat(mails.get(0).email().html()).contains("http://localhost:3000/trilhas");
        JsonNode row = await(() -> admin.get("/admin/emails/campaigns?limit=10").body().path("items").get(0),
                n -> n.path("sent").asLong() == 1);
        assertThat(row.path("sent").asLong()).isEqualTo(1);
        assertThat(row.path("audienceLabel").asText()).contains("Ana Lopes");
    }

    @Test
    void studentsWhoTurnEmailsOffAreLeftOutOfSegments() {
        Api admin = privileged("admin@exemplo.ao");
        Map<String, Object> all = Map.of("audience", "ALL_STUDENTS");
        int before = admin.post("/admin/emails/audience", all).body().path("recipients").asInt();

        Api kiala = student("kiala.afonso@exemplo.ao");
        assertThat(kiala.patch("/me", Map.of("emailOptIn", false)).body().path("emailOptIn").asBoolean()).isFalse();
        try {
            var after = admin.post("/admin/emails/audience", all).body();
            assertThat(after.path("recipients").asInt()).isEqualTo(before - 1);
            assertThat(after.path("optedOut").asInt()).isGreaterThanOrEqualTo(1);

            // as notificações importantes deixam de chegar por email; as de segurança chegam sempre
            UUID id = UUID.fromString(kiala.get("/me").body().path("id").asText());
            notifications.notify(id, "EXAM", "Exame corrigido", "Teste opt-out", "/exames");
            notifications.notify(id, "SECURITY", "Palavra-passe alterada", "Teste segurança", "/definicoes");
            var security = await(() -> sent.to("kiala.afonso@exemplo.ao").stream()
                    .filter(s -> s.email().subject().startsWith("Palavra-passe alterada")).toList(), l -> !l.isEmpty());
            assertThat(security).hasSize(1);
            assertThat(sent.to("kiala.afonso@exemplo.ao").stream().anyMatch(s -> s.email().text().contains("Teste opt-out"))).isFalse();
        } finally {
            kiala.patch("/me", Map.of("emailOptIn", true));
        }
    }

    @Test
    void importantNotificationsAlsoArriveByEmailButBadgesDoNot() {
        Api joao = student("joao.cassoma@exemplo.ao");
        UUID id = UUID.fromString(joao.get("/me").body().path("id").asText());
        notifications.notify(id, "BADGE", "Conquista nova", "Teste conquista", "/portefolio");
        notifications.notify(id, "EXAM", "Exame corrigido", "Teste exame por email", "/exames");
        var exam = await(() -> sent.to("joao.cassoma@exemplo.ao").stream()
                .filter(s -> s.email().text().contains("Teste exame por email")).toList(), l -> !l.isEmpty());
        assertThat(exam).hasSize(1);
        assertThat(exam.get(0).email().html()).contains("http://localhost:3000/exames").contains("/definicoes");
        assertThat(sent.to("joao.cassoma@exemplo.ao").stream().anyMatch(s -> s.email().text().contains("Teste conquista"))).isFalse();
    }

    @Test
    void unverifiedAccountsGetAtMostTheConfiguredReminders() {
        String email = "lembrete-" + UUID.randomUUID().toString().substring(0, 8) + "@exemplo.ao";
        var courses = api().get("/courses").body();
        assertThat(api().post("/auth/register", Map.of("name", "Estudante Esquecido", "email", email,
                "courseId", courses.get(0).path("id").asText(), "year", 2, "password", "Mussulo#Balancete-77", "consent", true)).status())
                .isEqualTo(202);
        java.util.function.Supplier<Long> reminded = () -> sent.to(email).stream()
                .filter(s -> s.email().subject().startsWith("A tua conta ainda está à espera")).count();

        reminders.runAutomatic();
        assertThat(reminded.get()).isZero(); // ainda não passaram 3 dias

        clock.advance(Duration.ofDays(4));
        reminders.runAutomatic();
        assertThat(await(reminded, n -> n == 1)).isEqualTo(1);
        reminders.runAutomatic();
        clock.advance(Duration.ofDays(8));
        reminders.runAutomatic();
        assertThat(await(reminded, n -> n == 2)).isEqualTo(2);
        clock.advance(Duration.ofDays(8));
        reminders.runAutomatic();
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        assertThat(reminded.get()).isEqualTo(2); // máximo de 2 automáticos

        Api admin = privileged("admin@exemplo.ao");
        String id = userId(admin, email);
        assertThat(admin.post("/admin/users/" + id + "/remind-verification", null).status()).isEqualTo(200);
        assertThat(await(reminded, n -> n == 3)).isEqualTo(3);
        assertThat(admin.post("/admin/users/" + id + "/remind-verification", null).code()).isEqualTo("REMINDER_TOO_SOON");
        assertThat(admin.get("/admin/emails/reminders").body().path("unverified").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(admin.post("/admin/users/" + userId(admin, "ana.lopes@exemplo.ao") + "/remind-verification", null).code())
                .isEqualTo("NOT_PENDING_VERIFICATION");
    }
}
