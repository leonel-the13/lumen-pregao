# Fontes normativas e valores «a confirmar»

Regra editorial (CLAUDE.md §8): não inventar legislação, números de diplomas, taxas nem estatísticas.
Tudo o que está abaixo foi introduzido como **dado provisório** e aparece na interface como «a confirmar»
até um revisor validar.

## Fontes registadas (`V2__reference_data.sql`)
| Fonte | Identificador | Versão | Estado |
|---|---|---|---|
| Plano Geral de Contabilidade | PGC · diploma a confirmar | a confirmar | `confirmed=false` |
| Código do IVA | Lei n.º 7/19 (indicado na spec §2) | a confirmar | `confirmed=false` |

## Contas do PGC (versão `PGC-a-confirmar`)
Só as contas que aparecem no design aprovado (11, 18, 26, 31, 32, 34, 37, 43, 45, 51, 88) e a conta 61 (Vendas),
necessária para a sala «Venda com IVA liquidado». Os nomes das classes são os habituais do PGC angolano e têm de ser
confirmados. As subcontas do IVA são **marcadores**, não códigos oficiais:

| Código | Designação | Nota |
|---|---|---|
| `34.5.x` | IVA suportado | estrutura de subcontas a confirmar (spec Anexo A) |
| `34.5.y` | IVA liquidado | marcador escolhido na implementação; substituir pelo código confirmado |
| `32.1` | Fornecedores · correntes | subconta a confirmar |

## Parâmetros normativos nos exercícios de desenvolvimento
- **Taxa de IVA 14 %**: usada no design como «parâmetro · confirmar com o revisor». Nos exercícios fica como constante
  ligada à fonte «Código do IVA» e mostra «a confirmar» no enunciado.

## Outros valores provisórios
- Pontos das conquistas (50 cada), penalizações (−5), bónus de sequência (+50 a cada 7 dias), nota × 6 nos exames,
  bloqueio das equipas (30 min), penalização por submissão errada no contest (10 min): spec §7, todos a confirmar.
- Pontos de contest no ranking geral = pontos da equipa (decisão de implementação, a confirmar).
- Só a primeira correcção de cada exame conta para o ranking (decisão de implementação, a confirmar).
- Nomes dos cursos: Contabilidade, Economia, Gestão Empresarial (a confirmar).
- Domínio institucional: variável `ALLOWED_EMAIL_DOMAIN`; em desenvolvimento usa-se `exemplo.ao` (fictício).

## Empresas
Todas fictícias (`companies.fictional = true`, garantido por `CHECK`): Distribuidora Mussulo, Lda., Indústrias Cuanza,
Kwanza Energia, Banco Mutamba, Talatona Telecom — os nomes do design aprovado.
