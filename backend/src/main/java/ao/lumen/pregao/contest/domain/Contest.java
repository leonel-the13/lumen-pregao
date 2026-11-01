package ao.lumen.pregao.contest.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import ao.lumen.pregao.common.error.DomainException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "contests")
public class Contest extends BaseEntity {
    private String slug;
    private String title;
    private String description;
    private String mode;
    @Column(name = "min_team")
    private int minTeam;
    @Column(name = "max_team")
    private int maxTeam;
    @Column(name = "registration_opens_at")
    private Instant registrationOpensAt;
    @Column(name = "registration_closes_at")
    private Instant registrationClosesAt;
    @Column(name = "starts_at")
    private Instant startsAt;
    @Column(name = "freeze_at")
    private Instant freezeAt;
    @Column(name = "ends_at")
    private Instant endsAt;
    @Column(name = "lock_minutes")
    private int lockMinutes;
    @Column(name = "wrong_penalty_minutes")
    private int wrongPenaltyMinutes;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "final_phase")
    private JsonNode finalPhase;
    @Enumerated(EnumType.STRING)
    private ContestState state = ContestState.DRAFT;
    @Column(name = "results_published_at")
    private Instant resultsPublishedAt;
    @Column(name = "created_by")
    private UUID createdBy;
    @Version
    private Long version;

    protected Contest() {}

    public Contest(String slug, UUID createdBy) {
        this.slug = slug;
        this.createdBy = createdBy;
    }

    public void define(String title, String description, String mode, int minTeam, int maxTeam,
                       Instant registrationOpensAt, Instant registrationClosesAt, Instant startsAt, Instant freezeAt,
                       Instant endsAt, int lockMinutes, int wrongPenaltyMinutes, JsonNode finalPhase) {
        if (state != ContestState.DRAFT) {
            throw DomainException.conflict("CONTEST_NOT_DRAFT", "Só é possível editar contests em rascunho.");
        }
        validateDates(registrationOpensAt, registrationClosesAt, startsAt, freezeAt, endsAt);
        if ("INDIVIDUAL".equals(mode)) { minTeam = 1; maxTeam = 1; }
        if (minTeam < 1 || maxTeam < minTeam) {
            throw DomainException.rule("TEAM_SIZE_INVALID", "O tamanho das equipas é inválido.");
        }
        this.title = title;
        this.description = description;
        this.mode = mode;
        this.minTeam = minTeam;
        this.maxTeam = maxTeam;
        this.registrationOpensAt = registrationOpensAt;
        this.registrationClosesAt = registrationClosesAt;
        this.startsAt = startsAt;
        this.freezeAt = freezeAt;
        this.endsAt = endsAt;
        this.lockMinutes = lockMinutes;
        this.wrongPenaltyMinutes = wrongPenaltyMinutes;
        this.finalPhase = finalPhase;
    }

    /** Datas monotónicas: abertura ≤ fecho ≤ início < congelamento < fim. */
    public static void validateDates(Instant opens, Instant closes, Instant starts, Instant freeze, Instant ends) {
        if (opens == null || closes == null || starts == null || freeze == null || ends == null
                || opens.isAfter(closes) || closes.isAfter(starts) || !starts.isBefore(freeze) || !freeze.isBefore(ends)) {
            throw DomainException.rule("CONTEST_DATES_INVALID",
                    "As datas têm de seguir a ordem: abertura ≤ fecho das inscrições ≤ início < congelamento < fim.");
        }
    }

    public void moveTo(ContestState next) {
        this.state = next;
        if (next == ContestState.RESULTS_PUBLISHED) resultsPublishedAt = Instant.now();
    }

    public boolean isTeamMode() { return "TEAM".equals(mode); }
    public Instant lockAt() { return startsAt.minusSeconds(lockMinutes * 60L); }

    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getMode() { return mode; }
    public int getMinTeam() { return minTeam; }
    public int getMaxTeam() { return maxTeam; }
    public Instant getRegistrationOpensAt() { return registrationOpensAt; }
    public Instant getRegistrationClosesAt() { return registrationClosesAt; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getFreezeAt() { return freezeAt; }
    public Instant getEndsAt() { return endsAt; }
    public int getLockMinutes() { return lockMinutes; }
    public int getWrongPenaltyMinutes() { return wrongPenaltyMinutes; }
    public JsonNode getFinalPhase() { return finalPhase; }
    public ContestState getState() { return state; }
    public Instant getResultsPublishedAt() { return resultsPublishedAt; }
    public UUID getCreatedBy() { return createdBy; }
}
