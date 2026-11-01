package ao.lumen.pregao.contest.domain;

import ao.lumen.pregao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "contest_problems")
public class ContestProblem extends BaseEntity {
    @Column(name = "contest_id")
    private UUID contestId;
    private String label;
    private String title;
    @Column(name = "company_name")
    private String companyName;
    @Column(name = "exercise_version_id")
    private UUID exerciseVersionId;
    private int points;
    private int position;

    protected ContestProblem() {}

    public ContestProblem(UUID contestId, String label, String title, String companyName, UUID exerciseVersionId,
                          int points, int position) {
        this.contestId = contestId;
        this.label = label;
        this.title = title;
        this.companyName = companyName;
        this.exerciseVersionId = exerciseVersionId;
        this.points = points;
        this.position = position;
    }

    public UUID getContestId() { return contestId; }
    public String getLabel() { return label; }
    public String getTitle() { return title; }
    public String getCompanyName() { return companyName; }
    public UUID getExerciseVersionId() { return exerciseVersionId; }
    public int getPoints() { return points; }
    public int getPosition() { return position; }
}
