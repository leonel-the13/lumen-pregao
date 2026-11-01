package ao.lumen.pregao.ranking.app;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Implementado pelo módulo contest: nomes de equipas e equipas de um utilizador. */
public interface TeamDirectory {
    record TeamInfo(UUID id, String name, String contestTitle, Set<UUID> memberIds) {}

    Map<UUID, TeamInfo> teams(Collection<UUID> ids);
}
