package ao.lumen.pregao.exam.infra;

import ao.lumen.pregao.exam.domain.ExamSession;
import ao.lumen.pregao.exam.domain.ExamSessionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamSessionRepository extends JpaRepository<ExamSession, UUID> {
    Optional<ExamSession> findByExamIdAndUserIdAndStatus(UUID examId, UUID userId, ExamSessionStatus status);

    List<ExamSession> findByUserIdOrderByStartedAtDesc(UUID userId);

    @Query("select s.id from ExamSession s where s.status = 'IN_PROGRESS' and s.endsAt < :before")
    List<UUID> findExpired(@Param("before") Instant before);

    @Query("select s.id from ExamSession s where s.status in ('SUBMITTED','EXPIRED_AUTO_SUBMITTED')")
    List<UUID> findUngraded();

    @Query("select count(s) from ExamSession s where s.examId = :examId and s.userId = :userId and s.status = 'GRADED'")
    long countGraded(@Param("examId") UUID examId, @Param("userId") UUID userId);
}
