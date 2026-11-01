package ao.lumen.pregao.identity.infra;

import ao.lumen.pregao.identity.domain.PasswordToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordTokenRepository extends JpaRepository<PasswordToken, UUID> {
    @EntityGraph(attributePaths = {"user", "user.roles"})
    Optional<PasswordToken> findByTokenHash(String tokenHash);

    /** Um novo link invalida os anteriores do mesmo utilizador. */
    @Modifying
    @Query("update PasswordToken t set t.usedAt = :now where t.user.id = :userId and t.usedAt is null")
    int invalidateAll(@Param("userId") UUID userId, @Param("now") Instant now);
}
