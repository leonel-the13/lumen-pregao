-- V4: cursos abertos a todos, notificações, avaliações de salas, feedback e códigos de recuperação do 2.º factor.

-- A plataforma é focada no DCSA mas aberta a estudantes de qualquer curso do ISPTEC.
INSERT INTO courses (id, code, name, created_at, updated_at)
VALUES (uuid_v7(), 'OUTRO', 'Outro curso do ISPTEC', now(), now())
ON CONFLICT (code) DO NOTHING;

CREATE TABLE notifications (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    kind        VARCHAR(32)  NOT NULL,
    title       VARCHAR(160) NOT NULL,
    body        VARCHAR(500),
    link        VARCHAR(300),
    read_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_notifications_user ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications (user_id) WHERE read_at IS NULL;

CREATE TABLE room_ratings (
    id          UUID PRIMARY KEY,
    user_id     UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    room_id     UUID          NOT NULL REFERENCES rooms (id),
    stars       INT           NOT NULL CHECK (stars BETWEEN 1 AND 5),
    comment     VARCHAR(1000),
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL,
    UNIQUE (user_id, room_id)
);
CREATE INDEX idx_room_ratings_room ON room_ratings (room_id);

CREATE TABLE feedback (
    id          UUID PRIMARY KEY,
    user_id     UUID REFERENCES users (id) ON DELETE SET NULL,
    category    VARCHAR(16)   NOT NULL CHECK (category IN ('SUGGESTION','BUG','CONTENT','PRAISE','OTHER')),
    rating      INT CHECK (rating BETWEEN 1 AND 5),
    message     VARCHAR(2000) NOT NULL,
    page        VARCHAR(300),
    status      VARCHAR(16)   NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW','READ','RESOLVED')),
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_feedback_status ON feedback (status, created_at DESC);

CREATE TABLE mfa_recovery_codes (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code_hash   VARCHAR(64) NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    UNIQUE (user_id, code_hash)
);

ALTER TABLE users ADD COLUMN deleted_at TIMESTAMPTZ;
