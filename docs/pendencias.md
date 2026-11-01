# O que falta — Lúmen Pregão

> Estado a 01/11/2026, depois da primeira implementação do MVP. Cada item diz **o que falta**, **porquê** e
> **onde mexer**. Prioridades:
> - 🔴 bloqueia a produção;
> - 🟠 falta para o MVP estar completo segundo a spec e o design;
> - 🟡 qualidade, testes e operação;
> - ⚪ fase posterior (já prevista na spec).
>
> O guia de deploy está em [`deploy.md`](deploy.md).

## 🔴 Bloqueia a produção (Cloudflare + Render + Neon)

✅ = resolvido a 01/11/2026: IP do cliente (`ClientIpFilter` + `PROXY_SECRET`), email pela API do Resend (`RESEND_API_KEY`), `server.port=${PORT:8080}`, adaptador OpenNext (`wrangler.jsonc`, `pnpm cf:deploy`) e `render.yaml`.

| # | O que falta | Porquê | Onde |
|---|---|---|---|
| P1 | **Sessões e SSE fora da memória** | Sessões HTTP e subscritores SSE vivem numa só instância. Um reinício (deploy, adormecimento no Render) termina todas as sessões. Escalar horizontalmente parte o SSE. | Spring Session com Redis (`spring-session-data-redis`); difusão SSE por Redis pub/sub em `ContestEventHub` |
| P2 | **Validar SSE através do Cloudflare** | Os eventos do contest passam pelo rewrite do Worker; não foi testado se o streaming chega sem buffer. | Testar com `opennextjs-cloudflare preview`; alternativa: `EventSource` directo ao Render com `SameSite=None` + CORS com credenciais |
| ✅ P3 | **IP real do cliente** | Atrás do Cloudflare, o backend pode ver o IP do Worker: o limite de 20 falhas de login por IP fica partilhado por todos (risco de bloquear o login a toda a gente) e a auditoria fica com IPs errados. | `middleware.ts` envia `CF-Connecting-IP` + segredo partilhado; `common/web/ClientIp.java` só aceita o cabeçalho com segredo válido |
| ✅ P4 | **Gestão de trilhas, módulos, salas, tarefas e exames** | Só existem no seed de desenvolvimento. Em produção a plataforma arranca vazia (sem trilhas nem exames). | Opção rápida: migração `V3__conteudo_inicial.sql` revista; opção completa: API + ecrã em Administração → Conteúdos |
| ✅ P5 | **Envio de email em produção** | Sem email não há verificação de conta. O Render gratuito bloqueia SMTP; faltam `starttls`/`auth` no perfil `prod`. | `application-prod.yml` (propriedades SMTP) ou envio por API HTTP no `IdentityMailer` |
| ✅ P6 | **Porta do Render** | O backend escuta em 8080; o Render usa `PORT`. | `application.yml`: `server.port: ${PORT:8080}` ou variável `PORT=8080` |
| ✅ P7 | **Adaptador Cloudflare** | Faltam `@opennextjs/cloudflare`, `wrangler.jsonc` e os scripts `preview`/`deploy`. | `frontend/` (ver `deploy.md` §5) |
| ✅ P8 | **Primeiro administrador** | Não há forma de criar o primeiro ADMIN pela interface. | Procedimento SQL documentado em `deploy.md` §7; opcional: comando de bootstrap com variável de ambiente |
| P9 | **Confirmar o domínio institucional** | `ALLOWED_EMAIL_DOMAIN` vazio recusa todos os registos. | Variável no Render (decisão da AE) |
| P10 | **Agendadores e Neon** | Os agendadores consultam a base a cada 1–5 s, por isso a Neon nunca suspende a computação. | Decidir o plano Neon, ou espaçar os agendadores fora das janelas de contest |

## 🟠 Falta para o MVP (spec / design)

### Contas e segurança
- ✅ **Recuperar palavra-passe («Esqueci-me»)** e **contas por convite** (definição obrigatória da palavra-passe): feito.
- **Códigos de recuperação do 2.º factor**: ainda não existem. ✅ O admin já consegue repor o 2.º factor de alguém.
- **Protecção contra reutilização de códigos TOTP** dentro da mesma janela de 30 s.
- **Rate limiting sem «fail-open»** no login: se o Redis falhar, o limite deixa de existir (`RateLimiter`).
- **Eliminação efectiva da conta**: o pedido fica em `DELETION_REQUESTED`; falta o processo de anonimização/eliminação
  e o email de confirmação (spec §4.1 CA 5, SEC-13).
- **Exportação de dados incompleta**: inclui conta, tentativas, submissões e pontos; faltam exames e contests
  (`PersonalDataContributor` nos módulos `exam` e `contest`).
- ✅ **Convidar utilizadores** (botão «Convidar» na Administração): feito.
- **Auditoria das operações do briefing §7.5**: o briefing não está no repositório, por isso a lista não foi
  confrontada. Hoje são auditados login/MFA, perfis, estado, exercícios, fontes, contests, equipas, exames e Scoreboard.
- **`SESSION_SECRET`** está documentada mas não é usada pelo código: usar ou remover.
- **Uploads (SEC-09)**: não existem (só são precisos para o tipo G, posterior).

### Aprendizagem e correcção
- ✅ **Gestão de conteúdo** (Administração → Conteúdos) e pacote de conteúdo v1. Falta: editar textos de trilhas/salas já criadas,
  reordenar e apontar tarefas para uma nova versão publicada de um exercício.
- **Banco de perguntas dos exames**: as perguntas são escolhidas por prefixo de tema (`questionPoolRule.topic`). Falta o
  sorteio por dificuldade e um ecrã para criar exames.
- **Requisitos de conclusão da trilha** (ex.: salas ≥ 70 % e exame ≥ 10/20): são só texto; não são verificados nem
  produzem «trilha concluída» formal.
- **Tipo F com avaliação por revisor**: hoje a justificação é avaliada só por regra (tamanho e termos).
- **Classificação de contas no tipo D** (spec §4.4: «classificação das contas»): corrige valores e relações, não a
  classificação.
- **Dicas no contest**: não existem no modo contest.

### Contests
- **Fase final / júri / grelha pública**: o design mostra «Ver grelha de avaliação da fase final»; o júri é posterior,
  mas a grelha pública (`Rubric`) devia existir antes do contest.
- **Indicadores de anomalia** para o júri (spec §4.15): não existem.
- **Edição de contest pela interface**: a API `PATCH /admin/contests/{id}` existe; o ecrã só cria, publica, publica
  resultados e encerra.
- **Página de equipa** (mapa do produto) e saída voluntária de um membro.

### Interface (design)
- **Notificações** (sino na barra superior do design).
- **Cartão do certificado** no portefólio: aparece como «fase posterior».
- **Fila offline das submissões**: os rascunhos ficam locais quando não há ligação, mas as submissões não são reenviadas
  automaticamente ao voltar a ligação (o design diz «são enviadas quando voltar»).
- **Pesquisa global** de contas do PGC na barra superior (o design menciona «contas do PGC»; hoje pesquisa salas).
- **Marca do «Ao vivo»** no menu só aparece quando um contest está a decorrer: confirmar se é o comportamento desejado.

## 🟡 Qualidade, testes e operação

- **Acessibilidade**: há navegação por teclado, foco visível, `prefers-reduced-motion` e textos nos estados, mas não
  houve auditoria WCAG 2.1 AA (contraste medido, leitor de ecrã, axe-core nos testes E2E).
- **Testes de permissões completos**: a matriz §3.2 está testada por amostragem; falta um teste por endpoint com cada
  perfil autorizado e um não autorizado (checklist §7).
- **Testes de número de queries** nos endpoints críticos (CLAUDE.md §6): não existem.
- **E2E de contest ao vivo** (SSE, congelamento visto por duas contas) e de **autoria/revisão** no browser.
- **k6**: o script `infra/k6/contest-submissions.js` existe mas não foi executado; faltam metas quantitativas (spec CON-09).
- **CI**: o workflow foi escrito mas nunca correu no GitHub (o repositório ainda não tem commits deste trabalho).
- **Observabilidade**: logs em JSON com `traceId`; faltam métricas, alertas e um destino para os logs.
- **Backups**: configurar o *point-in-time restore* da Neon e testar uma reposição.
- **Desempenho**: `ProgressService.snapshot` recalcula todas as salas em cada pedido de catálogo/painel; está bem para
  dezenas de salas, mas convém cache quando o conteúdo crescer.
- **Idempotência** só nas submissões; outras escritas repetíveis (criar equipa, convites) não aceitam `Idempotency-Key`.
- **Graphify**: correr `/graphify . --update` numa sessão nova do Claude Code para incluir a documentação no grafo
  (a CLI `graphify update .` só re-extrai código).

## ⚪ Fases posteriores (spec §11)
Simulação de empresa · mercado simulado · laboratório de dados · processos selectivos · avaliação por júri (caso
aberto, tipo G) · certificados com código de verificação · outros cursos. Os ecrãs existem como «Fase posterior».

## 📋 A confirmar pela equipa (não é código)
Ver também [`content/fontes-normativas.md`](content/fontes-normativas.md) e o Anexo A/B de [`spec.md`](spec.md).
- Domínio institucional; nomes oficiais dos cursos.
- Diploma e versão do PGC; códigos das subcontas do IVA (hoje marcadores `34.5.x` / `34.5.y`).
- Versões em vigor e artigos do Código do IVA e restantes diplomas; taxa de IVA usada nos exercícios.
- Valores de pontuação, penalizações, bónus, conquistas, bloqueio de equipas, congelamento.
- Pontos de contest no ranking geral (hoje = pontos da equipa) e regra de exames repetidos (hoje conta a primeira nota).
- Limites de rate limiting, validade dos links (24 h), tolerância de rede dos exames (2 s).
- Política dos alertas legais (`UNPUBLISH` ou `WARN`).
- Texto final da política de privacidade e aplicabilidade da Lei n.º 22/11.
