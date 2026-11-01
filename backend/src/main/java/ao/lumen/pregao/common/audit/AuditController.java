package ao.lumen.pregao.common.audit;

import ao.lumen.pregao.common.security.SessionUser;
import ao.lumen.pregao.common.web.CursorPage;
import ao.lumen.pregao.identity.app.UserDirectory;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {
    private final AuditLogRepository repo;
    private final UserDirectory directory;
    private final AuditService audit;

    public AuditController(AuditLogRepository repo, UserDirectory directory, AuditService audit) {
        this.repo = repo;
        this.directory = directory;
        this.audit = audit;
    }

    public record AuditRow(UUID id, Instant at, UUID actorId, String actorName, String action, String entityType,
                           String entityId, Object before, Object after, String ip) {}

    @GetMapping
    @Transactional(readOnly = true)
    public CursorPage<AuditRow> search(@RequestParam(required = false) UUID actor,
                                       @RequestParam(required = false) String entity,
                                       @RequestParam(required = false) Instant from,
                                       @RequestParam(required = false) Instant to,
                                       @RequestParam(required = false) UUID cursor,
                                       @RequestParam(required = false) Integer limit) {
        int lim = CursorPage.clamp(limit);
        List<AuditLog> rows = repo.search(actor, blank(entity), from, to, cursor, PageRequest.of(0, lim + 1));
        var names = directory.summaries(new HashSet<>(rows.stream().map(AuditLog::getActorId).filter(Objects::nonNull).toList()));
        return CursorPage.of(rows, lim, a -> new AuditRow(a.getId(), a.getAt(), a.getActorId(),
                a.getActorId() == null ? "Sistema" : names.containsKey(a.getActorId()) ? names.get(a.getActorId()).name() : "—",
                a.getAction(), a.getEntityType(), a.getEntityId(), a.getBefore(), a.getAfter(), a.getIp()),
                a -> a.getId().toString());
    }

    /** Exportação CSV (até 5000 linhas por pedido). A própria exportação fica auditada. */
    @GetMapping(value = "/export", produces = "text/csv")
    @Transactional
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal SessionUser me,
                                         @RequestParam(required = false) UUID actor,
                                         @RequestParam(required = false) String entity,
                                         @RequestParam(required = false) Instant from,
                                         @RequestParam(required = false) Instant to) {
        List<AuditLog> rows = repo.search(actor, blank(entity), from, to, null, PageRequest.of(0, 5000));
        StringBuilder sb = new StringBuilder("at,actor_id,action,entity_type,entity_id,ip\n");
        for (AuditLog a : rows) {
            sb.append(a.getAt()).append(',').append(csv(a.getActorId())).append(',').append(csv(a.getAction()))
                    .append(',').append(csv(a.getEntityType())).append(',').append(csv(a.getEntityId()))
                    .append(',').append(csv(a.getIp())).append('\n');
        }
        audit.record(me.id(), "AUDIT_EXPORTED", "AuditLog", null, null,
                java.util.Map.of("rows", rows.size()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"auditoria.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static String csv(Object o) {
        if (o == null) return "";
        String s = o.toString();
        if (s.startsWith("=") || s.startsWith("+") || s.startsWith("-") || s.startsWith("@")) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
