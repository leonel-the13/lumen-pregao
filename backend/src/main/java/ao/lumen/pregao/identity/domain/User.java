package ao.lumen.pregao.identity.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
public class User extends BaseEntity {
    private String email;
    @Column(name = "password_hash")
    private String passwordHash;
    private String name;
    @Enumerated(EnumType.STRING)
    private UserStatus status;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;
    private Integer year;
    private String pseudonym;
    @Enumerated(EnumType.STRING)
    @Column(name = "ranking_visibility")
    private RankingVisibility rankingVisibility = RankingVisibility.NAME;
    @Enumerated(EnumType.STRING)
    private Theme theme = Theme.DARK;
    @Column(name = "portfolio_public")
    private boolean portfolioPublic;
    @Column(name = "portfolio_handle")
    private String portfolioHandle;
    @Column(name = "mfa_secret_enc")
    private String mfaSecretEnc;
    @Column(name = "mfa_enabled")
    private boolean mfaEnabled;
    @Column(name = "consent_at")
    private Instant consentAt;
    @Column(name = "last_login_at")
    private Instant lastLoginAt;
    @Column(name = "deletion_requested_at")
    private Instant deletionRequestedAt;
    @Column(name = "deleted_at")
    private Instant deletedAt;
    @Column(name = "email_opt_in")
    private boolean emailOptIn = true;
    @Column(name = "last_seen_at")
    private Instant lastSeenAt;
    @Column(name = "verify_reminders")
    private int verifyReminders;
    @Column(name = "last_verify_reminder_at")
    private Instant lastVerifyReminderAt;
    @Column(name = "last_inactive_reminder_at")
    private Instant lastInactiveReminderAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    protected User() {}

    public User(String email, String passwordHash, String name, Course course, Integer year,
                String portfolioHandle, Instant consentAt) {
        this.email = email.toLowerCase(Locale.ROOT);
        this.passwordHash = passwordHash;
        this.name = name;
        this.course = course;
        this.year = year;
        this.portfolioHandle = portfolioHandle;
        this.consentAt = consentAt;
        this.status = UserStatus.PENDING_VERIFICATION;
    }

    public Set<String> roleCodes() {
        return roles.stream().map(Role::getCode).collect(Collectors.toUnmodifiableSet());
    }

    public void activate() { this.status = UserStatus.ACTIVE; }
    public void setStatus(UserStatus status) { this.status = status; }
    public void requestDeletion(Instant at) {
        this.status = UserStatus.DELETION_REQUESTED;
        this.deletionRequestedAt = at;
    }
    /** Remove os dados pessoais; mantém o id para a integridade dos registos imutáveis (pontos, auditoria). */
    public void anonymize(String unusableHash, Instant at) {
        String tag = getId().toString().substring(0, 8);
        this.email = "eliminado-" + tag + "@eliminado.invalid";
        this.name = "Conta eliminada";
        this.pseudonym = null;
        this.rankingVisibility = RankingVisibility.HIDDEN;
        this.portfolioPublic = false;
        this.portfolioHandle = "eliminado-" + tag;
        this.passwordHash = unusableHash;
        this.mfaSecretEnc = null;
        this.mfaEnabled = false;
        this.course = null;
        this.year = null;
        this.status = UserStatus.DELETED;
        this.emailOptIn = false;
        this.deletedAt = at;
    }

    public void disableMfa() {
        this.mfaSecretEnc = null;
        this.mfaEnabled = false;
    }

    public void enableMfa(String secretEnc) {
        this.mfaSecretEnc = secretEnc;
        this.mfaEnabled = true;
    }
    public void loggedIn(Instant at) {
        this.lastLoginAt = at;
        this.lastSeenAt = at;
    }
    public void setEmailOptIn(boolean emailOptIn) { this.emailOptIn = emailOptIn; }
    public void verificationReminded(Instant at) {
        this.verifyReminders++;
        this.lastVerifyReminderAt = at;
    }
    public void inactivityReminded(Instant at) { this.lastInactiveReminderAt = at; }
    public void setPseudonym(String pseudonym) { this.pseudonym = pseudonym; }
    public void setRankingVisibility(RankingVisibility v) { this.rankingVisibility = v; }
    public void setTheme(Theme theme) { this.theme = theme; }
    public void setPortfolioPublic(boolean portfolioPublic) { this.portfolioPublic = portfolioPublic; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Set<Role> getRoles() { return roles; }

    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getName() { return name; }
    public UserStatus getStatus() { return status; }
    public Course getCourse() { return course; }
    public Integer getYear() { return year; }
    public String getPseudonym() { return pseudonym; }
    public RankingVisibility getRankingVisibility() { return rankingVisibility; }
    public Theme getTheme() { return theme; }
    public boolean isPortfolioPublic() { return portfolioPublic; }
    public String getPortfolioHandle() { return portfolioHandle; }
    public String getMfaSecretEnc() { return mfaSecretEnc; }
    public boolean isMfaEnabled() { return mfaEnabled; }
    public Instant getConsentAt() { return consentAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Instant getDeletionRequestedAt() { return deletionRequestedAt; }
    public boolean isEmailOptIn() { return emailOptIn; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public int getVerifyReminders() { return verifyReminders; }
    public Instant getLastVerifyReminderAt() { return lastVerifyReminderAt; }
    public Instant getLastInactiveReminderAt() { return lastInactiveReminderAt; }

    /** Última actividade conhecida: visita, login ou, sem nenhum dos dois, a criação da conta. */
    public Instant lastActivity() {
        Instant a = lastSeenAt != null ? lastSeenAt : lastLoginAt;
        if (lastLoginAt != null && a != null && lastLoginAt.isAfter(a)) a = lastLoginAt;
        return a != null ? a : getCreatedAt();
    }
}
