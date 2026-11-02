package ao.lumen.pregao.it;

import static org.assertj.core.api.Assertions.assertThat;

import ao.lumen.pregao.support.Api;
import ao.lumen.pregao.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContentAdminIT extends IntegrationTest {

    @Test
    void packIsImportedIdempotentlyAndOnlyAdminsManageContent() {
        Api admin = privileged("admin@exemplo.ao");
        JsonNode tree = admin.get("/admin/content/tracks").body();
        assertThat(tree.size()).isGreaterThanOrEqualTo(3);
        int rooms = 0;
        for (JsonNode t : tree) for (JsonNode m : t.path("modules")) rooms += m.path("rooms").size();
        assertThat(rooms).isGreaterThanOrEqualTo(26);

        var again = admin.post("/admin/content/import", null).body();
        assertThat(again.path("exercisesCreated").asInt()).isZero();
        assertThat(again.path("roomsCreated").asInt()).isZero();
        assertThat(again.path("exercisesExisting").asInt()).isEqualTo(45);

        Api s = student("kiala.afonso@exemplo.ao");
        assertThat(s.get("/admin/content/tracks").status()).isEqualTo(403);
        assertThat(s.post("/admin/content/import", null).status()).isEqualTo(403);
        assertThat(admin.get("/admin/content/exams").body().size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void adminCreatesTrackModuleRoomAndTask() {
        Api admin = privileged("admin@exemplo.ao");
        String track = admin.post("/admin/content/tracks", Map.of("slug", "trilha-teste-admin", "title", "Trilha de teste",
                "summary", "Resumo", "difficulty", 1, "topic", "Contabilidade", "status", "DRAFT")).body().path("id").asText();
        String module = admin.post("/admin/content/tracks/" + track + "/modules", Map.of("title", "Módulo 1")).body().path("id").asText();
        String room = admin.post("/admin/content/modules/" + module + "/rooms", Map.of("title", "Sala 1", "description", "Descrição",
                "difficulty", 1, "estMinutes", 10, "status", "PUBLISHED")).body().path("id").asText();
        String version = admin.get("/authoring/exercises").body().get(0).path("latestVersionId").asText();
        var task = admin.post("/admin/content/rooms/" + room + "/tasks", Map.of("exerciseVersionId", version, "title", "Tarefa", "points", 10));
        assertThat(task.status()).isEqualTo(201);
        assertThat(admin.patch("/admin/content/tracks/" + track, Map.of("status", "PUBLISHED")).status()).isEqualTo(200);

        // Edição do que já existe.
        assertThat(admin.put("/admin/content/tracks/" + track, Map.of("slug", "trilha-teste-admin", "title", "Trilha revista",
                "summary", "Resumo revisto", "difficulty", 2, "topic", "Contabilidade")).status()).isEqualTo(200);
        assertThat(admin.put("/admin/content/rooms/" + room, Map.of("title", "Sala revista", "description", "Nova descrição",
                "difficulty", 2, "estMinutes", 15, "prerequisites", List.of(room))).code()).isEqualTo("PREREQUISITE_CYCLE");
        assertThat(admin.put("/admin/content/rooms/" + room, Map.of("title", "Sala revista", "description", "Nova descrição",
                "difficulty", 2, "estMinutes", 15, "objectives", List.of("Lançar a venda"))).status()).isEqualTo(200);
        String taskId = task.body().path("id").asText();
        assertThat(admin.put("/admin/content/tasks/" + taskId, Map.of("exerciseVersionId", version, "title", "Tarefa revista", "points", 25)).status()).isEqualTo(200);
        JsonNode edited = null;
        for (JsonNode t : admin.get("/admin/content/tracks").body()) if (t.path("id").asText().equals(track)) edited = t;
        assertThat(edited.path("title").asText()).isEqualTo("Trilha revista");
        JsonNode r = edited.path("modules").get(0).path("rooms").get(0);
        assertThat(r.path("title").asText()).isEqualTo("Sala revista");
        assertThat(r.path("objectives").get(0).asText()).isEqualTo("Lançar a venda");
        assertThat(r.path("tasks").get(0).path("points").asInt()).isEqualTo(25);
        assertThat(student("kiala.afonso@exemplo.ao").put("/admin/content/tasks/" + taskId, Map.of("exerciseVersionId", version,
                "title", "x", "points", 1)).status()).isEqualTo(403);
        assertThat(admin.post("/admin/content/tracks", Map.of("slug", "trilha-teste-admin", "title", "x", "summary", "x",
                "difficulty", 1, "topic", "x")).code()).isEqualTo("SLUG_TAKEN");
    }

    @Test
    void bulkTransitionRespectsEditorialRules() {
        Api reviewer = privileged("docente.p@exemplo.ao");
        JsonNode queue = reviewer.get("/authoring/review-queue").body();
        List<String> ids = new java.util.ArrayList<>();
        queue.forEach(q -> { if (q.path("status").asText().equals("IN_REVIEW")) ids.add(q.path("latestVersionId").asText()); });
        if (ids.isEmpty()) return;
        var r = reviewer.post("/authoring/versions/bulk-transition", Map.of("versionIds", ids, "to", "APPROVED", "referencesChecked", false)).body();
        assertThat(r.path("ok").asInt()).isZero();
        assertThat(r.path("failed").get(0).path("code").asText()).isEqualTo("REFERENCES_NOT_CHECKED");
    }
}
