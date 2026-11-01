package ao.lumen.pregao.contest.domain;

/** Ciclo de vida (spec §8). */
public enum ContestState {
    DRAFT, REGISTRATION, WAITING_ROOM, RUNNING, FROZEN, SUBMISSIONS_CLOSED, FINAL_EVALUATION, RESULTS_PUBLISHED, CLOSED;

    public boolean statementsReleased() {
        return ordinal() >= RUNNING.ordinal();
    }

    public boolean frozenForOthers() {
        return this == FROZEN || this == SUBMISSIONS_CLOSED || this == FINAL_EVALUATION;
    }
}
