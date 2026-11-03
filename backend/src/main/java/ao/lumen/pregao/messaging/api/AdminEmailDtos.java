package ao.lumen.pregao.messaging.api;

import ao.lumen.pregao.messaging.domain.Audience;
import ao.lumen.pregao.messaging.domain.MessageContent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class AdminEmailDtos {
    private AdminEmailDtos() {}

    /** Texto simples com variáveis ({nome}, {curso}, …). Cada linha do corpo é um parágrafo. */
    public record ContentIn(
            @NotBlank(message = "Escreve o assunto.") @Size(max = 120, message = "O assunto tem no máximo 120 caracteres.") String subject,
            @Size(max = 120, message = "O título tem no máximo 120 caracteres.") String title,
            @NotBlank(message = "Escreve a mensagem.") @Size(max = 5000, message = "A mensagem tem no máximo 5000 caracteres.") String body,
            @Size(max = 40, message = "O texto do botão tem no máximo 40 caracteres.") String ctaLabel,
            @Size(max = 200, message = "A página do botão tem no máximo 200 caracteres.") String ctaPath) {

        public MessageContent toContent() {
            return new MessageContent(subject, title, body, ctaLabel, ctaPath);
        }
    }

    public record TemplateIn(
            @NotBlank(message = "Dá um nome ao modelo.") @Size(max = 80, message = "O nome tem no máximo 80 caracteres.") String name,
            @Valid @NotNull ContentIn content) {}

    public record TemplateView(UUID id, String name, String subject, String title, String body, String ctaLabel, String ctaPath,
                               boolean builtIn, Instant updatedAt) {}

    public record AudienceIn(
            @NotNull(message = "Escolhe os destinatários.") Audience audience,
            UUID courseId,
            @Min(1) @Max(5) Integer year,
            @Min(value = 1, message = "Escolhe entre 1 e 365 dias.") @Max(value = 365, message = "Escolhe entre 1 e 365 dias.") Integer inactiveDays,
            UUID userId) {}

    /** {@code optedOut}: pessoas do segmento com os emails desligados (não recebem). */
    public record AudienceCount(int recipients, int optedOut, String label) {}

    public record PreviewIn(@Valid @NotNull ContentIn content) {}

    public record PreviewView(String subject, String html, String text) {}

    public record CampaignIn(@Valid @NotNull AudienceIn audience, @Valid @NotNull ContentIn content, UUID templateId) {}

    public record CampaignRow(UUID id, String subject, String audience, String audienceLabel, String createdBy, Instant createdAt,
                              long total, long sent, long failed, long skipped, long pending) {}

    public record VariableView(String key, String label) {}

    public record RetryResult(int requeued) {}
}
