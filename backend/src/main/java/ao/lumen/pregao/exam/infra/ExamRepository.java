package ao.lumen.pregao.exam.infra;

import ao.lumen.pregao.exam.domain.Exam;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepository extends JpaRepository<Exam, UUID> {
    List<Exam> findByStatusOrderByTitleAsc(String status);
}
