-- V3: convites/redefinição de palavra-passe, chave estável dos exercícios importados e contas adicionais do PGC.

-- Tokens de definição de palavra-passe: INVITE (conta criada por um administrador) e RESET («Esqueci-me»).
CREATE TABLE password_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose     VARCHAR(16) NOT NULL CHECK (purpose IN ('INVITE', 'RESET')),
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_password_tokens_user ON password_tokens (user_id);

-- Chave estável dos exercícios de um pacote de conteúdo (importação idempotente).
ALTER TABLE exercises ADD COLUMN external_key VARCHAR(80) UNIQUE;

-- Contas adicionais do PGC usadas no pacote de conteúdo. Diploma e versão a confirmar pelo revisor.
INSERT INTO pgc_accounts (id, version, code, name, class_code, class_name, placeholder, created_at, updated_at)
SELECT uuid_v7(), 'PGC-a-confirmar', c.code, c.name, c.cls, c.cls_name, FALSE, now(), now()
FROM (VALUES
    ('12', 'Imobilizações incorpóreas',                                  '1', 'Meios fixos e investimentos'),
    ('21', 'Compras',                                                    '2', 'Existências'),
    ('22', 'Matérias-primas, subsidiárias e de consumo',                 '2', 'Existências'),
    ('33', 'Empréstimos',                                                '3', 'Terceiros'),
    ('36', 'Pessoal',                                                    '3', 'Terceiros'),
    ('42', 'Depósitos a prazo',                                          '4', 'Meios monetários'),
    ('55', 'Reservas',                                                   '5', 'Capital e reservas'),
    ('62', 'Prestações de serviços',                                     '6', 'Proveitos'),
    ('71', 'Custo das mercadorias vendidas e das matérias consumidas',   '7', 'Custos'),
    ('72', 'Custos com o pessoal',                                       '7', 'Custos'),
    ('73', 'Amortizações do exercício',                                  '7', 'Custos'),
    ('75', 'Outros custos e perdas operacionais',                        '7', 'Custos')
) AS c(code, name, cls, cls_name)
ON CONFLICT (version, code) DO NOTHING;
