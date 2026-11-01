-- Dados de referência. Tudo o que é normativo fica «a confirmar» (confirmed = false / placeholder = true)
-- até um revisor validar. Ver docs/content/fontes-normativas.md.

CREATE FUNCTION uuid_v7() RETURNS uuid AS $$
DECLARE
    ts BIGINT := floor(extract(epoch FROM clock_timestamp()) * 1000);
    b  BYTEA  := uuid_send(gen_random_uuid());
BEGIN
    b := overlay(b PLACING substring(int8send(ts) FROM 3) FROM 1 FOR 6);
    b := set_byte(b, 6, (get_byte(b, 6) & 15) | 112);
    RETURN encode(b, 'hex')::uuid;
END;
$$ LANGUAGE plpgsql VOLATILE;

-- Os 7 perfis RBAC (spec §3.1)
INSERT INTO roles (id, code, created_at, updated_at)
SELECT uuid_v7(), c, now(), now()
FROM unnest(ARRAY['STUDENT','TEAM_CAPTAIN','AUTHOR','REVIEWER','JURY','CONTEST_ORGANIZER','ADMIN']) AS c;

-- Cursos (nomes oficiais a confirmar)
INSERT INTO courses (id, code, name, created_at, updated_at) VALUES
    (uuid_v7(), 'CONT', 'Contabilidade', now(), now()),
    (uuid_v7(), 'ECON', 'Economia', now(), now()),
    (uuid_v7(), 'GEST', 'Gestão Empresarial', now(), now());

-- Fontes normativas — versões por confirmar
INSERT INTO legal_sources (id, title, identifier, version, confirmed, created_at, updated_at) VALUES
    (uuid_v7(), 'Plano Geral de Contabilidade', 'PGC · diploma a confirmar', 'a confirmar', FALSE, now(), now()),
    (uuid_v7(), 'Código do IVA', 'Lei n.º 7/19', 'a confirmar', FALSE, now(), now());

-- Contas do PGC usadas no design aprovado. Versão e subcontas do IVA a confirmar pelo revisor.
INSERT INTO pgc_accounts (id, version, code, name, class_code, class_name, placeholder, created_at, updated_at) VALUES
    (uuid_v7(), 'PGC-a-confirmar', '11',     'Imobilizações corpóreas',          '1', 'Meios fixos e investimentos', FALSE, now(), now()),
    (uuid_v7(), 'PGC-a-confirmar', '18',     'Amortizações acumuladas',          '1', 'Meios fixos e investimentos', FALSE, now(), now()),
    (uuid_v7(), 'PGC-a-confirmar', '26',     'Mercadorias',                      '2', 'Existências',                 FALSE, now(), now()),
    (uuid_v7(), 'PGC-a-confirmar', '31',     'Clientes',                         '3', 'Terceiros',                   FALSE, now(), now()),
    (uuid_v7(), 'PGC-a-confirmar', '32',     'Fornecedores',                     '3', 'Terceiros',                   FALSE, now(), now()),
    (uuid_v7(), 'PGC-a-confirmar', '32.1',   'Fornecedores · correntes',         '3', 'Terceiros',                   TRUE,  now(), now()),
    (uuid_v7(), 'PGC-a-confirmar', '34',     'Estado',                           '3', 'Terceiros',                   FALSE, now(), now()),
    (uuid_v7(), 'PGC-a-confirmar', '34.5.x', 'IVA suportado',                    '3', 'Terceiros',                   TRUE,  now(), now()),
    (uuid_v7(), 'PGC-a-confirmar', '34.5.y', 'IVA liquidado',                    '3', 'Terceiros',                   TRUE,  now(), now()),
    (uuid_v7(), 'PGC-a-confirmar', '37',     'Outros valores a receber e a pagar','3', 'Terceiros',                  FALSE, now(), now()),
    (uuid_v7(), 'PGC-a-confirmar', '43',     'Depósitos à ordem',                '4', 'Meios monetários',            FALSE, now(), now()),
    (uuid_v7(), 'PGC-a-confirmar', '45',     'Caixa',                            '4', 'Meios monetários',            FALSE, now(), now()),
    (uuid_v7(), 'PGC-a-confirmar', '51',     'Capital',                          '5', 'Capital e reservas',          FALSE, now(), now()),
    (uuid_v7(), 'PGC-a-confirmar', '61',     'Vendas',                           '6', 'Proveitos',                   FALSE, now(), now()),
    (uuid_v7(), 'PGC-a-confirmar', '88',     'Resultado líquido do exercício',   '8', 'Resultados',                  FALSE, now(), now());

-- Empresas fictícias (Company.fictional = true é obrigatório)
INSERT INTO companies (id, name, sector, description, fictional, created_at, updated_at) VALUES
    (uuid_v7(), 'Distribuidora Mussulo, Lda.', 'Distribuição',     'Empresa fictícia de distribuição de mercadorias.', TRUE, now(), now()),
    (uuid_v7(), 'Indústrias Cuanza',           'Indústria',        'Empresa fictícia industrial.',                     TRUE, now(), now()),
    (uuid_v7(), 'Kwanza Energia',              'Energia',          'Empresa fictícia do sector energético.',           TRUE, now(), now()),
    (uuid_v7(), 'Banco Mutamba',               'Banca',            'Banco fictício.',                                  TRUE, now(), now()),
    (uuid_v7(), 'Talatona Telecom',            'Telecomunicações', 'Operador fictício de telecomunicações.',          TRUE, now(), now());

-- Época competitiva (datas a confirmar pela organização)
INSERT INTO seasons (id, name, starts_at, ends_at, created_at, updated_at) VALUES
    (uuid_v7(), 'Época 2026/27', '2026-08-31T23:00:00Z', '2027-08-31T23:00:00Z', now(), now());

-- Conquistas do design. Pontos iniciais a confirmar (spec §7).
INSERT INTO badges (id, code, title, description, points, rule, created_at, updated_at) VALUES
    (uuid_v7(), 'PARTIDAS_DOBRADAS', 'Partidas dobradas', '10 lançamentos equilibrados', 50, '{"kind":"balanced_journals","count":10}', now(), now()),
    (uuid_v7(), 'SEQUENCIA_10',      'Sequência de 10',   '10 dias seguidos',            50, '{"kind":"streak","days":10}', now(), now()),
    (uuid_v7(), 'PODIO',             'Pódio',             'Top 3 num contest',           50, '{"kind":"contest_top","rank":3}', now(), now()),
    (uuid_v7(), 'FISCALISTA',        'Fiscalista',        'Trilha IVA completa',         50, '{"kind":"track_complete","slug":"fiscalidade-iva"}', now(), now()),
    (uuid_v7(), 'ANALISTA',          'Analista',          '20 rácios correctos',         50, '{"kind":"correct_by_type","type":"F","count":20}', now(), now()),
    (uuid_v7(), 'RIGOR',             'Rigor',             'Exame com 20/20',             50, '{"kind":"exam_perfect"}', now(), now());
