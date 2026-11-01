package ao.lumen.pregao.authoring.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Definição avaliável. Depois de sair de rascunho não muda: alterar cria uma nova versão. */
@Entity
@Table(name = "exercise_versions")
public class ExerciseVersion extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;
    private int number;
    private String type;
    private String title;
    private String statement;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode presentation;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "param_spec")
    private JsonNode paramSpec;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "solution_spec")
    private JsonNode solutionSpec;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode tolerance;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode hints;
    @Column(name = "feedback_policy")
    private String feedbackPolicy;
    private int difficulty;
    private int points;
    @Column(name = "max_attempts")
    private int maxAttempts;
    @Column(name = "pgc_version")
    private String pgcVersion;
    @Column(name = "valid_until")
    private LocalDate validUntil;
    @Column(name = "change_note")
    private String changeNote;

    protected ExerciseVersion() {}

    public ExerciseVersion(Exercise exercise, int number) {
        this.exercise = exercise;
        this.number = number;
    }

    public void define(String type, String title, String statement, JsonNode presentation, JsonNode paramSpec,
                       JsonNode solutionSpec, JsonNode tolerance, JsonNode hints, String feedbackPolicy,
                       int difficulty, int points, int maxAttempts, String pgcVersion, LocalDate validUntil,
                       String changeNote) {
        this.type = type;
        this.title = title;
        this.statement = statement;
        this.presentation = presentation;
        this.paramSpec = paramSpec;
        this.solutionSpec = solutionSpec;
        this.tolerance = tolerance;
        this.hints = hints;
        this.feedbackPolicy = feedbackPolicy;
        this.difficulty = difficulty;
        this.points = points;
        this.maxAttempts = maxAttempts;
        this.pgcVersion = pgcVersion;
        this.validUntil = validUntil;
        this.changeNote = changeNote;
    }

    public Exercise getExercise() { return exercise; }
    public int getNumber() { return number; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getStatement() { return statement; }
    public JsonNode getPresentation() { return presentation; }
    public JsonNode getParamSpec() { return paramSpec; }
    public JsonNode getSolutionSpec() { return solutionSpec; }
    public JsonNode getTolerance() { return tolerance; }
    public JsonNode getHints() { return hints; }
    public String getFeedbackPolicy() { return feedbackPolicy; }
    public int getDifficulty() { return difficulty; }
    public int getPoints() { return points; }
    public int getMaxAttempts() { return maxAttempts; }
    public String getPgcVersion() { return pgcVersion; }
    public LocalDate getValidUntil() { return validUntil; }
    public String getChangeNote() { return changeNote; }
}
