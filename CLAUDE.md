# CLAUDE.md — Lúmen Lab

Referência de trabalho para o Claude Code neste repositório.
- **O que construir:** `docs/spec.md`. **O que vês:** o design aprovado (canvas «Lúmen Pregão — Design»; o produto chama-se agora **Lúmen Lab**: https://claude.ai/artifact/FrCErX1C4NuR7hTio4iWZS).
- **Em caso de conflito:** `spec.md` > design > este ficheiro. Se encontrares uma lacuna, pára e pergunta. Não inventes funcionalidades nem comportamentos.

---

## 0. Antes de escrever código: Graphify

1. Com o repositório criado e `CLAUDE.md` + `docs/spec.md` presentes, corre o Graphify **antes** de qualquer implementação:
   ```bash
   pip install graphifyy && graphify install   # uma vez por máquina
   /graphify .                                 # dentro do Claude Code: gera graphify-out/
   ```
2. Lê `graphify-out/GRAPH_REPORT.md` para perceber a estrutura, as relações e as dependências antes de gerar código.
3. Depois de mudanças estruturais, actualiza o grafo com `/graphify . --update`.
4. Opcional: `graphify hook install` mantém o grafo actualizado a cada commit.
5. **Não inventes comandos nem opções do Graphify.** Se algum comando acima falhar ou diferir da versão instalada, segue `graphify --help` e a documentação da ferramenta, e regista a diferença aqui.
6. **Diferenças registadas (01/11/2026):** o `pip` não estava no PATH — usar `python3 -m pip install graphifyy`.
   `/graphify .` é uma skill do Claude Code instalada por `graphify install` e só fica disponível numa sessão nova;
   na sessão de instalação usou-se o equivalente da CLI, `graphify update .` (re-extracção de código, sem LLM).

---

## 1. Arquitectura do repositório (monorepo)

```
/
├─ CLAUDE.md
├─ docs/
│  ├─ spec.md                  # especificação (fonte de verdade funcional)
│  ├─ adr/                     # decisões de arquitectura (ADR-0001-sse.md, …)
│  └─ content/                 # guias editoriais, fontes normativas registadas
├─ frontend/                   # Next.js (App Router, TypeScript)
│  ├─ app/
│  │  ├─ (public)/             # landing, entrar, registo, verificação, portefólio público
│  │  ├─ (app)/                # área autenticada: inicio, trilhas, salas, exames, competicoes, rankings, portefolio
│  │  └─ (gestao)/             # autoria, juri, admin (layout com verificação de perfil)
│  ├─ components/
│  │  ├─ ui/                   # Button, Input, MoneyInput, Select, Badge, Card, Tabs, Seg, Modal, Toast, Pagination, Timer, Progress
│  │  ├─ finance/              # JournalEditor, StatementEditor, TaxStepper, AccountSearch, LedgerTable
│  │  ├─ contest/              # Countdown, LiveScoreboard, SubmissionStatus
│  │  └─ charts/               # wrappers ECharts
│  ├─ lib/                     # api client, formatação Kz, sse, auth helpers
│  ├─ styles/tokens.css        # tokens do design (escuro/claro)
│  └─ tests/e2e/               # Playwright
├─ backend/                    # Spring Boot 3, Java 21
│  ├─ src/main/java/ao/lumen/pregao/
│  │  ├─ common/               # erros, auditoria, segurança partilhada, idempotência
│  │  ├─ identity/             # utilizadores, perfis, autenticação, MFA
│  │  ├─ learning/             # trilhas, módulos, salas, tarefas, tentativas, submissões
│  │  ├─ grading/              # motores de correcção por tipo (A–G), avaliação de expressões
│  │  ├─ authoring/            # exercícios, versões, fluxo editorial, fontes legais
│  │  ├─ exam/
│  │  ├─ contest/              # contests, equipas, scoreboard, agendador, SSE
│  │  ├─ ranking/              # PointEvent, projecção Redis
│  │  ├─ portfolio/            # perfil, conquistas, certificados
│  │  └─ simulation/           # [posterior] empresa, mercado
│  ├─ src/main/resources/
│  │  ├─ application.yml
│  │  ├─ application-dev.yml · application-test.yml · application-prod.yml
│  │  └─ db/migration/         # Flyway V1__init.sql, V2__…
│  └─ src/test/java/…          # JUnit 5 + Testcontainers
├─ infra/
│  ├─ docker-compose.yml       # postgres, redis, mailpit, backend, frontend
│  └─ k6/                      # testes de carga
└─ .github/workflows/          # CI: build, testes, gitleaks
```

Cada módulo do backend tem os pacotes `api` (controllers, DTOs), `app` (serviços, casos de uso), `domain` (entidades, regras) e `infra` (repositórios, integrações). Um módulo não acede ao repositório de outro; usa o serviço público desse módulo.

---

## 2. Stack obrigatória (não substituir)

| Camada | Tecnologia |
|---|---|
| Frontend | Next.js (App Router), TypeScript, React Server Components, CSS com tokens (`styles/tokens.css`) ou Tailwind configurado com os mesmos tokens; Apache ECharts |
| Backend | Java 21, Spring Boot 3, Spring Web, Spring Data JPA + Hibernate, Spring Security, Bean Validation, springdoc-openapi |
| Persistência | PostgreSQL + Flyway |
| Cache / tempo real | Redis (Sorted Sets para rankings, filas), SSE (ver ADR-0001) |
| Testes | JUnit 5, Testcontainers, Playwright; k6 para carga |
| Infra dev | Docker Compose |

É proibido introduzir outra base de dados, outro ORM, outro framework de frontend ou outra biblioteca de UI completa sem um ADR aprovado.

---

## 3. Comandos

```bash
# infra local
docker compose -f infra/docker-compose.yml up -d postgres redis mailpit

# backend
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # arranca (aplica migrações Flyway)
./mvnw test                                              # unitários + Testcontainers
./mvnw verify                                            # build completo
./mvnw flyway:info                                       # estado das migrações

# frontend
cd frontend
pnpm install
pnpm dev                 # http://localhost:3000
pnpm build && pnpm start
pnpm lint && pnpm typecheck
pnpm test:e2e            # Playwright

# tudo em contentores
docker compose -f infra/docker-compose.yml up --build

# carga
k6 run infra/k6/contest-submissions.js
```

O gestor de pacotes e o wrapper Maven são os indicados acima. Se mudarem, actualiza esta secção.

---

## 4. Convenções de código

**Gerais**
- O código, os identificadores e os commits ficam em inglês.
- Todo o texto visível ao utilizador fica em português de Angola/Portugal (ver §9).
- Os commits seguem Conventional Commits (`feat(contest): …`).

**Backend**
- **Organização:**
  - pacotes por módulo de domínio (§1);
  - controllers finos, sem lógica de negócio;
  - serviços transaccionais em `app`.
- **DTOs:**
  - em `record`;
  - nunca devolver entidades JPA;
  - DTOs de estudante e de autor são classes separadas, para que a solução nunca vaze.
- **Erros:**
  - excepções de domínio → `@RestControllerAdvice` → `application/problem+json`, com `type`, `title`, `status`, `code` estável (`ROOM_LOCKED`, `SUBMISSION_AFTER_DEADLINE`, …) e `detail` seguro.
- **Validação:** Bean Validation nos DTOs; as regras de negócio vivem no domínio e no serviço, não no controller.
- **Logging:**
  - SLF4J em JSON;
  - `traceId` por pedido;
  - nunca registar palavras-passe, tokens, soluções nem dados pessoais desnecessários.
- **Dinheiro:**
  - `BigDecimal` com escala 2 e `RoundingMode` explícito;
  - nunca `double` para valores.
- **Tempo:**
  - `Instant` em UTC em toda a persistência;
  - apresentação em `Africa/Luanda`;
  - o relógio é injectado (`Clock`) para os testes.

**API**
- Tudo em `/api/v1`, com recursos no plural e kebab-case.
- `202 Accepted` para submissões assíncronas.
- Paginação por cursor (`?cursor=&limit=`); `limit` ≤ 100.
- O contrato OpenAPI é gerado e verificado no CI. Qualquer mudança de contrato actualiza `docs/spec.md` §6.

**Frontend**
- **Dados:**
  - Server Components por defeito; `"use client"` só para interacção (editores, SSE, cronómetros);
  - pedidos com cookie de sessão; nada sensível em `localStorage`.
- **Componentes:**
  - os de `components/ui` seguem o canvas (variantes e estados);
  - não criar componentes duplicados.
- **Formatação:** `lib/format.ts` (`formatKz`, `formatPercent`, `formatAccount`): Kz depois do valor, espaço fino nos milhares, vírgula decimal, números tabulares.
- **Cronómetros:** usam `serverNow − clientNow` como desvio; nunca decidem prazos.

---

## 5. Configuração

- **`application.yml`:** só valores comuns e seguros; os perfis `dev`, `test` e `prod` em ficheiros próprios.
- **Variáveis de ambiente** (nunca no repositório):
  - `DB_URL`, `DB_USER`, `DB_PASSWORD`;
  - `REDIS_URL`;
  - `MAIL_*`;
  - `MFA_ENC_KEY` (as sessões ficam no Redis com Spring Session; não há `SESSION_SECRET`);
  - `ALLOWED_EMAIL_LOCAL_PATTERN` (parte local do email de estudante; por defeito `^[0-9]{8}$`);
  - `ALLOWED_EMAIL_DOMAIN` (**a confirmar**);
  - `APP_BASE_URL`.
- **`.env.example`:** documenta todas as variáveis, sem valores reais. `.env*` está no `.gitignore`.
- **Frontend:** só `NEXT_PUBLIC_*` para valores públicos (ex.: `NEXT_PUBLIC_API_BASE`); nada secreto no bundle.
- **CI:** o gitleaks falha o build se encontrar segredos.

---

## 6. Persistência e transacções

- **Migrações:**
  - o esquema muda **só** por Flyway (`V{n}__descricao.sql`);
  - nunca editar uma migração aplicada;
  - `spring.jpa.hibernate.ddl-auto=validate` em todos os perfis.
- **Entidades:**
  - `id` UUID;
  - relações `LAZY` por defeito;
  - sem `CascadeType.ALL` em agregados partilhados;
  - `equals/hashCode` pelo id.
- **`@Version`:** obrigatório em `Team`, `Scoreboard`, `Contest`, `Attempt`, `ExamSession`. Um `OptimisticLockException` dá uma nova tentativa limitada no serviço, ou 409.
- **Integridade:** chaves estrangeiras, `NOT NULL` e `UNIQUE` declarados na base de dados, e não só na aplicação (ver índices únicos em `spec.md` §5).
- **Transacções:**
  - curtas, em serviços;
  - nenhuma chamada externa (email, Redis, SSE) dentro da transacção: usar eventos `@TransactionalEventListener(AFTER_COMMIT)`.
- **Submissões:** persistir primeiro (QUEUED), corrigir depois. A pontuação persistida (`PointEvent`, `Scoreboard`) é a verdade; o Redis é uma projecção reconstruível.
- **Imutabilidade:**
  - `ExerciseVersion` publicada, `PointEvent`, `AuditLog` e `JuryAssessment` confirmada nunca são actualizados;
  - corrige-se com novos registos.
- **N+1:** usar `@EntityGraph` ou projecções; os testes de repositório verificam o número de queries nos endpoints críticos.

---

## 7. Checklist de segurança por Pull Request (obrigatória)

Copia para a descrição do PR e marca:

- [ ] **Autenticação:** os endpoints novos exigem sessão (ou estão justificados como públicos).
- [ ] **Autorização:** `@PreAuthorize` com o perfil certo, segundo a matriz em `spec.md` §3.2.
- [ ] **Propriedade:** o serviço verifica que o recurso pertence ao utilizador, à equipa ou ao júri atribuído.
- [ ] **Protecção das respostas:** nenhum DTO expõe `solutionSpec`, `paramSpec` nem tolerâncias secretas (há teste de serialização).
- [ ] **Validação de inputs:** Bean Validation nos DTOs; limites de tamanho; markdown sanitizado.
- [ ] **Rate limiting:** aplicado onde há login, submissões, convites ou uploads.
- [ ] **Uploads:** tipo por magic bytes, tamanho máximo, nome gerado, armazenamento fora da raiz web.
- [ ] **Dados pessoais:** só o necessário; nada em logs; exportação e eliminação continuam a funcionar.
- [ ] **Auditoria:** as operações sensíveis geram `AuditLog`.
- [ ] **Prazos:** decisões temporais usam o `Clock` do servidor.
- [ ] **Idempotência:** as escritas repetíveis aceitam `Idempotency-Key`.
- [ ] **Testes de permissões:** cada perfil autorizado e pelo menos um não autorizado.
- [ ] **Segredos:** nenhum no diff; gitleaks verde.

---

## 8. Regras editoriais (conteúdo académico)

- Não copiar livros, manuais nem enunciados protegidos. Os exercícios são originais.
- Cada exercício tem referência (`LegalSource`) com fonte, artigos, versão, data de consulta e validade.
- **Referências não confirmadas:** ficam com `confirmed=false` e aparecem como «a confirmar».
- **Publicação:** nada é publicado sem passar por Rascunho → Revisão → Aprovação → Publicação, e o autor não aprova o próprio exercício.
- **Empresas:** sempre fictícias (`Company.fictional = true`). Não usar dados de empresas reais nem materiais de processos selectivos privados sem autorização.
- **Legislação:** não inventar legislação, números de diplomas, taxas nem estatísticas. Os valores normativos usados como parâmetros (ex.: taxa de IVA) ligam-se a uma `LegalSource` e ficam «a confirmar» até um revisor validar.
- **Nos seeds e fixtures de desenvolvimento:** dados claramente fictícios.

---

## 9. Idioma e terminologia

- **Norma:** toda a interface em **português de Angola/Portugal**, na norma usada no briefing («objectivo», «correcção», «selecção», «Activo»). Evitar brasileirismos.
- **Termos fixos:** Activo, Passivo, Capital próprio, Proveitos, Custos, Balancete, Demonstração de resultados, IVA suportado, IVA liquidado, apuramento, Meios monetários, Existências, Terceiros.
- **Glossário:** a terminologia contabilística e fiscal vem do glossário em `spec.md` §2. Termos novos acrescentam-se lá primeiro.
- **Textos da interface:** usar os textos aprovados no canvas; mensagens de erro dizem o que aconteceu e o que fazer.
- **Rodapé:** em todas as páginas, «Iniciativa da Associação de Estudantes · Não é um canal oficial do ISPTEC».

---

## 10. Design e acessibilidade

- **Tokens:** são os do canvas (escuro por defeito, claro completo, preferência persistida no perfil). O amarelo `#FFC20E` é o único acento. Verde e vermelho só aparecem em estados, sempre com ícone ou texto.
- **Fontes:**
  - Archivo para títulos e interface;
  - Instrument Serif itálico para ênfases pontuais;
  - JetBrains Mono para números, Kz, contas do PGC, códigos e cronómetros.
- **Acessibilidade:**
  - WCAG 2.1 AA;
  - navegação completa por teclado;
  - foco visível (anel amarelo);
  - alvos ≥ 44 px;
  - `prefers-reduced-motion` respeitado.
- **Breakpoints:** < 720 · 720–1079 · 1080–1439 · ≥ 1440. Os editores contabilísticos são prioritários em desktop e passam a cartões em telemóvel.

---

## 11. Definição de concluído (DoD)

Uma funcionalidade só está concluída quando:
1. cumpre os critérios de aceitação de `docs/spec.md`;
2. respeita o design aprovado (componentes, estados, textos);
3. tem validação no frontend e no backend, quando aplicável;
4. implementa e testa as permissões (incluindo a propriedade);
5. tem testes adequados (unitários, integração com Testcontainers, E2E Playwright nos fluxos críticos);
6. passa a checklist de segurança (§7);
7. é acessível (teclado, contraste, leitores de ecrã) e funciona nos breakpoints;
8. não introduz erros de build, lint ou typecheck;
9. tem a documentação actualizada (`spec.md`, OpenAPI, ADR se houve decisão, grafo do Graphify actualizado).

---

## 12. Ordem de implementação (MVP)

1. Infra (Compose), esqueleto do backend e do frontend, tokens e componentes `ui`.
2. Identity: registo, verificação, login, MFA, RBAC, auditoria.
3. Learning: trilhas, módulos, salas, tarefas; tentativas e submissões idempotentes.
4. Grading: tipos A–F e o motor paramétrico.
5. Authoring: exercícios, versões, fluxo editorial, `LegalSource` e alertas.
6. Exames.
7. Ranking: `PointEvent`, projecção no Redis, painel inicial.
8. Contest: ciclo de vida, equipas, SSE, congelamento, k6.
9. Estados globais, modo claro, auditoria de acessibilidade, portefólio.

Em cada passo: implementa, testa, actualiza a documentação, corre `/graphify . --update`.
