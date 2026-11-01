# Graph Report - lumen-pregao  (2026-11-01)

## Corpus Check
- 327 files · ~107,074 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 12 file(s) not represented in the graph (top: (none) 6, .example 1, .toml 1)

## Summary
- 2757 nodes · 9337 edges · 128 communities (91 shown, 37 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 983 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `48424e09`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- ExerciseVersion
- types.ts
- Badge
- ExerciseEditor.tsx
- CLAUDE.md — Lúmen Pregão
- Room
- .rebuild
- uuid
- ExamService.java
- admin/page.tsx
- README.md
- BaseEntity
- next
- SessionUser
- Attempt
- ExerciseCatalog
- AuthController.java
- format.ts
- .notFound
- org.springframework.web.bind.annotation.GetMapping
- PortfolioService.java
- DevSeeder.java
- AppProperties
- org.springframework.stereotype.Component
- Contest
- com.fasterxml.jackson.databind.JsonNode
- with
- AuditLog
- GlobalExceptionHandler.java
- LegalSource
- ExamSession
- SecurityConfig.java
- LearningController
- .publishResults
- Api
- Submission
- Scoreboard
- User
- TotpAndPolicyTest.java
- IntegrationTest
- Badge
- AdminUserService.java
- RankingService
- .grade
- compilerOptions
- Track
- org.springframework.transaction.annotation.Transactional
- ContestEventHub
- TestInfra.java
- application-dev.yml
- GradersTest.java
- org.springframework.security.access.prepost.PreAuthorize
- 4. Funcionalidades
- package.json
- jakarta.servlet.http.HttpServletRequest
- Expr
- ExamDtos
- application.yml
- app
- spring
- org.junit.jupiter.api.Test
- app
- logging
- OpenApiContractIT.java
- environment
- services
- MutableClock
- Lúmen Pregão — Especificação do produto (`spec.md`)
- PortfolioView.tsx
- helpers.ts
- Formats
- AuditController
- ReviewWorkflow
- registo/page.tsx
- EditorialStatus
- TeamInvite
- Exam
- EmailVerificationToken
- rate-limit
- .run
- JournalGrader
- cookie
- hibernate
- Company
- PgcAccount
- datasource
- 1. Visão geral
- eslint.config.mjs
- services
- backend
- postgres
- healthcheck
- com.fasterxml.jackson.databind.ObjectMapper
- Task
- PregaoApplication.java
- StreakService
- ParamGeneratorTest.java
- dependencies
- scripts
- frontend
- LearningEvents
- Fontes normativas e valores «a confirmar»
- 6. API REST (resumo; contrato completo em OpenAPI)
- contest-submissions.js
- application.yml
- Exercise
- ADR-0001 — Tempo real nos contests: SSE
- org.springframework.transaction.event.TransactionalEventListener
- java.util.regex.Pattern
- Deploy — Cloudflare (frontend) · Render (backend) · Neon (PostgreSQL)
- ranking
- O que falta — Lúmen Pregão
- jackson
- 11. Roadmap
- .project
- middleware.ts
- ExerciseReference
- ao.lumen:pregao
- AttemptStatus
- Scope
- learning

## God Nodes (most connected - your core abstractions)
1. `SessionUser` - 98 edges
2. `User` - 67 edges
3. `BaseEntity` - 66 edges
4. `AppProperties` - 59 edges
5. `Attempt` - 59 edges
6. `ExerciseCatalog` - 54 edges
7. `Badge()` - 54 edges
8. `ContestService` - 53 edges
9. `Contest` - 53 edges
10. `next` - 50 edges

## Surprising Connections (you probably didn't know these)
- `Contas e segurança` --references--> `PersonalDataContributor`  [INFERRED]
  docs/pendencias.md → backend/src/main/java/ao/lumen/pregao/common/privacy/PersonalDataContributor.java
- `6.1 Autenticação` --references--> `token()`  [INFERRED]
  docs/spec.md → frontend/components/charts/EChart.tsx
- `4. Convenções de código` --references--> `formatKz()`  [INFERRED]
  CLAUDE.md → frontend/lib/format.ts
- `4. Convenções de código` --references--> `formatPercent()`  [INFERRED]
  CLAUDE.md → frontend/lib/format.ts
- `4. Convenções de código` --references--> `formatAccount()`  [INFERRED]
  CLAUDE.md → frontend/lib/format.ts

## Import Cycles
- None detected.

## Communities (128 total, 37 thin omitted)

### Community 1 - "types.ts"
Cohesion: 0.06
Nodes (72): ADR-0001, Contest(), metadata, Sessao(), ContestView(), STATE_BADGE, TYPE_NAME, BigCountdown() (+64 more)

### Community 2 - "Badge"
Cohesion: 0.07
Nodes (62): Competicoes(), metadata, STATE, Exames(), metadata, ACTIVITY, CONTEST_STATE, Inicio() (+54 more)

### Community 3 - "ExerciseEditor.tsx"
Cohesion: 0.09
Nodes (64): Definicoes(), metadata, ContestsAdmin(), local(), Published, STATE_LABEL, ASSIGNABLE, ROLE_LABEL (+56 more)

### Community 4 - "CLAUDE.md — Lúmen Pregão"
Cohesion: 0.15
Nodes (13): 0. Antes de escrever código: Graphify, 10. Design e acessibilidade, 11. Definição de concluído (DoD), 12. Ordem de implementação (MVP), 1. Arquitectura do repositório (monorepo), 2. Stack obrigatória (não substituir), 3. Comandos, 5. Configuração (+5 more)

### Community 5 - "Room"
Cohesion: 0.17
Nodes (3): RoomProgress, Room, TrackModule

### Community 8 - "ExamService.java"
Cohesion: 0.07
Nodes (12): DomainException, Roles, AnswerFormatException, TeamDirectory, TeamInfo, PointSource, BADGE, COMPENSATION (+4 more)

### Community 9 - "admin/page.tsx"
Cohesion: 0.08
Nodes (43): AppError(), Loading(), Admin(), Audit(), AuditRow, Contests(), MATRIX, metadata (+35 more)

### Community 10 - "README.md"
Cohesion: 0.31
Nodes (3): Arrancar em desenvolvimento, Lúmen Pregão, Testes

### Community 11 - "BaseEntity"
Cohesion: 0.11
Nodes (5): BaseEntity, UuidV7, Team, Course, Role

### Community 13 - "next"
Cohesion: 0.10
Nodes (34): AppLayout(), FocoLayout(), GestaoLayout(), archivo, instrument, jetbrains, metadata, RootLayout() (+26 more)

### Community 14 - "SessionUser"
Cohesion: 0.10
Nodes (15): SessionUser, ContestController, ContestDetail, ContestDtos, CreateTeam, DeleteTeam, Invite, InviteView (+7 more)

### Community 16 - "ExerciseCatalog"
Cohesion: 0.17
Nodes (13): AuditService, ContestIntegrations, ContestLifecycle, ContestScoringImpl, ContestService, ContestProblemRepository, ContestRepository, ScoreboardRepository (+5 more)

### Community 17 - "AuthController.java"
Cohesion: 0.08
Nodes (16): ClientIp, AuthController, Accepted, AuthDtos, CourseDto, CsrfResponse, LoginRequest, LoginStep (+8 more)

### Community 18 - "format.ts"
Cohesion: 0.11
Nodes (37): 4. Convenções de código, Account, AccountSearch(), JournalEditor(), JournalLine, LineStatus, STATUS_TEXT, toPayload() (+29 more)

### Community 20 - "org.springframework.web.bind.annotation.GetMapping"
Cohesion: 0.13
Nodes (10): LegalSourceDto, LegalSourceInput, LegalSourceUpdate, LegalSourceController, AdminUserController, StatusUpdate, MeController, PortfolioController (+2 more)

### Community 21 - "PortfolioService.java"
Cohesion: 0.08
Nodes (17): PersonalDataContributor, UserDirectory, Activity, ActivityService, Competency, CatalogService, HistoryItem, Portfolio (+9 more)

### Community 22 - "DevSeeder.java"
Cohesion: 0.18
Nodes (12): AuthoringService, ExerciseCatalog, CompanyRepository, ExerciseReferenceRepository, ExerciseVersionRepository, LegalSourceRepository, ReviewWorkflowRepository, DevSeeder (+4 more)

### Community 23 - "AppProperties"
Cohesion: 0.12
Nodes (17): AppProperties, Authoring, Contest, DevSeed, Exam, Identity, Learning, Ranking (+9 more)

### Community 25 - "Contest"
Cohesion: 0.06
Nodes (11): Contest, ContestState, CLOSED, DRAFT, FINAL_EVALUATION, FROZEN, REGISTRATION, RESULTS_PUBLISHED (+3 more)

### Community 26 - "com.fasterxml.jackson.databind.JsonNode"
Cohesion: 0.08
Nodes (7): Answers, Grader, NumericGrader, RatioGrader, StatementGrader, TaxStepGrader, Tolerance

### Community 27 - "with"
Cohesion: 0.07
Nodes (35): defaults, runs-on, steps, run, needs, runs-on, steps, GITHUB_TOKEN (+27 more)

### Community 31 - "ExamSession"
Cohesion: 0.11
Nodes (7): ExamSession, ExamSessionStatus, EXPIRED_AUTO_SUBMITTED, GRADED, IN_PROGRESS, NOT_STARTED, SUBMITTED

### Community 32 - "SecurityConfig.java"
Cohesion: 0.11
Nodes (3): CsrfCookieFilter, RevokedSessionFilter, SecurityConfig

### Community 33 - "LearningController"
Cohesion: 0.09
Nodes (20): ReferenceView, ContestCard, LearningController, AttemptView, Breadcrumb, HintView, LearningDtos, ModuleView (+12 more)

### Community 34 - ".publishResults"
Cohesion: 0.16
Nodes (6): ContestAdminController, AdminContestRow, ContestInput, ProblemInput, ContestAdminService, StateChanged

### Community 35 - "Api"
Cohesion: 0.19
Nodes (4): AuthoringIT, LearningIT, Api, Res

### Community 36 - "Submission"
Cohesion: 0.11
Nodes (7): Submission, SubmissionStatus, FAILED, GRADED, PROCESSING, QUEUED, REJECTED

### Community 38 - "User"
Cohesion: 0.09
Nodes (13): CourseRef, MeDtos, MeResponse, PatchMe, MeService, RankingVisibility, HIDDEN, NAME (+5 more)

### Community 39 - "TotpAndPolicyTest.java"
Cohesion: 0.13
Nodes (3): PasswordPolicy, Totp, TotpAndPolicyTest

### Community 40 - "IntegrationTest"
Cohesion: 0.14
Nodes (4): ClientIpIT, ExamIT, StreakTest, IntegrationTest

### Community 41 - "Badge"
Cohesion: 0.06
Nodes (6): Badge, PointEvent, Season, UserBadge, BadgeRepository, UserBadgeRepository

### Community 42 - "AdminUserService.java"
Cohesion: 0.10
Nodes (9): SessionRevocation, OffensiveFilter, AdminUserService, DisplayNames, UserStatus, ACTIVE, DELETION_REQUESTED, PENDING_VERIFICATION (+1 more)

### Community 43 - "RankingService"
Cohesion: 0.42
Nodes (4): Entry, MyPosition, RankingPage, RankingService

### Community 44 - ".grade"
Cohesion: 0.17
Nodes (4): ExerciseDefinition, GradeResult, GraderFixtures, GradersTest

### Community 45 - "compilerOptions"
Cohesion: 0.10
Nodes (19): compilerOptions, allowJs, esModuleInterop, incremental, isolatedModules, jsx, lib, module (+11 more)

### Community 46 - "Track"
Cohesion: 0.19
Nodes (3): Snapshot, Track, TrackRepository

### Community 48 - "ContestEventHub"
Cohesion: 0.27
Nodes (3): ContestEventHub, Subscriber, SubmissionFinished

### Community 49 - "TestInfra.java"
Cohesion: 0.12
Nodes (5): CoreConfig, AuthEvents, VerificationRequested, TestInfra, TokenCapture

### Community 50 - "application-dev.yml"
Cohesion: 0.11
Nodes (18): app, dev-seed, identity, secure, password, url, username, enabled (+10 more)

### Community 51 - "GradersTest.java"
Cohesion: 0.12
Nodes (7): PgcController, Outcome, CORRECT, INCORRECT, STEP_OK, AccountDto, PgcAccountService

### Community 52 - "org.springframework.security.access.prepost.PreAuthorize"
Cohesion: 0.18
Nodes (10): AuthoringController, AuthoringDtos, ExerciseDto, ExerciseSummary, PreviewResponse, ReferenceDto, ReferenceInput, TransitionRequest (+2 more)

### Community 53 - "4. Funcionalidades"
Cohesion: 0.11
Nodes (18): 4.10 Simulação de empresa [Posterior], 4.11 Mercado simulado [Posterior], 4.12 Simulação de processos selectivos [Posterior], 4.13 Laboratório de análise de dados [Posterior], 4.14 Autoria e fluxo editorial [MVP: criar e aprovar; alertas legais MVP], 4.15 Júri [Posterior], 4.16 Administração [MVP: utilizadores, perfis, contests, auditoria], 4.17 Estados globais [MVP] (+10 more)

### Community 54 - "package.json"
Cohesion: 0.11
Nodes (22): devDependencies, eslint@^9.30.0, eslint-config-next@15.5.28, @opennextjs/cloudflare@^1.20.15, @playwright/test@^1.64.1, @types/node@^22.15.0, @types/react@^19.1.0, @types/react-dom@^19.1.0 (+14 more)

### Community 55 - "jakarta.servlet.http.HttpServletRequest"
Cohesion: 0.16
Nodes (3): HttpServletRequestWrapper, PendingMfa, SessionManager

### Community 56 - "Expr"
Cohesion: 0.31
Nodes (3): Expr, ExprException, ExprTest

### Community 57 - "ExamDtos"
Cohesion: 0.16
Nodes (10): ExamController, AnswerInput, AnswerSaved, ExamCard, ExamDtos, QuestionView, Report, SessionStarted (+2 more)

### Community 58 - "application.yml"
Cohesion: 0.22
Nodes (9): health, web, include, probes, management, endpoint, endpoints, enabled (+1 more)

### Community 59 - "app"
Cohesion: 0.12
Nodes (17): app, authoring, base-url, contest, exam, identity, mail-from, proxy-secret (+9 more)

### Community 60 - "spring"
Cohesion: 0.10
Nodes (20): name, redis, enabled, locations, host, password, port, username (+12 more)

### Community 62 - "app"
Cohesion: 0.14
Nodes (13): app, contest, identity, security, scheduler-interval-ms, secure, allowed-email-domain, mfa-enc-key (+5 more)

### Community 63 - "logging"
Cohesion: 0.15
Nodes (12): secure, console, root, logging, level, structured, server, forward-headers-strategy (+4 more)

### Community 64 - "OpenApiContractIT.java"
Cohesion: 0.13
Nodes (3): MailConfig, ResendEmailSender, OpenApiContractIT

### Community 65 - "environment"
Cohesion: 0.15
Nodes (13): environment, ALLOWED_EMAIL_DOMAIN, APP_BASE_URL, DB_PASSWORD, DB_URL, DB_USER, MAIL_HOST, MAIL_PORT (+5 more)

### Community 67 - "services"
Cohesion: 0.09
Nodes (21): fromService, key, sync, value, name, property, type, services (+13 more)

### Community 69 - "Lúmen Pregão — Especificação do produto (`spec.md`)"
Cohesion: 0.12
Nodes (16): 10. Concorrência e integridade, 2. Glossário, 3.1 Perfis, 3.2 Matriz, 3.3 Regras, 3. Perfis e permissões (RBAC), 5. Modelo de domínio (JPA), 7. Regras de pontuação e ranking (+8 more)

### Community 70 - "PortfolioView.tsx"
Cohesion: 0.29
Nodes (9): metadata, Portefolio(), metadata, PublicPortfolio(), PortfolioView(), PortfolioActions(), serverFetchOptional(), formatDate() (+1 more)

### Community 71 - "helpers.ts"
Cohesion: 0.33
Nodes (5): kz(), login(), num(), PASSWORD, @playwright/test

### Community 74 - "AuditController"
Cohesion: 0.16
Nodes (7): AuditController, AuditRow, AuditLogRepository, CursorPage, AdminUserDtos, AdminUserRow, RolesUpdate

### Community 76 - "registo/page.tsx"
Cohesion: 0.29
Nodes (7): Entrar(), metadata, metadata, Registo(), metadata, Verificar(), AuthCard()

### Community 78 - "EditorialStatus"
Cohesion: 0.12
Nodes (7): EditorialStatus, APPROVED, ARCHIVED, DRAFT, IN_REVIEW, NEEDS_REVIEW, PUBLISHED

### Community 82 - "rate-limit"
Cohesion: 0.20
Nodes (10): security, invites-per-hour, login-max-failures, login-max-failures-per-ip, login-window, register-per-hour, submissions-per-minute, mfa-enc-key (+2 more)

### Community 83 - ".run"
Cohesion: 0.17
Nodes (4): ContentStatus, COMING_SOON, DRAFT, PUBLISHED

### Community 84 - "JournalGrader"
Cohesion: 0.46
Nodes (3): Expected, JournalGrader, Line

### Community 85 - "cookie"
Cohesion: 0.22
Nodes (9): http-only, name, path, same-site, secure, servlet, session, cookie (+1 more)

### Community 86 - "hibernate"
Cohesion: 0.22
Nodes (9): ddl-auto, default_batch_fetch_size, generate_statistics, jdbc.time_zone, hibernate, open-in-view, properties, hibernate (+1 more)

### Community 89 - "datasource"
Cohesion: 0.25
Nodes (8): hikari, password, url, username, connection-timeout, maximum-pool-size, minimum-idle, datasource

### Community 90 - "1. Visão geral"
Cohesion: 0.25
Nodes (8): 1.1 Contexto, 1.2 Problema, 1.3 Público-alvo, 1.4 Objectivos, 1.5 Não-objectivos, 1.6 Proposta de valor, 1.7 Rodapé institucional (obrigatório em todas as páginas), 1. Visão geral

### Community 91 - "eslint.config.mjs"
Cohesion: 0.25
Nodes (5): compat, config, __dirname, @eslint/eslintrc@^3.3.1, @eslint/eslintrc

### Community 92 - "services"
Cohesion: 0.25
Nodes (7): image, ports, name, services, mailpit, volumes, pgdata

### Community 93 - "backend"
Cohesion: 0.25
Nodes (8): build, depends_on, ports, postgres, redis, condition, condition, backend

### Community 94 - "postgres"
Cohesion: 0.25
Nodes (8): POSTGRES_DB, POSTGRES_PASSWORD, POSTGRES_USER, environment, image, ports, volumes, postgres

### Community 95 - "healthcheck"
Cohesion: 0.32
Nodes (8): interval, retries, test, healthcheck, healthcheck, image, ports, redis

### Community 96 - "com.fasterxml.jackson.databind.ObjectMapper"
Cohesion: 0.16
Nodes (10): ExamScheduler, ExamResult, ExamService, ExamSessionRepository, ChoiceGrader, ParamGenerator, Rendered, Segment (+2 more)

### Community 97 - "Task"
Cohesion: 0.10
Nodes (7): TaskState, AVAILABLE, FAILED, IN_PROGRESS, PASSED, UNAVAILABLE, Task

### Community 101 - "dependencies"
Cohesion: 0.29
Nodes (7): dependencies, echarts@^6.1.0, next@15.5.28, react@^19.1.0, react-dom@^19.1.0, server-only@^0.0.1, react-dom

### Community 102 - "scripts"
Cohesion: 0.20
Nodes (10): scripts, build, cf:build, cf:deploy, cf:preview, dev, lint, start (+2 more)

### Community 103 - "frontend"
Cohesion: 0.29
Nodes (7): BACKEND_INTERNAL_URL, NEXT_PUBLIC_API_BASE, build, depends_on, environment, ports, frontend

### Community 106 - "Fontes normativas e valores «a confirmar»"
Cohesion: 0.33
Nodes (6): Contas do PGC (versão `PGC-a-confirmar`), Empresas, Fontes normativas e valores «a confirmar», Fontes registadas (`V2__reference_data.sql`), Outros valores provisórios, Parâmetros normativos nos exercícios de desenvolvimento

### Community 107 - "6. API REST (resumo; contrato completo em OpenAPI)"
Cohesion: 0.22
Nodes (9): 6.1 Autenticação, 6.2 Aprendizagem, 6.3 Exames, 6.4 Contests, 6.5 Autoria, júri, administração, 6. API REST (resumo; contrato completo em OpenAPI), EChart(), token() (+1 more)

### Community 110 - "application.yml"
Cohesion: 0.13
Nodes (14): path, include-message, include-stacktrace, console, logging, structured, server, error (+6 more)

### Community 112 - "ADR-0001 — Tempo real nos contests: SSE"
Cohesion: 0.40
Nodes (4): ADR-0001 — Tempo real nos contests: SSE, Consequências, Contexto, Decisão

### Community 113 - "org.springframework.transaction.event.TransactionalEventListener"
Cohesion: 0.31
Nodes (3): EmailSender, DuplicateRegistration, IdentityMailer

### Community 114 - "java.util.regex.Pattern"
Cohesion: 0.24
Nodes (3): MarkdownSanitizer, ClientIpFilter, TraceIdFilter

### Community 115 - "Deploy — Cloudflare (frontend) · Render (backend) · Neon (PostgreSQL)"
Cohesion: 0.20
Nodes (10): 1. Arquitectura, 2. Neon (PostgreSQL), 3. Redis — Render Key Value, 4. Render (backend), 5. Cloudflare (frontend), 6. ⚠ IP do cliente e rate limiting, 7. Primeiro arranque em produção, 8. Checklist de go-live (+2 more)

### Community 116 - "ranking"
Cohesion: 0.50
Nodes (4): ranking, streak-bonus, streak-days, zone

### Community 117 - "O que falta — Lúmen Pregão"
Cohesion: 0.20
Nodes (10): 📋 A confirmar pela equipa (não é código), Aprendizagem e correcção, 🔴 Bloqueia a produção (Cloudflare + Render + Neon), Contas e segurança, Contests, 🟠 Falta para o MVP (spec / design), ⚪ Fases posteriores (spec §11), Interface (design) (+2 more)

### Community 118 - "jackson"
Cohesion: 0.50
Nodes (4): default-property-inclusion, serialization, write-dates-as-timestamps, jackson

### Community 119 - "11. Roadmap"
Cohesion: 0.50
Nodes (4): 11. Roadmap, Fases posteriores, Fora de âmbito até decisão, MVP

### Community 125 - "AttemptStatus"
Cohesion: 0.33
Nodes (4): AttemptStatus, FAILED, OPEN, PASSED

### Community 126 - "Scope"
Cohesion: 0.33
Nodes (6): Scope, COURSE, GLOBAL, SEASON, TEAMS, YEAR

### Community 127 - "learning"
Cohesion: 0.50
Nodes (4): learning, failed-attempt-penalty, hint-penalty, max-attempts-default

## Knowledge Gaps
- **65 isolated node(s):** `ao.lumen:pregao`, `DRAFT`, `IN_REVIEW`, `APPROVED`, `PUBLISHED` (+60 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 689 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **37 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `O que falta — Lúmen Pregão` connect `O que falta — Lúmen Pregão` to `README.md`?**
  _High betweenness centrality (0.257) - this node is a cross-community bridge._
- **What connects `ao.lumen:pregao`, `DRAFT`, `IN_REVIEW` to the rest of the system?**
  _65 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `types.ts` be split into smaller, more focused modules?**
  _Cohesion score 0.055900621118012424 - nodes in this community are weakly interconnected._
- **Why does `🟡 Qualidade, testes e operação` connect `O que falta — Lúmen Pregão` to `Task`?**
  _High betweenness centrality (0.246) - this node is a cross-community bridge._
- **Should `Badge` be split into smaller, more focused modules?**
  _Cohesion score 0.07372346528973035 - nodes in this community are weakly interconnected._
- **Why does `CLAUDE.md — Lúmen Pregão` connect `CLAUDE.md — Lúmen Pregão` to `README.md`, `format.ts`?**
  _High betweenness centrality (0.164) - this node is a cross-community bridge._
- **Should `ExerciseEditor.tsx` be split into smaller, more focused modules?**
  _Cohesion score 0.09122203098106713 - nodes in this community are weakly interconnected._