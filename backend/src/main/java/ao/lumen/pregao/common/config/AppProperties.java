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
        @DefaultValue DevSeed devSeed,
        @DefaultValue Mail mail) {

    public record Identity(String allowedEmailDomain, String emailLocalPattern, Duration verificationTtl, int rememberMeDays) {}

    public record Security(String mfaEncKey, RateLimit rateLimit) {}

    public record RateLimit(int loginMaxFailures, int loginMaxFailuresPerIp, Duration loginWindow, int submissionsPerMinute,
                            int registerPerHour, int invitesPerHour) {}

    public record Learning(int maxAttemptsDefault, int failedAttemptPenalty, int hintPenalty) {}

    public record Exam(Duration networkGrace, int pointsPerGradeUnit) {}

    public record Ranking(int streakDays, int streakBonus, String zone) {}

    public record Authoring(String legalChangePolicy) {}

    public record Contest(long schedulerIntervalMs, long sseClockIntervalMs) {}

    /**
     * Emails em massa e lembretes. minInterval espaça os envios (o Resend aceita 2 pedidos por segundo).
     * Lembretes: conta por confirmar há verifyAfter recebe até verifyMax lembretes, separados por verifyEvery;
     * estudante sem actividade há inactiveAfter recebe um lembrete, no máximo um por inactiveEvery.
     */
    public record Mail(@DefaultValue("600ms") Duration minInterval,
                       @DefaultValue("true") boolean remindersEnabled,
                       @DefaultValue("3d") Duration verifyAfter,
                       @DefaultValue("7d") Duration verifyEvery,
                       @DefaultValue("2") int verifyMax,
                       @DefaultValue("14d") Duration inactiveAfter,
                       @DefaultValue("30d") Duration inactiveEvery) {}

    public record DevSeed(@DefaultValue("false") boolean enabled) {}
}
