-- Lúmen Pregão — esquema inicial (MVP)
-- Convenções: ids UUID (v7, gerados na aplicação), instantes em timestamptz (UTC), enums em VARCHAR.

-- ───────────────────────── identity ─────────────────────────
CREATE TABLE courses (
    id          UUID PRIMARY KEY,
    code        VARCHAR(16)  NOT NULL UNIQUE,
    name        VARCHAR(120) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE roles (
    id          UUID PRIMARY KEY,
    code        VARCHAR(32) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

CREATE TABLE users (
    id                     UUID PRIMARY KEY,
    email                  VARCHAR(254) NOT NULL UNIQUE CHECK (email = lower(email)),
    password_hash          VARCHAR(255) NOT NULL,
    name                   VARCHAR(120) NOT NULL,
    status                 VARCHAR(32)  NOT NULL,
    course_id              UUID REFERENCES courses (id),
    year                   INT CHECK (year BETWEEN 1 AND 5),
    pseudonym              VARCHAR(24) UNIQUE CHECK (pseudonym ~ '^[a-z0-9_]{3,24}$'),
    ranking_visibility     VARCHAR(16)  NOT NULL DEFAULT 'NAME',
    theme                  VARCHAR(8)   NOT NULL DEFAULT 'DARK',
    portfolio_public       BOOLEAN      NOT NULL DEFAULT FALSE,
    portfolio_handle       VARCHAR(40)  NOT NULL UNIQUE,
    mfa_secret_enc         TEXT,
    mfa_enabled            BOOLEAN      NOT NULL DEFAULT FALSE,
    consent_at             TIMESTAMPTZ  NOT NULL,
    last_login_at          TIMESTAMPTZ,
    deletion_requested_at  TIMESTAMPTZ,
    created_at             TIMESTAMPTZ  NOT NULL,
    updated_at             TIMESTAMPTZ  NOT NULL
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles (id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE email_verification_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_evt_user ON email_verification_tokens (user_id);

-- ───────────────────────── auditoria (só inserção) ─────────────────────────
CREATE TABLE audit_logs (
    id           UUID PRIMARY KEY,
    actor_id     UUID,
    action       VARCHAR(64) NOT NULL,
    entity_type  VARCHAR(64) NOT NULL,
    entity_id    VARCHAR(64),
    before_data  JSONB,
    after_data   JSONB,
    ip           VARCHAR(64),
    at           TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_audit_at ON audit_logs (at DESC);
CREATE INDEX idx_audit_actor ON audit_logs (actor_id, at DESC);
CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id);

CREATE FUNCTION forbid_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'A tabela % é só de inserção', TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_logs_immutable BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION forbid_mutation();

-- ───────────────────────── authoring ─────────────────────────
CREATE TABLE legal_sources (
    id              UUID PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    identifier      VARCHAR(120) NOT NULL,
    version         VARCHAR(60)  NOT NULL,
    published_at    DATE,
    effective_from  DATE,
    effective_to    DATE,
    url             VARCHAR(500),
    confirmed       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL
);

CREATE TABLE companies (
    id          UUID PRIMARY KEY,
    name        VARCHAR(120) NOT NULL UNIQUE,
    sector      VARCHAR(80)  NOT NULL,
    description TEXT,
    fictional   BOOLEAN      NOT NULL CHECK (fictional = TRUE),
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE exercises (
    id          UUID PRIMARY KEY,
    code        VARCHAR(16)  NOT NULL UNIQUE,
    author_id   UUID         NOT NULL REFERENCES users (id),
    topic       VARCHAR(120) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);
CREATE SEQUENCE exercise_code_seq START 1;

CREATE TABLE exercise_versions (
    id               UUID PRIMARY KEY,
    exercise_id      UUID         NOT NULL REFERENCES exercises (id),
    number           INT          NOT NULL CHECK (number >= 1),
    type             VARCHAR(1)   NOT NULL CHECK (type IN ('A','B','C','D','E','F','G')),
    title            VARCHAR(200) NOT NULL,
    statement        TEXT         NOT NULL,
    presentation     JSONB        NOT NULL DEFAULT '{}'::jsonb,
    param_spec       JSONB        NOT NULL DEFAULT '{}'::jsonb,
    solution_spec    JSONB        NOT NULL DEFAULT '{}'::jsonb,
    tolerance        JSONB        NOT NULL DEFAULT '{}'::jsonb,
    hints            JSONB        NOT NULL DEFAULT '[]'::jsonb,
    feedback_policy  VARCHAR(16)  NOT NULL DEFAULT 'DETAILED',
    difficulty       INT     NOT NULL CHECK (difficulty BETWEEN 1 AND 4),
    points           INT          NOT NULL CHECK (points >= 0),
    max_attempts     INT          NOT NULL DEFAULT 3 CHECK (max_attempts >= 1),
    pgc_version      VARCHAR(32),
    valid_until      DATE,
    change_note      VARCHAR(200),
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    UNIQUE (exercise_id, number)
);

CREATE TABLE review_workflows (
    id                   UUID PRIMARY KEY,
    exercise_version_id  UUID        NOT NULL UNIQUE REFERENCES exercise_versions (id),
    status               VARCHAR(16) NOT NULL,
    reviewer_id          UUID REFERENCES users (id),
    notes                TEXT,
    submitted_at         TIMESTAMPTZ,
    approved_at          TIMESTAMPTZ,
    published_at         TIMESTAMPTZ,
    legal_alert_at       TIMESTAMPTZ,
    version              BIGINT      NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_review_status ON review_workflows (status);

CREATE TABLE exercise_references (
    id                   UUID PRIMARY KEY,
    exercise_version_id  UUID         NOT NULL REFERENCES exercise_versions (id),
    legal_source_id      UUID         NOT NULL REFERENCES legal_sources (id),
    articles             VARCHAR(200),
    consulted_at         DATE,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_ref_version ON exercise_references (exercise_version_id);
CREATE INDEX idx_ref_source ON exercise_references (legal_source_id);

-- ───────────────────────── grading ─────────────────────────
CREATE TABLE pgc_accounts (
    id           UUID PRIMARY KEY,
    version      VARCHAR(32)  NOT NULL,
    code         VARCHAR(16)  NOT NULL,
    name         VARCHAR(160) NOT NULL,
    class_code   VARCHAR(4)   NOT NULL,
    class_name   VARCHAR(80)  NOT NULL,
    placeholder  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    UNIQUE (version, code)
);

-- ───────────────────────── learning ─────────────────────────
CREATE TABLE tracks (
    id               UUID PRIMARY KEY,
    slug             VARCHAR(80)  NOT NULL UNIQUE,
    title            VARCHAR(200) NOT NULL,
    summary          VARCHAR(300) NOT NULL,
    description      TEXT         NOT NULL,
    difficulty       INT     NOT NULL CHECK (difficulty BETWEEN 1 AND 4),
    topic            VARCHAR(60)  NOT NULL,
    courses          JSONB        NOT NULL DEFAULT '[]'::jsonb,
    competencies     JSONB        NOT NULL DEFAULT '[]'::jsonb,
    completion_rule  JSONB        NOT NULL DEFAULT '{}'::jsonb,
    est_hours        INT,
    position         INT          NOT NULL DEFAULT 0,
    status           VARCHAR(16)  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL
);

CREATE TABLE modules (
    id          UUID PRIMARY KEY,
    track_id    UUID         NOT NULL REFERENCES tracks (id),
    position    INT          NOT NULL,
    title       VARCHAR(200) NOT NULL,
    summary     VARCHAR(300),
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    UNIQUE (track_id, position)
);

CREATE TABLE rooms (
    id             UUID PRIMARY KEY,
    module_id      UUID         NOT NULL REFERENCES modules (id),
    position       INT          NOT NULL,
    title          VARCHAR(200) NOT NULL,
    description    VARCHAR(500) NOT NULL,
    objectives     JSONB        NOT NULL DEFAULT '[]'::jsonb,
    difficulty     INT     NOT NULL CHECK (difficulty BETWEEN 1 AND 4),
    est_minutes    INT          NOT NULL,
    context        TEXT         NOT NULL DEFAULT '',
    materials      JSONB        NOT NULL DEFAULT '[]'::jsonb,
    prerequisites  JSONB        NOT NULL DEFAULT '[]'::jsonb,
    status         VARCHAR(16)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    UNIQUE (module_id, position)
);

CREATE TABLE tasks (
    id                   UUID PRIMARY KEY,
    room_id              UUID         NOT NULL REFERENCES rooms (id),
    position             INT          NOT NULL,
    title                VARCHAR(200) NOT NULL,
    exercise_version_id  UUID         NOT NULL REFERENCES exercise_versions (id),
    points               INT          NOT NULL CHECK (points >= 0),
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    UNIQUE (room_id, position)
);

-- ───────────────────────── ranking ─────────────────────────
CREATE TABLE seasons (
    id          UUID PRIMARY KEY,
    name        VARCHAR(60) NOT NULL UNIQUE,
    starts_at   TIMESTAMPTZ NOT NULL,
    ends_at     TIMESTAMPTZ NOT NULL CHECK (ends_at > starts_at),
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

-- ───────────────────────── contest ─────────────────────────
CREATE TABLE contests (
    id                      UUID PRIMARY KEY,
    slug                    VARCHAR(80)  NOT NULL UNIQUE,
    title                   VARCHAR(200) NOT NULL,
    description             TEXT         NOT NULL DEFAULT '',
    mode                    VARCHAR(16)  NOT NULL CHECK (mode IN ('INDIVIDUAL','TEAM')),
    min_team                INT          NOT NULL DEFAULT 1,
    max_team                INT          NOT NULL DEFAULT 1,
    registration_opens_at   TIMESTAMPTZ  NOT NULL,
    registration_closes_at  TIMESTAMPTZ  NOT NULL,
    starts_at               TIMESTAMPTZ  NOT NULL,
    freeze_at               TIMESTAMPTZ  NOT NULL,
    ends_at                 TIMESTAMPTZ  NOT NULL,
    lock_minutes            INT          NOT NULL DEFAULT 30,
    wrong_penalty_minutes   INT          NOT NULL DEFAULT 10,
    final_phase             JSONB,
    state                   VARCHAR(24)  NOT NULL,
    rubric_id               UUID,
    results_published_at    TIMESTAMPTZ,
    created_by              UUID         NOT NULL REFERENCES users (id),
    version                 BIGINT       NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ  NOT NULL,
    updated_at              TIMESTAMPTZ  NOT NULL,
    CHECK (min_team >= 1 AND max_team >= min_team),
    CHECK (registration_opens_at <= registration_closes_at
           AND registration_closes_at <= starts_at
           AND starts_at < freeze_at
           AND freeze_at < ends_at)
);
CREATE INDEX idx_contest_state ON contests (state);

CREATE TABLE contest_problems (
    id                   UUID PRIMARY KEY,
    contest_id           UUID         NOT NULL REFERENCES contests (id),
    label                VARCHAR(4)   NOT NULL,
    title                VARCHAR(200) NOT NULL,
    company_name         VARCHAR(120),
    exercise_version_id  UUID         NOT NULL REFERENCES exercise_versions (id),
    points               INT          NOT NULL CHECK (points >= 0),
    position             INT          NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    UNIQUE (contest_id, label)
);

CREATE TABLE teams (
    id          UUID PRIMARY KEY,
    contest_id  UUID        NOT NULL REFERENCES contests (id),
    name        VARCHAR(60) NOT NULL,
    individual  BOOLEAN     NOT NULL DEFAULT FALSE,
    locked_at   TIMESTAMPTZ,
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    UNIQUE (contest_id, name)
);

CREATE TABLE team_members (
    id            UUID PRIMARY KEY,
    team_id       UUID        NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    contest_id    UUID        NOT NULL REFERENCES contests (id),
    user_id       UUID        NOT NULL REFERENCES users (id),
    role          VARCHAR(16) NOT NULL CHECK (role IN ('CAPTAIN','MEMBER')),
    confirmed_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    UNIQUE (contest_id, user_id)
);
CREATE INDEX idx_team_members_team ON team_members (team_id);

CREATE TABLE team_invites (
    id               UUID PRIMARY KEY,
    team_id          UUID        NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    invitee_user_id  UUID        NOT NULL REFERENCES users (id),
    invited_by       UUID        NOT NULL REFERENCES users (id),
    status           VARCHAR(16) NOT NULL,
    responded_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL
);
CREATE UNIQUE INDEX uq_team_invite_pending ON team_invites (team_id, invitee_user_id) WHERE status = 'PENDING';

CREATE TABLE scoreboards (
    id                UUID PRIMARY KEY,
    contest_id        UUID        NOT NULL REFERENCES contests (id),
    participant_type  VARCHAR(8)  NOT NULL CHECK (participant_type IN ('TEAM','USER')),
    participant_id    UUID        NOT NULL,
    points            INT         NOT NULL DEFAULT 0,
    penalty_minutes   INT         NOT NULL DEFAULT 0,
    solved_count      INT         NOT NULL DEFAULT 0,
    last_correct_at   TIMESTAMPTZ,
    frozen_snapshot   JSONB,
    version           BIGINT      NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL,
    UNIQUE (contest_id, participant_type, participant_id)
);

-- ───────────────────────── tentativas e submissões ─────────────────────────
CREATE TABLE attempts (
    id                   UUID PRIMARY KEY,
    exercise_version_id  UUID        NOT NULL REFERENCES exercise_versions (id),
    exercise_type        VARCHAR(1)  NOT NULL,
    task_id              UUID REFERENCES tasks (id),
    contest_problem_id   UUID REFERENCES contest_problems (id),
    contest_id           UUID REFERENCES contests (id),
    user_id              UUID REFERENCES users (id),
    team_id              UUID REFERENCES teams (id),
    seed                 BIGINT      NOT NULL,
    params               JSONB       NOT NULL,
    status               VARCHAR(16) NOT NULL,
    attempts_used        INT         NOT NULL DEFAULT 0,
    hints_used           INT         NOT NULL DEFAULT 0,
    score                INT         NOT NULL DEFAULT 0,
    draft                JSONB,
    version              BIGINT      NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL,
    CHECK ((user_id IS NULL) <> (team_id IS NULL)),
    CHECK ((task_id IS NULL) <> (contest_problem_id IS NULL))
);
CREATE UNIQUE INDEX uq_attempt_task_user ON attempts (task_id, user_id) WHERE task_id IS NOT NULL;
CREATE UNIQUE INDEX uq_attempt_problem_team ON attempts (contest_problem_id, team_id) WHERE contest_problem_id IS NOT NULL;
CREATE INDEX idx_attempt_user ON attempts (user_id);

CREATE TABLE submissions (
    id                     UUID PRIMARY KEY,
    attempt_id             UUID        NOT NULL REFERENCES attempts (id),
    idempotency_key        VARCHAR(64) NOT NULL,
    submitted_by           UUID        NOT NULL REFERENCES users (id),
    payload                JSONB       NOT NULL,
    received_at            TIMESTAMPTZ NOT NULL,
    status                 VARCHAR(16) NOT NULL,
    result                 JSONB,
    correct                BOOLEAN,
    points                 INT,
    error_code             VARCHAR(64),
    retries                INT         NOT NULL DEFAULT 0,
    processing_started_at  TIMESTAMPTZ,
    graded_at              TIMESTAMPTZ,
    created_at             TIMESTAMPTZ NOT NULL,
    updated_at             TIMESTAMPTZ NOT NULL,
    UNIQUE (attempt_id, idempotency_key)
);
CREATE INDEX idx_submission_status ON submissions (status, received_at);
CREATE INDEX idx_submission_by ON submissions (submitted_by, received_at DESC);

-- ───────────────────────── exames ─────────────────────────
CREATE TABLE exams (
    id                  UUID PRIMARY KEY,
    slug                VARCHAR(80)  NOT NULL UNIQUE,
    title               VARCHAR(200) NOT NULL,
    module_id           UUID REFERENCES modules (id),
    duration_min        INT          NOT NULL CHECK (duration_min > 0),
    question_pool_rule  JSONB        NOT NULL,
    pass_mark           NUMERIC(4,2) NOT NULL,
    status              VARCHAR(16)  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL
);

CREATE TABLE exam_sessions (
    id             UUID PRIMARY KEY,
    exam_id        UUID         NOT NULL REFERENCES exams (id),
    user_id        UUID         NOT NULL REFERENCES users (id),
    started_at     TIMESTAMPTZ  NOT NULL,
    ends_at        TIMESTAMPTZ  NOT NULL,
    submitted_at   TIMESTAMPTZ,
    status         VARCHAR(32)  NOT NULL,
    submit_reason  VARCHAR(16),
    questions      JSONB        NOT NULL,
    answers        JSONB        NOT NULL DEFAULT '{}'::jsonb,
    marked         JSONB        NOT NULL DEFAULT '[]'::jsonb,
    grade          NUMERIC(4,2),
    report         JSONB,
    version        BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    CHECK (ends_at > started_at)
);
CREATE UNIQUE INDEX uq_exam_session_open ON exam_sessions (exam_id, user_id) WHERE status = 'IN_PROGRESS';
CREATE INDEX idx_exam_session_status ON exam_sessions (status, ends_at);

-- ───────────────────────── pontos e conquistas ─────────────────────────
CREATE TABLE point_events (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id),
    team_id     UUID REFERENCES teams (id),
    source      VARCHAR(16) NOT NULL CHECK (source IN ('TASK','EXAM','STREAK','BADGE','CONTEST','COMPENSATION')),
    source_id   VARCHAR(80) NOT NULL,
    delta       INT         NOT NULL,
    at          TIMESTAMPTZ NOT NULL,
    UNIQUE (user_id, source, source_id)
);
CREATE INDEX idx_point_events_user ON point_events (user_id, at DESC);
CREATE INDEX idx_point_events_at ON point_events (at);

CREATE TRIGGER point_events_immutable BEFORE UPDATE OR DELETE ON point_events
    FOR EACH ROW EXECUTE FUNCTION forbid_mutation();

CREATE TABLE badges (
    id           UUID PRIMARY KEY,
    code         VARCHAR(32)  NOT NULL UNIQUE,
    title        VARCHAR(80)  NOT NULL,
    description  VARCHAR(200) NOT NULL,
    points       INT          NOT NULL DEFAULT 0,
    rule         JSONB        NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL
);

CREATE TABLE user_badges (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id),
    badge_id    UUID        NOT NULL REFERENCES badges (id),
    awarded_at  TIMESTAMPTZ NOT NULL,
    UNIQUE (user_id, badge_id)
);

-- Dias com prática (fuso Africa/Luanda) para as sequências
CREATE TABLE practice_days (
    user_id  UUID NOT NULL REFERENCES users (id),
    day      DATE NOT NULL,
    PRIMARY KEY (user_id, day)
);
