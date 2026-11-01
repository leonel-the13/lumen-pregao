package ao.lumen.pregao.common.audit;

import ao.lumen.pregao.common.domain.UuidV7;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Registo de auditoria: só inserção (há trigger na base de dados que impede UPDATE/DELETE). */
@Entity
@Immutable
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    private UUID id;
    @Column(name = "actor_id")
    private UUID actorId;
    private String action;
    @Column(name = "entity_type")
    private String entityType;
    @Column(name = "entity_id")
    private String entityId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_data")
    private JsonNode before;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_data")
    private JsonNode after;
    private String ip;
    private Instant at;

    protected AuditLog() {}

    public AuditLog(UUID actorId, String action, String entityType, String entityId,
                    JsonNode before, JsonNode after, String ip, Instant at) {
        this.id = UuidV7.next();
        this.actorId = actorId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.before = before;
        this.after = after;
        this.ip = ip;
        this.at = at;
    }

    public UUID getId() { return id; }
    public UUID getActorId() { return actorId; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public JsonNode getBefore() { return before; }
    public JsonNode getAfter() { return after; }
    public String getIp() { return ip; }
    public Instant getAt() { return at; }
}
