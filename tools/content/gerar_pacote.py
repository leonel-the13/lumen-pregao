# Gera backend/src/main/resources/content/pack-v1.json. Uso: python3 tools/content/gerar_pacote.py backend/src/main/resources/content/pack-v1.json
import json, sys
out = sys.argv[1]

EMP = {"name": "empresa", "type": "text", "rule": {"kind": "entity", "source": "companies"}}
TAXA = {"name": "taxa", "type": "percent", "rule": {"kind": "constant", "value": 14, "legalSource": "IVA"}}
def money(n, a, b, s, visible=True):
    p = {"name": n, "type": "money", "rule": {"kind": "range", "min": a, "max": b, "step": s}}
    if not visible: p["visible"] = False
    return p
def setp(n, values, typ="int"):
    return {"name": n, "type": typ, "rule": {"kind": "set", "values": values}}
ABS = {"type": "ABS", "value": 0.01}
def opts(*texts):
    return {"options": [{"key": chr(65 + i), "text": t} for i, t in enumerate(texts)]}
def lines(*ls):
    return {"lines": [{"account": a, "side": s, "expr": e} for a, s, e in ls]}

EX = []
def ex(key, topic, type, title, statement, presentation=None, params=None, solution=None, tol=None, hints=None,
       difficulty=1, points=40, refs=("PGC",), feedback="DETAILED", max_attempts=3):
    EX.append({"key": key, "topic": topic, "type": type, "title": title, "statement": statement,
               "presentation": presentation or {}, "paramSpec": {"params": params or []}, "solution": solution or {},
               "tolerance": tol or {}, "hints": hints or [], "difficulty": difficulty, "points": points,
               "refs": list(refs), "feedbackPolicy": feedback, "maxAttempts": max_attempts})

# ───────────── PGC · Fundamentos ─────────────
ex("pgc-classe-meios", "PGC · Fundamentos", "A", "Classe dos meios monetários",
   "Em que classe do PGC (versão do exercício) se registam os meios monetários, como a caixa e os depósitos à ordem?",
   opts("Classe 2", "Classe 3", "Classe 4", "Classe 5"), solution={"correct": ["C"]}, points=40)
ex("pgc-classe-mercadorias", "PGC · Fundamentos", "A", "Classe das existências",
   "As mercadorias que a empresa compra para revender são existências. Em que classe do PGC (versão do exercício) se registam?",
   opts("Classe 1", "Classe 2", "Classe 3", "Classe 4"), solution={"correct": ["B"]}, points=30)
ex("pgc-contas-activo", "PGC · Fundamentos", "A", "Contas do activo",
   "Quais destas contas representam, em regra, elementos do activo da empresa? Escolhe todas as que se aplicam.",
   {**opts("11 Imobilizações corpóreas", "31 Clientes", "32 Fornecedores", "45 Caixa", "51 Capital"), "multiple": True},
   solution={"correct": ["A", "B", "D"]}, points=40, difficulty=1,
   hints=["O activo reúne os bens e direitos da empresa; as dívidas a terceiros pertencem ao passivo."])
ex("pgc-partidas-dobradas", "PGC · Fundamentos", "A", "Princípio das partidas dobradas",
   "No método das partidas dobradas, o que se verifica em qualquer lançamento correctamente registado?",
   opts("A soma dos débitos é igual à soma dos créditos", "Os débitos são sempre superiores aos créditos",
        "Cada lançamento movimenta uma única conta", "Os créditos registam sempre entradas de dinheiro"),
   solution={"correct": ["A"]}, points=30)
ex("pgc-equacao-cp", "PGC · Fundamentos", "B", "Equação fundamental da contabilidade",
   "A [empresa] apresenta um activo total de [activo] e um passivo total de [passivo]. Qual é o valor do capital próprio (Kz)?",
   {"unit": "Kz", "precision": 2}, [EMP, money("activo", 5000000, 12000000, 100000), money("passivo", 1000000, 4000000, 100000)],
   {"expr": "activo - passivo", "scale": 2}, ABS, ["Activo = Capital próprio + Passivo."], 1, 40)
ex("pgc-constituicao", "PGC · Fundamentos", "C", "Constituição de uma sociedade",
   "Os sócios constituíram a [empresa] (empresa fictícia) com um capital social de [capital], integralmente realizado em dinheiro "
   "e depositado na conta bancária da empresa. Regista a operação no Diário.",
   {"currency": "Kz"}, [EMP, money("capital", 1000000, 10000000, 500000)],
   lines(("43", "D", "capital"), ("51", "C", "capital")), ABS,
   ["O dinheiro entrou numa conta bancária: a conta de depósitos à ordem é debitada.",
    "O capital realizado pelos sócios regista-se na classe 5."], 1, 60)

# ───────────── PGC · Meios monetários e terceiros ─────────────
ex("pgc-deposito", "PGC · Meios monetários", "C", "Depósito no banco",
   "A [empresa] depositou na sua conta bancária [valor] que estavam na caixa. Regista a operação.",
   {"currency": "Kz"}, [EMP, money("valor", 50000, 900000, 10000)], lines(("43", "D", "valor"), ("45", "C", "valor")), ABS,
   ["O dinheiro sai da caixa e entra no banco."], 1, 40)
ex("pgc-levantamento", "PGC · Meios monetários", "C", "Levantamento para a caixa",
   "A [empresa] levantou [valor] da conta bancária para reforçar a caixa. Regista a operação.",
   {"currency": "Kz"}, [EMP, money("valor", 50000, 600000, 10000)], lines(("45", "D", "valor"), ("43", "C", "valor")), ABS,
   ["É a operação inversa de um depósito."], 1, 40)
ex("pgc-recebimento-cliente", "PGC · Terceiros", "C", "Recebimento de um cliente",
   "Um cliente da [empresa] pagou por transferência bancária uma dívida de [valor] relativa a vendas anteriores. Regista o recebimento.",
   {"currency": "Kz"}, [EMP, money("valor", 100000, 2000000, 10000)], lines(("43", "D", "valor"), ("31", "C", "valor")), ABS,
   ["O direito sobre o cliente diminui: a conta 31 é creditada."], 1, 50)
ex("pgc-saldo-conta", "PGC · Terceiros", "B", "Saldo de uma conta",
   "No mês, a conta 31 Clientes da [empresa] teve movimentos a débito de [debitos] e movimentos a crédito de [creditos]. "
   "Qual é o saldo devedor da conta no fim do mês (Kz)?",
   {"unit": "Kz", "precision": 2}, [EMP, money("debitos", 2000000, 5000000, 50000), money("creditos", 500000, 1900000, 50000)],
   {"expr": "debitos - creditos", "scale": 2}, ABS, ["Saldo devedor = total a débito − total a crédito."], 1, 40)

# ───────────── PGC · Existências ─────────────
ex("pgc-cmv-lancamento", "PGC · Existências", "C", "Custo das mercadorias vendidas",
   "A [empresa] vendeu mercadorias cujo custo de aquisição foi [custo]. Usando o sistema de inventário permanente, "
   "regista o custo das mercadorias vendidas (sem o registo da venda).",
   {"currency": "Kz"}, [EMP, money("custo", 200000, 1500000, 10000)], lines(("71", "D", "custo"), ("26", "C", "custo")), ABS,
   ["As mercadorias saem do armazém (crédito em 26) e o seu custo vai para a classe 7."], 2, 50)
ex("pgc-cmv-calculo", "PGC · Existências", "B", "Cálculo do CMV",
   "A [empresa] tinha existências iniciais de mercadorias de [ei]. No período comprou [compras] e no fim do período "
   "o inventário apurou existências finais de [ef]. Calcula o custo das mercadorias vendidas (Kz).",
   {"unit": "Kz", "precision": 2}, [EMP, money("ei", 300000, 900000, 10000), money("compras", 1000000, 3000000, 50000),
                                    money("ef", 200000, 800000, 10000)],
   {"expr": "ei + compras - ef", "scale": 2}, ABS, ["CMV = Existências iniciais + Compras − Existências finais."], 2, 50)
ex("pgc-margem-bruta", "PGC · Existências", "B", "Margem bruta",
   "No período, a [empresa] vendeu [vendas] e o custo das mercadorias vendidas foi [cmv]. "
   "Qual é a margem bruta em percentagem das vendas (%, duas casas decimais)?",
   {"unit": "%", "precision": 2}, [EMP, money("vendas", 2000000, 5000000, 100000), money("cmv", 1000000, 1900000, 50000)],
   {"expr": "round((vendas - cmv) / vendas * 100, 2)", "scale": 2}, ABS, ["Margem bruta (%) = (Vendas − CMV) ÷ Vendas × 100."], 2, 40)

# ───────────── IVA (compras, vendas, apuramento) ─────────────
COMPRA = [EMP, setp("fornecedor", ["Indústrias Cuanza", "Kwanza Energia"], "text"), money("valor", 300000, 900000, 10000), TAXA]
ex("iva-contexto-compra", "IVA · Compras", "A", "Contexto da operação",
   "A [empresa] comprou mercadorias ao fornecedor [fornecedor], a crédito (30 dias). Que tipo de operação é?",
   opts("Compra a pronto pagamento", "Compra a crédito", "Venda a crédito", "Devolução de mercadorias"),
   COMPRA, {"correct": ["B"]}, refs=("IVA",), points=30)
ex("iva-contas-compra", "IVA · Compras", "A", "Identificar as contas",
   "Que contas são movimentadas no registo da compra a crédito da [empresa]? Escolhe todas as que se aplicam.",
   {**opts("26 Mercadorias", "34.5.x IVA suportado", "32 Fornecedores", "45 Caixa", "61 Vendas"), "multiple": True},
   COMPRA, {"correct": ["A", "B", "C"]}, refs=("PGC",), points=20)
ex("iva-registar-compra", "IVA · Compras", "C", "Registar a compra a crédito",
   "Em 3 de março, a [empresa] (empresa fictícia) comprou mercadorias ao fornecedor [fornecedor], a crédito (30 dias). "
   "Valor das mercadorias: [valor]. Taxa de IVA do exercício: [taxa]. Regista o lançamento no Diário.",
   {"currency": "Kz"}, COMPRA,
   lines(("26", "D", "valor"), ("34.5.x", "D", "valor * taxa / 100"), ("32", "C", "valor * (1 + taxa / 100)")), ABS,
   ["A compra foi a crédito: a dívida ao fornecedor regista-se numa conta de Terceiros.",
    "O IVA suportado calcula-se sobre o valor das mercadorias."], 2, 40, ("PGC", "IVA"))
ex("iva-compra-pronto", "IVA · Compras", "C", "Compra a pronto com IVA",
   "A [empresa] comprou mercadorias no valor de [valor] (base), pagas de imediato por transferência bancária. "
   "Taxa de IVA do exercício: [taxa]. Regista a compra.",
   {"currency": "Kz"}, [EMP, money("valor", 200000, 800000, 10000), TAXA],
   lines(("26", "D", "valor"), ("34.5.x", "D", "valor * taxa / 100"), ("43", "C", "valor * (1 + taxa / 100)")), ABS,
   ["O pagamento foi imediato: a contrapartida é a conta bancária, não o fornecedor."], 2, 60, ("PGC", "IVA"))
ex("iva-pagamento-fornecedor", "IVA · Compras", "C", "Pagamento ao fornecedor",
   "Trinta dias depois, a [empresa] pagou por transferência bancária a dívida ao fornecedor [fornecedor] relativa à compra "
   "de [valor] de mercadorias, com IVA à taxa [taxa]. Regista o pagamento.",
   {"currency": "Kz"}, COMPRA,
   lines(("32", "D", "valor * (1 + taxa / 100)"), ("43", "C", "valor * (1 + taxa / 100)")), ABS,
   ["O pagamento foi por transferência: usa a conta de depósitos à ordem."], 2, 30, ("PGC",))
ex("iva-venda", "IVA · Vendas", "C", "Venda a crédito com IVA",
   "A [empresa] vendeu mercadorias a crédito a um cliente por [valor] (base). Taxa de IVA do exercício: [taxa]. "
   "Regista a venda (sem o custo das mercadorias vendidas).",
   {"currency": "Kz"}, [EMP, money("valor", 300000, 1200000, 10000), TAXA],
   lines(("31", "D", "valor * (1 + taxa / 100)"), ("61", "C", "valor"), ("34.5.y", "C", "valor * taxa / 100")), ABS,
   ["O IVA liquidado na venda é uma dívida ao Estado."], 2, 100, ("PGC", "IVA"))
ex("iva-prestacao-servicos", "IVA · Vendas", "C", "Prestação de serviços com IVA",
   "A [empresa] prestou serviços de manutenção a um cliente, a crédito, por [valor] (base). Taxa de IVA do exercício: [taxa]. "
   "Regista a operação.",
   {"currency": "Kz"}, [EMP, money("valor", 150000, 900000, 10000), TAXA],
   lines(("31", "D", "valor * (1 + taxa / 100)"), ("62", "C", "valor"), ("34.5.y", "C", "valor * taxa / 100")), ABS,
   ["Serviços prestados não são vendas de mercadorias: usa a conta de prestações de serviços."], 2, 80, ("PGC", "IVA"))
ex("iva-total-factura", "IVA · Liquidação", "B", "Total de uma factura",
   "Uma factura de venda tem base tributável de [base] e IVA à taxa do exercício de [taxa]. Qual é o total da factura (Kz)?",
   {"unit": "Kz", "precision": 2}, [money("base", 50000, 2000000, 5000), TAXA], {"expr": "base * (1 + taxa / 100)", "scale": 2}, ABS,
   ["Total = base + IVA."], 1, 40, ("IVA",))
ex("iva-natureza", "IVA · Liquidação", "A", "Natureza do IVA liquidado",
   "Do ponto de vista contabilístico, o IVA liquidado pela empresa nas suas vendas é:",
   opts("Uma dívida ao Estado, registada em Terceiros", "Um proveito do exercício", "Um custo do exercício",
        "Um elemento do capital próprio"), solution={"correct": ["A"]}, refs=("PGC", "IVA"), points=30)
IVAMES = [EMP, money("vendas", 800000, 1600000, 20000), money("compras", 300000, 700000, 10000), TAXA]
ex("iva-liquidado-venda", "IVA · Liquidação", "B", "IVA liquidado numa venda",
   "Uma empresa vendeu mercadorias por [vendas] (base). Com a taxa do exercício de [taxa], qual é o IVA liquidado (Kz)?",
   {"unit": "Kz", "precision": 2}, IVAMES, {"expr": "vendas * taxa / 100", "scale": 2}, ABS, refs=("IVA",), points=20)
ex("iva-dedutivel", "IVA · Dedução", "B", "IVA dedutível nas compras",
   "Uma empresa comprou mercadorias por [compras] (base), com factura válida. Com a taxa do exercício de [taxa], "
   "qual é o IVA dedutível (Kz)?",
   {"unit": "Kz", "precision": 2}, IVAMES, {"expr": "compras * taxa / 100", "scale": 2}, ABS, refs=("IVA",), points=20)
ex("iva-apurar-mes", "IVA · Apuramento", "E", "Apurar o IVA do mês",
   "No mês de março, a [empresa] vendeu [vendas] (base) e comprou [compras] (base) de mercadorias. "
   "Taxa de IVA do exercício: [taxa]. Apura o IVA do período, passo a passo.",
   {"steps": [{"key": "liquidado", "label": "IVA liquidado nas vendas", "formula": "base das vendas × taxa"},
              {"key": "dedutivel", "label": "IVA suportado dedutível", "formula": "base das compras × taxa",
               "help": "Considera apenas IVA com documento válido e dedutível."},
              {"key": "apurado", "label": "Imposto apurado", "formula": "IVA liquidado − IVA dedutível"}]},
   IVAMES, {"steps": [{"key": "liquidado", "expr": "vendas * taxa / 100"}, {"key": "dedutivel", "expr": "compras * taxa / 100"},
                      {"key": "apurado", "expr": "(vendas - compras) * taxa / 100"}]}, ABS,
   ["Se o IVA dedutível for maior do que o liquidado, o resultado é um crédito de imposto."], 2, 40, ("IVA",))
ex("iva-apuramento-mensal", "IVA · Apuramento", "E", "Apuramento mensal do IVA",
   "A [empresa] apresenta, no período, vendas de [vendas] e compras de [compras] (bases tributáveis). "
   "Taxa aplicável: [taxa]. Apura o IVA do período.",
   {"steps": [{"key": "liquidado", "label": "IVA liquidado nas vendas", "formula": "base das vendas × taxa"},
              {"key": "dedutivel", "label": "IVA suportado dedutível", "formula": "base das compras × taxa",
               "help": "Considera apenas IVA com documento válido e dedutível."},
              {"key": "apurado", "label": "Imposto apurado", "formula": "IVA liquidado − IVA dedutível"},
              {"key": "entregar", "label": "Resultado do período", "formula": "a entregar ao Estado (se positivo)"}]},
   IVAMES, {"steps": [{"key": "liquidado", "expr": "vendas * taxa / 100"}, {"key": "dedutivel", "expr": "compras * taxa / 100"},
                      {"key": "apurado", "expr": "(vendas - compras) * taxa / 100"},
                      {"key": "entregar", "expr": "max((vendas - compras) * taxa / 100, 0)"}]}, ABS,
   ["Se o IVA dedutível for maior do que o liquidado, o resultado é um crédito de imposto a reportar."], 2, 140, ("IVA",))
ex("iva-credito-imposto", "IVA · Apuramento", "E", "Crédito de imposto",
   "Num mês de investimento, a [empresa] vendeu [vendas] (base) e comprou [compras] (base) de mercadorias e equipamento, "
   "com facturas válidas. Taxa de IVA do exercício: [taxa]. Apura o resultado do período.",
   {"steps": [{"key": "liquidado", "label": "IVA liquidado nas vendas", "formula": "base das vendas × taxa"},
              {"key": "dedutivel", "label": "IVA suportado dedutível", "formula": "base das compras × taxa"},
              {"key": "credito", "label": "Crédito de imposto a reportar", "formula": "IVA dedutível − IVA liquidado"}]},
   [EMP, money("vendas", 300000, 600000, 10000), money("compras", 800000, 1200000, 10000), TAXA],
   {"steps": [{"key": "liquidado", "expr": "vendas * taxa / 100"}, {"key": "dedutivel", "expr": "compras * taxa / 100"},
              {"key": "credito", "expr": "(compras - vendas) * taxa / 100"}]}, ABS,
   ["Quando o IVA dedutível excede o liquidado, não há imposto a entregar: a diferença é um crédito."], 2, 120, ("IVA",))
ex("iva-resultado-apuramento", "IVA · Apuramento", "A", "Resultado do apuramento",
   "Uma empresa liquidou 168 000,00 Kz de IVA nas vendas e suportou 70 000,00 Kz de IVA dedutível nas compras do mesmo período. "
   "Qual é o resultado do apuramento?",
   opts("IVA a entregar ao Estado de 98 000,00 Kz", "Crédito de imposto de 98 000,00 Kz", "IVA a entregar de 238 000,00 Kz",
        "Não há imposto a apurar"), solution={"correct": ["A"]}, refs=("IVA",), points=30)
ex("iva-deducao-documento", "IVA · Dedução", "A", "Condição para deduzir o IVA",
   "Para que o IVA suportado numa compra possa, em regra, ser deduzido, a empresa precisa de:",
   opts("Factura ou documento equivalente válido, emitido em seu nome", "Pagar o fornecedor em numerário",
        "Que a compra ultrapasse um valor mínimo", "Que o fornecedor seja estrangeiro"),
   solution={"correct": ["A"]}, refs=("IVA",), points=40,
   hints=["As condições exactas de dedução constam do Código do IVA (artigos a indicar pelo revisor)."])

# ───────────── PGC · Regularizações ─────────────
AMORT = [EMP, money("custo", 1200000, 6000000, 400000), setp("vida", [4, 5, 8, 10])]
ex("pgc-balancete", "PGC · Regularizações", "A", "Balancete de verificação",
   "Num balancete de verificação correctamente elaborado:",
   opts("O total dos movimentos a débito é igual ao total dos movimentos a crédito", "Todas as contas têm saldo devedor",
        "Só aparecem contas do activo", "O resultado líquido é sempre positivo"), solution={"correct": ["A"]}, points=30)
ex("pgc-amortizacao-quota", "PGC · Regularizações", "B", "Quota de amortização",
   "A [empresa] adquiriu um equipamento por [custo], com vida útil estimada de [vida] anos e valor residual nulo. "
   "Pelo método das quotas constantes, qual é a amortização anual (Kz)?",
   {"unit": "Kz", "precision": 2}, AMORT, {"expr": "custo / vida", "scale": 2}, ABS,
   ["Quotas constantes: custo de aquisição ÷ anos de vida útil."], 2, 50)
ex("pgc-amortizacao-lancamento", "PGC · Regularizações", "C", "Registar a amortização",
   "No fim do exercício, a [empresa] regista a amortização de um equipamento adquirido por [custo], com vida útil de [vida] anos, "
   "valor residual nulo e método das quotas constantes. Regista a amortização do exercício.",
   {"currency": "Kz"}, AMORT, lines(("73", "D", "custo / vida"), ("18", "C", "custo / vida")), ABS,
   ["A amortização é um custo do exercício; a contrapartida acumula-se na conta de amortizações acumuladas."], 2, 60)
SAL = [EMP, money("salarios", 800000, 3000000, 50000)]
ex("pgc-salarios", "PGC · Regularizações", "C", "Processamento de salários",
   "A [empresa] processou os salários do mês, com remunerações brutas de [salarios]. Para simplificar, não consideres retenções "
   "nem contribuições (fora do âmbito desta sala). Regista o processamento.",
   {"currency": "Kz"}, SAL, lines(("72", "D", "salarios"), ("36", "C", "salarios")), ABS,
   ["Processar salários cria um custo e uma dívida ao pessoal, que ainda não foi paga."], 2, 50)
ex("pgc-pagamento-salarios", "PGC · Regularizações", "C", "Pagamento de salários",
   "A [empresa] pagou por transferência bancária os salários processados, no valor de [salarios]. Regista o pagamento.",
   {"currency": "Kz"}, SAL, lines(("36", "D", "salarios"), ("43", "C", "salarios")), ABS,
   ["O pagamento extingue a dívida ao pessoal."], 1, 40)

# ───────────── Demonstrações financeiras ─────────────
ex("pgc-balanco", "PGC · Demonstrações financeiras", "D", "Balanço a 31 de março",
   "Com base no balancete final (excerto) da [empresa], elabora o balanço a 31 de março. Valores fictícios gerados para esta tentativa.",
   {"ledger": [
       {"account": "11", "label": "Imobilizações corpóreas", "value": {"$expr": "imob"}, "side": "D"},
       {"account": "18", "label": "Amortizações acumuladas", "value": {"$expr": "amort"}, "side": "C"},
       {"account": "26", "label": "Mercadorias", "value": {"$expr": "merc"}, "side": "D"},
       {"account": "31", "label": "Clientes", "value": {"$expr": "clientes"}, "side": "D"},
       {"account": "43", "label": "Depósitos à ordem", "value": {"$expr": "depositos"}, "side": "D"},
       {"account": "32", "label": "Fornecedores", "value": {"$expr": "fornecedores"}, "side": "C"},
       {"account": "34", "label": "Estado", "value": {"$expr": "estado"}, "side": "C"},
       {"account": "51", "label": "Capital", "value": {"$expr": "capital"}, "side": "C"},
       {"account": "88", "label": "Resultado líquido", "value": {"$expr": "resultado"}, "side": "C"}],
    "sections": [
       {"key": "activo", "title": "Activo", "totalKey": "total_activo", "totalLabel": "Total do Activo", "groups": [
           {"title": "Activo não corrente", "items": [{"key": "imob_liq", "label": "Imobilizações corpóreas (líquidas)"}]},
           {"title": "Activo corrente", "items": [{"key": "existencias", "label": "Existências"}, {"key": "clientes", "label": "Clientes"},
                                                {"key": "meios", "label": "Meios monetários"}]}]},
       {"key": "cp_passivo", "title": "Capital próprio e passivo", "totalKey": "total_cp_passivo",
        "totalLabel": "Total do Capital próprio e Passivo", "groups": [
           {"title": "Capital próprio", "items": [{"key": "capital", "label": "Capital"}, {"key": "resultado", "label": "Resultado líquido do exercício"}]},
           {"title": "Passivo corrente", "items": [{"key": "fornecedores", "label": "Fornecedores"}, {"key": "estado", "label": "Estado (IVA a pagar)"}]}]}],
    "relations": [{"label": "Activo não corrente = bruto − amortizações"}, {"label": "Activo corrente = soma das rubricas"},
                  {"label": "Activo = Capital próprio + Passivo"}]},
   [EMP, money("imob", 2000000, 3000000, 100000, False),
    {"name": "amort", "type": "money", "visible": False, "rule": {"kind": "expr", "expr": "imob * 0.2", "scale": 2}},
    money("merc", 600000, 1000000, 20000, False), money("clientes", 400000, 800000, 20000, False),
    money("depositos", 700000, 1200000, 20000, False), money("fornecedores", 500000, 900000, 20000, False),
    money("estado", 50000, 120000, 10000, False),
    {"name": "capital", "type": "money", "visible": False, "rule": {"kind": "constant", "value": 2500000}},
    {"name": "resultado", "type": "money", "visible": False,
     "rule": {"kind": "expr", "expr": "imob - amort + merc + clientes + depositos - fornecedores - estado - capital", "scale": 2}}],
   {"items": {"imob_liq": "imob - amort", "existencias": "merc", "clientes": "clientes", "meios": "depositos",
              "total_activo": "imob - amort + merc + clientes + depositos", "capital": "capital", "resultado": "resultado",
              "fornecedores": "fornecedores", "estado": "estado", "total_cp_passivo": "capital + resultado + fornecedores + estado"},
    "relations": [{"label": "Activo = Capital próprio + Passivo", "left": "total_activo", "right": "total_cp_passivo"},
                  {"label": "Activo corrente = soma das rubricas", "left": "total_activo", "right": "imob_liq + existencias + clientes + meios"}]},
   ABS, ["O resultado líquido do exercício faz parte do capital próprio."], 3, 200)
ex("pgc-demonstracao-resultados", "PGC · Demonstrações financeiras", "D", "Demonstração de resultados",
   "Com base nos saldos das contas de proveitos e de custos da [empresa] no fim do exercício, elabora a demonstração de resultados "
   "(por natureza, simplificada) e apura o resultado operacional. Valores fictícios gerados para esta tentativa.",
   {"kind": "income", "ledger": [
       {"account": "61", "label": "Vendas", "value": {"$expr": "vendas"}, "side": "C"},
       {"account": "62", "label": "Prestações de serviços", "value": {"$expr": "servicos"}, "side": "C"},
       {"account": "71", "label": "Custo das mercadorias vendidas", "value": {"$expr": "cmv"}, "side": "D"},
       {"account": "72", "label": "Custos com o pessoal", "value": {"$expr": "pessoal"}, "side": "D"},
       {"account": "73", "label": "Amortizações do exercício", "value": {"$expr": "amortizacoes"}, "side": "D"},
       {"account": "75", "label": "Outros custos e perdas operacionais", "value": {"$expr": "outros"}, "side": "D"}],
    "sections": [
       {"key": "proveitos", "title": "Proveitos operacionais", "totalKey": "total_proveitos", "totalLabel": "Total dos proveitos operacionais",
        "groups": [{"title": "Proveitos", "items": [{"key": "vendas", "label": "Vendas"}, {"key": "servicos", "label": "Prestações de serviços"}]}]},
       {"key": "custos", "title": "Custos operacionais", "totalKey": "total_custos", "totalLabel": "Total dos custos operacionais",
        "groups": [{"title": "Custos", "items": [{"key": "cmv", "label": "Custo das mercadorias vendidas"},
                                                 {"key": "pessoal", "label": "Custos com o pessoal"},
                                                 {"key": "amortizacoes", "label": "Amortizações do exercício"},
                                                 {"key": "outros", "label": "Outros custos operacionais"}]}]},
       {"key": "resultado", "title": "Resultado", "totalKey": "total_resultado", "totalLabel": "Resultado operacional",
        "groups": [{"title": "Resultado operacional", "items": [{"key": "resultado_operacional", "label": "Proveitos − Custos"}]}]}],
    "relations": [{"label": "Resultado operacional = proveitos operacionais − custos operacionais"}]},
   [EMP, money("vendas", 3000000, 6000000, 100000, False), money("servicos", 200000, 800000, 50000, False),
    money("cmv", 1500000, 2900000, 50000, False), money("pessoal", 400000, 900000, 50000, False),
    money("amortizacoes", 100000, 300000, 10000, False), money("outros", 50000, 200000, 10000, False)],
   {"items": {"vendas": "vendas", "servicos": "servicos", "total_proveitos": "vendas + servicos", "cmv": "cmv", "pessoal": "pessoal",
              "amortizacoes": "amortizacoes", "outros": "outros", "total_custos": "cmv + pessoal + amortizacoes + outros",
              "resultado_operacional": "vendas + servicos - cmv - pessoal - amortizacoes - outros",
              "total_resultado": "vendas + servicos - cmv - pessoal - amortizacoes - outros"},
    "relations": [{"label": "Resultado operacional = proveitos − custos", "left": "total_resultado",
                   "right": "total_proveitos - total_custos"}]},
   ABS, ["Os proveitos têm saldo credor; os custos, saldo devedor."], 3, 200)

# ───────────── Análise de demonstrações financeiras ─────────────
def ratio_f(key, topic, title, statement, params, expr, keywords, points=120, difficulty=2):
    ex(key, topic, "F", title, statement, {"unit": "", "precision": 2, "justification": {"minLength": 80}}, params,
       {"expr": expr, "scale": 2, "rounding": "HALF_UP", "justification": {"minLength": 80, "keywordsAny": keywords, "keywordsMin": 1}},
       ABS, [], difficulty, points, ())
def ratio_b(key, topic, title, statement, params, expr, unit="", points=60, hints=()):
    ex(key, topic, "B", title, statement, {"unit": unit, "precision": 2}, params, {"expr": expr, "scale": 2}, ABS, list(hints), 2, points, ())

ratio_f("an-liquidez-geral", "Análise · Liquidez", "Liquidez geral",
        "A [empresa] apresenta activo corrente de [ac] e passivo corrente de [pc]. Calcula a liquidez geral (duas casas decimais) "
        "e interpreta o resultado.",
        [EMP, money("ac", 800000, 2400000, 50000), money("pc", 500000, 1500000, 50000)], "ac / pc",
        ["liquidez", "curto prazo", "passivo corrente", "solvência"])
ratio_f("an-liquidez-reduzida", "Análise · Liquidez", "Liquidez reduzida",
        "A [empresa] apresenta activo corrente de [ac], dos quais [exist] são existências, e passivo corrente de [pc]. "
        "Calcula a liquidez reduzida (duas casas decimais) e explica porque se excluem as existências.",
        [EMP, money("ac", 1500000, 3000000, 50000), money("exist", 300000, 700000, 50000), money("pc", 800000, 1400000, 50000)],
        "(ac - exist) / pc", ["existências", "liquidez", "curto prazo"])
ratio_b("an-liquidez-imediata", "Análise · Liquidez", "Liquidez imediata",
        "A [empresa] tem meios monetários (caixa e depósitos) de [disp] e passivo corrente de [pc]. Calcula a liquidez imediata "
        "(duas casas decimais).", [EMP, money("disp", 100000, 600000, 10000), money("pc", 800000, 1500000, 50000)],
        "round(disp / pc, 2)", hints=["Liquidez imediata = meios monetários ÷ passivo corrente."])
ratio_b("an-autonomia", "Análise · Estrutura", "Autonomia financeira",
        "O balanço da [empresa] mostra capital próprio de [cp] e activo total de [activo]. Qual é a autonomia financeira "
        "(%, duas casas decimais)?", [EMP, money("cp", 2000000, 5000000, 100000), money("activo", 6000000, 10000000, 100000)],
        "round(cp / activo * 100, 2)", "%", hints=["Autonomia financeira = capital próprio ÷ activo × 100."])
ratio_b("an-endividamento", "Análise · Estrutura", "Endividamento",
        "O balanço da [empresa] mostra passivo total de [passivo] e activo total de [activo]. Qual é o rácio de endividamento "
        "(%, duas casas decimais)?", [EMP, money("passivo", 2000000, 5000000, 100000), money("activo", 6000000, 10000000, 100000)],
        "round(passivo / activo * 100, 2)", "%", hints=["Endividamento = passivo ÷ activo × 100."])
ratio_f("an-solvabilidade", "Análise · Estrutura", "Solvabilidade",
        "A [empresa] apresenta capital próprio de [cp] e passivo total de [passivo]. Calcula o rácio de solvabilidade "
        "(duas casas decimais) e interpreta-o na perspectiva dos credores.",
        [EMP, money("cp", 1500000, 5000000, 100000), money("passivo", 1000000, 4000000, 100000)], "cp / passivo",
        ["solvabilidade", "credores", "capital próprio", "passivo"])
ratio_b("an-roe", "Análise · Rendibilidade", "Rendibilidade dos capitais próprios",
        "A [empresa] obteve um resultado líquido de [rl] e tem capital próprio de [cp]. Qual é a rendibilidade dos capitais "
        "próprios (%, duas casas decimais)?", [EMP, money("rl", 200000, 900000, 10000), money("cp", 2000000, 6000000, 100000)],
        "round(rl / cp * 100, 2)", "%", hints=["ROE = resultado líquido ÷ capital próprio × 100."])
ratio_b("an-margem-liquida", "Análise · Rendibilidade", "Margem líquida",
        "A [empresa] vendeu [vendas] no exercício e obteve um resultado líquido de [rl]. Qual é a margem líquida "
        "(%, duas casas decimais)?", [EMP, money("vendas", 3000000, 9000000, 100000), money("rl", 150000, 900000, 10000)],
        "round(rl / vendas * 100, 2)", "%", hints=["Margem líquida = resultado líquido ÷ vendas × 100."])
ratio_f("an-roa", "Análise · Rendibilidade", "Rendibilidade do activo",
        "A [empresa] obteve um resultado líquido de [rl] com um activo total de [activo]. Calcula a rendibilidade do activo "
        "(%, duas casas decimais) e interpreta o resultado.",
        [EMP, money("rl", 200000, 900000, 10000), money("activo", 5000000, 12000000, 100000)], "rl / activo * 100",
        ["rendibilidade", "activo", "recursos"])

# ───────────── Trilhas ─────────────
def room(key, title, desc, objectives, difficulty, minutes, context, tasks, prereq=()):
    return {"key": key, "title": title, "description": desc, "objectives": objectives, "difficulty": difficulty,
            "estMinutes": minutes, "context": context, "prerequisites": list(prereq),
            "tasks": [{"exercise": e, "title": t, "points": p} for e, t, p in tasks]}

FICT = "Todas as empresas e valores são fictícios. As referências ao PGC e ao Código do IVA estão **a confirmar** por revisor."
TRACKS = [
 {"slug": "contabilidade-pgc", "title": "Contabilidade Financeira segundo o PGC", "summary": "Balancete, lançamentos e demonstrações financeiras.",
  "description": "Objectivos: dominar o quadro de contas do PGC, registar as operações correntes pelo método das partidas dobradas, "
                 "preparar o balancete e elaborar o balanço e a demonstração de resultados.",
  "difficulty": 1, "topic": "Contabilidade", "courses": ["CONT", "ECON", "GEST"],
  "competencies": ["Registo contabilístico", "IVA", "Demonstrações financeiras"],
  "completionRule": "Requisito de conclusão: todas as salas ≥ 70 % e exame final ≥ 10/20 (a confirmar por trilha).",
  "estHours": 18, "position": 0, "status": "PUBLISHED", "modules": [
   {"title": "Fundamentos e o PGC", "summary": "Quadro de contas, classes e partidas dobradas", "rooms": [
     room("pgc-r-quadro", "O quadro de contas do PGC", "Classes do PGC e natureza das contas.", ["Reconhecer as classes do PGC", "Distinguir activo de passivo"], 1, 20,
          "O Plano Geral de Contabilidade organiza as contas por classes, cada uma com um código. Nesta sala revês as classes e a natureza "
          "das contas mais usadas. " + FICT,
          [("pgc-classe-meios", "Classe dos meios monetários", 40), ("pgc-classe-mercadorias", "Classe das existências", 30),
           ("pgc-contas-activo", "Contas do activo", 40)]),
     room("pgc-r-equacao", "Equação fundamental e partidas dobradas", "Activo, capital próprio e passivo.", ["Aplicar a equação fundamental", "Compreender as partidas dobradas"], 1, 20,
          "Toda a contabilidade assenta na igualdade Activo = Capital próprio + Passivo e no princípio de que cada débito tem um crédito "
          "de igual valor. " + FICT,
          [("pgc-partidas-dobradas", "Princípio das partidas dobradas", 30), ("pgc-equacao-cp", "Equação fundamental", 40)], ["pgc-r-quadro"]),
     room("pgc-r-constituicao", "Constituição de uma sociedade", "O primeiro lançamento de uma empresa.", ["Registar a realização do capital"], 1, 15,
          "Na constituição, os sócios entregam bens ou dinheiro à sociedade em troca do capital social. " + FICT,
          [("pgc-constituicao", "Registar a constituição", 60)], ["pgc-r-equacao"])]},
   {"title": "Meios monetários e terceiros", "summary": "Caixa, depósitos, clientes e fornecedores", "rooms": [
     room("pgc-r-caixa", "Caixa e depósitos", "Movimentos entre a caixa e o banco.", ["Registar depósitos e levantamentos"], 1, 20,
          "A caixa e os depósitos à ordem são meios monetários: movimentá-los entre si não altera o património total. " + FICT,
          [("pgc-deposito", "Depósito no banco", 40), ("pgc-levantamento", "Levantamento para a caixa", 40)]),
     room("pgc-r-terceiros", "Clientes e fornecedores", "Recebimentos, pagamentos e saldos.", ["Registar recebimentos e pagamentos", "Calcular o saldo de uma conta"], 2, 30,
          "As contas de Terceiros registam direitos (clientes) e obrigações (fornecedores). O saldo mostra o que está por receber ou por pagar. " + FICT,
          [("pgc-recebimento-cliente", "Recebimento de um cliente", 50), ("pgc-saldo-conta", "Saldo da conta de clientes", 40),
           ("iva-pagamento-fornecedor", "Pagamento a um fornecedor", 50)], ["pgc-r-caixa"])]},
   {"title": "Existências", "summary": "Compras, mercadorias e custo das vendas", "rooms": [
     room("pgc-r-compras", "Compras de mercadorias", "Compra a pronto pagamento com IVA.", ["Registar uma compra a pronto", "Separar o IVA suportado"], 2, 25,
          "Quando a compra é paga de imediato, a contrapartida é o meio monetário usado e não o fornecedor. " + FICT,
          [("iva-compra-pronto", "Compra a pronto com IVA", 60)]),
     room("pgc-r-cmv", "Custo das mercadorias vendidas", "CMV, inventário e margem bruta.", ["Calcular o CMV", "Registar o CMV", "Calcular a margem bruta"], 2, 35,
          "O custo das mercadorias vendidas liga as existências aos resultados. Com inventário periódico, CMV = EI + Compras − EF. " + FICT,
          [("pgc-cmv-calculo", "Cálculo do CMV", 50), ("pgc-cmv-lancamento", "Registar o CMV", 50), ("pgc-margem-bruta", "Margem bruta", 40)],
          ["pgc-r-compras"])]},
   {"title": "IVA nas operações correntes", "summary": "Suportado, liquidado e apuramento", "rooms": [
     room("pgc-r-compra-iva", "Compra de mercadorias com IVA", "Lançamentos, IVA suportado e pagamento.",
          ["Registar a compra a crédito", "Separar o IVA suportado", "Verificar o equilíbrio"], 2, 35,
          "A **Distribuidora Mussulo, Lda.** e as restantes entidades são empresas fictícias. Os valores destacados são únicos para a tua "
          "tentativa. A taxa de IVA é um parâmetro do exercício (**a confirmar** com o revisor).",
          [("iva-contexto-compra", "Contexto da operação", 30), ("iva-contas-compra", "Identificar as contas", 20),
           ("iva-registar-compra", "Registar o lançamento", 40), ("iva-apurar-mes", "Apurar o IVA do mês", 40),
           ("iva-pagamento-fornecedor", "Pagamento ao fornecedor", 30)]),
     room("pgc-r-venda-iva", "Venda com IVA liquidado", "Registo de vendas a crédito com IVA.", ["Registar a venda", "Separar o IVA liquidado"], 2, 30,
          "Na venda, o IVA liquidado é uma dívida ao Estado até ao apuramento. " + FICT,
          [("iva-venda", "Registar a venda", 100)], ["pgc-r-compra-iva"]),
     room("pgc-r-servicos", "Prestação de serviços com IVA", "Serviços, IVA e total da factura.", ["Registar uma prestação de serviços", "Calcular o total de uma factura"], 2, 30,
          "As prestações de serviços seguem a mesma lógica das vendas, mas usam uma conta de proveitos própria. " + FICT,
          [("iva-prestacao-servicos", "Registar a prestação de serviços", 80), ("iva-total-factura", "Total da factura", 40)], ["pgc-r-venda-iva"])]},
   {"title": "Balancete e regularizações", "summary": "Balancete, amortizações e salários", "rooms": [
     room("pgc-r-balancete", "Balancete de verificação", "Controlo do equilíbrio dos registos.", ["Interpretar o balancete"], 1, 15,
          "O balancete lista as contas com os totais a débito e a crédito e os saldos, e verifica o equilíbrio dos registos. " + FICT,
          [("pgc-balancete", "Balancete de verificação", 30)]),
     room("pgc-r-amortizacoes", "Amortizações do exercício", "Quotas constantes e registo.", ["Calcular a quota anual", "Registar a amortização"], 2, 30,
          "A amortização reparte o custo de um activo fixo pelos anos de vida útil. Nesta sala usa-se o método das quotas constantes "
          "e valor residual nulo. " + FICT,
          [("pgc-amortizacao-quota", "Quota de amortização", 50), ("pgc-amortizacao-lancamento", "Registar a amortização", 60)], ["pgc-r-balancete"]),
     room("pgc-r-salarios", "Processamento de salários", "Custos com o pessoal e pagamento.", ["Registar o processamento", "Registar o pagamento"], 2, 25,
          "Primeiro reconhece-se o custo e a dívida ao pessoal; depois regista-se o pagamento. Retenções e contribuições ficam para uma "
          "sala própria (taxas a confirmar). " + FICT,
          [("pgc-salarios", "Processamento de salários", 50), ("pgc-pagamento-salarios", "Pagamento de salários", 40)], ["pgc-r-amortizacoes"])]},
   {"title": "Demonstrações financeiras", "summary": "Balanço e demonstração de resultados", "rooms": [
     room("pgc-r-balanco", "Balanço", "Do balancete final ao balanço.", ["Classificar as contas do balancete", "Verificar Activo = Capital próprio + Passivo"], 3, 40,
          "Usa o balancete final para preencher o balanço. A estrutura de rubricas segue o modelo do PGC (**a confirmar**).",
          [("pgc-balanco", "Elaborar o balanço", 200)], ["pgc-r-salarios"]),
     room("pgc-r-dr", "Demonstração de resultados", "Proveitos, custos e resultado operacional.", ["Agrupar proveitos e custos", "Apurar o resultado operacional"], 3, 40,
          "A demonstração de resultados por natureza mostra os proveitos e os custos do exercício. Nesta sala apura-se o resultado "
          "operacional; o modelo completo do PGC está **a confirmar**.",
          [("pgc-demonstracao-resultados", "Elaborar a demonstração de resultados", 200)], ["pgc-r-balanco"])]}]},
 {"slug": "fiscalidade-iva", "title": "Fiscalidade Angolana · IVA", "summary": "Incidência, liquidação, dedução e apuramento.",
  "description": "Pratica o cálculo do IVA passo a passo. Base: Código do IVA, Lei n.º 7/19 (versão em vigor e artigos a confirmar).",
  "difficulty": 2, "topic": "Fiscalidade", "courses": ["CONT", "ECON", "GEST"], "competencies": ["IVA", "Apuramento"],
  "completionRule": "Requisito de conclusão a confirmar.", "estHours": 10, "position": 1, "status": "PUBLISHED", "modules": [
   {"title": "Incidência e liquidação", "summary": "Natureza do IVA e liquidação", "rooms": [
     room("iva-r-natureza", "Natureza do IVA", "O IVA na contabilidade da empresa.", ["Classificar o IVA liquidado", "Calcular o total de uma factura"], 1, 20,
          "O IVA é cobrado pela empresa aos clientes e entregue ao Estado; contabilisticamente é uma conta de Terceiros. " + FICT,
          [("iva-natureza", "Natureza do IVA liquidado", 30), ("iva-total-factura", "Total de uma factura", 40)]),
     room("iva-r-liquidacao", "Liquidação e dedução", "IVA liquidado e IVA dedutível.", ["Calcular o IVA de uma operação"], 1, 20,
          "Exercícios numéricos sobre o IVA de operações simples. " + FICT,
          [("iva-liquidado-venda", "IVA liquidado numa venda", 20), ("iva-dedutivel", "IVA dedutível nas compras", 20)], ["iva-r-natureza"])]},
   {"title": "Apuramento", "summary": "Imposto a entregar e crédito de imposto", "rooms": [
     room("iva-r-apuramento", "Apuramento mensal do IVA", "Calculadora fiscal passo a passo.", ["Calcular o IVA liquidado e o dedutível", "Apurar o imposto do período"], 2, 30,
          "Cada passo é corrigido no servidor antes de avançares para o seguinte. " + FICT,
          [("iva-apuramento-mensal", "Apuramento mensal do IVA", 140)]),
     room("iva-r-credito", "Crédito de imposto", "Quando o IVA dedutível excede o liquidado.", ["Apurar um crédito de imposto", "Interpretar o resultado do apuramento"], 2, 30,
          "Em meses de investimento, o IVA suportado pode ultrapassar o liquidado. As regras de reporte e reembolso estão no Código do IVA "
          "(artigos a confirmar). " + FICT,
          [("iva-credito-imposto", "Crédito de imposto", 120), ("iva-resultado-apuramento", "Resultado do apuramento", 30)], ["iva-r-apuramento"])]},
   {"title": "Dedução", "summary": "Condições de dedução do IVA", "rooms": [
     room("iva-r-deducao", "Requisitos de dedução", "Documentos e condições.", ["Identificar as condições de dedução"], 2, 15,
          "Só o IVA suportado com documento válido e nas condições legais é dedutível. " + FICT,
          [("iva-deducao-documento", "Condição para deduzir o IVA", 40)])]}]},
 {"slug": "analise-demonstracoes", "title": "Análise de Demonstrações Financeiras", "summary": "Rácios de liquidez, estrutura e rendibilidade.",
  "description": "Calcula e interpreta rácios a partir de demonstrações de empresas fictícias.", "difficulty": 2, "topic": "Análise financeira",
  "courses": ["CONT", "ECON", "GEST"], "competencies": ["Análise de rácios"], "completionRule": "Requisito de conclusão a confirmar.",
  "estHours": 12, "position": 2, "status": "PUBLISHED", "modules": [
   {"title": "Liquidez", "summary": "Liquidez geral, reduzida e imediata", "rooms": [
     room("an-r-liquidez-geral", "Liquidez geral", "Cálculo e interpretação.", ["Calcular a liquidez geral", "Interpretar o rácio"], 2, 25,
          "Os rácios de liquidez medem a capacidade de pagar as dívidas de curto prazo com os activos de curto prazo. " + FICT,
          [("an-liquidez-geral", "Liquidez geral", 120)]),
     room("an-r-liquidez", "Liquidez reduzida e imediata", "Rácios mais exigentes.", ["Excluir as existências", "Usar só meios monetários"], 2, 30,
          "A liquidez reduzida exclui as existências, que demoram a converter-se em dinheiro; a imediata considera só os meios monetários. " + FICT,
          [("an-liquidez-reduzida", "Liquidez reduzida", 120), ("an-liquidez-imediata", "Liquidez imediata", 60)], ["an-r-liquidez-geral"])]},
   {"title": "Estrutura financeira", "summary": "Autonomia, endividamento e solvabilidade", "rooms": [
     room("an-r-estrutura", "Autonomia e endividamento", "Como a empresa se financia.", ["Calcular a autonomia financeira", "Calcular o endividamento"], 2, 25,
          "A estrutura financeira mostra a parte do activo financiada por capitais próprios e por capitais alheios. " + FICT,
          [("an-autonomia", "Autonomia financeira", 60), ("an-endividamento", "Endividamento", 60)]),
     room("an-r-solvabilidade", "Solvabilidade", "A perspectiva dos credores.", ["Calcular e interpretar a solvabilidade"], 2, 25,
          "A solvabilidade compara o capital próprio com o passivo: quanto maior, maior a garantia dada aos credores. " + FICT,
          [("an-solvabilidade", "Solvabilidade", 120)], ["an-r-estrutura"])]},
   {"title": "Rendibilidade", "summary": "ROE, margem líquida e ROA", "rooms": [
     room("an-r-roe", "Rendibilidade dos capitais próprios", "ROE e margem líquida.", ["Calcular o ROE", "Calcular a margem líquida"], 2, 25,
          "A rendibilidade relaciona o resultado com os capitais investidos ou com as vendas. " + FICT,
          [("an-roe", "Rendibilidade dos capitais próprios", 60), ("an-margem-liquida", "Margem líquida", 60)]),
     room("an-r-roa", "Rendibilidade do activo", "Eficiência na utilização dos recursos.", ["Calcular e interpretar o ROA"], 2, 25,
          "O ROA mede quanto resultado a empresa gera por cada kwanza de activo. " + FICT,
          [("an-roa", "Rendibilidade do activo", 120)], ["an-r-roe"])]}]},
 {"slug": "power-bi-financas", "title": "Power BI para Finanças", "summary": "Datasets, medidas e visualizações.", "description": "Fase posterior.",
  "difficulty": 2, "topic": "Power BI", "courses": ["CONT", "ECON", "GEST"], "competencies": [], "estHours": 9, "position": 3, "status": "COMING_SOON", "modules": []},
 {"slug": "mercado-capitais", "title": "Mercado de Capitais", "summary": "Empresas fictícias, relatórios e carteira virtual.", "description": "Fase posterior.",
  "difficulty": 3, "topic": "Mercado", "courses": ["ECON", "GEST"], "competencies": [], "estHours": 8, "position": 4, "status": "COMING_SOON", "modules": []},
]

EXAMS = [
 {"slug": "exame-modulo-4-iva", "title": "Exame · Módulo 4 · IVA nas operações correntes", "track": "contabilidade-pgc", "module": 4,
  "durationMin": 20, "topic": "IVA", "count": 6, "passMark": 10},
 {"slug": "exame-final-pgc", "title": "Exame final · Contabilidade Financeira segundo o PGC", "track": "contabilidade-pgc", "module": 6,
  "durationMin": 40, "topic": "PGC", "count": 10, "passMark": 10},
 {"slug": "exame-analise-financeira", "title": "Exame · Análise de Demonstrações Financeiras", "track": "analise-demonstracoes", "module": 3,
  "durationMin": 25, "topic": "Análise", "count": 5, "passMark": 10},
]

keys = {e["key"] for e in EX}
assert len(keys) == len(EX), "chaves repetidas"
for t in TRACKS:
    for m in t["modules"]:
        for r in m["rooms"]:
            for k in r["tasks"]:
                assert k["exercise"] in keys, k["exercise"]
json.dump({"version": 1, "exercises": EX, "tracks": TRACKS, "exams": EXAMS}, open(out, "w"), ensure_ascii=False, indent=1)
print(len(EX), "exercícios;", sum(len(m["rooms"]) for t in TRACKS for m in t["modules"]), "salas;",
      sum(len(t["modules"]) for t in TRACKS), "módulos;", len(EXAMS), "exames")
