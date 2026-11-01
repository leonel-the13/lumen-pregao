package ao.lumen.pregao.feedback;

import ao.lumen.pregao.common.security.SessionUser;
import ao.lumen.pregao.common.web.CursorPage;
import ao.lumen.pregao.feedback.FeedbackService.FeedbackView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class FeedbackController {
    private final FeedbackService service;

    public FeedbackController(FeedbackService service) {
        this.service = service;
    }

    public record FeedbackInput(@NotBlank String category, @Min(1) @Max(5) Integer rating,
                                @NotBlank(message = "Escreve a tua mensagem.") @Size(min = 5, max = 2000, message = "A mensagem tem de ter entre 5 e 2000 caracteres.") String message,
                                @Size(max = 300) String page) {}

    public record StatusInput(@NotBlank String status) {}

    @PostMapping("/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> submit(@AuthenticationPrincipal SessionUser me, @Valid @RequestBody FeedbackInput in) {
        return Map.of("id", service.submit(me.id(), in.category(), in.rating(), in.message(), in.page()));
    }

    @GetMapping("/admin/feedback")
    @PreAuthorize("hasRole('ADMIN')")
    public CursorPage<FeedbackView> list(@RequestParam(required = false) String status, @RequestParam(required = false) String cursor,
                                         @RequestParam(required = false) Integer limit) {
        return service.page(status, cursor, limit);
    }

    @PatchMapping("/admin/feedback/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void status(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id, @Valid @RequestBody StatusInput in) {
        service.setStatus(me.id(), id, in.status());
    }
}
