package ao.lumen.pregao.common.security;

import java.util.Set;

public final class Roles {
    public static final String STUDENT = "STUDENT";
    public static final String TEAM_CAPTAIN = "TEAM_CAPTAIN";
    public static final String AUTHOR = "AUTHOR";
    public static final String REVIEWER = "REVIEWER";
    public static final String JURY = "JURY";
    public static final String CONTEST_ORGANIZER = "CONTEST_ORGANIZER";
    public static final String ADMIN = "ADMIN";

    /** Perfis que exigem segundo factor (R-AUTH-3). */
    public static final Set<String> PRIVILEGED = Set.of(REVIEWER, JURY, CONTEST_ORGANIZER, ADMIN);
    public static final Set<String> ALL = Set.of(STUDENT, TEAM_CAPTAIN, AUTHOR, REVIEWER, JURY, CONTEST_ORGANIZER, ADMIN);

    private Roles() {}
}
