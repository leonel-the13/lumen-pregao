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
import ao.lumen.pregao.content.ContentPackImporter;
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
import ao.lumen.pregao.grading.app.PgcAccountService;
import ao.lumen.pregao.identity.domain.User;
import ao.lumen.pregao.identity.infra.CourseRepository;
import ao.lumen.pregao.identity.infra.RoleRepository;
import ao.lumen.pregao.identity.infra.UserRepository;
import ao.lumen.pregao.learning.infra.TrackRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    private final ContestRepository contests;
    private final ContestProblemRepository problems;
    private final TeamRepository teams;
    private final TeamMemberRepository members;
    private final ScoreboardRepository scoreboards;
    private final ContentPackImporter importer;
    private final PasswordEncoder encoder;
    private final TransactionTemplate tx;
    private final ObjectMapper json;
    private final Clock clock;
    private final String password;

    public DevSeeder(UserRepository users, RoleRepository roles, CourseRepository courses, ExerciseRepository exercises,
                     ExerciseVersionRepository versions, ReviewWorkflowRepository workflows,
                     ExerciseReferenceRepository references, LegalSourceRepository sources, TrackRepository tracks,
                     ContestRepository contests, ContestProblemRepository problems,
                     TeamRepository teams, TeamMemberRepository members, ScoreboardRepository scoreboards,
                     ContentPackImporter importer, PasswordEncoder encoder, TransactionTemplate tx, ObjectMapper json, Clock clock,
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
        this.contests = contests;
        this.problems = problems;
        this.teams = teams;
        this.members = members;
        this.scoreboards = scoreboards;
        this.importer = importer;
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

        // Conteúdo: o mesmo pacote da produção, aprovado e publicado aqui pelo revisor de teste (só em desenvolvimento).
        importer.importPack(null);
        for (var w : workflows.findByStatusInOrderBySubmittedAtAsc(List.of(EditorialStatus.IN_REVIEW))) {
            w.moveTo(EditorialStatus.APPROVED, docente.getId(), "Aprovado (dados de desenvolvimento).", clock.instant());
            w.moveTo(EditorialStatus.PUBLISHED, docente.getId(), null, clock.instant());
        }
        UUID liquidez = version("an-liquidez-geral");
        UUID lancamento = version("iva-registar-compra");
        UUID apuramento = version("iva-apuramento-mensal");
        UUID balanco = version("pgc-balanco");
        UUID pagamento = version("iva-pagamento-fornecedor");
        UUID ivaVendaB = version("iva-liquidado-venda");

        // Um exercício em revisão, para a fila do revisor (autor ≠ revisor).
        draftInReview(marta, "IVA · Compras", "IVA nas compras com desconto comercial",
                sources.findAll().stream().filter(x -> x.getTitle().startsWith("Código do IVA")).findFirst().orElseThrow().getId());

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

    private UUID version(String key) {
        var e = exercises.findByExternalKey(key).orElseThrow();
        return versions.findByExerciseIdOrderByNumberDesc(e.getId()).getFirst().getId();
    }

    private LegalSource source(String title) {
        return sources.findAll().stream().filter(s -> s.getTitle().equals(title)).findFirst().orElseThrow();
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
