package ao.lumen.pregao.identity.infra;

import ao.lumen.pregao.identity.domain.Course;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    List<Course> findAllByOrderByNameAsc();

    Optional<Course> findByCode(String code);
}
