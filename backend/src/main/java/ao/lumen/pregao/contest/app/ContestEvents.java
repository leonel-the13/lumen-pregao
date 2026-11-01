package ao.lumen.pregao.contest.app;

import ao.lumen.pregao.contest.domain.ContestState;
import java.util.UUID;

public final class ContestEvents {
    private ContestEvents() {}

    public record StateChanged(UUID contestId, ContestState state) {}

    public record ScoreboardChanged(UUID contestId, UUID teamId, int points) {}
}
