package ao.lumen.pregao.common.security;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Principal guardado na sessão. Não contém segredos. */
public record SessionUser(UUID id, String email, String name, Set<String> roles, Instant authenticatedAt)
        implements Serializable {

    public boolean has(String role) {
        return roles.contains(role);
    }
}
