# Lúmen Lab — Especificação do produto (`spec.md`)

> **Estado:** v1.0 · derivada do design aprovado em 01/11/2026 (canvas «Lúmen Pregão — Design»).
> **Nome do produto:** **Lúmen Lab** (decidido a 02/11/2026; antes «Lúmen Pregão»). Os identificadores técnicos
> (pacote `ao.lumen.pregao`, serviços `lumen-pregao-*`, repositório) mantêm o nome antigo.
> **Regra deste documento:** tudo o que não está decidido aparece como **«a confirmar»**. Não se inventa legislação, números de diplomas, estatísticas nem dados institucionais.

---

## 1. Visão geral

### 1.1 Contexto
Iniciativa da Associação de Estudantes do ISPTEC — Chapa D, Lúmen (mandato 2026–2028). Concretiza o pilar 03, «Prestígio em todos os cursos», ponto DCSA: maratona de casos empresariais, oficinas de Power BI e de fiscalidade angolana. Liga-se ao GAP Digital (pilar 01).

### 1.2 Problema
Os estudantes do DCSA estudam teoria sem um ambiente onde praticar, com correcção imediata, tarefas profissionais: lançamentos, demonstrações financeiras, cálculo de impostos, análise de dados. Faltam ainda preparação para os processos selectivos da banca e das Big 4 e um palco competitivo regular.

### 1.3 Público-alvo
Estudantes de Contabilidade, Economia e Gestão Empresarial do ISPTEC, do 1.º ao 5.º ano. Mais tarde, outros cursos.

### 1.4 Objectivos
- **Praticar:** aplicar conhecimentos em situações realistas, com correcção no servidor.
- **Fiscalidade:** praticar contabilidade (PGC) e fiscalidade angolana.
- **Interpretar:** ler demonstrações financeiras e rácios.
- **Competir:** organizar competições individuais e por equipas (a Maratona).
- **Portefólio:** construir um portefólio de competências apresentável a recrutadores.

### 1.5 Não-objectivos
- **LMS genérico:** não é um sistema de gestão de cursos nem substitui aulas.
- **Dinheiro real:** não há dinheiro real, transacções reais nem dados de mercado em tempo real.
- **Canal oficial:** não é canal oficial do ISPTEC enquanto não houver acordo institucional.
- **Castigos automáticos:** não há castigos automáticos por suspeita de batota.
- **Conteúdo de terceiros:** não reproduz livros, manuais nem enunciados protegidos.

### 1.6 Proposta de valor
«Aprender não é apenas estudar a teoria. É saber aplicar, analisar, resolver e demonstrar competências.»

### 1.7 Rodapé institucional (obrigatório em todas as páginas)
«Iniciativa da Associação de Estudantes · Não é um canal oficial do ISPTEC».

---

## 2. Glossário

| Termo | Definição |
|---|---|
| **PGC** | Plano Geral de Contabilidade aplicável em Angola. O diploma de aprovação e a versão estão **a confirmar** por revisor. |
| **IVA** | Imposto sobre o Valor Acrescentado. Base: Código do IVA, Lei n.º 7/19; versão em vigor **a confirmar**. |
| **Balancete** | Lista das contas com os totais a débito e a crédito e os saldos. Verifica o equilíbrio. |
| **Demonstrações financeiras** | Balanço, demonstração de resultados e demonstração de fluxos de caixa. |
| **Rácio** | Indicador calculado a partir das demonstrações (ex.: liquidez geral, endividamento). |
| **Trilha** | Percurso de aprendizagem por área, composto por módulos. |
| **Módulo** | Grupo ordenado de salas dentro de uma trilha. |
| **Sala** | Unidade prática com contexto, materiais e uma lista de tarefas. |
| **Tarefa** | Passo de uma sala, ligado a um exercício. |
| **Exercício** | Definição avaliável (tipo, parâmetros, solução, tolerância, referência normativa). Pode ser paramétrico. |
| **Tentativa** | Instância de um exercício para um utilizador ou equipa, com os parâmetros gerados guardados. |
| **Submissão** | Resposta enviada numa tentativa. Tem chave de idempotência e estado de processamento. |
| **Contest** | Competição com janela temporal controlada pelo servidor (ex.: a Maratona). |
| **Ranking** | Classificação por pontos (geral, curso, ano, época, equipas). |
| **Congelamento** | Período final do contest em que o ranking público deixa de ser actualizado. |
| **Época** | Período competitivo usado para rankings sazonais. |

---

## 3. Perfis e permissões (RBAC)

### 3.1 Perfis
`STUDENT`, `TEAM_CAPTAIN` (contextual a uma equipa), `AUTHOR`, `REVIEWER`, `JURY`, `CONTEST_ORGANIZER`, `ADMIN`.

Um utilizador pode ter vários perfis. `TEAM_CAPTAIN` não é global: deriva de `TeamMember.role = CAPTAIN`.

### 3.2 Matriz
Legenda: R = ler · C = criar · E = editar · A = aprovar · P = publicar ou administrar.

| Recurso | Estudante | Autor | Revisor | Júri | Organizador | Admin |
|---|---|---|---|---|---|---|
| Trilhas, salas (resolver) | R | R | R | — | — | R |
| Exercícios | — | C E (próprios, em rascunho) | R A | — | — | A |
| Publicar conteúdo | — | — | P | — | — | P |
| Contests | R (inscrever) | — | — | R (atribuídos) | C E P | C E P |
| Correcção de casos abertos | — | — | — | E (atribuídos) | R | R |
| Utilizadores e perfis | — | — | — | — | — | C E |
| Auditoria | — | — | — | — | — | R |

### 3.3 Regras
- **R-AUTH-1:** todas as permissões são verificadas no backend em cada endpoint. A interface esconde o que não é permitido, mas isso nunca é mecanismo de segurança.
- **R-AUTH-2:** a propriedade é verificada à parte. Um autor só edita os seus rascunhos; um membro de equipa só vê as submissões da sua equipa; um júri só vê os casos que lhe foram atribuídos.
- **R-AUTH-3:** `REVIEWER`, `JURY`, `CONTEST_ORGANIZER` e `ADMIN` exigem segundo factor ao iniciar sessão.
- **R-AUTH-4:** um autor não pode aprovar um exercício de que seja autor.

---

## 4. Funcionalidades

> Formato: objectivo · fluxo · regras · estados · validações · permissões · critérios de aceitação (CA).
> Fase: **[MVP]** ou **[Posterior]**.

### 4.1 Autenticação e conta [MVP]
- **Objectivo:** acesso exclusivo para estudantes do ISPTEC.
- **Fluxo:** registo (nome, email institucional, curso, ano, palavra-passe, consentimento) → email de verificação → primeira entrada → painel inicial.
- **Regras:**
  - O domínio institucional permitido é **a confirmar** e configurável por variável de ambiente.
  - A palavra-passe tem no mínimo 12 caracteres e é guardada com Argon2id (preferido) ou bcrypt.
  - A sessão vive num cookie `httpOnly`, `Secure`, `SameSite=Lax`, com renovação.
  - Os links de verificação expiram ao fim de 24 h (**a confirmar**).
- **Estados da conta:** `PENDING_VERIFICATION`, `ACTIVE`, `SUSPENDED`, `DELETION_REQUESTED`.
- **CA:**
  1. Um registo com domínio não permitido é recusado com 422.
  2. Um login com credenciais erradas devolve uma mensagem genérica, que não revela se o email existe.
  3. Depois de 5 falhas, a conta ou o IP ficam limitados durante 15 min (**a confirmar**).
  4. Os perfis privilegiados sem 2.º factor não chegam a ter sessão activa.
  5. O utilizador pode exportar os seus dados e pedir a eliminação da conta.

### 4.2 Painel inicial do estudante [MVP]
- **Conteúdo:** pontuação, sequência de dias, posição no ranking do curso, salas concluídas, trilhas em curso, salas recomendadas, próximo contest e actividade recente.
- **Regras:**
  - Recomendação: a próxima sala por concluir da trilha activa, mais salas de temas com erros recentes.
  - Sequência: há prática num dia se houver pelo menos uma tarefa submetida nesse dia, no fuso Africa/Luanda.
- **CA:** os valores coincidem com os da API do ranking e do progresso; o estado vazio aparece para utilizadores novos.

### 4.3 Trilhas e catálogo [MVP]
- **Conteúdo de cada trilha:**
  - descrição e objectivos;
  - dificuldade (1–4);
  - competências;
  - módulos ordenados;
  - requisitos de conclusão (ex.: todas as salas ≥ 70 % e exame final ≥ 10/20, **a confirmar** por trilha);
  - percentagem de conclusão.
- **Catálogo:** pesquisa e filtros por curso, tema e dificuldade.
- **Estados de uma sala para o utilizador:** `LOCKED`, `AVAILABLE`, `IN_PROGRESS`, `COMPLETED`. O desbloqueio depende de pré-requisitos definidos na sala.
- **CA:**
  1. Uma sala `LOCKED` devolve 403 nos endpoints de tentativa, e não apenas na interface.
  2. A percentagem da trilha é igual a pontos obtidos ÷ pontos possíveis das salas publicadas.

### 4.4 Salas práticas e tarefas [MVP]
- **Conteúdo de cada sala:**
  - título, descrição e objectivos;
  - dificuldade e tempo estimado;
  - materiais e contexto;
  - tarefas ordenadas;
  - pontuação e resultado.
- **Fluxo de uma tarefa:**
  1. pedir tentativa (o servidor gera os parâmetros);
  2. resolver;
  3. submeter;
  4. a correcção chega do servidor;
  5. feedback e pontos;
  6. próxima tarefa.
- **Regras:**
  - Há no máximo 3 tentativas por tarefa (configurável).
  - Cada tentativa falhada retira 5 pts e cada dica usada retira 5 pts (ver §7).
  - O rascunho é guardado automaticamente.
  - O feedback após a submissão segue a política do exercício: só certo/errado, ou detalhe por linha.
- **Tipos de tarefa e validação** (sempre no servidor):

| Tipo | Validação |
|---|---|
| A · Escolha múltipla | Conjunto exacto de opções correctas; suporta uma ou várias. |
| B · Numérica | Valor ± tolerância (absoluta ou relativa); regra de arredondamento; precisão. |
| C · Lançamentos | Contas existentes no PGC da versão do exercício; contas certas por linha; equilíbrio débito = crédito; valores ± tolerância; a ordem das linhas é irrelevante. |
| D · Demonstrações | Valor por rubrica ± tolerância; relações (ex.: Activo = Capital próprio + Passivo); classificação das contas. |
| E · Cálculo fiscal | Passo a passo; cada passo é validado antes de desbloquear o seguinte; fundamentação ligada à referência. |
| F · Rácios | Valor ± tolerância e resposta justificada (texto) avaliada por regra ou por revisor. |
| G · Caso aberto | Texto e anexos; correcção por docente ou júri com grelha. [Posterior no MVP de salas; usado na fase final do contest] |

- **Editor de lançamentos (design aprovado):**
  - pesquisa de contas por código ou designação com sugestões;
  - linhas com conta, débito, crédito e remoção;
  - totais em rodapé;
  - antes de submeter, o cliente só valida o formato e o equilíbrio;
  - depois da correcção, mostra o estado por linha.
- **Editor de demonstrações:** rubricas agrupadas, campos em Kz, totais e relações recalculados no cliente apenas para orientar.
- **CA:**
  1. A resposta de `GET` de uma tentativa nunca inclui solução, tolerância secreta nem regras de geração.
  2. Reenviar uma submissão com a mesma `Idempotency-Key` devolve o resultado original e não cria nova submissão.
  3. A correcção pode ser reproduzida a partir dos parâmetros guardados.

### 4.5 Exercícios paramétricos [MVP]
- **Regras:**
  - Os parâmetros são definidos por regra: intervalo com passo, conjunto, lista de entidades aprovadas (ex.: empresas fictícias) ou constante ligada a uma referência normativa.
  - A geração é feita no servidor com uma semente por tentativa.
  - A semente e os valores ficam guardados na tentativa.
  - A solução é uma expressão sobre os parâmetros, avaliada no servidor.
- **CA:** dois estudantes podem receber valores diferentes; a mesma tentativa reavaliada dá sempre o mesmo resultado.

### 4.6 Exames [MVP]
- **Características:**
  - modo concentração, sem navegação lateral;
  - cronómetro;
  - banco de perguntas com sorteio por tema e dificuldade;
  - navegação entre perguntas e marcação para rever;
  - gravação de cada resposta ao alterar;
  - confirmação de submissão;
  - correcção automática;
  - relatório com pontos fortes e fracos.
- **Regras:**
  - O início e o fim oficiais são do servidor; `endsAt` é fixado no arranque.
  - As respostas recebidas depois de `endsAt` (com tolerância de rede de 2 s, **a confirmar**) são rejeitadas.
  - No fim do tempo, a submissão é automática.
- **Estados:** `NOT_STARTED`, `IN_PROGRESS`, `SUBMITTED`, `EXPIRED_AUTO_SUBMITTED`, `GRADED`.
- **CA:** mudar o relógio do cliente não altera o prazo; a submissão automática corrige as respostas guardadas.

### 4.7 Contests — a Maratona [MVP: um contest funcional; júri em Posterior]
- **Características:**
  - participação individual ou por equipas de 2 a 5;
  - inscrição e confirmação dos membros;
  - sala de espera e contagem decrescente;
  - libertação dos enunciados pelo servidor;
  - variantes por equipa;
  - submissões com estados;
  - ranking ao vivo e congelamento;
  - fase final com caso aberto, júri e grelha pública;
  - anúncio de resultados.
- O ciclo de vida está em §8; as regras de pontuação em §7.
- **Regras:**
  - Os membros ficam bloqueados N min antes do início (N **a confirmar**; por defeito 30).
  - Durante o congelamento, cada equipa continua a ver os seus próprios resultados.
- **CA:** ver §10 (concorrência).

### 4.8 Rankings e progressão [MVP]
- **Classificações:** geral, por curso, por ano, por época e de equipas.
- **Complementos:** pontuação, sequência de dias, medalhas e conquistas, histórico.
- **Visibilidade do nome no ranking:** nome, pseudónimo ou oculto.
- **Regras:**
  - O pseudónimo é único, com 3 a 24 caracteres `[a-z0-9_]`, e passa por um filtro de linguagem ofensiva.
  - As regras de pontuação ficam públicas numa página.
- **CA:** o ranking de leitura vem do Redis; os pontos são persistidos em PostgreSQL e o Redis é reconstruível a partir deles.

### 4.9 Perfil e portefólio [MVP: perfil; certificados Posterior]
- **Conteúdo:**
  - dados académicos essenciais;
  - trilhas e salas concluídas;
  - competências com evidências (n.º de salas e exames);
  - exames e contests;
  - melhores posições;
  - conquistas;
  - certificados [Posterior];
  - histórico.
- **Portefólio público:** opt-in, com link partilhável e pré-visualização «como recrutador».
- **Certificados:**
  - têm código de verificação;
  - enquanto não houver acordo institucional, indicam «iniciativa da Associação de Estudantes».

### 4.10 Simulação de empresa [Posterior]
- **Recebido pela equipa:** um balancete inicial e um mês de operações (compras, vendas, IVA, salários, amortizações, outras).
- **Ambiente:** calendário, operações pendentes, área de trabalho (lançamentos), balancete e painel de acompanhamento.
- **Fecho do período, em 4 passos:**
  1. validar movimentos;
  2. demonstrações financeiras;
  3. apuramento de impostos;
  4. submeter (irreversível, com confirmação dupla).
- **Regras:**
  - As operações ficam disponíveis por data simulada.
  - O fecho só fica activo com todas as operações registadas.
  - As taxas de retenção (IRT, segurança social) estão **a confirmar**.

### 4.11 Mercado simulado [Posterior]
- **Conteúdo:**
  - empresas fictícias com relatórios e contas simulados por trimestre;
  - indicadores e gráficos;
  - carteira virtual;
  - decisões (comprar, vender, manter) com justificação obrigatória (≥ 120 caracteres);
  - histórico de decisões e resumo do desempenho.
- **Regras:**
  - Os períodos (`MarketPeriod`) são libertados pelo servidor; os dados futuros nunca vão para o cliente.
  - Faixa obrigatória na interface: «Simulação educativa · empresas fictícias · sem dinheiro real».

### 4.12 Simulação de processos selectivos [Posterior]
Casos com tempo limite, submissão de conclusões e avaliação por critérios. Reutiliza o Contest no modo individual e a correcção do tipo G.

### 4.13 Laboratório de análise de dados [Posterior]
- **O que o estudante faz:** explora datasets, escolhe indicadores, constrói gráficos (Apache ECharts), compara e interpreta.
- **Datasets:** só de empresas fictícias aprovadas.

### 4.14 Autoria e fluxo editorial [MVP: criar e aprovar; alertas legais MVP]
- **Fluxo:** Rascunho → Revisão → Aprovação → Publicação. O conteúdo não aprovado nunca é servido a estudantes.
- **Editor:**
  - tipo de tarefa;
  - enunciado com `[parâmetros]`;
  - parâmetros e regras de geração;
  - solução e tolerâncias;
  - metadados (tema, dificuldade, pontos);
  - referência normativa (fonte, artigos, versão, data de consulta, validade);
  - versões;
  - pré-visualização como estudante.
- **Alertas legais:** quando um revisor marca nova versão de uma `LegalSource`, todos os exercícios ligados passam a `NEEDS_REVIEW` e saem de publicação, ou ficam com aviso, conforme a política (**a confirmar**).
- **CA:** publicar sem referência normativa obrigatória falha; o autor não aprova os seus exercícios; cada transição fica em auditoria.

### 4.15 Júri [Posterior]
- **Área do júri:**
  - fila de casos atribuídos;
  - submissão anónima com anexos;
  - grelha pública (critérios, descrição, peso, nota 0–4, comentário);
  - pontuação ponderada calculada;
  - comentário geral visível após os resultados;
  - confirmação registada em auditoria.
- **Indicadores de anomalia:** são mostrados ao júri como sinais e nunca aplicam castigo automático.

### 4.16 Administração [MVP: utilizadores, perfis, contests, auditoria]
Utilizadores, perfis e permissões, conteúdos, trilhas, contests, auditoria (pesquisa e exportação) e configurações gerais.

### 4.17 Estados globais [MVP]
Todos os ecrãs implementam:
- loading e skeleton;
- vazio e sem resultados;
- erro com código;
- acesso negado;
- sessão expirada (com o rascunho preservado);
- submissão em processamento e rejeitada;
- falha de ligação;
- sucesso;
- confirmação de operações críticas (texto de confirmação nas destrutivas).

---

## 5. Modelo de domínio (JPA)

Convenções:
- `id` UUID (v7) em todas as entidades;
- `createdAt` e `updatedAt` em `Instant` (UTC);
- `@Version long version` onde indicado (★);
- dinheiro em `BigDecimal` com escala 2 (Kz) e arredondamento `HALF_UP` (**a confirmar** por exercício);
- enums guardados como `VARCHAR`.

| Entidade | Responsabilidade | Atributos essenciais | Relações | Restrições e integridade |
|---|---|---|---|---|
| **User** | Conta | email (único, minúsculas), passwordHash, name, status, courseId, year, pseudonym (único, opcional), rankingVisibility (NAME/PSEUDONYM/HIDDEN), mfaSecret (cifrado), consentAt | N:N Role; N:1 Course | O domínio do email é validado no registo |
| **Role** | Perfil RBAC | code (único) | N:N User | Os 7 perfis são semeados por migração |
| **Course** | Curso | code, name | 1:N User | Contabilidade, Economia, Gestão (nomes oficiais **a confirmar**) |
| **Track** | Trilha | slug, title, description, difficulty, competencies[], completionRule (JSON), status | 1:N Module | Publicada só se todos os módulos estiverem publicados |
| **Module** | Módulo | trackId, position, title | 1:N Room | `(trackId, position)` único |
| **Room** | Sala | moduleId, position, title, objectives[], difficulty, estMinutes, context (markdown), materials[], prerequisites[] | 1:N Task | |
| **Task** | Passo da sala | roomId, position, exerciseVersionId, points | N:1 ExerciseVersion | `(roomId, position)` único |
| **Exercise** | Identidade do exercício | code (ex.: EX-0142), authorId, topic | 1:N ExerciseVersion | |
| **ExerciseVersion** | Definição imutável publicada | exerciseId, number, type (A–G), statement, paramSpec (JSON), solutionSpec (JSON, nunca exposto), tolerance, feedbackPolicy, difficulty, points, editorialStatus, validUntil | N:N Reference; 1:1 ReviewWorkflow | Imutável depois de PUBLISHED; alterar cria nova versão |
| **Reference** | Ligação exercício ↔ fonte | exerciseVersionId, legalSourceId, articles, consultedAt | N:1 LegalSource | |
| **LegalSource** | Diploma, norma ou obra | title, identifier (ex.: «Lei n.º 7/19»), version, publishedAt, effectiveFrom/To, url, confirmed (bool) | 1:N Reference | `confirmed=false` mostra «a confirmar» |
| **Attempt** ★ | Instância de exercício | exerciseVersionId, userId? / teamId?, contestId?, seed, params (JSON), status, attemptsUsed, score | 1:N Submission | Exactamente um entre userId e teamId |
| **Submission** | Resposta enviada | attemptId, idempotencyKey, payload (JSON), receivedAt (servidor), status (QUEUED/PROCESSING/GRADED/REJECTED/FAILED), result (JSON), points | N:1 Attempt | `(attemptId, idempotencyKey)` único |
| **Exam** | Definição de exame | title, durationMin, questionPoolRule (JSON), passMark | 1:N ExamSession | |
| **ExamSession** ★ | Exame de um utilizador | examId, userId, startedAt, endsAt, status, answers (JSON), grade | | `endsAt` é fixado pelo servidor no início |
| **Contest** ★ | Competição | title, mode (INDIVIDUAL/TEAM), minTeam, maxTeam, registrationOpensAt/ClosesAt, startsAt, freezeAt, endsAt, finalPhase (JSON), state, rubricId | 1:N Team, 1:N ContestProblem | Datas monotónicas: abertura ≤ fecho ≤ início < congelamento < fim |
| **ContestProblem** | Problema do contest | contestId, label, exerciseVersionId, points | | |
| **Team** ★ | Equipa | contestId, name, lockedAt | 1:N TeamMember | `(contestId, name)` único |
| **TeamMember** | Membro | teamId, userId, role (CAPTAIN/MEMBER), confirmedAt | | Um utilizador só pode estar numa equipa por contest |
| **Scoreboard** ★ | Pontuação persistida por participante | contestId, participantType, participantId, points, penaltyTime, lastCorrectAt, frozenSnapshot (JSON) | | Fonte de verdade; o Redis é a projecção |
| **Rubric / RubricCriterion** | Grelha de avaliação | name, criteria (name, description, weight, scale) | | Soma dos pesos = 100 % |
| **JuryAssessment** ★ | Avaliação de um caso | submissionId, juryUserId, scores (JSON), comment, confirmedAt | | Imutável depois de confirmada (correcções criam nova linha com motivo) |
| **Company** | Empresa fictícia | name, sector, fictional=true, description | 1:N MarketPeriod | `fictional` obrigatoriamente `true` |
| **MarketPeriod** | Trimestre simulado | companyId, seasonId, index, releasedAt, financials (JSON) | | Não é servido antes de `releasedAt` |
| **Portfolio** | Carteira virtual | userId, seasonId, cash, holdings (JSON) | 1:N Decision | Só valores virtuais |
| **Decision** | Decisão de investimento | portfolioId, periodId, action, quantity, justification | | Justificação ≥ 120 caracteres |
| **Season** | Época competitiva | name, startsAt, endsAt | | |
| **PointEvent** | Registo imutável de pontos | userId, teamId?, source (TASK/EXAM/STREAK/BADGE/CONTEST), sourceId, delta, at | | Base dos rankings; nunca é actualizado, só compensado |
| **Badge / UserBadge** | Conquistas | code, title, rule | | |
| **Certificate** | Certificado [Posterior] | userId, trackId, issuedAt, verificationCode | | `verificationCode` único |
| **ReviewWorkflow** | Estado editorial | exerciseVersionId, status (DRAFT/IN_REVIEW/APPROVED/PUBLISHED/NEEDS_REVIEW/ARCHIVED), reviewerId, notes | | Transições validadas em serviço |
| **AuditLog** | Auditoria | actorId, action, entityType, entityId, before/after (JSON), ip, at | | Só inserção; nunca editável pela aplicação |

Notas:
- `Scoreboard` é a pontuação persistida do contest. Os rankings gerais derivam de `PointEvent`, agregado por âmbito e materializado no Redis.
- `ExerciseVersion` imutável garante que tentativas antigas são reproduzíveis.

---

## 6. API REST (resumo; contrato completo em OpenAPI)

- **Base:** `/api/v1` · JSON · erros em `application/problem+json` (RFC 9457) com `code` estável.
- **Autenticação:** cookie de sessão. As escritas exigem token CSRF (cabeçalho `X-CSRF-Token`).
- **Erros comuns:** 400 validação · 401 sem sessão · 403 sem permissão ou sala bloqueada · 404 · 409 conflito de versão ou estado · 422 regra de negócio · 429 rate limit.

### 6.1 Autenticação
| Método | Caminho | Objectivo | Perm. |
|---|---|---|---|
| POST | `/auth/register` | Criar conta (`name, email, courseId, year, password, consent`) | público |
| POST | `/auth/verify` | Confirmar email (`token`) | público |
| POST | `/auth/login` | Entrar (`email, password`) → 204 + cookie, ou `mfa_required` | público |
| POST | `/auth/mfa` | 2.º factor (`code`) | sessão parcial |
| POST | `/auth/logout` | Terminar sessão | autenticado |
| GET | `/me` | Perfil, perfis, preferências | autenticado |
| PATCH | `/me` | Pseudónimo, visibilidade, tema | autenticado |
| GET | `/me/export` · POST `/me/deletion-request` | Dados pessoais | autenticado |

### 6.2 Aprendizagem
| Método | Caminho | Objectivo |
|---|---|---|
| GET | `/tracks?course=&topic=&difficulty=&q=` | Catálogo com progresso do utilizador |
| GET | `/tracks/{slug}` | Trilha, módulos, estado das salas |
| GET | `/rooms/{id}` | Sala (contexto, materiais, tarefas, estado) |
| POST | `/tasks/{id}/attempts` | Criar ou obter tentativa → `{attemptId, params visíveis, statementRendered}` (403 se bloqueada) |
| PUT | `/attempts/{id}/draft` | Guardar rascunho |
| POST | `/attempts/{id}/submissions` | Submeter (cabeçalho `Idempotency-Key` obrigatório) → 202 `{submissionId, status}` |
| GET | `/submissions/{id}` | Estado e resultado |
| POST | `/attempts/{id}/hints` | Revelar dica (aplica penalização) |
| GET | `/pgc/accounts?q=&version=` | Pesquisa de contas do PGC da versão do exercício |

### 6.3 Exames
| Método | Caminho | Objectivo |
|---|---|---|
| POST | `/exams/{id}/sessions` | Iniciar → `{sessionId, endsAt, serverNow}` |
| PUT | `/exam-sessions/{id}/answers/{q}` | Gravar resposta (rejeita após `endsAt`) |
| POST | `/exam-sessions/{id}/submit` | Submeter |
| GET | `/exam-sessions/{id}/report` | Relatório |

### 6.4 Contests
| Método | Caminho | Objectivo | Perm. |
|---|---|---|---|
| GET | `/contests` · `/contests/{id}` | Lista e detalhe (enunciados só depois de `startsAt`) | autenticado |
| POST | `/contests/{id}/teams` | Criar equipa (criador = capitão) | estudante |
| POST | `/teams/{id}/invites` · POST `/teams/{id}/join` | Convidar e aceitar (≤ maxTeam, antes de `lockedAt`) | capitão / convidado |
| GET | `/contests/{id}/problems` | Problemas e variantes da equipa | membro inscrito, estado RUNNING |
| POST | `/contests/{id}/problems/{label}/submissions` | Submeter (Idempotency-Key) | membro inscrito |
| GET | `/contests/{id}/scoreboard` | Ranking (congelado para terceiros quando FROZEN) | autenticado |
| GET | `/contests/{id}/events` | SSE: `clock`, `state`, `scoreboard`, `submission` | membro / público |
| POST/PATCH | `/admin/contests` · `/admin/contests/{id}` | Criar e editar | organizador |
| POST | `/admin/contests/{id}/publish-results` | Publicar resultados | organizador |

### 6.5 Autoria, júri, administração
| Método | Caminho | Objectivo | Perm. |
|---|---|---|---|
| POST/PATCH | `/authoring/exercises` · `/authoring/exercises/{id}/versions/{n}` | Criar e editar rascunho | autor (próprio) |
| POST | `/authoring/versions/{id}/preview` | Gerar exemplo com parâmetros | autor/revisor |
| POST | `/authoring/versions/{id}/transition` (`to`) | Fluxo editorial | autor → IN_REVIEW; revisor → APPROVED/PUBLISHED |
| GET/POST | `/legal-sources` · PATCH `/legal-sources/{id}` | Fontes; nova versão gera alertas | revisor |
| GET | `/jury/assignments` · GET `/jury/submissions/{id}` | Fila e caso | júri (atribuído) |
| POST | `/jury/submissions/{id}/assessment` | Avaliar | júri (atribuído) |
| GET/PATCH | `/admin/users` · `/admin/users/{id}/roles` | Utilizadores e perfis | admin |
| GET | `/admin/audit?actor=&entity=&from=&to=` | Auditoria | admin |

---

## 7. Regras de pontuação e ranking

> Valores iniciais propostos; **todos a confirmar** pela organização e publicados antes de cada época.

| Acção | Pontos |
|---|---|
| Tarefa correcta | pontos da tarefa (definidos no exercício) |
| Tentativa falhada | −5 (o mínimo da tarefa é 0) |
| Dica usada | −5 |
| Exame de módulo | nota (0–20) × 6 |
| Sequência de 7 dias | +50 |
| Conquista | conforme a conquista |

**Contest (fase 1):**
- Cada problema certo soma os seus pontos.
- Penalização de tempo: tempo desde `startsAt` até à resposta correcta, mais 10 min por submissão errada anterior (**a confirmar**).
- Desempate, por esta ordem:
  1. mais pontos;
  2. menor penalização acumulada;
  3. `lastCorrectAt` mais cedo.

**Fase final:** soma ponderada da grelha do júri; a combinação com a fase 1 é **a confirmar**.

**Congelamento:**
- A partir de `freezeAt`, `GET scoreboard` devolve `frozenSnapshot` a não-membros.
- Cada participante vê a sua própria pontuação real.
- Em `RESULTS_PUBLISHED` o snapshot é substituído.

**Auditoria:** cada `PointEvent` e cada alteração de `Scoreboard` ficam registados; recalcular a partir de `Submission` e `PointEvent` deve dar o mesmo resultado.

---

## 8. Ciclo de vida de um contest

```
DRAFT → REGISTRATION → WAITING_ROOM → RUNNING → FROZEN → SUBMISSIONS_CLOSED → FINAL_EVALUATION → RESULTS_PUBLISHED → CLOSED
```

| Transição | Condição | Quem |
|---|---|---|
| DRAFT → REGISTRATION | Organizador publica; datas válidas; problemas aprovados | Organizador |
| REGISTRATION → WAITING_ROOM | `now ≥ registrationClosesAt` | **Servidor** (agendador) |
| WAITING_ROOM → RUNNING | `now ≥ startsAt`; enunciados libertados | **Servidor** |
| RUNNING → FROZEN | `now ≥ freezeAt` | **Servidor** |
| FROZEN → SUBMISSIONS_CLOSED | `now ≥ endsAt` | **Servidor** |
| SUBMISSIONS_CLOSED → FINAL_EVALUATION | Fila de correcção vazia; fase final configurada | **Servidor** / Organizador |
| FINAL_EVALUATION → RESULTS_PUBLISHED | Todas as avaliações confirmadas | Organizador |
| RESULTS_PUBLISHED → CLOSED | Manual | Organizador |

As transições temporais são executadas por um agendador idempotente que compara com o relógio do servidor (UTC, sincronizado por NTP). O cliente apenas apresenta.

---

## 9. Segurança (requisitos verificáveis)

Referência: OWASP ASVS nível 2.

| ID | Requisito | Verificação |
|---|---|---|
| SEC-01 | Palavras-passe com Argon2id; mínimo de 12 caracteres | Teste unitário + revisão de configuração |
| SEC-02 | Cookie de sessão `httpOnly; Secure; SameSite=Lax`; rotação no login | Teste de integração aos cabeçalhos |
| SEC-03 | CSRF em todas as escritas | Teste: POST sem token → 403 |
| SEC-04 | 2.º factor obrigatório para REVIEWER, JURY, CONTEST_ORGANIZER, ADMIN | Teste de fluxo |
| SEC-05 | Autorização por endpoint (`@PreAuthorize`) + propriedade em serviço | Testes de permissões por perfil (matriz §3.2) |
| SEC-06 | Nenhum DTO de estudante expõe `solutionSpec`, `paramSpec` nem tolerâncias secretas | Teste de serialização + contrato OpenAPI |
| SEC-07 | Rate limiting: login (5/15 min por conta e IP), submissões (ex.: 10/min por utilizador, **a confirmar**) | Teste de carga leve |
| SEC-08 | Validação Bean Validation em todos os DTOs; sanitização de markdown | Testes |
| SEC-09 | Uploads: lista de tipos (PDF, XLSX, PNG, JPG), ≤ 10 MB (**a confirmar**), verificação por magic bytes, nome gerado, fora da raiz web | Testes |
| SEC-10 | Erros sem stack trace nem detalhes internos | Teste |
| SEC-11 | Auditoria das operações listadas no briefing §7.5 | Testes por operação |
| SEC-12 | Segredos só por variáveis de ambiente; nada no repositório | Verificação no CI (gitleaks) |
| SEC-13 | Protecção de dados: minimização, consentimento, exportação, eliminação. Aplicabilidade da Lei n.º 22/11 **a confirmar** | Testes de fluxo |

---

## 10. Concorrência e integridade

| ID | Regra técnica | Critério de aceitação |
|---|---|---|
| CON-01 | O relógio do servidor é a única referência; respostas incluem `serverNow` | Mudar o relógio do cliente não altera prazos (teste E2E) |
| CON-02 | `Idempotency-Key` obrigatória em submissões; índice único `(attemptId, key)` | 100 pedidos idênticos em paralelo → 1 submissão |
| CON-03 | `@Version` em Team, Scoreboard, Contest, Attempt, ExamSession | Actualizações concorrentes → um sucesso, os outros 409 ou nova tentativa controlada |
| CON-04 | Fluxo: submissão persistida (QUEUED) → worker corrige → `PointEvent` + Scoreboard na mesma transacção → projecção no Redis → difusão por SSE | Matar o worker a meio não perde pontos; ao reiniciar, a submissão é reprocessada |
| CON-05 | Redis é projecção; reconstrução a partir de PostgreSQL | Apagar o Redis e reconstruir dá o mesmo ranking |
| CON-06 | Submissões com `receivedAt > endsAt` são rejeitadas, seja qual for o horário enviado pelo cliente | Teste com timestamp forjado |
| CON-07 | Transacções curtas; nenhuma chamada externa dentro de transacção | Revisão de código |
| CON-08 | Threads virtuais (Java 21) para I/O; pool de ligações dimensionado e limitado | Teste de carga sem esgotar ligações |
| CON-09 | Testes de carga com k6: submissões simultâneas no fim do contest | Sem duplicados, sem pontos perdidos, ranking consistente; metas quantitativas **a confirmar** depois de conhecida a infraestrutura |

**SSE ou WebSocket:** escolhe-se **SSE**. O fluxo é servidor → cliente (relógio, estado, ranking, estado de submissões); as submissões seguem por REST com idempotência. O SSE reconecta-se nativamente, passa por proxies HTTP e simplifica a autorização por cookie. Se surgir colaboração em tempo real entre membros da equipa, reavalia-se WebSocket.

---

## 11. Roadmap

### MVP
1. Autenticação, perfis, RBAC, auditoria.
2. Trilha «Contabilidade Financeira segundo o PGC» (salas e exercícios dos tipos A–F).
3. Conteúdos de IVA (calculadora passo a passo).
4. Exercícios paramétricos e fluxo editorial com referências normativas.
5. Exames.
6. Rankings (geral, curso, ano, época, equipas) e painel inicial.
7. Um contest funcional (fase 1 automática, congelamento, resultados).
8. Estados globais, modo claro e escuro, acessibilidade WCAG 2.1 AA.

### Fases posteriores
Simulação de empresa · mercado simulado · laboratório de dados · processos selectivos · avaliação por júri (caso aberto) · certificados · expansão para outros cursos.

### Fora de âmbito até decisão
Integração com sistemas do ISPTEC (depende de acordo institucional); pagamentos de qualquer tipo.

---

## Anexo A — Itens «a confirmar»
- Domínio de email institucional.
- Diploma e versão do PGC; estrutura de subcontas do IVA (34.5.x).
- Versão em vigor e artigos do Código do IVA (Lei n.º 7/19), do Imposto Industrial (Lei n.º 26/20), do IRT (Lei n.º 28/20), do CGT (Lei n.º 21/14), da LSC (Lei n.º 1/04), do CVM (Lei n.º 22/15), da LGT (Lei n.º 12/23) e da LPDP (Lei n.º 22/11).
- Aplicabilidade das IAS/IFRS e instruções do BNA.
- Taxas usadas em exercícios (IVA, retenções).
- Valores de pontuação, penalizações, congelamento e bloqueio de equipas.
- Limites de rate limiting, de upload e de expiração de links.
- Metas de desempenho e concorrência.
- Nomes oficiais dos cursos.
- Combinação entre a fase 1 e a fase final.

---

## Anexo B — Adenda de implementação (01/11/2026)

Registo do que a implementação do MVP acrescentou ou decidiu onde a spec era omissa. Cada ponto é **a confirmar**
pela equipa; o contrato completo está em `docs/openapi.json` (verificado no CI).

### B.1 Modelo de domínio (complementa §5)
- **ExerciseVersion:** `presentation` (JSON visível ao estudante: opções, passos, rubricas, balancete), `hints` (só
  reveladas a pedido), `maxAttempts` (por defeito 3), `title`, `changeNote`. O estado editorial vive em `ReviewWorkflow`
  (não é duplicado na versão); `ReviewWorkflow.legalAlertAt` marca alertas legais.
- **Attempt:** `taskId` ou `contestProblemId` (exactamente um), `exerciseType`, `hintsUsed`, `draft`.
  `attemptsUsed` conta **tentativas falhadas**; no tipo E, passos certos mas incompletos não contam.
- **Submission:** `correct`, `errorCode`, `retries` (reprocessamento automático em caso de falha).
- **Team.individual:** num contest individual cada participante tem uma «equipa» de 1; o Scoreboard é sempre por equipa.
- **TeamInvite** (convites pendentes) e **practice_days** (dias com prática, para as sequências).
- **User:** `theme` (DARK/LIGHT), `portfolioPublic`, `portfolioHandle`, `mfaEnabled`.

### B.2 Endpoints acrescentados (complementa §6)
| Método | Caminho | Objectivo | Perm. |
|---|---|---|---|
| GET | `/auth/csrf` | Emite o token CSRF (cookie `XSRF-TOKEN`) | público |
| POST | `/auth/resend-verification` | Reenviar email (resposta genérica, 1 por 45 s) | público |
| GET | `/courses` | Cursos para o registo | público |
| GET | `/me/summary` · `/me/dashboard` · `/me/portfolio` | Barra superior, painel inicial, portefólio | autenticado |
| GET | `/public/portfolios/{handle}` | Portefólio público (opt-in, sem email) | público |
| GET | `/rooms?q=&topic=&difficulty=` | Todas as salas com estado | autenticado |
| GET | `/attempts/{id}` | Tentativa (dono) | autenticado |
| GET | `/exams` · `/exam-sessions/{id}` | Lista e sessão de exame | autenticado |
| POST | `/contests/{id}/registrations` | Inscrição em contest individual | estudante |
| POST | `/teams/{id}/decline` · DELETE `/teams/{id}` | Recusar convite · eliminar equipa (`ELIMINAR`) | convidado · capitão |
| GET | `/rankings?scope=GLOBAL\|COURSE\|YEAR\|SEASON\|TEAMS` | Rankings (Redis) | autenticado |
| GET | `/scoring-rules` | Regras de pontuação públicas | público |
| POST | `/admin/rankings/rebuild` | Reconstruir a projecção a partir de PostgreSQL | admin |
| POST | `/admin/contests/{id}/publish` · `/close` | Abrir inscrições · encerrar | organizador |
| PATCH | `/admin/users/{id}` | Estado da conta | admin |
| GET | `/authoring/exercises` · `/authoring/exercises/{id}` · `/authoring/review-queue` | Listas e detalhe | autor/revisor |
| POST | `/authoring/exercises/{id}/versions` | Nova versão a partir da mais recente | autor (próprio) |
| GET | `/legal-sources/alerts` · `/admin/audit/export` | Alertas legais · exportação CSV | revisor · admin |

### B.3 Decisões de comportamento
- **2.º factor:** TOTP (RFC 6238). No primeiro login de um perfil privilegiado, o login devolve
  `mfa_enrollment_required` com a chave; a sessão só fica activa depois do código.
- **Login:** 5 falhas por conta ou 20 por IP em 15 min (o IP do campus é partilhado).
- **Registo duplicado:** responde como um registo novo e avisa o titular por email (não revela a existência da conta).
- **Verificação:** ao confirmar o email, entra directamente se o perfil não exigir 2.º factor.
- **Aprovação editorial:** o revisor declara ter validado a versão em vigor das referências (`referencesChecked`);
  a publicação exige pelo menos uma referência.
- **Alertas legais:** política configurável (`UNPUBLISH` por defeito: os exercícios ligados passam a NEEDS_REVIEW).
- **Contest:** equipas com menos de `minTeam` membros confirmados não podem submeter; resultados publicáveis a partir de
  SUBMISSIONS_CLOSED quando não há fase final (júri é posterior).
- **Sessões:** em memória do servidor (uma instância). Mudança de perfis ou suspensão termina as sessões do utilizador.

### B.4 Contas por convite, recuperação de acesso e gestão de conteúdo (01/11/2026)
- **Estado `INVITED`:** a administração cria contas por convite (`POST /admin/users/invite`, com perfis). A pessoa recebe um
  email e só entra depois de definir a palavra-passe pelo link (72 h, uso único). Perfis privilegiados configuram o 2.º factor
  no primeiro login. Acções: `POST /admin/users/{id}/resend-invite`, `POST /admin/users/{id}/reset-mfa`.
- **«Esqueci-me»:** `POST /auth/password/forgot` (resposta sempre igual) e `POST /auth/password/reset` (link de 1 h, uso único;
  termina todas as sessões). Tokens guardados só como hash (`password_tokens`, migração V3).
- **Gestão de conteúdo (admin):** `GET/POST /admin/content/tracks`, `PATCH /admin/content/tracks/{id}`,
  `POST /admin/content/tracks/{id}/modules`, `POST /admin/content/modules/{id}/rooms`, `PATCH /admin/content/rooms/{id}`,
  `POST /admin/content/rooms/{id}/tasks`, `DELETE /admin/content/tasks/{id}` (só sem tentativas), `GET/POST/PATCH /admin/content/exams`.
- **Pacote de conteúdo v1** (`backend/src/main/resources/content/pack-v1.json`, gerado por `tools/content/gerar_pacote.py`):
  45 exercícios originais (A–F), 26 salas, 12 módulos, 3 exames. `POST /admin/content/import` é idempotente e cada exercício
  passa pela auto-verificação da correcção antes de entrar. Os exercícios ficam **em revisão** com autoria da conta técnica
  «Equipa editorial» (sem acesso); aprovação e publicação por revisor/admin, também em lote (`POST /authoring/versions/bulk-transition`).
- **Pré-requisitos:** dentro de cada módulo, as salas seguem em sequência; os módulos podem estudar-se em paralelo, excepto
  as Demonstrações financeiras, que exigem o módulo anterior.

### B.5 Abertura a todos os cursos, notificações, avaliações e feedback (01/11/2026)
- **Quem se regista:** só emails do domínio institucional (`ALLOWED_EMAIL_DOMAIN=isptec.co.ao`) cuja parte local é o número
  de estudante com 8 dígitos (`ALLOWED_EMAIL_LOCAL_PATTERN`, por defeito `^[0-9]{8}$`; ex.: `20221987@isptec.co.ao`).
  Erro `EMAIL_NOT_STUDENT`. Docentes e membros da AE entram por convite.
- **Cursos:** a plataforma é focada no DCSA mas aberta a todo o ISPTEC; V4 acrescenta o curso «Outro curso do ISPTEC» (`OUTRO`).
- **Notificações** (`notifications`): `GET /me/notifications` (cursor), `GET /me/notifications/unread-count`,
  `POST /me/notifications/{id}/read`, `POST /me/notifications/read-all`. Geradas por: boas-vindas, conquistas, convites e
  saídas de equipa, mudanças de estado do contest e publicação de resultados, exame corrigido automaticamente, fluxo editorial
  (revisores recebem «para rever»; autores recebem aprovação, publicação, devolução e alertas legais), feedback novo (admins),
  segurança (palavra-passe alterada, código de recuperação usado).
- **Emails em HTML** com a identidade visual (texto alternativo incluído) e personalizados com o primeiro nome.
- **Códigos de recuperação do 2.º factor:** 8 códigos `XXXX-XXXX` de uso único, devolvidos uma vez por `POST /auth/mfa`
  (200 `{status:"enrolled", recoveryCodes}`) ao configurar o 2.º factor; aceites no lugar do código TOTP;
  `POST /me/mfa/recovery-codes` gera novos. Só o hash fica guardado.
- **Avaliações de salas** (`room_ratings`): `GET/PUT /rooms/{id}/rating` (1–5 estrelas, comentário opcional ≤ 1000). Só quem
  já iniciou uma tarefa da sala pode avaliar (`RATING_NOT_ALLOWED`); uma avaliação por pessoa e sala (actualizável).
- **Feedback da plataforma** (`feedback`): `POST /feedback` (categoria `SUGGESTION|BUG|CONTENT|PRAISE|OTHER`, nota opcional,
  mensagem, página; 5 por hora); `GET /admin/feedback?status=`, `PATCH /admin/feedback/{id}` (`NEW|READ|RESOLVED`, auditado).
- **Números públicos:** `GET /public/stats` (sem sessão): trilhas, salas, exercícios publicados, estudantes activos,
  submissões corrigidas e média das avaliações (só com ≥ 5 respostas). Só agregados.
- **Eliminação efectiva:** `POST /admin/users/{id}/anonymize` executa um pedido `DELETION_REQUESTED`: apaga nome, email,
  pseudónimo, 2.º factor e perfis, estado `DELETED`, termina sessões e retira a conta dos rankings. Os registos imutáveis
  (pontos, auditoria) ficam ligados a um id sem dados pessoais.
- **Exportação de dados:** passa a incluir exames e contests.
- **Sessões em Redis** (Spring Session): sobrevivem a deploys e reinícios.
- **Edição de conteúdo:** `PUT /admin/content/tracks/{id}`, `PUT /admin/content/rooms/{id}`, `PUT /admin/content/tasks/{id}`
  (título, pontos e versão do exercício; as tentativas antigas mantêm a sua versão).
- **Equipas:** `POST /teams/{id}/leave` (membro não capitão, antes do bloqueio; o capitão é notificado).
