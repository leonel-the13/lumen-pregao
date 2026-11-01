package ao.lumen.pregao.identity.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Token de definição de palavra-passe (convite ou «Esqueci-me»). Guarda só o hash SHA-256. */
@Entity
@Table(name = "password_tokens")
public class PasswordToken extends BaseEntity {
    public static final String INVITE = "INVITE";
    public static final String RESET = "RESET";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
    private String purpose;
    @Column(name = "token_hash")
    private String tokenHash;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Column(name = "used_at")
    private Instant usedAt;

    protected PasswordToken() {}

    public PasswordToken(User user, String purpose, String tokenHash, Instant expiresAt) {
        this.user = user;
        this.purpose = purpose;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean usable(Instant now) { return usedAt == null && now.isBefore(expiresAt); }
    public void use(Instant now) { this.usedAt = now; }
    public User getUser() { return user; }
    public String getPurpose() { return purpose; }
    public Instant getUsedAt() { return usedAt; }
}
