package ao.lumen.pregao.common.privacy;

import java.util.UUID;

/** Cada módulo contribui com os dados pessoais que guarda para a exportação (SEC-13). */
public interface PersonalDataContributor {
    String section();

    Object export(UUID userId);
}
