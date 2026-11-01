package ao.lumen.pregao.authoring.infra;

import ao.lumen.pregao.authoring.domain.ExerciseVersion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExerciseVersionRepository extends JpaRepository<ExerciseVersion, UUID> {
    @EntityGraph(attributePaths = "exercise")
    @Query("select v from ExerciseVersion v where v.id = :id")
    Optional<ExerciseVersion> findWithExercise(@Param("id") UUID id);

    @EntityGraph(attributePaths = "exercise")
    List<ExerciseVersion> findByIdIn(Collection<UUID> ids);

    List<ExerciseVersion> findByExerciseIdOrderByNumberDesc(UUID exerciseId);

    Optional<ExerciseVersion> findByExerciseIdAndNumber(UUID exerciseId, int number);

    @EntityGraph(attributePaths = "exercise")
    @Query("""
            select v from ExerciseVersion v, ReviewWorkflow w
            where w.exerciseVersion = v and w.status = 'PUBLISHED'
              and v.type in :types
              and lower(v.exercise.topic) like lower(concat(cast(:topic as string), '%'))
            """)
    List<ExerciseVersion> publishedByTopic(@Param("types") Collection<String> types, @Param("topic") String topic);
}
