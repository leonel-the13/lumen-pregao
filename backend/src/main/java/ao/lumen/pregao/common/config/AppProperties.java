package ao.lumen.pregao.common.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String baseUrl,
        String mailFrom,
        String resendApiKey,
        String proxySecret,
        Identity identity,
        Security security,
        Learning learning,
        Exam exam,
        Ranking ranking,
        Authoring authoring,
        Contest contest,
        @DefaultValue DevSeed devSeed) {

    public record Identity(String allowedEmailDomain, String emailLocalPattern, Duration verificationTtl, int rememberMeDays) {}

    public record Security(String mfaEncKey, RateLimit rateLimit) {}

    public record RateLimit(int loginMaxFailures, int loginMaxFailuresPerIp, Duration loginWindow, int submissionsPerMinute,
                            int registerPerHour, int invitesPerHour) {}

    public record Learning(int maxAttemptsDefault, int failedAttemptPenalty, int hintPenalty) {}

    public record Exam(Duration networkGrace, int pointsPerGradeUnit) {}

    public record Ranking(int streakDays, int streakBonus, String zone) {}

    public record Authoring(String legalChangePolicy) {}

    public record Contest(long schedulerIntervalMs, long sseClockIntervalMs) {}

    public record DevSeed(@DefaultValue("false") boolean enabled) {}
}
