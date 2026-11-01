package ao.lumen.pregao.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.identity.app.PasswordPolicy;
import ao.lumen.pregao.identity.app.Totp;
import ao.lumen.pregao.identity.domain.DisplayNames;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

class TotpAndPolicyTest {
    /** Vector do RFC 6238 (SHA1, segredo «12345678901234567890» em base32), truncado a 6 dígitos. */
    static final String RFC_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @Test
    void totpMatchesRfcVectors() {
        assertThat(Totp.generate(RFC_SECRET, 59L / 30)).isEqualTo("287082");
        assertThat(Totp.generate(RFC_SECRET, 1111111109L / 30)).isEqualTo("081804");
        assertThat(Totp.verify(RFC_SECRET, "081804", Instant.ofEpochSecond(1111111109L))).isTrue();
        assertThat(Totp.verify(RFC_SECRET, "081805", Instant.ofEpochSecond(1111111109L))).isFalse();
        assertThat(Totp.verify(RFC_SECRET, "abc", Instant.now())).isFalse();
    }

    @Test
    void passwordPolicyRequires12CharsAndRejectsObvious() {
        assertThatThrownBy(() -> PasswordPolicy.check("curta", "kiala@exemplo.ao", "Kiala"))
                .isInstanceOf(DomainException.class).hasMessageContaining("12");
        assertThatThrownBy(() -> PasswordPolicy.check("123456789012", "kiala@exemplo.ao", "Kiala"))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> PasswordPolicy.check("kiala-segredo-forte", "kiala@exemplo.ao", "Kiala Afonso"))
                .isInstanceOf(DomainException.class);
        PasswordPolicy.check("Mussulo#Balancete-77", "kiala@exemplo.ao", "Kiala Afonso");
    }

    @Test
    void passwordsAreHashedWithArgon2id() {
        String hash = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8().encode("Mussulo#Balancete-77");
        assertThat(hash).startsWith("$argon2id$");
    }

    @Test
    void shortNames() {
        assertThat(DisplayNames.shortName("Kiala Manuel Afonso")).isEqualTo("Kiala A.");
        assertThat(DisplayNames.initials("debito_certo")).isEqualTo("DC");
    }
}
