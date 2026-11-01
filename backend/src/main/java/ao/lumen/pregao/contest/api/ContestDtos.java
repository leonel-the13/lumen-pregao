package ao.lumen.pregao.contest.api;

import ao.lumen.pregao.contest.domain.ContestState;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ContestDtos {
    private ContestDtos() {}

    public record MemberView(String name, String initials, String course, Integer year, String role, boolean confirmed,
                             boolean me) {}

    public record TeamView(UUID id, String name, boolean individual, boolean locked, Instant lockedAt, int confirmed,
                           int maxTeam, int minTeam, boolean captain, List<MemberView> members,
                           List<String> pendingInvites) {}

    public record InviteView(UUID teamId, String teamName, UUID contestId, String contestTitle, String invitedBy) {}

    public record ContestCard(UUID id, String slug, String title, String mode, int minTeam, int maxTeam,
                              ContestState state, Instant registrationOpensAt, Instant registrationClosesAt,
                              Instant startsAt, Instant freezeAt, Instant endsAt, String myTeamName,
                              Integer myTeamConfirmed) {}

    public record ContestDetail(UUID id, String slug, String title, String description, String mode, int minTeam,
                                int maxTeam, ContestState state, Instant registrationOpensAt,
                                Instant registrationClosesAt, Instant startsAt, Instant freezeAt, Instant endsAt,
                                Instant lockAt, int lockMinutes, int wrongPenaltyMinutes, JsonNode finalPhase,
                                int problemsCount, TeamView myTeam, List<InviteView> myInvites, Instant serverNow) {}

    public record ProblemStatus(String state, int wrongAttempts, String lastSubmissionId, String lastStatus,
                                Integer points) {}

    public record ProblemView(String label, String title, String companyName, String type, int points,
                              UUID attemptId, String statement, JsonNode segments, JsonNode presentation,
                              ProblemStatus status) {}

    public record ScoreRow(long position, String name, String initials, int points, int penaltyMinutes,
                           int solved, Instant lastCorrectAt, boolean mine) {}

    public record ScoreboardView(ContestState state, boolean frozen, Instant freezeAt, List<ScoreRow> rows,
                                 Instant serverNow) {}

    public record CreateTeam(@NotBlank @Size(min = 3, max = 40) String name) {}

    public record Invite(@NotBlank @Email @Size(max = 254) String email) {}

    public record DeleteTeam(@NotBlank String confirmation) {}

    public record ProblemInput(@NotBlank @Pattern(regexp = "[A-Z]{1,2}") String label,
                               @NotBlank @Size(max = 200) String title,
                               @Size(max = 120) String companyName,
                               @NotNull UUID exerciseVersionId,
                               @NotNull @Min(0) @Max(1000) Integer points) {}

    public record ContestInput(
            @NotBlank @Pattern(regexp = "[a-z0-9-]{3,80}") String slug,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 4000) String description,
            @NotBlank @Pattern(regexp = "INDIVIDUAL|TEAM") String mode,
            @Min(1) @Max(10) int minTeam,
            @Min(1) @Max(10) int maxTeam,
            @NotNull Instant registrationOpensAt,
            @NotNull Instant registrationClosesAt,
            @NotNull Instant startsAt,
            @NotNull Instant freezeAt,
            @NotNull Instant endsAt,
            @Min(0) @Max(1440) int lockMinutes,
            @Min(0) @Max(120) int wrongPenaltyMinutes,
            JsonNode finalPhase,
            @Size(max = 26) List<@Valid ProblemInput> problems) {}

    public record AdminContestRow(UUID id, String slug, String title, String mode, ContestState state,
                                  Instant startsAt, Instant endsAt, int teams, int problems, long pendingSubmissions) {}
}
