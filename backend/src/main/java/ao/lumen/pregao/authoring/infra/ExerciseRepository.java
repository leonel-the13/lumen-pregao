package ao.lumen.pregao.authoring.infra;

import ao.lumen.pregao.authoring.domain.Exercise;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {
    Optional<Exercise> findByCode(String code);

    Optional<Exercise> findByExternalKey(String externalKey);

    List<Exercise> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);

    List<Exercise> findAllByOrderByCreatedAtDesc();

    @Query(value = "select nextval('exercise_code_seq')", nativeQuery = true)
    long nextCode();
}
