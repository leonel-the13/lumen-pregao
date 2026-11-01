# Lúmen Pregão

Laboratório digital de competências para o DCSA do ISPTEC: trilhas, salas práticas com correcção no servidor,
exames, rankings e a Maratona de casos. Iniciativa da Associação de Estudantes · Não é um canal oficial do ISPTEC.

- Especificação: [`docs/spec.md`](docs/spec.md) · Regras de trabalho: [`CLAUDE.md`](CLAUDE.md)
- Decisões: [`docs/adr/`](docs/adr) · Referências «a confirmar»: [`docs/content/fontes-normativas.md`](docs/content/fontes-normativas.md)
- Deploy (Cloudflare · Render · Neon): [`docs/deploy.md`](docs/deploy.md) · O que falta: [`docs/pendencias.md`](docs/pendencias.md)
- Contrato da API: [`docs/openapi.json`](docs/openapi.json) (também em `/api/v1/docs` com o backend a correr)

## Arrancar em desenvolvimento
```bash
docker compose -f infra/docker-compose.yml up -d postgres redis mailpit
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev     # http://localhost:8080
cd frontend && pnpm install && pnpm dev                                 # http://localhost:3000
```
Emails de verificação: Mailpit em http://localhost:8025.

Contas de desenvolvimento (fictícias, palavra-passe `lumen-dev-2026!`):

| Email | Perfis |
|---|---|
| kiala.afonso@exemplo.ao | Estudante · capitão da equipa «Partidas Dobradas» |
| marta.neto@exemplo.ao | Estudante · Autora |
| joao.cassoma@exemplo.ao · ana.lopes@exemplo.ao | Estudantes |
| docente.p@exemplo.ao | Revisor · Júri (2.º factor) |
| organizador@exemplo.ao | Organizador de contests (2.º factor) |
| admin@exemplo.ao | Administrador (2.º factor) |

## Testes
```bash
cd backend && ./mvnw test        # unitários + integração (Testcontainers)
cd frontend && pnpm lint && pnpm typecheck && pnpm test:e2e
```
