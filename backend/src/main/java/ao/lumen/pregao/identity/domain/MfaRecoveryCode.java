package ao.lumen.pregao.identity.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Código de recuperação do 2.º factor (uso único; só o hash SHA-256 é guardado). */
@Entity
@Table(name = "mfa_recovery_codes")
public class MfaRecoveryCode extends BaseEntity {
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "code_hash")
    private String codeHash;
    @Column(name = "used_at")
    private Instant usedAt;

    protected MfaRecoveryCode() {}

    public MfaRecoveryCode(UUID userId, String codeHash) {
        this.userId = userId;
        this.codeHash = codeHash;
    }

    public void use(Instant at) { this.usedAt = at; }
    public Instant getUsedAt() { return usedAt; }
}
