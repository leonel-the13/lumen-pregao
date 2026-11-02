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

    @Test
    void domainSettingToleratesCommonFormats() {
        for (String setting : new String[] {"isptec.co.ao", "@isptec.co.ao", " isptec.co.ao ", "\"isptec.co.ao\"", "ISPTEC.co.ao", "exemplo.ao, isptec.co.ao"}) {
            assertThat(AuthService.domainAllowed(setting, "20221987@isptec.co.ao")).as(setting).isTrue();
        }
        assertThat(AuthService.domainAllowed("isptec.co.ao", "20221987@gmail.com")).isFalse();
        assertThat(AuthService.domainAllowed("", "20221987@isptec.co.ao")).isFalse();
    }
}
