-- V5: emails da administração (modelos e campanhas), lembretes automáticos e notificações por email.

-- Preferência de emails (notificações, lembretes e campanhas; os de segurança chegam sempre).
ALTER TABLE users ADD COLUMN email_opt_in BOOLEAN NOT NULL DEFAULT TRUE;
-- Última actividade (o login com «lembrar-me» dura dias; last_login_at não chega para saber quem anda inactivo).
ALTER TABLE users ADD COLUMN last_seen_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN verify_reminders INT NOT NULL DEFAULT 0 CHECK (verify_reminders >= 0);
ALTER TABLE users ADD COLUMN last_verify_reminder_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN last_inactive_reminder_at TIMESTAMPTZ;
UPDATE users SET last_seen_at = last_login_at;
CREATE INDEX idx_users_status ON users (status);

CREATE TABLE email_templates (
    id          UUID PRIMARY KEY,
    name        VARCHAR(80)   NOT NULL UNIQUE,
    subject     VARCHAR(120)  NOT NULL,
    title       VARCHAR(120)  NOT NULL,
    body        VARCHAR(5000) NOT NULL,
    cta_label   VARCHAR(40),
    cta_path    VARCHAR(200) CHECK (cta_path IS NULL OR (cta_path LIKE '/%' AND cta_path NOT LIKE '//%')),
    created_by  UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL
);

CREATE TABLE email_campaigns (
    id              UUID PRIMARY KEY,
    created_by      UUID          NOT NULL REFERENCES users (id),
    idempotency_key VARCHAR(64)   NOT NULL UNIQUE,
    template_id     UUID REFERENCES email_templates (id) ON DELETE SET NULL,
    audience        VARCHAR(16)   NOT NULL CHECK (audience IN ('ALL_STUDENTS','COURSE','INACTIVE','USER')),
    course_id       UUID REFERENCES courses (id),
    year            INT CHECK (year BETWEEN 1 AND 5),
    inactive_days   INT CHECK (inactive_days BETWEEN 1 AND 365),
    target_user_id  UUID REFERENCES users (id),
    audience_label  VARCHAR(160)  NOT NULL,
    subject         VARCHAR(120)  NOT NULL,
    title           VARCHAR(120)  NOT NULL,
    body            VARCHAR(5000) NOT NULL,
    cta_label       VARCHAR(40),
    cta_path        VARCHAR(200),
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL
);

-- Um registo por destinatário: a campanha fica persistida antes do envio e sobrevive a reinícios.
CREATE TABLE email_deliveries (
    id           UUID PRIMARY KEY,
    campaign_id  UUID         NOT NULL REFERENCES email_campaigns (id) ON DELETE CASCADE,
    user_id      UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status       VARCHAR(16)  NOT NULL CHECK (status IN ('PENDING','SENT','FAILED','SKIPPED')),
    attempts     INT          NOT NULL DEFAULT 0,
    last_error   VARCHAR(300),
    sent_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    UNIQUE (campaign_id, user_id)
);
CREATE INDEX idx_email_deliveries_pending ON email_deliveries (created_at) WHERE status = 'PENDING';

-- Modelos de partida (originais; a administração edita-os ou apaga-os).
INSERT INTO email_templates (id, name, subject, title, body, cta_label, cta_path, created_at, updated_at) VALUES
(uuid_v7(), 'Balancete desequilibrado', '{nome}, o teu Balancete está desequilibrado',
 'Há um lançamento à tua espera',
 'Fizemos as contas e encontrámos um problema: os teus Proveitos de conhecimento estão parados desde a última vez que entraste.

Nada que uma sala de dez minutos não resolva. O Balancete agradece, e o ranking também.',
 'Voltar às trilhas', '/trilhas', now(), now()),
(uuid_v7(), 'Auditoria surpresa', 'Auditoria surpresa, {nome}',
 'Isto não é uma inspecção. Ou será?',
 'A equipa de auditoria (nós) reparou que já lá vão {dias_sem_entrar} dias sem passares pelo Lúmen Lab.

O relatório final ainda pode sair «sem reservas». Basta resolveres uma tarefa esta semana.',
 'Resolver uma tarefa', '/salas', now(), now()),
(uuid_v7(), 'O ranking não pára', '{nome}, o ranking não pára',
 'Enquanto lês isto, alguém está a resolver uma tarefa',
 'O ranking de {curso} mexe-se todos os dias. Cada resposta certa conta, e as primeiras tentativas contam ainda mais.

Uma sala por dia e chegas ao lugar que é teu.',
 'Ver os rankings', '/rankings', now(), now()),
(uuid_v7(), 'Maratona a aquecer', 'A Maratona está a aquecer, {nome}',
 'Equipa formada? Calculadora pronta?',
 'Há competições a caminho e as equipas já se estão a organizar.

Junta-te a colegas, treina nas salas e chega ao dia com o raciocínio afinado.',
 'Ver as competições', '/competicoes', now(), now()),
(uuid_v7(), 'Novidades', 'Novidades no Lúmen Lab',
 'Olá, {nome}!',
 'Escreve aqui as novidades: salas novas, exames, mudanças na plataforma.

Um abraço da Associação de Estudantes.',
 'Entrar no Lúmen Lab', '/inicio', now(), now()),
(uuid_v7(), 'Precisas de ajuda?', 'Podemos ajudar, {nome}?',
 'Olá, {nome}!',
 'Escreve aqui a ajuda para esta pessoa: o que reparaste, o que pode fazer a seguir e onde encontrar a sala certa.

Se tiveres dúvidas, envia-nos feedback a partir de qualquer página da plataforma.',
 'Abrir o Lúmen Lab', '/inicio', now(), now());
