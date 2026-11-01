package ao.lumen.pregao.authoring.infra;

import ao.lumen.pregao.authoring.domain.ExerciseReference;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExerciseReferenceRepository extends JpaRepository<ExerciseReference, UUID> {
    @EntityGraph(attributePaths = "legalSource")
    @Query("select r from ExerciseReference r where r.exerciseVersion.id in :ids")
    List<ExerciseReference> findByVersionIds(@Param("ids") Collection<UUID> ids);

    @Modifying
    @Query("delete from ExerciseReference r where r.exerciseVersion.id = :versionId")
    void deleteByVersionId(@Param("versionId") UUID versionId);
}
