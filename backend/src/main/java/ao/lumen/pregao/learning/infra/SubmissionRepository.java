package ao.lumen.pregao.learning.infra;

import ao.lumen.pregao.learning.domain.Submission;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    /** Inserção idempotente por (attemptId, idempotencyKey) — CON-02. */
    @Modifying
    @Query(value = """
            insert into submissions (id, attempt_id, idempotency_key, submitted_by, payload, received_at, status,
                                     correct, points, error_code, retries, created_at, updated_at)
            values (:id, :attemptId, :key, :by, cast(:payload as jsonb), :receivedAt, :status,
                    null, null, :errorCode, 0, :receivedAt, :receivedAt)
            on conflict (attempt_id, idempotency_key) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("attemptId") UUID attemptId, @Param("key") String key,
                       @Param("by") UUID by, @Param("payload") String payload, @Param("receivedAt") Instant receivedAt,
                       @Param("status") String status, @Param("errorCode") String errorCode);

    Optional<Submission> findByAttemptIdAndIdempotencyKey(UUID attemptId, String key);

    /** Reclama a submissão para correcção (só um worker ganha). */
    @Modifying
    @Query(value = """
            update submissions set status = 'PROCESSING', processing_started_at = :now, updated_at = :now
            where id = :id and status = 'QUEUED'
            """, nativeQuery = true)
    int claim(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Query(value = """
            update submissions set status = 'FAILED', error_code = :code, retries = retries + 1, updated_at = :now
            where id = :id and status = 'PROCESSING'
            """, nativeQuery = true)
    int markFailed(@Param("id") UUID id, @Param("code") String code, @Param("now") Instant now);

    /** Devolve à fila as submissões presas (worker morto) ou falhadas com tentativas restantes — CON-04. */
    @Modifying
    @Query(value = """
            update submissions set status = 'QUEUED', updated_at = :now
            where (status = 'PROCESSING' and processing_started_at < :staleBefore)
               or (status = 'FAILED' and retries < :maxRetries and updated_at < :retryBefore)
            """, nativeQuery = true)
    int requeueStale(@Param("now") Instant now, @Param("staleBefore") Instant staleBefore,
                     @Param("retryBefore") Instant retryBefore, @Param("maxRetries") int maxRetries);

    @Query("select s.id from Submission s where s.status = 'QUEUED' and s.receivedAt < :before order by s.receivedAt")
    List<UUID> findQueuedBefore(@Param("before") Instant before, Pageable page);

    @Query("select count(s) from Submission s where s.status in ('QUEUED','PROCESSING') and s.receivedAt < :at")
    long queuePositionBefore(@Param("at") Instant at);

    @Query("select count(s) from Submission s where s.status in ('QUEUED','PROCESSING')")
    long pendingCount();

    @Query("""
            select count(s) from Submission s
            where s.attemptId = :attemptId and s.id <> :except and s.status in ('QUEUED','PROCESSING')
            """)
    long pendingForAttemptExcept(@Param("attemptId") UUID attemptId, @Param("except") UUID except);

    List<Submission> findByAttemptIdOrderByReceivedAtDesc(UUID attemptId, Pageable page);

    List<Submission> findByAttemptIdInOrderByReceivedAtDesc(Collection<UUID> attemptIds);

    List<Submission> findBySubmittedByOrderByReceivedAtDesc(UUID userId, Pageable page);

    @Query("""
            select count(s) from Submission s, Attempt a
            where s.attemptId = a.id and a.userId = :userId and a.exerciseType = :type and s.correct = true
            """)
    long countCorrectByType(@Param("userId") UUID userId, @Param("type") String type);
}
