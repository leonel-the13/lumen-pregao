# Deploy — Cloudflare (frontend) · Render (backend) · Neon (PostgreSQL)

> Estado: guia preparado a 01/11/2026. Já implementado no código: IP real do cliente (`PROXY_SECRET`), email pela API do Resend
> (`RESEND_API_KEY`), `PORT`, adaptador OpenNext (`frontend/wrangler.jsonc`, `pnpm cf:deploy`) e blueprint `render.yaml`. Os pontos marcados ⚠ exigem uma alteração ao código
> ou uma verificação antes de ir para produção (estão também em [`pendencias.md`](pendencias.md)).

## 1. Arquitectura

```
Browser ──https──▶ Cloudflare (Next.js via OpenNext)  ──https──▶ Render (Spring Boot, Docker)
                    · páginas e Server Components                 · /api/v1/**
                    · /api/v1/* reencaminhado (rewrite)           ├──▶ Neon (PostgreSQL)
                                                                  └──▶ Redis (Render Key Value)
```

**Decisão importante:** o browser fala **só** com o domínio do frontend. O `next.config.ts` reencaminha `/api/v1/*`
para o backend. Assim:
- os cookies `LP_SESSION` e `XSRF-TOKEN` ficam no domínio do frontend (first-party, `SameSite=Lax`), sem CORS;
- os Server Components chamam o backend directamente com o cookie do utilizador (`BACKEND_INTERNAL_URL`);
- o domínio do Render nunca é usado pelo browser.

O Redis é obrigatório (rate limiting, rankings, revogação de sessões). Não foi mencionado no plano: a opção mais simples
é o **Render Key Value** na mesma região do backend.

## 2. Neon (PostgreSQL)

1. Criar o projecto (PostgreSQL 16 ou 17) na região mais próxima do Render (ex.: ambos em Frankfurt / `eu-central-1`).
2. Criar a base `lumen` e um utilizador dedicado (não usar o dono do projecto na aplicação, se possível).
3. Usar a **ligação directa** (hostname **sem** `-pooler`):
   - o Flyway corre as migrações no arranque e a Neon recomenda ligação directa para migrações;
   - o PgBouncer da Neon trabalha em modo *transaction*.
4. Converter a connection string da Neon para JDBC. A Neon dá `postgresql://user:pass@ep-xxx.region.aws.neon.tech/lumen?sslmode=require`;
   o backend espera variáveis separadas:
   ```
   DB_URL=jdbc:postgresql://ep-xxx.region.aws.neon.tech/lumen?sslmode=require
   DB_USER=<utilizador>
   DB_PASSWORD=<palavra-passe>
   ```
5. Limitar o pool: `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=10` (o valor por defeito no código é 20).

⚠ **Scale to zero não vai acontecer.** O backend tem agendadores que consultam a base de dados a cada 1–5 s
(ciclo de vida dos contests, fila de correcção, exames expirados). A computação da Neon fica sempre activa: confirma os
limites de horas de computação do teu plano antes de escolher o plano gratuito.

**Migrações:** só por Flyway, no arranque do backend. Nunca editar `V1`/`V2` depois do primeiro deploy: criar `V3__…`.

## 3. Redis — Render Key Value

1. Criar uma instância Key Value na **mesma região** do backend.
2. Copiar o *Internal URL* (ex.: `redis://red-xxxx:6379`) para `REDIS_URL` no backend.
3. Política de memória: `noeviction` (as chaves de rate limiting e de revogação não devem desaparecer).
   Os rankings reconstroem-se a partir da base de dados (`POST /api/v1/admin/rankings/rebuild`).

## 4. Render (backend)

1. **New → Web Service → repositório Git**, com runtime **Docker**, *Root Directory* `backend` e Dockerfile `backend/Dockerfile`.
2. **Health check path:** `/actuator/health`.
3. **Porta:** o backend escuta em 8080. ⚠ Define `PORT=8080` nas variáveis do serviço, ou altera `server.port` para
   `${PORT:8080}` em `application.yml`.
4. **Variáveis de ambiente:**

| Variável | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` (**nunca** `dev`: o perfil dev cria contas de teste) |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Neon (ver §2) |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | `10` |
| `REDIS_URL` | Internal URL do Key Value |
| `MFA_ENC_KEY` | `openssl rand -base64 32` (**guardar cópia segura**: se mudar, os 2.º factores registados deixam de funcionar) |
| `SESSION_SECRET` | `openssl rand -base64 32` (reservado; hoje não é usado pelo código) |
| `ALLOWED_EMAIL_DOMAIN` | domínio institucional do ISPTEC (**a confirmar**; vazio = ninguém se regista) |
| `APP_BASE_URL` | URL público do frontend, ex.: `https://pregao.exemplo.ao` (usado nos links dos emails) |
| `SESSION_COOKIE_SECURE` | `true` |
| `RESEND_API_KEY`, `MAIL_FROM` | API do Resend; `MAIL_FROM` num domínio verificado no Resend |
| `PROXY_SECRET` | `openssl rand -hex 32`, igual ao secret do Worker |

5. **Plano:**
   - ⚠ Nas instâncias gratuitas, o serviço adormece por inactividade. Os agendadores param e as transições dos contests
     (abrir sala de espera, congelar, fechar) atrasam até alguém acordar o serviço. As ligações SSE também caem.
   - ⚠ As instâncias gratuitas **bloqueiam SMTP** (portas 25, 465 e 587) desde Setembro de 2025. Sem SMTP não há emails
     de verificação, logo **ninguém consegue activar a conta**.
   - **Recomendação:** instância paga (Starter) para produção. A alternativa é enviar email por API HTTP (Mailgun, Postmark…),
     o que exige alterar o `IdentityMailer`.
   - Com SMTP na porta 587, ⚠ falta activar STARTTLS e autenticação em `application-prod.yml`:
     ```yaml
     spring:
       mail:
         properties:
           mail.smtp.auth: true
           mail.smtp.starttls.enable: true
     ```
6. **Uma só instância.** As sessões e os subscritores SSE vivem em memória (ver `pendencias.md` P1). Não activar
   escalonamento horizontal enquanto isso não mudar.

## 5. Cloudflare (frontend)

O frontend é Next.js 15 com Server Components, middleware e rewrites. No Cloudflare, isso corre em **Workers** através do
adaptador **OpenNext** (`@opennextjs/cloudflare`); não serve um export estático (Pages estático).

> A documentação da Cloudflare indica agora o **vinext** como recomendação para aplicações Next.js novas, mantendo o
> OpenNext para aplicações existentes. Os passos abaixo usam OpenNext; avalia o vinext antes de fixar a escolha.

1. **Instalar o adaptador** (⚠ ainda não feito no repositório):
   ```bash
   cd frontend
   pnpm add -D @opennextjs/cloudflare wrangler
   ```
2. Criar `frontend/wrangler.jsonc` com o flag `nodejs_compat` e `compatibility_date` ≥ `2024-09-23`, e o
   `open-next.config.ts` gerado pelo assistente do OpenNext. Acrescentar ao `package.json`:
   ```json
   "preview": "opennextjs-cloudflare build && opennextjs-cloudflare preview",
   "deploy": "opennextjs-cloudflare build && opennextjs-cloudflare deploy"
   ```
3. **Variáveis — atenção ao momento em que são lidas:**

| Variável | Quando | Valor |
|---|---|---|
| `BACKEND_INTERNAL_URL` | **build** (os rewrites do `next.config.ts` são fixados no build) **e** runtime (Server Components) | `https://<servico>.onrender.com` |
| `NEXT_PUBLIC_API_BASE` | build | `/api/v1` |

   No Cloudflare, definir `BACKEND_INTERNAL_URL` tanto nas variáveis de build como nas do Worker.
4. **Domínio:** associar o domínio personalizado ao Worker. É este o URL a pôr em `APP_BASE_URL` no Render.
5. **Testar antes de publicar** (`pnpm preview`):
   - login e cookies (`LP_SESSION` deve aparecer no domínio do frontend, `HttpOnly`, `Secure`);
   - ⚠ **SSE através do Worker**: abrir um contest em curso e confirmar que os eventos `clock` chegam a cada 5 s.
     Se o proxy do Worker não fizer streaming, a alternativa é ligar o `EventSource` directamente ao Render, mas isso
     exige cookie `SameSite=None` e CORS com credenciais (ver `pendencias.md` P2);
   - middleware (redirecção para `/entrar` sem sessão).
6. O `frontend/Dockerfile` e o `output: "standalone"` servem só para Docker Compose e outros alojamentos. Se o build do
   OpenNext se queixar do `standalone`, remove essa linha para o Cloudflare.

## 6. ⚠ IP do cliente e rate limiting

Com o Cloudflare à frente, os pedidos chegam ao Render vindos do Worker. O backend usa `request.getRemoteAddr()`
(com `forward-headers-strategy: framework` em `prod`), mas o IP que vê pode ser o do Cloudflare e não o do estudante.
Consequências:
- o limite de **20 falhas de login por IP** passa a ser partilhado por **todos** os utilizadores, e um atacante pode
  bloquear o login de todos;
- a auditoria regista IPs do Cloudflare.

**Correcção necessária antes de produção** (P3 em `pendencias.md`):
- o `middleware.ts` acrescenta `X-Client-IP` com o valor de `CF-Connecting-IP` e um cabeçalho secreto partilhado;
- o backend só confia nesse cabeçalho quando o segredo coincide.

## 7. Primeiro arranque em produção

1. O deploy do backend aplica `V1` e `V2` (perfis, cursos, fontes «a confirmar», contas PGC provisórias, empresas
   fictícias, época, conquistas). **Não há trilhas, salas nem exercícios** (o seed só existe no perfil dev).
2. **Primeiro administrador.** Não há forma de o criar pela interface:
   1. regista-te normalmente com o email institucional e confirma o email;
   2. no SQL Editor da Neon, dá-te o perfil ADMIN:
      ```sql
      INSERT INTO user_roles (user_id, role_id)
      SELECT u.id, r.id FROM users u, roles r
      WHERE u.email = 'o-teu-email@dominio' AND r.code = 'ADMIN';
      ```
   3. Entra de novo: o login pede o registo do 2.º factor (aplicação TOTP).
   4. Atribui os outros perfis (autor, revisor, organizador) em **Administração → Utilizadores**.
3. **Conteúdo.** Os exercícios criam-se em Autoria e publicam-se depois de revistos. As **trilhas, módulos, salas,
   tarefas e exames ainda não têm ecrã de gestão** (P4): têm de ser carregados por uma migração `V3__conteudo_inicial.sql`,
   revista pela equipa editorial.
4. Confirmar `/actuator/health` = `UP` e `GET /api/v1/scoring-rules` através do domínio do frontend.

## 8. Checklist de go-live

- [ ] `SPRING_PROFILES_ACTIVE=prod` (nunca `dev`)
- [ ] `MFA_ENC_KEY` gerada e guardada fora do Render
- [ ] `ALLOWED_EMAIL_DOMAIN` confirmado com a AE
- [ ] Email de verificação recebido numa conta real (SMTP ou API)
- [ ] IP do cliente correcto nos logs de auditoria (§6)
- [ ] SSE a funcionar através do Cloudflare (§5.5)
- [ ] Backup / *point-in-time restore* da Neon configurado
- [ ] Instância Render sem adormecer durante contests
- [ ] Primeiro admin criado e com 2.º factor
- [ ] Conteúdo inicial revisto e publicado
- [ ] Textos «a confirmar» revistos (`docs/content/fontes-normativas.md`)

## Fontes consultadas
- [Render — free web services deixam de permitir SMTP](https://render-web.onrender.com/changelog/free-web-services-will-no-longer-allow-outbound-traffic-to-smtp-ports)
- [Neon — connection pooling (PgBouncer em modo transaction, sufixo `-pooler`)](https://neon.tech/docs/connect/connection-pooling/)
- [Neon — ferramentas que precisam de ligação directa](https://neon.com/faqs/postgres-tools-high-volumes-short-lived-connections)
- [Cloudflare — guia OpenNext](https://developers.cloudflare.com/workers/framework-guides/web-apps/opennext/index.md)
- [OpenNext Cloudflare — get started (`nodejs_compat`, data de compatibilidade)](https://opennext.js.org/cloudflare/0.3/get-started)
- [Next.js — rewrites para URLs externos](https://github.com/vercel/next.js/blob/f2336f81f99f139c712f53d1048b723bb263a18e/docs/api-reference/next.config.js/rewrites.md)
