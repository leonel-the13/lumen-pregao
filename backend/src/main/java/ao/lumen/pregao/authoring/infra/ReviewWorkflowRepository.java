package ao.lumen.pregao.authoring.infra;

import ao.lumen.pregao.authoring.domain.EditorialStatus;
import ao.lumen.pregao.authoring.domain.ReviewWorkflow;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewWorkflowRepository extends JpaRepository<ReviewWorkflow, UUID> {
    @Query("select w from ReviewWorkflow w where w.exerciseVersion.id = :versionId")
    Optional<ReviewWorkflow> findByVersionId(@Param("versionId") UUID versionId);

    @Query("select w from ReviewWorkflow w where w.exerciseVersion.id in :ids")
    List<ReviewWorkflow> findByVersionIds(@Param("ids") Collection<UUID> ids);

    @EntityGraph(attributePaths = {"exerciseVersion", "exerciseVersion.exercise"})
    List<ReviewWorkflow> findByStatusInOrderBySubmittedAtAsc(Collection<EditorialStatus> statuses);

    @EntityGraph(attributePaths = {"exerciseVersion", "exerciseVersion.exercise"})
    @Query("""
            select w from ReviewWorkflow w
            where w.exerciseVersion.id in (
                select r.exerciseVersion.id from ExerciseReference r where r.legalSource.id = :sourceId)
            """)
    List<ReviewWorkflow> findLinkedToSource(@Param("sourceId") UUID sourceId);

    @Query("select count(w) from ReviewWorkflow w where w.legalAlertAt is not null and w.status <> 'ARCHIVED'")
    long countLegalAlerts();
}
