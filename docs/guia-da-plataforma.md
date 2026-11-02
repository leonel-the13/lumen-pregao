# Lúmen Lab: como a plataforma funciona

> Guia curto para a equipa da Associação de Estudantes. Estado a 02/11/2026 (MVP em teste).
> Iniciativa da Associação de Estudantes · Não é um canal oficial do ISPTEC.

## 1. O que é

O Lúmen Lab é um **laboratório de prática** para os estudantes do ISPTEC. O foco está nas Ciências Sociais Aplicadas
(Contabilidade, Economia e Gestão), mas **qualquer curso** pode participar. Os estudantes:

- seguem **trilhas** (ex.: Contabilidade segundo o PGC, IVA, Análise financeira), divididas em módulos e **salas**;
- resolvem **tarefas** (lançamentos, demonstrações, cálculos de imposto, rácios, escolha múltipla), corrigidas na hora;
- fazem **exames** com cronómetro;
- competem na **Maratona de casos**, sozinhos ou em equipa, com ranking ao vivo;
- ganham **pontos e conquistas**, aparecem nos **rankings** e podem ter um **portefólio** público.

Tudo usa **empresas fictícias**. Os pontos não contam para as notas do ISPTEC.

## 2. Endereços

| O quê | Onde |
|---|---|
| Plataforma (site) | `https://lab.lumenao.tech` |
| Servidor (API) | `https://lumen-pregao-api.onrender.com` |
| Código | repositório no GitHub (`main`) |

O servidor gratuito do Render **adormece** quando ninguém o usa durante algum tempo. O primeiro acesso depois disso pode
demorar cerca de 1 minuto. É normal.

## 3. Quem entra e como

| Quem | Como entra |
|---|---|
| **Estudantes** | Criam conta em **Criar conta** com o email de estudante, que começa pelo número de 8 dígitos (ex.: `20221987@isptec.co.ao`). Recebem um email para confirmar. |
| **Docentes, revisores, júri, organização** | São **convidados** pela administração (Administração → Utilizadores → Convidar). Recebem um email para definir a palavra-passe. |

**Perfis e o que podem fazer:**

| Perfil | Pode |
|---|---|
| Estudante | resolver salas, fazer exames, participar em contests, avaliar salas, dar feedback |
| Autor | criar e editar exercícios |
| Revisor | rever, aprovar e publicar exercícios (nunca os próprios) |
| Júri | avaliar a fase final dos contests |
| Organizador | criar e gerir contests |
| Administrador | tudo, incluindo utilizadores, conteúdos, auditoria e feedback |

Os perfis com privilégios (revisor, júri, organizador, administrador) usam obrigatoriamente o **2.º factor**: uma aplicação
como Google Authenticator ou Microsoft Authenticator. No primeiro login aparece a chave para configurar a aplicação e, a seguir,
**8 códigos de recuperação**. **Guardem esses códigos**: se perderem o telemóvel, cada um substitui o código da aplicação uma vez.

## 4. O dia-a-dia do estudante

1. Entra e vê o **painel** (Início): progresso, sequência de dias e próxima sala recomendada.
2. Abre uma **trilha** e uma **sala**. As salas abrem-se por ordem dentro de cada módulo.
3. Resolve as tarefas. Cada estudante recebe **valores próprios**, por isso copiar não ajuda. A correcção é feita no servidor.
4. Depois de iniciar uma tarefa, pode **avaliar a sala** (1 a 5 estrelas e um comentário).
5. O **sino** no topo mostra as notificações: conquistas, convites de equipa, início e resultados de contests, exames corrigidos.
6. O botão **Feedback** (balão no topo) envia sugestões, erros ou elogios à administração.
7. Em **Perfil e definições**: tema claro/escuro, pseudónimo, esconder-se do ranking, exportar os dados, pedir a eliminação da conta.

## 5. Conteúdos: do rascunho à publicação

Nenhum exercício chega aos estudantes sem revisão:

```
Rascunho → Em revisão → Aprovado → Publicado
```

- O **autor** cria o exercício (Autoria → Novo) e envia-o para revisão.
- O **revisor** confirma as referências e aprova. Quem escreveu não pode aprovar o próprio exercício.
- Depois de publicado, o exercício só muda com uma **nova versão**. As tentativas antigas ficam com a versão antiga.
- Cada exercício indica a **norma** usada (ex.: Código do IVA, PGC). Normas ainda não confirmadas aparecem como «a confirmar».
- Se uma norma muda, os autores recebem um **alerta** para rever os exercícios afectados.

**Pacote de conteúdo inicial:** a plataforma traz 45 exercícios, 26 salas e 3 exames. Para os carregar em produção:

1. Administração → **Conteúdos** → **Importar pacote**.
2. Autoria → **Fila de revisão** → seleccionar → **Aprovar e publicar**.
3. Administração → Conteúdos → pôr as trilhas e as salas como **Publicada**.

Em Administração → Conteúdos também se editam trilhas, salas e tarefas (título, pontos, versão do exercício).

## 6. Contests (Maratona)

1. O **organizador** cria o contest (datas, tamanho das equipas, problemas) e publica-o.
2. Os estudantes inscrevem-se sozinhos ou criam uma **equipa** e convidam colegas por email. Os membros podem sair até ao bloqueio.
3. Antes do início, as equipas **bloqueiam** e abre a sala de espera.
4. No início, os enunciados aparecem. O **relógio oficial é o do servidor**.
5. Perto do fim, o ranking **congela**: cada equipa continua a ver os próprios resultados.
6. A organização **publica os resultados**, e todos os participantes são notificados.

## 7. Administração

| Separador | Para quê |
|---|---|
| Utilizadores | procurar pessoas, mudar perfis e estado, convidar, reenviar convites, repor o 2.º factor, executar pedidos de eliminação |
| Contests | criar, publicar, publicar resultados, encerrar |
| Conteúdos | importar o pacote, criar e editar trilhas, módulos, salas, tarefas e exames |
| Feedback | ler o que os estudantes enviam e marcar como lido ou resolvido |
| Auditoria | ver quem fez o quê (entradas, mudanças de perfil, publicações, …) |

**Pedidos de eliminação:** quando um estudante pede para eliminar a conta, aparece «Eliminação pedida». O administrador
carrega em **Eliminar dados** e escreve `ELIMINAR`. Nome, email e perfis são apagados e a conta sai dos rankings. **Não é reversível.**

## 8. Privacidade e regras

- Páginas públicas: **Termos e condições** (`/termos`), **Política de privacidade** (`/privacidade`), **Regras e fontes** (`/regras`).
- Guardamos só o necessário, não vendemos dados, usamos só cookies essenciais.
- As médias das avaliações só aparecem na página inicial com pelo menos 5 respostas, e nunca com nomes.

## 9. O que ainda falta (importante para os testes)

1. **Email para todos.** O domínio `lumenao.tech` foi configurado no Resend (registos SPF, DKIM e DMARC). Os emails saem de
   `nao-responder@lumenao.tech`. Se algum email não chegar, vejam a pasta de spam e avisem pelo Feedback.
2. Textos legais finais (privacidade, Lei n.º 22/11) e referências normativas por confirmar.
3. Auditoria formal de acessibilidade e teste de carga antes de um contest grande.

A lista técnica completa está em [`pendencias.md`](pendencias.md). O guia de deploy está em [`deploy.md`](deploy.md).

## 10. Como reportar problemas

Usem o botão **Feedback** dentro da plataforma (categoria «Algo não funciona»), com a página onde aconteceu e o que estavam a
fazer. Nunca enviem palavras-passe, chaves do 2.º factor nem códigos de recuperação por mensagem.
