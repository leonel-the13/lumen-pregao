package ao.lumen.pregao.learning.app;

import java.util.UUID;

public final class LearningEvents {
    private LearningEvents() {}

    /** Publicado na transacção da submissão; o worker arranca depois do commit. */
    public record SubmissionQueued(UUID submissionId) {}

    /** Publicado quando a correcção termina (GRADED/REJECTED); usado pelo SSE do contest. */
    public record SubmissionFinished(UUID submissionId, UUID attemptId, UUID submittedBy, UUID teamId,
                                     UUID contestId, UUID contestProblemId, String status, Boolean correct) {}
}
