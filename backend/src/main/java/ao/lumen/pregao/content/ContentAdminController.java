package ao.lumen.pregao.content;

import ao.lumen.pregao.common.security.SessionUser;
import ao.lumen.pregao.exam.app.ExamAdminService;
import ao.lumen.pregao.exam.app.ExamAdminService.ExamAdmin;
import ao.lumen.pregao.exam.app.ExamAdminService.ExamInput;
import ao.lumen.pregao.learning.api.ContentDtos.ModuleInput;
import ao.lumen.pregao.learning.api.ContentDtos.RoomInput;
import ao.lumen.pregao.learning.api.ContentDtos.StatusInput;
import ao.lumen.pregao.learning.api.ContentDtos.TaskInput;
import ao.lumen.pregao.learning.api.ContentDtos.TrackAdmin;
import ao.lumen.pregao.learning.api.ContentDtos.TrackInput;
import ao.lumen.pregao.learning.app.ContentAdminService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Gestão de conteúdo (Administração → Conteúdos). */
@RestController
@RequestMapping("/api/v1/admin/content")
@PreAuthorize("hasRole('ADMIN')")
public class ContentAdminController {
    private final ContentAdminService content;
    private final ExamAdminService exams;
    private final ContentPackImporter importer;

    public ContentAdminController(ContentAdminService content, ExamAdminService exams, ContentPackImporter importer) {
        this.content = content;
        this.exams = exams;
        this.importer = importer;
    }

    @GetMapping("/tracks")
    public List<TrackAdmin> tree() {
        return content.tree();
    }

    @PostMapping("/tracks")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> createTrack(@AuthenticationPrincipal SessionUser me, @Valid @RequestBody TrackInput in) {
        return Map.of("id", content.createTrack(me.id(), in));
    }

    @PatchMapping("/tracks/{id}")
    public void trackStatus(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id, @Valid @RequestBody StatusInput in) {
        content.setTrackStatus(me.id(), id, in.status());
    }

    @PostMapping("/tracks/{id}/modules")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> addModule(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id, @Valid @RequestBody ModuleInput in) {
        return Map.of("id", content.addModule(me.id(), id, in));
    }

    @PostMapping("/modules/{id}/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> addRoom(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id, @Valid @RequestBody RoomInput in) {
        return Map.of("id", content.addRoom(me.id(), id, in));
    }

    @PatchMapping("/rooms/{id}")
    public void roomStatus(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id, @Valid @RequestBody StatusInput in) {
        content.setRoomStatus(me.id(), id, in.status());
    }

    @PostMapping("/rooms/{id}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> addTask(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id, @Valid @RequestBody TaskInput in) {
        return Map.of("id", content.addTask(me.id(), id, in));
    }

    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTask(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        content.removeTask(me.id(), id);
    }

    @GetMapping("/exams")
    public List<ExamAdmin> exams() {
        return exams.list();
    }

    @PostMapping("/exams")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> createExam(@AuthenticationPrincipal SessionUser me, @Valid @RequestBody ExamInput in) {
        return Map.of("id", exams.create(me.id(), in));
    }

    public record ExamStatus(@jakarta.validation.constraints.NotBlank String status) {}

    @PatchMapping("/exams/{id}")
    public void examStatus(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id, @Valid @RequestBody ExamStatus in) {
        exams.setStatus(me.id(), id, in.status());
    }

    /** Importa o pacote de conteúdo incluído na aplicação. Os exercícios ficam em revisão. */
    @PostMapping("/import")
    public ContentPackImporter.ImportResult importPack(@AuthenticationPrincipal SessionUser me) {
        return importer.importPack(me.id());
    }
}
