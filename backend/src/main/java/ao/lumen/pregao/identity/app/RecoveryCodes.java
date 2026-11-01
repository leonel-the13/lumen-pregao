package ao.lumen.pregao.identity.app;

import ao.lumen.pregao.identity.domain.MfaRecoveryCode;
import ao.lumen.pregao.identity.infra.MfaRecoveryCodeRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Códigos de recuperação do 2.º factor: 8 códigos XXXX-XXXX, mostrados uma única vez. */
@Component
public class RecoveryCodes {
    static final int COUNT = 8;
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sem 0/O/1/I
    private static final SecureRandom RANDOM = new SecureRandom();
    private final MfaRecoveryCodeRepository repo;

    public RecoveryCodes(MfaRecoveryCodeRepository repo) {
        this.repo = repo;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<String> regenerate(UUID userId) {
        repo.deleteAllForUser(userId);
        repo.flush();
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < COUNT; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 8; j++) {
                if (j == 4) sb.append('-');
                sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
            codes.add(sb.toString());
            repo.save(new MfaRecoveryCode(userId, hash(sb.toString())));
        }
        return codes;
    }

    /** Consome um código válido. Devolve false se não existir ou já tiver sido usado. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean consume(UUID userId, String code, Instant now) {
        return repo.findByUserIdAndCodeHashAndUsedAtIsNull(userId, hash(code)).map(c -> { c.use(now); return true; }).orElse(false);
    }

    public long remaining(UUID userId) {
        return repo.countByUserIdAndUsedAtIsNull(userId);
    }

    static boolean looksLikeRecovery(String code) {
        return code != null && normalize(code).matches("[A-Z0-9]{4}-[A-Z0-9]{4}");
    }

    private static String hash(String code) {
        return AuthService.sha256(normalize(code));
    }

    private static String normalize(String code) {
        String c = code.strip().toUpperCase(Locale.ROOT).replace(" ", "");
        return c.length() == 8 ? c.substring(0, 4) + "-" + c.substring(4) : c;
    }
}
