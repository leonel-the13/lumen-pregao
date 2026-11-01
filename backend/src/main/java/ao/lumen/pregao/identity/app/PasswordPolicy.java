package ao.lumen.pregao.identity.app;

import ao.lumen.pregao.common.error.DomainException;
import java.util.Locale;
import java.util.Set;

/** Mínimo de 12 caracteres e recusa de palavras-passe óbvias (SEC-01). */
public final class PasswordPolicy {
    private static final Set<String> OBVIOUS = Set.of("123456789012", "palavrapasse", "password1234",
            "qwertyuiopas", "isptec123456", "000000000000", "abcdefghijkl", "contabilidade");

    private PasswordPolicy() {}

    public static void check(String password, String email, String name) {
        if (password == null || password.length() < 12) {
            throw DomainException.rule("PASSWORD_TOO_SHORT", "A palavra-passe tem de ter pelo menos 12 caracteres.");
        }
        String p = password.toLowerCase(Locale.ROOT);
        String local = email.substring(0, Math.max(0, email.indexOf('@')));
        boolean repeated = p.chars().distinct().count() <= 2;
        if (OBVIOUS.stream().anyMatch(p::contains) || repeated || (!local.isEmpty() && p.contains(local))) {
            throw DomainException.rule("PASSWORD_WEAK", "Escolhe uma palavra-passe menos óbvia.");
        }
        for (String part : name.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (part.length() >= 4 && p.contains(part)) {
                throw DomainException.rule("PASSWORD_WEAK", "A palavra-passe não pode conter o teu nome.");
            }
        }
    }
}
