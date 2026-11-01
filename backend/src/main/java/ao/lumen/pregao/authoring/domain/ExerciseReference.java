package ao.lumen.pregao.authoring.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "exercise_references")
public class ExerciseReference extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_version_id")
    private ExerciseVersion exerciseVersion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_source_id")
    private LegalSource legalSource;
    private String articles;
    @Column(name = "consulted_at")
    private LocalDate consultedAt;

    protected ExerciseReference() {}

    public ExerciseReference(ExerciseVersion v, LegalSource s, String articles, LocalDate consultedAt) {
        this.exerciseVersion = v;
        this.legalSource = s;
        this.articles = articles;
        this.consultedAt = consultedAt;
    }

    public ExerciseVersion getExerciseVersion() { return exerciseVersion; }
    public LegalSource getLegalSource() { return legalSource; }
    public String getArticles() { return articles; }
    public LocalDate getConsultedAt() { return consultedAt; }
}
