package ao.lumen.pregao.identity.app;

import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.identity.domain.DisplayNames;
import ao.lumen.pregao.identity.domain.RankingVisibility;
import ao.lumen.pregao.identity.domain.User;
import ao.lumen.pregao.identity.domain.UserStatus;
import ao.lumen.pregao.identity.infra.UserRepository;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** API pública do módulo identity para os outros módulos (sem expor entidades). */
@Service
@Transactional(readOnly = true)
public class UserDirectory {
    private final UserRepository users;

    public UserDirectory(UserRepository users) {
        this.users = users;
    }

    public record UserSummary(UUID id, String name, String shortName, String initials, String courseCode,
                              String courseName, Integer year, String pseudonym, RankingVisibility visibility,
                              String portfolioHandle, boolean portfolioPublic, boolean active) {

        /** Nome público no ranking, segundo a preferência do utilizador. */
        public String publicName() {
            return switch (visibility) {
                case NAME -> shortName;
                case PSEUDONYM -> pseudonym != null ? pseudonym : shortName;
                case HIDDEN -> "Pseudónimo oculto";
            };
        }

        public String publicInitials() {
            return switch (visibility) {
                case NAME -> initials;
                case PSEUDONYM -> pseudonym != null ? DisplayNames.initials(pseudonym) : initials;
                case HIDDEN -> "—";
            };
        }
    }

    public Map<UUID, UserSummary> summaries(Collection<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return users.findByIdIn(ids).stream().map(UserDirectory::toSummary)
                .collect(Collectors.toMap(UserSummary::id, Function.identity()));
    }

    public UserSummary summary(UUID id) {
        return summaries(java.util.List.of(id)).values().stream().findFirst()
                .orElseThrow(() -> DomainException.notFound("USER_NOT_FOUND", "O utilizador não existe."));
    }

    public Optional<UserSummary> byEmail(String email) {
        return users.findByEmail(email.strip().toLowerCase(Locale.ROOT)).map(UserDirectory::toSummary);
    }

    public Optional<UserSummary> byPublicHandle(String handle) {
        return users.findByPortfolioHandle(handle).filter(User::isPortfolioPublic)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE).map(UserDirectory::toSummary);
    }

    static UserSummary toSummary(User u) {
        var c = u.getCourse();
        return new UserSummary(u.getId(), u.getName(), DisplayNames.shortName(u.getName()),
                DisplayNames.initials(u.getName()), c == null ? null : c.getCode(), c == null ? null : c.getName(),
                u.getYear(), u.getPseudonym(), u.getRankingVisibility(), u.getPortfolioHandle(),
                u.isPortfolioPublic(), u.getStatus() == UserStatus.ACTIVE);
    }
}
