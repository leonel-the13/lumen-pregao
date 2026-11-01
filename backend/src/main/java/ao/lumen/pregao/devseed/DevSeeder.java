package ao.lumen.pregao.devseed;

import ao.lumen.pregao.authoring.domain.EditorialStatus;
import ao.lumen.pregao.authoring.domain.Exercise;
import ao.lumen.pregao.authoring.domain.ExerciseReference;
import ao.lumen.pregao.authoring.domain.ExerciseVersion;
import ao.lumen.pregao.authoring.domain.LegalSource;
import ao.lumen.pregao.authoring.domain.ReviewWorkflow;
import ao.lumen.pregao.authoring.infra.ExerciseReferenceRepository;
import ao.lumen.pregao.authoring.infra.ExerciseRepository;
import ao.lumen.pregao.authoring.infra.ExerciseVersionRepository;
import ao.lumen.pregao.authoring.infra.LegalSourceRepository;
import ao.lumen.pregao.authoring.infra.ReviewWorkflowRepository;
import ao.lumen.pregao.contest.domain.Contest;
import ao.lumen.pregao.contest.domain.ContestProblem;
import ao.lumen.pregao.contest.domain.ContestState;
import ao.lumen.pregao.contest.domain.Scoreboard;
import ao.lumen.pregao.contest.domain.Team;
import ao.lumen.pregao.contest.domain.TeamMember;
import ao.lumen.pregao.contest.infra.ContestProblemRepository;
import ao.lumen.pregao.contest.infra.ContestRepository;
import ao.lumen.pregao.contest.infra.ScoreboardRepository;
import ao.lumen.pregao.contest.infra.TeamMemberRepository;
import ao.lumen.pregao.contest.infra.TeamRepository;
import ao.lumen.pregao.exam.domain.Exam;
import ao.lumen.pregao.exam.infra.ExamRepository;
import ao.lumen.pregao.grading.app.PgcAccountService;
import ao.lumen.pregao.identity.domain.User;
import ao.lumen.pregao.identity.infra.CourseRepository;
import ao.lumen.pregao.identity.infra.RoleRepository;
import ao.lumen.pregao.identity.infra.UserRepository;
import ao.lumen.pregao.learning.domain.ContentStatus;
import ao.lumen.pregao.learning.domain.Room;
import ao.lumen.pregao.learning.domain.Task;
import ao.lumen.pregao.learning.domain.Track;
import ao.lumen.pregao.learning.domain.TrackModule;
import ao.lumen.pregao.learning.infra.TrackRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Dados de desenvolvimento: utilizadores, exercícios publicados, trilhas, um exame e contests.
 * Só no perfil dev. Tudo fictício (pessoas, empresas, valores). Taxas e normas ficam «a confirmar».
 * Excepção documentada à regra de módulos: o seeder escreve directamente nos repositórios de cada módulo.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "app.dev-seed", name = "enabled", havingValue = "true")
public class DevSeeder {
    private static final Logger log = LoggerFactory.getLogger(DevSeeder.class);

    private final UserRepository users;
    private final RoleRepository roles;
    private final CourseRepository courses;
    private final ExerciseRepository exercises;
    private final ExerciseVersionRepository versions;
    private final ReviewWorkflowRepository workflows;
    private final ExerciseReferenceRepository references;
    private final LegalSourceRepository sources;
    private final TrackRepository tracks;
    private final ExamRepository exams;
    private final ContestRepository contests;
    private final ContestProblemRepository problems;
    private final TeamRepository teams;
    private final TeamMemberRepository members;
    private final ScoreboardRepository scoreboards;
    private final EntityManager em;
    private final PasswordEncoder encoder;
    private final TransactionTemplate tx;
    private final ObjectMapper json;
    private final Clock clock;
    private final String password;

    public DevSeeder(UserRepository users, RoleRepository roles, CourseRepository courses, ExerciseRepository exercises,
                     ExerciseVersionRepository versions, ReviewWorkflowRepository workflows,
                     ExerciseReferenceRepository references, LegalSourceRepository sources, TrackRepository tracks,
                     ExamRepository exams, ContestRepository contests, ContestProblemRepository problems,
                     TeamRepository teams, TeamMemberRepository members, ScoreboardRepository scoreboards,
                     EntityManager em, PasswordEncoder encoder, TransactionTemplate tx, ObjectMapper json, Clock clock,
                     @Value("${DEV_SEED_PASSWORD:lumen-dev-2026!}") String password) {
        this.users = users;
        this.roles = roles;
        this.courses = courses;
        this.exercises = exercises;
        this.versions = versions;
        this.workflows = workflows;
        this.references = references;
        this.sources = sources;
        this.tracks = tracks;
        this.exams = exams;
        this.contests = contests;
        this.problems = problems;
        this.teams = teams;
        this.members = members;
        this.scoreboards = scoreboards;
        this.em = em;
        this.encoder = encoder;
        this.tx = tx;
        this.json = json;
        this.clock = clock;
        this.password = password;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(0)
    public void seed() {
        if (tracks.count() > 0) return;
        tx.executeWithoutResult(s -> run());
        log.info("Dados de desenvolvimento criados (utilizadores @exemplo.ao)");
    }

    private void run() {
        String hash = encoder.encode(password);
        User kiala = user("kiala.afonso@exemplo.ao", "Kiala Afonso", "CONT", 3, hash, "STUDENT");
        User marta = user("marta.neto@exemplo.ao", "Marta Neto", "CONT", 4, hash, "STUDENT", "AUTHOR");
        User joao = user("joao.cassoma@exemplo.ao", "João Cassoma", "ECON", 3, hash, "STUDENT");
        User docente = user("docente.p@exemplo.ao", "Docente Pedro", "CONT", null, hash, "REVIEWER", "JURY");
        user("organizador@exemplo.ao", "Organização AE", "GEST", null, hash, "CONTEST_ORGANIZER");
        user("admin@exemplo.ao", "Admin AE", "GEST", null, hash, "ADMIN");
        user("ana.lopes@exemplo.ao", "Ana Lopes", "GEST", 2, hash, "STUDENT");

        LegalSource pgc = source("Plano Geral de Contabilidade");
        LegalSource iva = source("Código do IVA");
        UUID ivaId = iva.getId();

        // ───────────── exercícios (todos fictícios; taxa ligada a fonte por confirmar) ─────────────
        String compraParams = """
                {"params":[
                  {"name":"empresa","type":"text","rule":{"kind":"entity","source":"companies"}},
                  {"name":"fornecedor","type":"text","rule":{"kind":"set","values":["Indústrias Cuanza","Kwanza Energia"]}},
                  {"name":"valor","type":"money","rule":{"kind":"range","min":300000,"max":900000,"step":10000}},
                  {"name":"taxa","type":"percent","rule":{"kind":"constant","value":14,"legalSourceId":"%s"}}
                ]}""".formatted(ivaId);

        UUID contexto = publish(marta, docente, "IVA · Compras", "A", "Contexto da operação",
                "A [empresa] comprou mercadorias ao fornecedor [fornecedor], a crédito (30 dias). Que tipo de operação é?",
                """
                {"options":[{"key":"A","text":"Compra a pronto pagamento"},{"key":"B","text":"Compra a crédito"},
                            {"key":"C","text":"Venda a crédito"},{"key":"D","text":"Devolução de mercadorias"}],"multiple":false}""",
                compraParams, """
                {"correct":["B"]}""", "{}", "[]", 1, 30, List.of(iva));

        UUID contas = publish(marta, docente, "IVA · Compras", "A", "Identificar as contas",
                "Que contas são movimentadas no registo da compra a crédito da [empresa]? Escolhe todas as que se aplicam.",
                """
                {"options":[{"key":"A","text":"26 Mercadorias"},{"key":"B","text":"34.5.x IVA suportado"},
                            {"key":"C","text":"32 Fornecedores"},{"key":"D","text":"45 Caixa"},{"key":"E","text":"61 Vendas"}],
                 "multiple":true}""",
                compraParams, """
                {"correct":["A","B","C"]}""", "{}", "[]", 1, 20, List.of(pgc));

        UUID lancamento = publish(marta, docente, "IVA · Compras", "C", "Registar o lançamento",
                "Em 3 de março, a [empresa] (empresa fictícia) comprou mercadorias ao fornecedor [fornecedor], a crédito (30 dias). "
                        + "Valor das mercadorias: [valor]. Taxa de IVA do exercício: [taxa]. Regista o lançamento no Diário.",
                """
                {"currency":"Kz","summary":{"$expr":"valor","format":"money"}}""",
                compraParams, """
                {"lines":[{"account":"26","side":"D","expr":"valor"},
                          {"account":"34.5.x","side":"D","expr":"valor * taxa / 100"},
                          {"account":"32","side":"C","expr":"valor * (1 + taxa / 100)"}]}""",
                """
                {"type":"ABS","value":0.01}""",
                """
                ["A compra foi a crédito: a dívida ao fornecedor regista-se numa conta de Terceiros.",
                 "O IVA suportado calcula-se sobre o valor das mercadorias."]""", 2, 40, List.of(pgc, iva));

        String ivaMesParams = """
                {"params":[
                  {"name":"empresa","type":"text","rule":{"kind":"entity","source":"companies"}},
                  {"name":"vendas","type":"money","rule":{"kind":"range","min":800000,"max":1600000,"step":20000}},
                  {"name":"compras","type":"money","rule":{"kind":"range","min":300000,"max":700000,"step":10000}},
                  {"name":"taxa","type":"percent","rule":{"kind":"constant","value":14,"legalSourceId":"%s"}}
                ]}""".formatted(ivaId);

        UUID apurarMes = publish(marta, docente, "IVA · Apuramento", "E", "Apurar o IVA do mês",
                "No mês de março, a [empresa] vendeu [vendas] (base) e comprou [compras] (base) de mercadorias. "
                        + "Taxa de IVA do exercício: [taxa]. Apura o IVA do período, passo a passo.",
                """
                {"steps":[{"key":"liquidado","label":"IVA liquidado nas vendas","formula":"base das vendas × taxa"},
                          {"key":"dedutivel","label":"IVA suportado dedutível","formula":"base das compras × taxa",
                           "help":"Considera apenas IVA com documento válido e dedutível."},
                          {"key":"apurado","label":"Imposto apurado","formula":"IVA liquidado − IVA dedutível"}]}""",
                ivaMesParams, """
                {"steps":[{"key":"liquidado","expr":"vendas * taxa / 100"},
                          {"key":"dedutivel","expr":"compras * taxa / 100"},
                          {"key":"apurado","expr":"(vendas - compras) * taxa / 100"}]}""",
                """
                {"type":"ABS","value":0.01}""",
                """
                ["Se o IVA dedutível for maior do que o liquidado, o resultado é um crédito de imposto."]""", 2, 40, List.of(iva));

        UUID pagamento = publish(marta, docente, "IVA · Compras", "C", "Pagamento ao fornecedor",
                "Trinta dias depois, a [empresa] pagou por transferência bancária a dívida ao fornecedor [fornecedor] "
                        + "relativa à compra de [valor] de mercadorias, com IVA à taxa [taxa]. Regista o pagamento.",
                "{}", compraParams, """
                {"lines":[{"account":"32","side":"D","expr":"valor * (1 + taxa / 100)"},
                          {"account":"43","side":"C","expr":"valor * (1 + taxa / 100)"}]}""",
                """
                {"type":"ABS","value":0.01}""", """
                ["O pagamento foi por transferência: usa a conta de depósitos à ordem."]""", 2, 30, List.of(pgc));

        UUID venda = publish(marta, docente, "IVA · Vendas", "C", "Venda com IVA liquidado",
                "A [empresa] vendeu mercadorias a crédito a um cliente por [valor] (base). Taxa de IVA do exercício: [taxa]. "
                        + "Regista a venda (sem o custo das mercadorias vendidas).",
                "{}", compraParams, """
                {"lines":[{"account":"31","side":"D","expr":"valor * (1 + taxa / 100)"},
                          {"account":"61","side":"C","expr":"valor"},
                          {"account":"34.5.y","side":"C","expr":"valor * taxa / 100"}]}""",
                """
                {"type":"ABS","value":0.01}""", """
                ["O IVA liquidado na venda é uma dívida ao Estado."]""", 2, 100, List.of(pgc, iva));

        UUID classes = publish(marta, docente, "PGC · Fundamentos", "A", "Classes do PGC",
                "Em que classe do PGC (versão do exercício) se registam os meios monetários, como a caixa e os depósitos à ordem?",
                """
                {"options":[{"key":"A","text":"Classe 2"},{"key":"B","text":"Classe 3"},{"key":"C","text":"Classe 4"},{"key":"D","text":"Classe 5"}]}""",
                "{\"params\":[]}", """
                {"correct":["C"]}""", "{}", "[]", 1, 40, List.of(pgc));

        UUID balanco = publish(marta, docente, "Demonstrações financeiras", "D", "Balanço a 31 de março",
                "Com base no balancete final (excerto) da [empresa], elabora o balanço a 31 de março. Valores fictícios gerados para esta tentativa.",
                """
                {"ledger":[
                   {"account":"11","label":"Imobilizações corpóreas","value":{"$expr":"imob"},"side":"D"},
                   {"account":"18","label":"Amortizações acumuladas","value":{"$expr":"amort"},"side":"C"},
                   {"account":"26","label":"Mercadorias","value":{"$expr":"merc"},"side":"D"},
                   {"account":"31","label":"Clientes","value":{"$expr":"clientes"},"side":"D"},
                   {"account":"43","label":"Depósitos à ordem","value":{"$expr":"depositos"},"side":"D"},
                   {"account":"32","label":"Fornecedores","value":{"$expr":"fornecedores"},"side":"C"},
                   {"account":"34","label":"Estado","value":{"$expr":"estado"},"side":"C"},
                   {"account":"51","label":"Capital","value":{"$expr":"capital"},"side":"C"},
                   {"account":"88","label":"Resultado líquido","value":{"$expr":"resultado"},"side":"C"}],
                 "sections":[
                   {"key":"activo","title":"Activo","totalKey":"total_activo","totalLabel":"Total do Activo","groups":[
                     {"title":"Activo não corrente","items":[{"key":"imob_liq","label":"Imobilizações corpóreas (líquidas)"}]},
                     {"title":"Activo corrente","items":[{"key":"existencias","label":"Existências"},{"key":"clientes","label":"Clientes"},
                                                          {"key":"meios","label":"Meios monetários"}]}]},
                   {"key":"cp_passivo","title":"Capital próprio e passivo","totalKey":"total_cp_passivo",
                    "totalLabel":"Total do Capital próprio e Passivo","groups":[
                     {"title":"Capital próprio","items":[{"key":"capital","label":"Capital"},{"key":"resultado","label":"Resultado líquido do exercício"}]},
                     {"title":"Passivo corrente","items":[{"key":"fornecedores","label":"Fornecedores"},{"key":"estado","label":"Estado (IVA a pagar)"}]}]}],
                 "relations":[{"label":"Activo não corrente = bruto − amortizações"},{"label":"Activo corrente = soma das rubricas"},
                              {"label":"Activo = Capital próprio + Passivo"}]}""",
                """
                {"params":[
                  {"name":"empresa","type":"text","rule":{"kind":"entity","source":"companies"}},
                  {"name":"imob","type":"money","visible":false,"rule":{"kind":"range","min":2000000,"max":3000000,"step":100000}},
                  {"name":"amort","type":"money","visible":false,"rule":{"kind":"expr","expr":"imob * 0.2","scale":2}},
                  {"name":"merc","type":"money","visible":false,"rule":{"kind":"range","min":600000,"max":1000000,"step":20000}},
                  {"name":"clientes","type":"money","visible":false,"rule":{"kind":"range","min":400000,"max":800000,"step":20000}},
                  {"name":"depositos","type":"money","visible":false,"rule":{"kind":"range","min":700000,"max":1200000,"step":20000}},
                  {"name":"fornecedores","type":"money","visible":false,"rule":{"kind":"range","min":500000,"max":900000,"step":20000}},
                  {"name":"estado","type":"money","visible":false,"rule":{"kind":"range","min":50000,"max":120000,"step":10000}},
                  {"name":"capital","type":"money","visible":false,"rule":{"kind":"constant","value":2500000}},
                  {"name":"resultado","type":"money","visible":false,
                   "rule":{"kind":"expr","expr":"imob - amort + merc + clientes + depositos - fornecedores - estado - capital","scale":2}}
                ]}""",
                """
                {"items":{"imob_liq":"imob - amort","existencias":"merc","clientes":"clientes","meios":"depositos",
                          "total_activo":"imob - amort + merc + clientes + depositos","capital":"capital","resultado":"resultado",
                          "fornecedores":"fornecedores","estado":"estado",
                          "total_cp_passivo":"capital + resultado + fornecedores + estado"},
                 "relations":[{"label":"Activo = Capital próprio + Passivo","left":"total_activo","right":"total_cp_passivo"},
                              {"label":"Activo corrente = soma das rubricas","left":"total_activo","right":"imob_liq + existencias + clientes + meios"}]}""",
                """
                {"type":"ABS","value":0.01}""", """
                ["O resultado líquido do exercício faz parte do capital próprio."]""", 3, 200, List.of(pgc));

        UUID apuramento = publish(marta, docente, "IVA · Apuramento", "E", "Apuramento mensal do IVA",
                "A [empresa] apresenta, no período, vendas de [vendas] e compras de [compras] (bases tributáveis). "
                        + "Taxa aplicável: [taxa]. Apura o IVA do período.",
                """
                {"steps":[{"key":"liquidado","label":"IVA liquidado nas vendas","formula":"base das vendas × taxa"},
                          {"key":"dedutivel","label":"IVA suportado dedutível","formula":"base das compras × taxa",
                           "help":"Considera apenas IVA com documento válido e dedutível."},
                          {"key":"apurado","label":"Imposto apurado","formula":"IVA liquidado − IVA dedutível"},
                          {"key":"entregar","label":"Resultado do período","formula":"a entregar ao Estado (se positivo)"}]}""",
                ivaMesParams, """
                {"steps":[{"key":"liquidado","expr":"vendas * taxa / 100"},
                          {"key":"dedutivel","expr":"compras * taxa / 100"},
                          {"key":"apurado","expr":"(vendas - compras) * taxa / 100"},
                          {"key":"entregar","expr":"max((vendas - compras) * taxa / 100, 0)"}]}""",
                """
                {"type":"ABS","value":0.01}""", """
                ["Se o IVA dedutível for maior do que o liquidado, o resultado é um crédito de imposto a reportar."]""",
                2, 140, List.of(iva));

        UUID liquidez = publish(marta, docente, "Análise · Liquidez", "F", "Liquidez geral",
                "A [empresa] apresenta activo corrente de [ac] e passivo corrente de [pc]. Calcula a liquidez geral "
                        + "(duas casas decimais) e interpreta o resultado.",
                """
                {"unit":"","precision":2,"justification":{"minLength":80}}""",
                """
                {"params":[
                  {"name":"empresa","type":"text","rule":{"kind":"entity","source":"companies"}},
                  {"name":"ac","type":"money","rule":{"kind":"range","min":800000,"max":2400000,"step":50000}},
                  {"name":"pc","type":"money","rule":{"kind":"range","min":500000,"max":1500000,"step":50000}}
                ]}""",
                """
                {"expr":"ac / pc","scale":2,"rounding":"HALF_UP",
                 "justification":{"minLength":80,"keywordsAny":["liquidez","curto prazo","passivo corrente","solvência"],"keywordsMin":1}}""",
                """
                {"type":"ABS","value":0.01}""", "[]", 2, 120, List.of());

        UUID ivaVendaB = publish(marta, docente, "IVA · Liquidação", "B", "IVA liquidado numa venda",
                "Uma empresa vendeu mercadorias por [vendas] (base). Com a taxa do exercício de [taxa], qual é o IVA liquidado (Kz)?",
                """
                {"unit":"Kz","precision":2}""", ivaMesParams, """
                {"expr":"vendas * taxa / 100","scale":2}""", """
                {"type":"ABS","value":0.01}""", "[]", 1, 20, List.of(iva));

        UUID ivaResultadoA = publish(marta, docente, "IVA · Apuramento", "A", "Resultado do apuramento",
                "Uma empresa liquidou 168 000,00 Kz de IVA nas vendas e suportou 70 000,00 Kz de IVA dedutível nas compras do mesmo período. "
                        + "Qual é o resultado do apuramento?",
                """
                {"options":[{"key":"A","text":"IVA a entregar ao Estado de 98 000,00 Kz"},
                            {"key":"B","text":"Crédito de imposto de 98 000,00 Kz"},
                            {"key":"C","text":"IVA a entregar de 238 000,00 Kz"},
                            {"key":"D","text":"Não há imposto a apurar"}]}""",
                "{\"params\":[]}", """
                {"correct":["A"]}""", "{}", "[]", 1, 20, List.of(iva));

        UUID ivaDedutivelB = publish(marta, docente, "IVA · Dedução", "B", "IVA dedutível nas compras",
                "Uma empresa comprou mercadorias por [compras] (base), com documento válido. Com a taxa do exercício de [taxa], "
                        + "qual é o IVA dedutível (Kz)?",
                """
                {"unit":"Kz","precision":2}""", ivaMesParams, """
                {"expr":"compras * taxa / 100","scale":2}""", """
                {"type":"ABS","value":0.01}""", "[]", 1, 20, List.of(iva));

        // Um exercício em revisão, para a fila do revisor (autor ≠ revisor).
        draftInReview(marta, "IVA · Compras", "IVA nas compras com desconto comercial", ivaId);

        // ───────────── trilhas ─────────────
        Track pgcTrack = track("contabilidade-pgc", "Contabilidade Financeira segundo o PGC",
                "Balancete, lançamentos e demonstrações financeiras.",
                "Objectivos: registar operações correntes, preparar o balancete e elaborar o balanço e a demonstração de resultados.",
                1, "Contabilidade", List.of("CONT", "ECON", "GEST"),
                List.of("Registo contabilístico", "IVA", "Demonstrações financeiras"),
                "Requisito de conclusão: todas as salas ≥ 70 % e exame final ≥ 10/20 (a confirmar por trilha).", 18, 0,
                ContentStatus.PUBLISHED);
        TrackModule m1 = module(pgcTrack, 1, "Fundamentos e o PGC", "Quadro de contas, classes e movimentação");
        Room r11 = room(m1, 1, "Quadro de contas e classes", "Classes do PGC e a sua movimentação.", 1, 15,
                List.of("Reconhecer as classes do PGC"), List.of(),
                "O Plano Geral de Contabilidade organiza as contas por classes. Nesta sala revês a classe de cada grupo de contas. "
                        + "A versão do PGC usada no exercício está **a confirmar** pelo revisor.");
        task(r11, 1, "Classes do PGC", classes, 40);

        TrackModule m2 = module(pgcTrack, 2, "Meios monetários e terceiros", "Caixa, depósitos, clientes, fornecedores");
        Room r21 = room(m2, 1, "Pagamento a fornecedores", "Liquidação de dívidas a fornecedores.", 2, 20,
                List.of("Registar pagamentos por transferência"), List.of(r11.getId()),
                "Os pagamentos a fornecedores reduzem a dívida registada em Terceiros e os meios monetários.");
        task(r21, 1, "Pagamento ao fornecedor", pagamento, 30);

        module(pgcTrack, 3, "Existências", "Compras, mercadorias, inventário");

        TrackModule m4 = module(pgcTrack, 4, "IVA nas operações correntes", "Suportado, liquidado, apuramento");
        Room r42 = room(m4, 2, "Compra de mercadorias com IVA", "Lançamentos, IVA suportado e pagamento.", 2, 35,
                List.of("Registar a compra a crédito", "Separar o IVA suportado", "Verificar o equilíbrio"), List.of(r11.getId()),
                "A **Distribuidora Mussulo, Lda.** e as restantes entidades são empresas fictícias. Os valores destacados "
                        + "são únicos para a tua tentativa. A taxa de IVA é um parâmetro do exercício (**a confirmar** com o revisor).");
        task(r42, 1, "Contexto da operação", contexto, 30);
        task(r42, 2, "Identificar as contas", contas, 20);
        task(r42, 3, "Registar o lançamento", lancamento, 40);
        task(r42, 4, "Apurar o IVA do mês", apurarMes, 40);
        task(r42, 5, "Pagamento ao fornecedor", pagamento, 30);
        Room r43 = room(m4, 3, "Venda com IVA liquidado", "Registo de vendas a crédito com IVA.", 2, 30,
                List.of("Registar a venda", "Separar o IVA liquidado"), List.of(r42.getId()),
                "Na venda, o IVA liquidado é uma dívida ao Estado até ao apuramento.");
        task(r43, 1, "Registar a venda", venda, 100);

        module(pgcTrack, 5, "Balancete e regularizações", "Balancete, acréscimos e diferimentos");
        TrackModule m6 = module(pgcTrack, 6, "Demonstrações financeiras", "Balanço e demonstração de resultados");
        Room r61 = room(m6, 1, "Balanço a 31 de março", "Do balancete final ao balanço.", 3, 40,
                List.of("Classificar as contas do balancete", "Verificar Activo = Capital próprio + Passivo"), List.of(r43.getId()),
                "Usa o balancete final para preencher o balanço. A estrutura de rubricas segue o modelo do PGC (**a confirmar**).");
        task(r61, 1, "Elaborar o balanço", balanco, 200);

        Track ivaTrack = track("fiscalidade-iva", "Fiscalidade Angolana · IVA",
                "Incidência, taxas, liquidação e regularizações.",
                "Pratica o cálculo do IVA passo a passo. Base: Código do IVA, Lei n.º 7/19 (versão em vigor a confirmar).",
                2, "Fiscalidade", List.of("CONT", "ECON", "GEST"), List.of("IVA", "Apuramento"),
                "Requisito de conclusão a confirmar.", 10, 1, ContentStatus.PUBLISHED);
        TrackModule f1 = module(ivaTrack, 1, "Incidência e taxas", "Liquidação e dedução");
        Room rf1 = room(f1, 1, "Apuramento mensal do IVA", "Calculadora fiscal passo a passo.", 2, 30,
                List.of("Calcular o IVA liquidado e o dedutível", "Apurar o imposto do período"), List.of(),
                "Cada passo é corrigido no servidor antes de avançares para o seguinte.");
        task(rf1, 1, "Apuramento mensal do IVA", apuramento, 140);
        Room rf2 = room(f1, 2, "Liquidação em vendas", "IVA liquidado e IVA dedutível.", 1, 20,
                List.of("Calcular o IVA de uma operação"), List.of(),
                "Exercícios numéricos sobre o IVA de operações simples. Valores fictícios.");
        task(rf2, 1, "IVA liquidado numa venda", ivaVendaB, 20);
        task(rf2, 2, "IVA dedutível nas compras", ivaDedutivelB, 20);

        Track analise = track("analise-demonstracoes", "Análise de Demonstrações Financeiras",
                "Rácios de liquidez, rendibilidade e estrutura.", "Calcula e interpreta rácios a partir de demonstrações fictícias.",
                2, "Análise financeira", List.of("CONT", "ECON", "GEST"), List.of("Análise de rácios"),
                "Requisito de conclusão a confirmar.", 12, 2, ContentStatus.PUBLISHED);
        TrackModule a1 = module(analise, 1, "Liquidez", "Liquidez geral, reduzida e imediata");
        Room ra1 = room(a1, 1, "Liquidez geral", "Cálculo e interpretação.", 2, 25, List.of("Calcular a liquidez geral",
                "Interpretar o rácio"), List.of(), "Os dados são de empresas fictícias.");
        task(ra1, 1, "Liquidez geral", liquidez, 120);

        track("power-bi-financas", "Power BI para Finanças", "Datasets, medidas e visualizações.",
                "Fase posterior.", 2, "Power BI", List.of("CONT", "ECON", "GEST"), List.of(), null, 9, 3, ContentStatus.COMING_SOON);
        track("mercado-capitais", "Mercado de Capitais", "Empresas fictícias, relatórios e carteira virtual.",
                "Fase posterior.", 3, "Mercado", List.of("ECON", "GEST"), List.of(), null, 8, 4, ContentStatus.COMING_SOON);

        // ───────────── exame ─────────────
        exams.save(new Exam("exame-modulo-4-iva", "Exame · Módulo 4 · IVA nas operações correntes", m4.getId(), 20,
                read("""
                        {"topic":"IVA","count":5,"types":["A","B"]}"""), new BigDecimal("10.00"), "PUBLISHED"));

        // ───────────── contests ─────────────
        Instant now = clock.instant().truncatedTo(ChronoUnit.MINUTES);
        Contest maratona = new Contest("maratona-ronda-1", docente.getId());
        maratona.define("Maratona de casos · Ronda 1",
                "Os enunciados são libertados pelo servidor à hora de início. Fase 1: exercícios corrigidos automaticamente. "
                        + "Fase 2 (caso aberto avaliado por júri) em fase posterior.",
                "TEAM", 2, 5, now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(6)),
                now.plus(Duration.ofDays(6)).plus(Duration.ofHours(4)),
                now.plus(Duration.ofDays(6)).plus(Duration.ofHours(6)).plus(Duration.ofMinutes(30)),
                now.plus(Duration.ofDays(6)).plus(Duration.ofHours(7)), 30, 10, null);
        maratona.moveTo(ContestState.REGISTRATION);
        contests.save(maratona);
        problem(maratona, "A", "Rácios de liquidez do T3", "Kwanza Energia", liquidez, 100, 0);
        problem(maratona, "B", "Registar a compra a crédito", "Banco Mutamba", lancamento, 150, 1);
        problem(maratona, "C", "IVA do trimestre", "Talatona Telecom", apuramento, 150, 2);
        problem(maratona, "D", "Balanço", "Indústrias Cuanza", balanco, 200, 3);
        problem(maratona, "E", "Pagamento a fornecedores", "Distribuidora Mussulo, Lda.", pagamento, 200, 4);
        Team pd = teams.save(new Team(maratona.getId(), "Partidas Dobradas", false));
        members.save(new TeamMember(pd.getId(), maratona.getId(), kiala.getId(), "CAPTAIN", now));
        members.save(new TeamMember(pd.getId(), maratona.getId(), marta.getId(), "MEMBER", now));
        members.save(new TeamMember(pd.getId(), maratona.getId(), joao.getId(), "MEMBER", now));
        scoreboards.save(new Scoreboard(maratona.getId(), pd.getId()));

        // Treino individual que começa pouco depois do arranque: permite ver o contest ao vivo em desenvolvimento.
        Contest treino = new Contest("treino-ronda-0", docente.getId());
        treino.define("Treino · Ronda 0 (individual)", "Treino individual para experimentar o relógio do servidor, as submissões e o ranking ao vivo.",
                "INDIVIDUAL", 1, 1, now.minus(Duration.ofHours(1)), now.plus(Duration.ofMinutes(10)),
                now.plus(Duration.ofMinutes(10)), now.plus(Duration.ofMinutes(100)), now.plus(Duration.ofMinutes(130)), 5, 10, null);
        treino.moveTo(ContestState.REGISTRATION);
        contests.save(treino);
        problem(treino, "A", "IVA liquidado numa venda", "Distribuidora Mussulo, Lda.", ivaVendaB, 100, 0);
        problem(treino, "B", "Registar o lançamento", "Indústrias Cuanza", lancamento, 150, 1);
        problem(treino, "C", "Apuramento do IVA", "Talatona Telecom", apuramento, 150, 2);
    }

    // ───────────────────────── auxiliares ─────────────────────────

    private User user(String email, String name, String course, Integer year, String hash, String... roleCodes) {
        User u = new User(email, hash, name, courses.findByCode(course).orElseThrow(), year,
                email.substring(0, email.indexOf('@')).replace('.', '-'), clock.instant());
        u.activate();
        u.getRoles().addAll(roles.findByCodeIn(List.of(roleCodes)));
        return users.save(u);
    }

    private LegalSource source(String title) {
        return sources.findAll().stream().filter(s -> s.getTitle().equals(title)).findFirst().orElseThrow();
    }

    private UUID publish(User author, User reviewer, String topic, String type, String title, String statement,
                         String presentation, String paramSpec, String solution, String tolerance, String hints,
                         int difficulty, int points, List<LegalSource> refs) {
        Exercise e = exercises.save(new Exercise("EX-%04d".formatted(exercises.nextCode()), author.getId(), topic));
        ExerciseVersion v = new ExerciseVersion(e, 1);
        v.define(type, title, statement, read(presentation), read(paramSpec), read(solution), read(tolerance), read(hints),
                "DETAILED", difficulty, points, 3, PgcAccountService.DEFAULT_VERSION, null, "Criado (dados de desenvolvimento)");
        versions.save(v);
        ReviewWorkflow w = new ReviewWorkflow(v);
        Instant now = clock.instant();
        w.moveTo(EditorialStatus.IN_REVIEW, author.getId(), null, now);
        w.moveTo(EditorialStatus.APPROVED, reviewer.getId(), "Referências por confirmar.", now);
        w.moveTo(EditorialStatus.PUBLISHED, reviewer.getId(), null, now);
        workflows.save(w);
        for (LegalSource s : refs) {
            references.save(new ExerciseReference(v, s, "a indicar pelo revisor", java.time.LocalDate.now(clock)));
        }
        return v.getId();
    }

    private void draftInReview(User author, String topic, String title, UUID ivaId) {
        Exercise e = exercises.save(new Exercise("EX-%04d".formatted(exercises.nextCode()), author.getId(), topic));
        ExerciseVersion v = new ExerciseVersion(e, 1);
        String params = """
                {"params":[
                  {"name":"empresa","type":"text","rule":{"kind":"entity","source":"companies"}},
                  {"name":"valor_bruto","type":"money","rule":{"kind":"range","min":300000,"max":900000,"step":10000}},
                  {"name":"desconto","type":"percent","rule":{"kind":"set","values":[2,5,10]}},
                  {"name":"taxa","type":"percent","rule":{"kind":"constant","value":14,"legalSourceId":"%s"}}
                ]}""".formatted(ivaId);
        v.define("C", title,
                "A [empresa] comprou mercadorias no valor de [valor_bruto], com desconto comercial de [desconto]. "
                        + "Regista a compra com IVA à taxa [taxa].", read("{}"), read(params), read("""
                        {"lines":[{"account":"26","side":"D","expr":"valor_bruto * (1 - desconto / 100)"},
                                  {"account":"34.5.x","side":"D","expr":"valor_bruto * (1 - desconto / 100) * taxa / 100"},
                                  {"account":"32","side":"C","expr":"valor_bruto * (1 - desconto / 100) * (1 + taxa / 100)"}]}"""),
                read("{\"type\":\"ABS\",\"value\":0.01}"), read("[]"), "DETAILED", 2, 60, 3,
                PgcAccountService.DEFAULT_VERSION, null, "Tolerância ajustada");
        versions.save(v);
        ReviewWorkflow w = new ReviewWorkflow(v);
        w.moveTo(EditorialStatus.IN_REVIEW, author.getId(), null, clock.instant());
        workflows.save(w);
        references.save(new ExerciseReference(v, source("Código do IVA"), "a indicar pelo revisor", java.time.LocalDate.now(clock)));
    }

    private Track track(String slug, String title, String summary, String description, int difficulty, String topic,
                        List<String> courses, List<String> competencies, String rule, Integer hours, int position,
                        ContentStatus status) {
        var t = new Track(slug, title, summary, description, difficulty, topic, new ArrayList<>(courses),
                new ArrayList<>(competencies), rule == null ? read("{}") : json.createObjectNode().put("text", rule), hours,
                position, status);
        em.persist(t);
        return t;
    }

    private TrackModule module(Track t, int position, String title, String summary) {
        var m = new TrackModule(t, position, title, summary);
        em.persist(m);
        return m;
    }

    private Room room(TrackModule m, int position, String title, String description, int difficulty, int minutes,
                      List<String> objectives, List<UUID> prerequisites, String context) {
        var r = new Room(m, position, title, description, new ArrayList<>(objectives), difficulty, minutes, context,
                read("""
                        [{"title":"Plano Geral de Contabilidade · excerto de contas usadas","kind":"pgc"},
                         {"title":"Regras de pontuação","kind":"rules"}]"""),
                new ArrayList<>(prerequisites), ContentStatus.PUBLISHED);
        em.persist(r);
        return r;
    }

    private void task(Room r, int position, String title, UUID versionId, int points) {
        em.persist(new Task(r, position, title, versionId, points));
    }

    private void problem(Contest c, String label, String title, String company, UUID versionId, int points, int pos) {
        problems.save(new ContestProblem(c.getId(), label, title, company, versionId, points, pos));
    }

    private JsonNode read(String s) {
        try {
            return json.readTree(s);
        } catch (Exception e) {
            throw new IllegalStateException("JSON inválido no seeder: " + s, e);
        }
    }
}
