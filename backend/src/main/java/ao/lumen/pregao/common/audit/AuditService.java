package ao.lumen.pregao.common.audit;

import ao.lumen.pregao.common.web.ClientIp;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Regista operações sensíveis. Participa na transacção do chamador (falha a operação se a auditoria falhar). */
@Service
public class AuditService {
    private final AuditLogRepository repo;
    private final ObjectMapper json;
    private final Clock clock;

    public AuditService(AuditLogRepository repo, ObjectMapper json, Clock clock) {
        this.repo = repo;
        this.json = json;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID actorId, String action, String entityType, Object entityId, Object before, Object after) {
        repo.save(new AuditLog(actorId, action, entityType, entityId == null ? null : entityId.toString(),
                before == null ? null : json.valueToTree(before),
                after == null ? null : json.valueToTree(after),
                ClientIp.current(), clock.instant()));
    }

    /** Para eventos sem transacção de negócio (ex.: tentativa de login falhada). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordStandalone(UUID actorId, String action, String entityType, Object entityId, Object after) {
        record(actorId, action, entityType, entityId, null, after);
    }
}
