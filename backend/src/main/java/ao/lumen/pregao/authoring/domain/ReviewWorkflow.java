package ao.lumen.pregao.authoring.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import ao.lumen.pregao.common.error.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Estado editorial de uma versão: Rascunho → Revisão → Aprovação → Publicação. */
@Entity
@Table(name = "review_workflows")
public class ReviewWorkflow extends BaseEntity {
    private static final Map<EditorialStatus, Set<EditorialStatus>> ALLOWED = Map.of(
            EditorialStatus.DRAFT, Set.of(EditorialStatus.IN_REVIEW),
            EditorialStatus.IN_REVIEW, Set.of(EditorialStatus.APPROVED, EditorialStatus.DRAFT),
            EditorialStatus.APPROVED, Set.of(EditorialStatus.PUBLISHED, EditorialStatus.DRAFT),
            EditorialStatus.PUBLISHED, Set.of(EditorialStatus.ARCHIVED, EditorialStatus.NEEDS_REVIEW),
            EditorialStatus.NEEDS_REVIEW, Set.of(EditorialStatus.IN_REVIEW, EditorialStatus.ARCHIVED),
            EditorialStatus.ARCHIVED, Set.of());

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_version_id")
    private ExerciseVersion exerciseVersion;
    @Enumerated(EnumType.STRING)
    private EditorialStatus status = EditorialStatus.DRAFT;
    @Column(name = "reviewer_id")
    private UUID reviewerId;
    private String notes;
    @Column(name = "submitted_at")
    private Instant submittedAt;
    @Column(name = "approved_at")
    private Instant approvedAt;
    @Column(name = "published_at")
    private Instant publishedAt;
    @Column(name = "legal_alert_at")
    private Instant legalAlertAt;
    @Version
    private Long version;

    protected ReviewWorkflow() {}

    public ReviewWorkflow(ExerciseVersion exerciseVersion) {
        this.exerciseVersion = exerciseVersion;
    }

    public void moveTo(EditorialStatus to, UUID actor, String notes, Instant now) {
        if (!ALLOWED.get(status).contains(to)) {
            throw DomainException.conflict("INVALID_TRANSITION",
                    "Não é possível passar de " + status + " para " + to + ".");
        }
        switch (to) {
            case IN_REVIEW -> { submittedAt = now; legalAlertAt = null; }
            case APPROVED -> { approvedAt = now; reviewerId = actor; }
            case PUBLISHED -> { publishedAt = now; reviewerId = actor; }
            case DRAFT -> reviewerId = actor;
            default -> { }
        }
        if (notes != null) this.notes = notes;
        this.status = to;
    }

    /** Alerta legal: nova versão de uma norma ligada. */
    public void legalAlert(Instant now, boolean unpublish) {
        legalAlertAt = now;
        if (unpublish && (status == EditorialStatus.PUBLISHED)) status = EditorialStatus.NEEDS_REVIEW;
        if (status == EditorialStatus.APPROVED) status = EditorialStatus.IN_REVIEW;
    }

    public ExerciseVersion getExerciseVersion() { return exerciseVersion; }
    public EditorialStatus getStatus() { return status; }
    public UUID getReviewerId() { return reviewerId; }
    public String getNotes() { return notes; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getApprovedAt() { return approvedAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getLegalAlertAt() { return legalAlertAt; }
}
