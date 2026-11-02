package ao.lumen.pregao.it;

import static org.assertj.core.api.Assertions.assertThat;

import ao.lumen.pregao.identity.app.Totp;
import ao.lumen.pregao.support.Api;
import ao.lumen.pregao.support.IntegrationTest;
import ao.lumen.pregao.support.TestInfra.TokenCapture;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class IdentityIT extends IntegrationTest {
    @Autowired
    TokenCapture tokens;

    Map<String, Object> registration(String email) {
        var courses = api().get("/courses").body();
        return Map.of("name", "Estudante Teste", "email", email, "courseId", courses.get(0).path("id").asText(),
                "year", 2, "password", "Mussulo#Balancete-77", "consent", true);
    }

    @Test
    void registrationWithForeignDomainIs422() {
        var r = api().post("/auth/register", registration("alguem@gmail.com"));
        assertThat(r.status()).isEqualTo(422);
        assertThat(r.code()).isEqualTo("EMAIL_DOMAIN_NOT_ALLOWED");
    }

    @Test
    void validationErrorsHaveCodeAndFields() {
        var body = new java.util.HashMap<>(registration("curta@exemplo.ao"));
        body.put("password", "curta");
        var r = api().post("/auth/register", body);
        assertThat(r.status()).isEqualTo(400);
        assertThat(r.code()).isEqualTo("VALIDATION_FAILED");
        assertThat(r.body().path("fields").has("password")).isTrue();
    }

    @Test
    void registerVerifyAndEnter() {
        Api a = api();
        assertThat(a.post("/auth/register", registration("novo.estudante@exemplo.ao")).status()).isEqualTo(202);
        assertThat(api().login("novo.estudante@exemplo.ao", "Mussulo#Balancete-77").code()).isEqualTo("EMAIL_NOT_VERIFIED");
        String token = tokens.byEmail.get("novo.estudante@exemplo.ao");
        var v = a.post("/auth/verify", Map.of("token", token));
        assertThat(v.status()).isEqualTo(200);
        assertThat(v.body().path("signedIn").asBoolean()).isTrue();
        assertThat(a.get("/me").body().path("roles").toString()).contains("STUDENT");
        assertThat(api().post("/auth/verify", Map.of("token", token)).code()).isEqualTo("TOKEN_USED");
    }

    @Test
    void duplicateRegistrationDoesNotRevealAccount() {
        assertThat(api().post("/auth/register", registration("kiala.afonso@exemplo.ao")).status()).isEqualTo(202);
    }

    @Test
    void registeringAgainBeforeVerifyingSendsANewVerificationLink() {
        assertThat(api().post("/auth/register", registration("por.confirmar@exemplo.ao")).status()).isEqualTo(202);
        String first = tokens.byEmail.get("por.confirmar@exemplo.ao");
        redis.delete("rl:resend:por.confirmar@exemplo.ao");
        resetLimitsNow();
        assertThat(api().post("/auth/register", registration("por.confirmar@exemplo.ao")).status()).isEqualTo(202);
        String second = tokens.byEmail.get("por.confirmar@exemplo.ao");
        assertThat(second).isNotEqualTo(first);
        assertThat(api().post("/auth/verify", Map.of("token", second)).status()).isEqualTo(200);
    }

    @Test
    void loginErrorIsGenericForUnknownEmailAndWrongPassword() {
        var unknown = api().login("ninguem@exemplo.ao", "qualquer-coisa-123");
        var wrong = api().login("joao.cassoma@exemplo.ao", "errada-errada-123");
        assertThat(unknown.status()).isEqualTo(401);
        assertThat(wrong.status()).isEqualTo(401);
        assertThat(unknown.body().path("detail")).isEqualTo(wrong.body().path("detail"));
        assertThat(unknown.code()).isEqualTo(wrong.code());
    }

    @Test
    void fiveFailuresLimitTheAccount() {
        for (int i = 0; i < 5; i++) api().login("ana.lopes@exemplo.ao", "errada-errada-" + i);
        var r = api().login("ana.lopes@exemplo.ao", PASSWORD);
        assertThat(r.status()).isEqualTo(429);
    }

    @Test
    void writesWithoutCsrfTokenAreForbidden() {
        Api a = student("joao.cassoma@exemplo.ao");
        var r = a.postWithoutCsrf("/auth/logout", null);
        assertThat(r.status()).isEqualTo(403);
        assertThat(r.code()).isEqualTo("CSRF_INVALID");
    }

    @Test
    void sessionCookieIsHardenedAndRotatedOnLogin() {
        Api a = api();
        a.get("/auth/csrf");
        a.post("/auth/login", Map.of("email", "nobody@exemplo.ao", "password", "qualquer-coisa-123"));
        String before = a.cookie("LP_SESSION");
        a.post("/auth/login", Map.of("email", "joao.cassoma@exemplo.ao", "password", PASSWORD));
        String setCookie = a.lastSetCookies.stream().filter(c -> c.startsWith("LP_SESSION=")).findFirst().orElseThrow();
        assertThat(setCookie).contains("HttpOnly").contains("Secure").contains("SameSite=Lax");
        assertThat(a.cookie("LP_SESSION")).isNotEqualTo(before);
    }

    @Test
    void privilegedProfilesNeedSecondFactorBeforeAnySession() {
        Api a = api();
        var step = a.login("organizador@exemplo.ao", PASSWORD);
        assertThat(step.status()).isEqualTo(200);
        assertThat(step.body().path("status").asText()).isIn("mfa_enrollment_required", "mfa_required");
        assertThat(a.get("/me").status()).isEqualTo(401);
        assertThat(a.post("/auth/mfa", Map.of("code", "000000")).status()).isEqualTo(401);
        if (step.body().has("secret")) {
            rememberSecret("organizador@exemplo.ao", step.body().path("secret").asText());
            String code = Totp.generate(step.body().path("secret").asText(), Instant.now().getEpochSecond() / 30);
            var enrolled = a.post("/auth/mfa", Map.of("code", code));
            assertThat(enrolled.status()).isEqualTo(200);
            assertThat(enrolled.body().path("recoveryCodes").size()).isEqualTo(8);
            assertThat(a.get("/me").body().path("roles").toString()).contains("CONTEST_ORGANIZER");

            // Um código de recuperação substitui o código da aplicação, uma única vez.
            String recovery = enrolled.body().path("recoveryCodes").get(0).asText();
            Api b = api();
            assertThat(b.login("organizador@exemplo.ao", PASSWORD).body().path("status").asText()).isEqualTo("mfa_required");
            assertThat(b.post("/auth/mfa", Map.of("code", recovery)).status()).isEqualTo(204);
            Api c = api();
            c.login("organizador@exemplo.ao", PASSWORD);
            assertThat(c.post("/auth/mfa", Map.of("code", recovery)).status()).isEqualTo(401);
        }
    }

    @Test
    void roleMatrixIsEnforcedOnTheServer() {
        Api s = student("kiala.afonso@exemplo.ao");
        assertThat(s.get("/admin/users").status()).isEqualTo(403);
        assertThat(s.get("/admin/audit").status()).isEqualTo(403);
        assertThat(s.get("/authoring/exercises").status()).isEqualTo(403);
        assertThat(s.get("/admin/contests").status()).isEqualTo(403);
        Api admin = privileged("admin@exemplo.ao");
        assertThat(admin.get("/admin/users").status()).isEqualTo(200);
        assertThat(admin.get("/admin/audit").status()).isEqualTo(200);
        assertThat(api().get("/me").status()).isEqualTo(401);
    }

    @Test
    void profilePreferencesAndExport() {
        Api a = student("joao.cassoma@exemplo.ao");
        assertThat(a.patch("/me", Map.of("theme", "LIGHT")).body().path("theme").asText()).isEqualTo("LIGHT");
        assertThat(a.patch("/me", Map.of("pseudonym", "Maiusculas")).status()).isEqualTo(400);
        assertThat(a.patch("/me", Map.of("pseudonym", "admin_x")).code()).isEqualTo("PSEUDONYM_REJECTED");
        assertThat(a.patch("/me", Map.of("pseudonym", "razao_alta", "rankingVisibility", "PSEUDONYM")).status()).isEqualTo(200);
        var export = a.get("/me/export").body();
        assertThat(export.has("account")).isTrue();
        assertThat(export.toString()).doesNotContain("passwordHash").doesNotContain("mfa");
    }
}
