# Graph Report - lumen-pregao  (2026-11-03)

## Corpus Check
- 413 files · ~161,855 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 16 file(s) not represented in the graph (top: (none) 9, .example 1, .toml 1)

## Summary
- 3446 nodes · 12356 edges · 145 communities (103 shown, 42 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 1420 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `a6890121`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- ExerciseVersion
- types.ts
- Badge
- ExerciseEditor.tsx
- CLAUDE.md — Lúmen Pregão
- Room
- RankingService
- uuid
- PointSource
- admin/page.tsx
- Fontes normativas e valores «a confirmar»
- BaseEntity
- com.fasterxml.jackson.databind.ObjectMapper
- next
- .scoreboard
- Attempt
- ExerciseCatalog
- jakarta.servlet.http.HttpServletRequest
- format.ts
- ContestService
- org.springframework.web.bind.annotation.PostMapping
- org.springframework.stereotype.Service
- ContentPackImporter.java
- AppProperties
- org.springframework.stereotype.Component
- Contest
- com.fasterxml.jackson.databind.JsonNode
- with
- AuditLog
- SecurityConfig.java
- LegalSource
- ExamService.java
- CampaignService.java
- LearningController
- .publishResults
- Api
- Submission
- Scoreboard
- User
- TotpAndPolicyTest.java
- IntegrationTest
- Badge
- UserRepository
- CampaignService
- .grade
- compilerOptions
- Track
- org.springframework.transaction.annotation.Transactional
- ContestEventHub
- TestInfra.java
- application-dev.yml
- RateLimiterFallbackTest.java
- EditorialStatus
- 4. Funcionalidades
- package.json
- ClientIpFilter.java
- Expr
- Delivery
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
- MessageTemplate
- helpers.ts
- Formats
- UserDirectory
- WorkflowAndDatesTest.java
- registo/page.tsx
- AccountReminders
- Campaign
- Exam
- EmailVerificationToken
- rate-limit
- ContentStatus
- RoomRating
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
- AuditService
- Task
- PregaoApplication.java
- PortfolioService
- ParamGeneratorTest.java
- dependencies
- scripts
- frontend
- set
- ContentAdminService.java
- 6. API REST (resumo; contrato completo em OpenAPI)
- contest-submissions.js
- application.yml
- .gradeAndApply
- ADR-0001 — Tempo real nos contests: SSE
- org.springframework.transaction.event.TransactionalEventListener
- AdminUserRow
- Deploy — Cloudflare (frontend) · Render (backend) · Neon (PostgreSQL)
- ranking
- O que falta — Lúmen Pregão
- jackson
- EmailsIT
- .reset
- .enqueue
- MeService
- ao.lumen:pregao
- AttemptStatus
- Lúmen Lab: como a plataforma funciona
- learning
- gerar_pacote.py
- mail
- Feedback
- Notification
- RecoveryCodes
- Anexo B — Adenda de implementação (01/11/2026)
- SubmissionStatus
- Season
- ContestPersonalData
- TaskState
- MfaRecoveryCode
- .snapshot
- mail
- Role
- pnpm

## God Nodes (most connected - your core abstractions)
1. `SessionUser` - 142 edges
2. `User` - 95 edges
3. `BaseEntity` - 82 edges
4. `AppProperties` - 75 edges
5. `next` - 65 edges
6. `AuditService` - 63 edges
7. `Badge()` - 63 edges
8. `react` - 63 edges
9. `ExerciseCatalog` - 60 edges
10. `Button()` - 60 edges

## Surprising Connections (you probably didn't know these)
- `6.1 Autenticação` --references--> `token()`  [INFERRED]
  docs/spec.md → frontend/components/charts/EChart.tsx
- `4. Convenções de código` --references--> `formatKz()`  [INFERRED]
  CLAUDE.md → frontend/lib/format.ts
- `4. Convenções de código` --references--> `formatPercent()`  [INFERRED]
  CLAUDE.md → frontend/lib/format.ts
- `4. Convenções de código` --references--> `formatAccount()`  [INFERRED]
  CLAUDE.md → frontend/lib/format.ts
- `AccountReminders` --references--> `Mail`  [EXTRACTED]
  backend/src/main/java/ao/lumen/pregao/identity/app/AccountReminders.java → backend/src/main/java/ao/lumen/pregao/common/config/AppProperties.java

## Import Cycles
- None detected.

## Communities (145 total, 42 thin omitted)

### Community 0 - "ExerciseVersion"
Cohesion: 0.08
Nodes (8): ExerciseDto, ReferenceInput, VersionInput, AuthoringService, Exercise, ExerciseReference, ExerciseVersion, ReviewWorkflow

### Community 1 - "types.ts"
Cohesion: 0.06
Nodes (65): ADR-0001, Contest(), metadata, Sessao(), ContestView(), STATE_BADGE, TYPE_NAME, BigCountdown() (+57 more)

### Community 2 - "Badge"
Cohesion: 0.06
Nodes (78): Competicoes(), metadata, STATE, Exames(), metadata, ACTIVITY, CONTEST_STATE, Inicio() (+70 more)

### Community 3 - "ExerciseEditor.tsx"
Cohesion: 0.07
Nodes (97): Definicoes(), metadata, CONTENT_STATUS, ContentAdmin(), Dialog, ExerciseOption, ModuleAdmin, RoomAdmin (+89 more)

### Community 4 - "CLAUDE.md — Lúmen Pregão"
Cohesion: 0.15
Nodes (13): 0. Antes de escrever código: Graphify, 10. Design e acessibilidade, 11. Definição de concluído (DoD), 12. Ordem de implementação (MVP), 1. Arquitectura do repositório (monorepo), 2. Stack obrigatória (não substituir), 3. Comandos, 5. Configuração (+5 more)

### Community 6 - "RankingService"
Cohesion: 0.11
Nodes (12): PointsAwarded, RankingKeys, Entry, MyPosition, RankingPage, RankingService, Scope, COURSE (+4 more)

### Community 7 - "uuid"
Cohesion: 0.08
Nodes (3): MfaRecoveryCodeRepository, RoomRatingRepository, NotificationRepository

### Community 8 - "PointSource"
Cohesion: 0.14
Nodes (8): PointEvent, PointSource, BADGE, COMPENSATION, CONTEST, EXAM, STREAK, TASK

### Community 9 - "admin/page.tsx"
Cohesion: 0.05
Nodes (64): AppError(), Loading(), Admin(), Audit(), AuditRow, Content(), Contests(), Emails() (+56 more)

### Community 10 - "Fontes normativas e valores «a confirmar»"
Cohesion: 0.16
Nodes (9): Contas do PGC (versão `PGC-a-confirmar`), Empresas, Fontes normativas e valores «a confirmar», Fontes registadas (`V2__reference_data.sql`), Outros valores provisórios, Parâmetros normativos nos exercícios de desenvolvimento, Arrancar em desenvolvimento, Lúmen Lab (+1 more)

### Community 12 - "com.fasterxml.jackson.databind.ObjectMapper"
Cohesion: 0.09
Nodes (5): ChoiceGrader, NumericGrader, RatioGrader, StatementGrader, TaxStepGrader

### Community 13 - "next"
Cohesion: 0.07
Nodes (47): AppLayout(), FocoLayout(), GestaoLayout(), archivo, instrument, jetbrains, metadata, RootLayout() (+39 more)

### Community 16 - "ExerciseCatalog"
Cohesion: 0.10
Nodes (14): ContestIntegrations, ContestLifecycle, ContestScoringImpl, TeamMember, ContestProblemRepository, ContestRepository, ScoreboardRepository, TeamMemberRepository (+6 more)

### Community 17 - "jakarta.servlet.http.HttpServletRequest"
Cohesion: 0.06
Nodes (18): RateLimiter, Window, ClientIp, Accepted, AuthDtos, CourseDto, ForgotRequest, LoginRequest (+10 more)

### Community 18 - "format.ts"
Cohesion: 0.12
Nodes (36): 4. Convenções de código, AccountSearch(), JournalEditor(), JournalLine, LineStatus, STATUS_TEXT, toPayload(), totals() (+28 more)

### Community 19 - "ContestService"
Cohesion: 0.07
Nodes (15): ContestController, ContestDetail, ContestDtos, CreateTeam, DeleteTeam, Invite, InviteView, MemberView (+7 more)

### Community 20 - "org.springframework.web.bind.annotation.PostMapping"
Cohesion: 0.09
Nodes (13): LegalSourceInput, LegalSourceUpdate, LegalSourceController, CsrfResponse, ResendRequest, MeController, AdminEmailController, PreviewIn (+5 more)

### Community 21 - "org.springframework.stereotype.Service"
Cohesion: 0.07
Nodes (23): ExerciseCatalog, CompanyRepository, DomainException, MarkdownSanitizer, Activity, ActivityService, Competency, AttemptService (+15 more)

### Community 22 - "ContentPackImporter.java"
Cohesion: 0.09
Nodes (17): ExerciseReferenceRepository, ExerciseRepository, ExerciseVersionRepository, LegalSourceRepository, ReviewWorkflowRepository, ContentPackImporter, ImportResult, DevSeeder (+9 more)

### Community 23 - "AppProperties"
Cohesion: 0.09
Nodes (13): AppProperties, Authoring, Contest, DevSeed, Exam, Identity, Learning, Mail (+5 more)

### Community 24 - "org.springframework.stereotype.Component"
Cohesion: 0.18
Nodes (5): EmailTemplate, MailQueue, ExamScheduler, AccountReminderScheduler, NotificationEmailBridge

### Community 25 - "Contest"
Cohesion: 0.05
Nodes (14): ContestEvents, ScoreboardChanged, StateChanged, Contest, ContestState, CLOSED, DRAFT, FINAL_EVALUATION (+6 more)

### Community 26 - "com.fasterxml.jackson.databind.JsonNode"
Cohesion: 0.14
Nodes (7): Answers, ExerciseDefinition, GradeResult, Expected, JournalGrader, Line, Tolerance

### Community 27 - "with"
Cohesion: 0.07
Nodes (35): defaults, runs-on, steps, run, needs, runs-on, steps, GITHUB_TOKEN (+27 more)

### Community 29 - "SecurityConfig.java"
Cohesion: 0.05
Nodes (4): GlobalExceptionHandler, CsrfCookieFilter, RevokedSessionFilter, SecurityConfig

### Community 30 - "LegalSource"
Cohesion: 0.19
Nodes (3): LegalSourceDto, LegalSourceService, LegalSource

### Community 31 - "ExamService.java"
Cohesion: 0.06
Nodes (21): ExamController, AnswerInput, AnswerSaved, ExamCard, ExamDtos, QuestionView, Report, SessionStarted (+13 more)

### Community 32 - "CampaignService.java"
Cohesion: 0.08
Nodes (14): CursorPage, FeedbackController, FeedbackInput, StatusInput, FeedbackRepository, FeedbackService, FeedbackView, AdminEmailDtos (+6 more)

### Community 33 - "LearningController"
Cohesion: 0.08
Nodes (19): ReferenceView, LearningController, AttemptView, Breadcrumb, HintView, LearningDtos, ModuleView, RoomCard (+11 more)

### Community 34 - ".publishResults"
Cohesion: 0.22
Nodes (6): SessionUser, ContestAdminController, AdminContestRow, ContestInput, ProblemInput, ContestAdminService

### Community 35 - "Api"
Cohesion: 0.17
Nodes (5): AuthoringIT, ContentAdminIT, EngagementIT, LearningIT, Res

### Community 37 - "Scoreboard"
Cohesion: 0.08
Nodes (3): ContestResult, ContestProblem, Scoreboard

### Community 38 - "User"
Cohesion: 0.08
Nodes (4): Theme, DARK, LIGHT, User

### Community 40 - "IntegrationTest"
Cohesion: 0.13
Nodes (7): AdminUsersIT, ClientIpIT, ExamIT, InviteAndResetIT, Api, IntegrationTest, TokenCapture

### Community 42 - "UserRepository"
Cohesion: 0.08
Nodes (17): SessionRevocation, AuthController, AdminUserService, AuthService, PasswordService, SecretCipher, Course, UserStatus (+9 more)

### Community 43 - "CampaignService"
Cohesion: 0.16
Nodes (8): MailingDirectory, Recipient, ContentIn, PreviewView, CampaignService, Resolved, MessageRenderer, CampaignRepository

### Community 44 - ".grade"
Cohesion: 0.13
Nodes (8): Grader, Outcome, CORRECT, INCORRECT, STEP_OK, GradingService, GraderFixtures, GradersTest

### Community 45 - "compilerOptions"
Cohesion: 0.10
Nodes (19): compilerOptions, allowJs, esModuleInterop, incremental, isolatedModules, jsx, lib, module (+11 more)

### Community 47 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.12
Nodes (3): RoomInput, TrackInput, ContentAdminService

### Community 50 - "application-dev.yml"
Cohesion: 0.10
Nodes (19): app, dev-seed, identity, secure, password, url, username, enabled (+11 more)

### Community 52 - "EditorialStatus"
Cohesion: 0.08
Nodes (17): AuthoringController, BulkFailure, BulkResult, BulkTransition, AuthoringDtos, ExerciseSummary, PreviewResponse, ReferenceDto (+9 more)

### Community 53 - "4. Funcionalidades"
Cohesion: 0.11
Nodes (18): 4.10 Simulação de empresa [Posterior], 4.11 Mercado simulado [Posterior], 4.12 Simulação de processos selectivos [Posterior], 4.13 Laboratório de análise de dados [Posterior], 4.14 Autoria e fluxo editorial [MVP: criar e aprovar; alertas legais MVP], 4.15 Júri [Posterior], 4.16 Administração [MVP: utilizadores, perfis, contests, auditoria], 4.17 Estados globais [MVP] (+10 more)

### Community 54 - "package.json"
Cohesion: 0.11
Nodes (22): devDependencies, eslint@^9.30.0, eslint-config-next@15.5.28, @opennextjs/cloudflare@^1.20.15, @playwright/test@^1.64.1, @types/node@^22.15.0, @types/react@^19.1.0, @types/react-dom@^19.1.0 (+14 more)

### Community 55 - "ClientIpFilter.java"
Cohesion: 0.18
Nodes (3): ClientIpFilter, HttpServletRequestWrapper, TraceIdFilter

### Community 57 - "Delivery"
Cohesion: 0.13
Nodes (9): CampaignWorker, Prepared, Delivery, DeliveryStatus, FAILED, PENDING, SENT, SKIPPED (+1 more)

### Community 58 - "application.yml"
Cohesion: 0.15
Nodes (13): health, web, include, mail, probes, show-components, enabled, management (+5 more)

### Community 59 - "app"
Cohesion: 0.11
Nodes (18): app, authoring, base-url, contest, exam, identity, mail-from, proxy-secret (+10 more)

### Community 60 - "spring"
Cohesion: 0.11
Nodes (19): name, redis, enabled, locations, problemdetails, enabled, flush-mode, namespace (+11 more)

### Community 61 - "org.junit.jupiter.api.Test"
Cohesion: 0.12
Nodes (4): ExprTest, StudentEmailTest, IdentityIT, StreakTest

### Community 62 - "app"
Cohesion: 0.13
Nodes (14): app, contest, identity, security, scheduler-interval-ms, secure, allowed-email-domain, email-local-pattern (+6 more)

### Community 63 - "logging"
Cohesion: 0.08
Nodes (24): enabled, secure, health, enabled, console, show-components, env, root (+16 more)

### Community 64 - "OpenApiContractIT.java"
Cohesion: 0.13
Nodes (3): MailConfig, ResendEmailSender, OpenApiContractIT

### Community 65 - "environment"
Cohesion: 0.17
Nodes (12): environment, ALLOWED_EMAIL_DOMAIN, APP_BASE_URL, DB_PASSWORD, DB_URL, DB_USER, MAIL_HOST, MAIL_PORT (+4 more)

### Community 67 - "services"
Cohesion: 0.08
Nodes (23): paths, fromService, key, sync, value, name, property, type (+15 more)

### Community 69 - "Lúmen Pregão — Especificação do produto (`spec.md`)"
Cohesion: 0.12
Nodes (16): 10. Concorrência e integridade, 11. Roadmap, 2. Glossário, 3.1 Perfis, 3.2 Matriz, 3.3 Regras, 3. Perfis e permissões (RBAC), 5. Modelo de domínio (JPA) (+8 more)

### Community 70 - "MessageTemplate"
Cohesion: 0.20
Nodes (5): TemplateIn, TemplateView, MessageTemplateService, MessageTemplate, MessageTemplateRepository

### Community 71 - "helpers.ts"
Cohesion: 0.32
Nodes (5): kz(), login(), num(), PASSWORD, @playwright/test

### Community 73 - "Formats"
Cohesion: 0.23
Nodes (5): Formats, Rendered, Segment, StatementRenderer, VisibleParam

### Community 74 - "UserDirectory"
Cohesion: 0.08
Nodes (8): AuditController, AuditRow, AuditLogRepository, UserDirectory, UserSummary, RankingPersonalData, PointEventRepository, SeasonRepository

### Community 76 - "registo/page.tsx"
Cohesion: 0.19
Nodes (11): Entrar(), metadata, Esqueci(), metadata, metadata, Redefinir(), metadata, Registo() (+3 more)

### Community 78 - "AccountReminders"
Cohesion: 0.21
Nodes (4): AdminReminderController, AccountReminders, ReminderResult, ReminderStats

### Community 79 - "Campaign"
Cohesion: 0.12
Nodes (7): Audience, ALL_STUDENTS, COURSE, INACTIVE, USER, Campaign, MessageContent

### Community 82 - "rate-limit"
Cohesion: 0.22
Nodes (9): security, invites-per-hour, login-max-failures, login-max-failures-per-ip, login-window, register-per-hour, submissions-per-minute, mfa-enc-key (+1 more)

### Community 83 - "ContentStatus"
Cohesion: 0.33
Nodes (4): ContentStatus, COMING_SOON, DRAFT, PUBLISHED

### Community 84 - "RoomRating"
Cohesion: 0.17
Nodes (3): RatingInput, RatingSummary, RoomRating

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

### Community 96 - "AuditService"
Cohesion: 0.18
Nodes (8): AuditService, ContentAdminController, ExamStatus, ExamAdmin, ExamAdminService, ExamInput, ExamRepository, StatusInput

### Community 99 - "PortfolioService"
Cohesion: 0.10
Nodes (9): PublicStatsController, ContestCard, PortfolioController, Dashboard, HistoryItem, Portfolio, PortfolioService, Summary (+1 more)

### Community 101 - "dependencies"
Cohesion: 0.29
Nodes (7): dependencies, echarts@^6.1.0, next@15.5.28, react@^19.1.0, react-dom@^19.1.0, server-only@^0.0.1, react-dom

### Community 102 - "scripts"
Cohesion: 0.20
Nodes (10): scripts, build, cf:build, cf:deploy, cf:preview, dev, lint, start (+2 more)

### Community 103 - "frontend"
Cohesion: 0.29
Nodes (7): BACKEND_INTERNAL_URL, NEXT_PUBLIC_API_BASE, build, depends_on, environment, ports, frontend

### Community 105 - "set"
Cohesion: 0.08
Nodes (11): Roles, CourseRef, MeDtos, MeResponse, PatchMe, PasswordPolicy, DisplayNames, RankingVisibility (+3 more)

### Community 106 - "ContentAdminService.java"
Cohesion: 0.23
Nodes (7): ContentDtos, ModuleAdmin, ModuleInput, RoomAdmin, TaskAdmin, TaskInput, TrackAdmin

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
Cohesion: 0.12
Nodes (10): Email, AccountDeleted, AuthEvents, DuplicateRegistration, InactivityReminder, PasswordResetRequested, UserInvited, VerificationRequested (+2 more)

### Community 114 - "AdminUserRow"
Cohesion: 0.24
Nodes (6): AdminUserController, AdminUserDtos, AdminUserRow, InviteRequest, RolesUpdate, StatusUpdate

### Community 115 - "Deploy — Cloudflare (frontend) · Render (backend) · Neon (PostgreSQL)"
Cohesion: 0.15
Nodes (13): 10. Carregar o conteúdo em produção, 1. Arquitectura, 2. Neon (PostgreSQL), 3. Redis — Render Key Value, 4. Render (backend), 5. Cloudflare (frontend), 6. ⚠ IP do cliente e rate limiting, 7. Primeiro arranque em produção (+5 more)

### Community 116 - "ranking"
Cohesion: 0.50
Nodes (4): ranking, streak-bonus, streak-days, zone

### Community 117 - "O que falta — Lúmen Pregão"
Cohesion: 0.20
Nodes (10): 📋 A confirmar pela equipa (não é código), Aprendizagem e correcção, 🔴 Bloqueia a produção (Cloudflare + Render + Neon), Contas e segurança, Contests, 🟠 Falta para o MVP (spec / design), ⚪ Fases posteriores (spec §11), Interface (design) (+2 more)

### Community 118 - "jackson"
Cohesion: 0.50
Nodes (4): default-property-inclusion, serialization, write-dates-as-timestamps, jackson

### Community 119 - "EmailsIT"
Cohesion: 0.30
Nodes (3): EmailsIT, Sent, SentMail

### Community 121 - ".enqueue"
Cohesion: 0.24
Nodes (3): LearningEvents, SubmissionFinished, SubmissionQueued

### Community 122 - "MeService"
Cohesion: 0.24
Nodes (3): PersonalDataContributor, OffensiveFilter, MeService

### Community 125 - "AttemptStatus"
Cohesion: 0.40
Nodes (4): AttemptStatus, FAILED, OPEN, PASSED

### Community 126 - "Lúmen Lab: como a plataforma funciona"
Cohesion: 0.18
Nodes (11): 10. Como reportar problemas, 1. O que é, 2. Endereços, 3. Quem entra e como, 4. O dia-a-dia do estudante, 5. Conteúdos: do rascunho à publicação, 6. Contests (Maratona), 7. Administração (+3 more)

### Community 127 - "learning"
Cohesion: 0.50
Nodes (4): learning, failed-attempt-penalty, hint-penalty, max-attempts-default

### Community 128 - "gerar_pacote.py"
Cohesion: 0.22
Nodes (3): ex(), ratio_b(), ratio_f()

### Community 130 - "mail"
Cohesion: 0.20
Nodes (10): mail, campaign-poll-ms, inactive-after, inactive-every, min-interval, reminders-cron, reminders-enabled, verify-after (+2 more)

### Community 134 - "Anexo B — Adenda de implementação (01/11/2026)"
Cohesion: 0.25
Nodes (8): Anexo B — Adenda de implementação (01/11/2026), B.1 Modelo de domínio (complementa §5), B.2 Endpoints acrescentados (complementa §6), B.3 Decisões de comportamento, B.4 Contas por convite, recuperação de acesso e gestão de conteúdo (01/11/2026), B.5 Abertura a todos os cursos, notificações, avaliações e feedback (01/11/2026), B.6 Confirmação de email em qualquer dispositivo (02/11/2026), B.7 Emails da administração, lembretes e notificações por email (03/11/2026)

### Community 135 - "SubmissionStatus"
Cohesion: 0.29
Nodes (6): SubmissionStatus, FAILED, GRADED, PROCESSING, QUEUED, REJECTED

### Community 138 - "TaskState"
Cohesion: 0.33
Nodes (6): TaskState, AVAILABLE, FAILED, IN_PROGRESS, PASSED, UNAVAILABLE

### Community 141 - "mail"
Cohesion: 0.40
Nodes (5): host, password, port, username, mail

### Community 144 - "pnpm"
Cohesion: 0.67
Nodes (3): postcss, pnpm, overrides

## Knowledge Gaps
- **75 isolated node(s):** `ao.lumen:pregao`, `DRAFT`, `IN_REVIEW`, `APPROVED`, `PUBLISHED` (+70 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 813 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **42 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `O que falta — Lúmen Lab` connect `O que falta — Lúmen Pregão` to `Fontes normativas e valores «a confirmar»`?**
  _High betweenness centrality (0.318) - this node is a cross-community bridge._
- **What connects `ao.lumen:pregao`, `DRAFT`, `IN_REVIEW` to the rest of the system?**
  _75 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `ExerciseVersion` be split into smaller, more focused modules?**
  _Cohesion score 0.07863849765258216 - nodes in this community are weakly interconnected._
- **Why does `🟡 Qualidade, testes e operação` connect `O que falta — Lúmen Pregão` to `.snapshot`?**
  _High betweenness centrality (0.316) - this node is a cross-community bridge._
- **Should `types.ts` be split into smaller, more focused modules?**
  _Cohesion score 0.05759623861298854 - nodes in this community are weakly interconnected._
- **Why does `CLAUDE.md — Lúmen Lab` connect `CLAUDE.md — Lúmen Pregão` to `Fontes normativas e valores «a confirmar»`, `format.ts`?**
  _High betweenness centrality (0.228) - this node is a cross-community bridge._
- **Should `Badge` be split into smaller, more focused modules?**
  _Cohesion score 0.0562478366216684 - nodes in this community are weakly interconnected._