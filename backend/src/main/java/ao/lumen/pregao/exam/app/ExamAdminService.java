package ao.lumen.pregao.exam.app;

import ao.lumen.pregao.common.audit.AuditService;
import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.exam.domain.Exam;
import ao.lumen.pregao.exam.infra.ExamRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Exames (administração): o banco de perguntas é escolhido por tema entre exercícios A/B publicados. */
@Service
public class ExamAdminService {
    private final ExamRepository exams;
    private final AuditService audit;
    private final ObjectMapper json;

    public ExamAdminService(ExamRepository exams, AuditService audit, ObjectMapper json) {
        this.exams = exams;
        this.audit = audit;
        this.json = json;
    }

    public record ExamAdmin(UUID id, String slug, String title, int durationMin, String topic, int count,
                            BigDecimal passMark, String status) {}

    public record ExamInput(
            @NotBlank @Pattern(regexp = "[a-z0-9-]{3,80}", message = "Usa minúsculas, números e -.") String slug,
            @NotBlank @Size(max = 200) String title,
            UUID moduleId,
            @NotNull @Min(5) @Max(240) Integer durationMin,
            @NotBlank @Size(max = 120) String topic,
            @NotNull @Min(1) @Max(60) Integer count,
            @NotNull @DecimalMin("0") @DecimalMax("20") BigDecimal passMark,
            @Pattern(regexp = "DRAFT|PUBLISHED") String status) {}

    @Transactional(readOnly = true)
    public List<ExamAdmin> list() {
        return exams.findAll().stream().sorted(Comparator.comparing(Exam::getTitle))
                .map(e -> new ExamAdmin(e.getId(), e.getSlug(), e.getTitle(), e.getDurationMin(),
                        e.getQuestionPoolRule().path("topic").asText(), e.getQuestionPoolRule().path("count").asInt(),
                        e.getPassMark(), e.getStatus())).toList();
    }

    @Transactional
    public UUID create(UUID actor, ExamInput in) {
        if (exams.findAll().stream().anyMatch(e -> e.getSlug().equals(in.slug()))) {
            throw DomainException.conflict("SLUG_TAKEN", "Já existe um exame com este identificador.");
        }
        var rule = json.createObjectNode().put("topic", in.topic()).put("count", in.count());
        rule.putArray("types").add("A").add("B");
        Exam e = exams.save(new Exam(in.slug(), in.title(), in.moduleId(), in.durationMin(), rule, in.passMark(),
                in.status() == null ? "DRAFT" : in.status()));
        audit.record(actor, "EXAM_CREATED", "Exam", e.getId(), null, Map.of("slug", in.slug()));
        return e.getId();
    }

    @Transactional
    public void setStatus(UUID actor, UUID id, String status) {
        if (!status.equals("DRAFT") && !status.equals("PUBLISHED")) throw DomainException.badRequest("STATUS_INVALID", "Estado inválido.");
        Exam e = exams.findById(id).orElseThrow(() -> DomainException.notFound("EXAM_NOT_FOUND", "O exame não existe."));
        String before = e.getStatus();
        e.setStatus(status);
        audit.record(actor, "EXAM_STATUS", "Exam", id, Map.of("status", before), Map.of("status", status));
    }
}
