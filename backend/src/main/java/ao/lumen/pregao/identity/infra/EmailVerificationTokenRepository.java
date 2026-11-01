package ao.lumen.pregao.identity.infra;

import ao.lumen.pregao.identity.domain.EmailVerificationToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    @EntityGraph(attributePaths = {"user", "user.roles"})
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
}
