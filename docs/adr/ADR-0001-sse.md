# ADR-0001 — Tempo real nos contests: SSE

- **Estado:** aceite (spec §10)
- **Data:** 01/11/2026

## Contexto
Os contests precisam de relógio do servidor, mudanças de estado, ranking ao vivo e estado das submissões no cliente.
O fluxo é quase todo servidor → cliente; as submissões já seguem por REST com `Idempotency-Key`.

## Decisão
Usar **Server-Sent Events** em `GET /api/v1/contests/{id}/events`, com os eventos:

| Evento | Destinatários | Conteúdo |
|---|---|---|
| `clock` | todos (ao ligar e a cada 5 s) | `serverNow`, `state` |
| `state` | todos | novo estado do ciclo de vida |
| `scoreboard` | todos | aviso para voltar a pedir `GET /scoreboard` (as regras de congelamento aplicam-se no GET) |
| `submission` | só membros da equipa | id, problema, estado, certo/errado |

- O cliente usa `EventSource` (reconexão nativa) e cookie de sessão; o pedido passa pelas mesmas regras de autorização.
- Os eventos são emitidos **depois do commit** (`@TransactionalEventListener`), nunca dentro da transacção.
- O `scoreboard` não leva dados para não contornar o congelamento.

## Consequências
- Simples de proteger e de fazer passar por proxies (o backend envia `Cache-Control: no-cache` e `X-Accel-Buffering: no`).
- **Limitação actual:** o registo de subscritores é em memória de uma instância. Com várias instâncias do backend,
  publicar os eventos por Redis pub/sub (já existe Redis na stack) antes de escalar horizontalmente.
- Se surgir colaboração em tempo real entre membros da equipa, reavaliar WebSocket.
