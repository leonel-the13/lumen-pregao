package ao.lumen.pregao.identity.app;

import ao.lumen.pregao.common.audit.AuditService;
import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.common.privacy.PersonalDataContributor;
import ao.lumen.pregao.common.security.SessionRevocation;
import ao.lumen.pregao.common.text.OffensiveFilter;
import ao.lumen.pregao.identity.api.MeDtos.CourseRef;
import ao.lumen.pregao.identity.api.MeDtos.MeResponse;
import ao.lumen.pregao.identity.api.MeDtos.PatchMe;
import ao.lumen.pregao.identity.domain.DisplayNames;
import ao.lumen.pregao.identity.domain.RankingVisibility;
import ao.lumen.pregao.identity.domain.User;
import ao.lumen.pregao.identity.infra.UserRepository;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeService {
    private final UserRepository users;
    private final OffensiveFilter offensive;
    private final AuditService audit;
    private final SessionRevocation revocation;
    private final List<PersonalDataContributor> contributors;
    private final Clock clock;

    public MeService(UserRepository users, OffensiveFilter offensive, AuditService audit, SessionRevocation revocation,
                     List<PersonalDataContributor> contributors, Clock clock) {
        this.users = users;
        this.offensive = offensive;
        this.audit = audit;
        this.revocation = revocation;
        this.contributors = contributors;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MeResponse get(UUID id) {
        return toDto(load(id));
    }

    @Transactional
    public MeResponse patch(UUID id, PatchMe p) {
        User u = load(id);
        Map<String, Object> before = snapshot(u);
        if (p.pseudonym() != null) {
            String ps = p.pseudonym().isEmpty() ? null : p.pseudonym();
            if (ps != null && !ps.equals(u.getPseudonym())) {
                if (offensive.isOffensive(ps)) {
                    throw DomainException.rule("PSEUDONYM_REJECTED", "Este pseudónimo não é permitido. Escolhe outro.");
                }
                if (users.existsByPseudonym(ps)) {
                    throw DomainException.conflict("PSEUDONYM_TAKEN", "Este pseudónimo já está a ser usado. Escolhe outro.");
                }
            }
            u.setPseudonym(ps);
            if (ps == null && u.getRankingVisibility() == RankingVisibility.PSEUDONYM) {
                u.setRankingVisibility(RankingVisibility.NAME);
            }
        }
        if (p.rankingVisibility() != null) {
            if (p.rankingVisibility() == RankingVisibility.PSEUDONYM && u.getPseudonym() == null) {
                throw DomainException.rule("PSEUDONYM_REQUIRED", "Define primeiro um pseudónimo para aparecer com ele no ranking.");
            }
            u.setRankingVisibility(p.rankingVisibility());
        }
        if (p.theme() != null) u.setTheme(p.theme());
        if (p.portfolioPublic() != null) u.setPortfolioPublic(p.portfolioPublic());
        audit.record(id, "PROFILE_UPDATED", "User", id, before, snapshot(u));
        return toDto(u);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> export(UUID id) {
        User u = load(id);
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> account = new LinkedHashMap<>();
        account.put("id", u.getId());
        account.put("name", u.getName());
        account.put("email", u.getEmail());
        account.put("course", u.getCourse() == null ? null : u.getCourse().getName());
        account.put("year", u.getYear());
        account.put("pseudonym", u.getPseudonym());
        account.put("rankingVisibility", u.getRankingVisibility());
        account.put("theme", u.getTheme());
        account.put("portfolioPublic", u.isPortfolioPublic());
        account.put("roles", u.roleCodes());
        account.put("consentAt", u.getConsentAt());
        account.put("createdAt", u.getCreatedAt());
        account.put("lastLoginAt", u.getLastLoginAt());
        out.put("exportedAt", clock.instant());
        out.put("account", account);
        for (PersonalDataContributor c : contributors) out.put(c.section(), c.export(id));
        return out;
    }

    @Transactional
    public void requestDeletion(UUID id) {
        User u = load(id);
        var now = clock.instant();
        u.requestDeletion(now);
        audit.record(id, "DELETION_REQUESTED", "User", id, null, null);
        revocation.revokeAll(id, now);
    }

    private User load(UUID id) {
        return users.findWithRolesById(id)
                .orElseThrow(() -> DomainException.notFound("USER_NOT_FOUND", "A conta não existe."));
    }

    private static Map<String, Object> snapshot(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pseudonym", u.getPseudonym());
        m.put("rankingVisibility", u.getRankingVisibility());
        m.put("theme", u.getTheme());
        m.put("portfolioPublic", u.isPortfolioPublic());
        return m;
    }

    static MeResponse toDto(User u) {
        var c = u.getCourse();
        return new MeResponse(u.getId(), u.getName(), DisplayNames.shortName(u.getName()), DisplayNames.initials(u.getName()),
                u.getEmail(), c == null ? null : new CourseRef(c.getId(), c.getCode(), c.getName()), u.getYear(),
                u.roleCodes(), u.getPseudonym(), u.getRankingVisibility(), u.getTheme(), u.isPortfolioPublic(),
                u.getPortfolioHandle(), u.isMfaEnabled(), u.getStatus().name());
    }
}
