package ao.lumen.pregao.identity.infra;

import ao.lumen.pregao.identity.domain.MfaRecoveryCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCode, UUID> {
    Optional<MfaRecoveryCode> findByUserIdAndCodeHashAndUsedAtIsNull(UUID userId, String codeHash);

    long countByUserIdAndUsedAtIsNull(UUID userId);

    @Modifying
    @Query("delete from MfaRecoveryCode c where c.userId = :userId")
    void deleteAllForUser(@Param("userId") UUID userId);
}
