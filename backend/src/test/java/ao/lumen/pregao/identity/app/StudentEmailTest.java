package ao.lumen.pregao.identity.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StudentEmailTest {
    static final String PATTERN = "^[0-9]{8}$";

    @Test
    void studentEmailStartsWithEightDigitNumber() {
        assertThat(AuthService.localPartAllowed(PATTERN, "20221987@isptec.co.ao")).isTrue();
        assertThat(AuthService.localPartAllowed(PATTERN, "2022198@isptec.co.ao")).isFalse();
        assertThat(AuthService.localPartAllowed(PATTERN, "202219870@isptec.co.ao")).isFalse();
        assertThat(AuthService.localPartAllowed(PATTERN, "joao.silva@isptec.co.ao")).isFalse();
        assertThat(AuthService.localPartAllowed("", "joao.silva@isptec.co.ao")).isTrue();
    }
}
