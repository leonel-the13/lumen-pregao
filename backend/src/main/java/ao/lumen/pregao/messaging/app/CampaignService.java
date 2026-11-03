package ao.lumen.pregao.messaging.app;

import ao.lumen.pregao.common.audit.AuditService;
import ao.lumen.pregao.common.config.AppProperties;
import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.common.mail.MailQueue;
import ao.lumen.pregao.common.security.RateLimiter;
import ao.lumen.pregao.common.web.CursorPage;
import ao.lumen.pregao.identity.app.MailingDirectory;
import ao.lumen.pregao.identity.app.MailingDirectory.Recipient;
import ao.lumen.pregao.identity.app.UserDirectory;
import ao.lumen.pregao.messaging.api.AdminEmailDtos.AudienceCount;
import ao.lumen.pregao.messaging.api.AdminEmailDtos.AudienceIn;
import ao.lumen.pregao.messaging.api.AdminEmailDtos.CampaignIn;
import ao.lumen.pregao.messaging.api.AdminEmailDtos.CampaignRow;
import ao.lumen.pregao.messaging.api.AdminEmailDtos.ContentIn;
import ao.lumen.pregao.messaging.api.AdminEmailDtos.PreviewView;
import ao.lumen.pregao.messaging.domain.Audience;
import ao.lumen.pregao.messaging.domain.Campaign;
import ao.lumen.pregao.messaging.domain.Delivery;
import ao.lumen.pregao.messaging.domain.DeliveryStatus;
import ao.lumen.pregao.messaging.domain.MessageContent;
import ao.lumen.pregao.messaging.infra.CampaignRepository;
import ao.lumen.pregao.messaging.infra.DeliveryRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emails da administração: a todos os estudantes, a um curso/ano, aos inactivos ou a uma pessoa.
 * A campanha e uma entrega por destinatário ficam persistidas antes do envio; o {@link CampaignWorker} envia-as.
 */
@Service
public class CampaignService {
    private static final Pattern KEY = Pattern.compile("^[A-Za-z0-9-]{8,64}$");
    static final int CAMPAIGNS_PER_HOUR = 10;
    static final int TESTS_PER_HOUR = 20;

    private final CampaignRepository campaigns;
    private final DeliveryRepository deliveries;
    private final MailingDirectory directory;
    private final UserDirectory users;
    private final MailQueue mail;
    private final RateLimiter limiter;
    private final AuditService audit;
    private final AppProperties props;
    private final Clock clock;

    public CampaignService(CampaignRepository campaigns, DeliveryRepository deliveries, MailingDirectory directory,
                           UserDirectory users, MailQueue mail, RateLimiter limiter, AuditService audit, AppProperties props,
                           Clock clock) {
        this.campaigns = campaigns;
        this.deliveries = deliveries;
        this.directory = directory;
        this.users = users;
        this.mail = mail;
        this.limiter = limiter;
        this.audit = audit;
        this.props = props;
        this.clock = clock;
    }

    record Resolved(List<Recipient> recipients, int optedOut, String label) {}

    @Transactional(readOnly = true)
    public AudienceCount count(AudienceIn a) {
        Resolved r = resolve(a);
        return new AudienceCount(r.recipients().size(), r.optedOut(), r.label());
    }

    public PreviewView preview(UUID me, ContentIn in) {
        MessageContent c = MessageRenderer.validate(in.toContent());
        var email = MessageRenderer.render(c, self(me), props.baseUrl(), clock.instant(), true);
        return new PreviewView(email.subject(), email.html(), email.text());
    }

    /** Envia o email só para quem está a escrevê-lo (com os seus dados nas variáveis). */
    public void sendTest(UUID me, ContentIn in) {
        MessageContent c = MessageRenderer.validate(in.toContent());
        if (!limiter.tryAcquire("email-test:" + me, TESTS_PER_HOUR, Duration.ofHours(1))) {
            throw DomainException.tooMany("Já enviaste muitos testes nesta hora. Usa a pré-visualização ou tenta mais tarde.");
        }
        Recipient r = self(me);
        var email = MessageRenderer.render(c, r, props.baseUrl(), clock.instant(), true);
        mail.enqueue(r.email(), new ao.lumen.pregao.common.mail.EmailTemplate.Email("[Teste] " + email.subject(), email.text(), email.html()));
    }

    @Transactional
    public CampaignRow create(UUID me, String key, CampaignIn in) {
        if (key == null || !KEY.matcher(key).matches()) {
            throw DomainException.badRequest("IDEMPOTENCY_KEY_REQUIRED",
                    "O cabeçalho Idempotency-Key é obrigatório (8 a 64 caracteres: letras, números e -).");
        }
        var existing = campaigns.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            if (!existing.get().getCreatedBy().equals(me)) {
                throw DomainException.conflict("IDEMPOTENCY_KEY_REUSED", "Esta chave já foi usada noutro pedido.");
            }
            return rows(List.of(existing.get())).get(0);
        }
        MessageContent c = MessageRenderer.validate(in.content().toContent());
        Resolved r = resolve(in.audience());
        if (r.recipients().isEmpty()) {
            throw DomainException.rule("NO_RECIPIENTS", "Ninguém deste grupo pode receber o email (sem contas activas ou com os emails desligados).");
        }
        if (!limiter.tryAcquire("email-campaign:" + me, CAMPAIGNS_PER_HOUR, Duration.ofHours(1))) {
            throw DomainException.tooMany("Já enviaste " + CAMPAIGNS_PER_HOUR + " emails nesta hora. Tenta mais tarde.");
        }
        AudienceIn a = in.audience();
        Campaign campaign = campaigns.save(new Campaign(me, key, in.templateId(), a.audience(),
                a.audience() == Audience.COURSE ? a.courseId() : null, a.audience() == Audience.COURSE ? a.year() : null,
                a.audience() == Audience.INACTIVE ? a.inactiveDays() : null, a.audience() == Audience.USER ? a.userId() : null,
                r.label(), c));
        deliveries.saveAll(r.recipients().stream().map(x -> new Delivery(campaign.getId(), x.id())).toList());
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("audience", r.label());
        after.put("recipients", r.recipients().size());
        after.put("subject", c.subject());
        audit.record(me, "EMAIL_CAMPAIGN_CREATED", "EmailCampaign", campaign.getId(), null, after);
        return rows(List.of(campaign)).get(0);
    }

    @Transactional(readOnly = true)
    public CursorPage<CampaignRow> page(String cursor, Integer limit) {
        int lim = CursorPage.clamp(limit);
        UUID c = cursor == null || cursor.isBlank() ? null : UUID.fromString(cursor);
        List<Campaign> list = campaigns.page(c, PageRequest.of(0, lim + 1));
        boolean more = list.size() > lim;
        List<Campaign> page = more ? list.subList(0, lim) : list;
        return new CursorPage<>(rows(page), more ? page.get(page.size() - 1).getId().toString() : null);
    }

    /** Volta a pôr na fila as entregas que falharam (ex.: limite diário do fornecedor de email). */
    @Transactional
    public int retryFailed(UUID me, UUID id) {
        campaigns.findById(id).orElseThrow(() -> DomainException.notFound("CAMPAIGN_NOT_FOUND", "O email não existe."));
        int n = deliveries.retryFailed(id);
        audit.record(me, "EMAIL_CAMPAIGN_RETRIED", "EmailCampaign", id, null, Map.of("requeued", n));
        return n;
    }

    Resolved resolve(AudienceIn a) {
        Instant now = clock.instant();
        return switch (a.audience()) {
            case USER -> {
                if (a.userId() == null) throw DomainException.badRequest("USER_REQUIRED", "Escolhe a pessoa que vai receber o email.");
                Recipient r = directory.active(a.userId()).orElseThrow(() ->
                        DomainException.rule("RECIPIENT_NOT_ACTIVE", "Esta conta não está activa. Para contas por confirmar, usa «Lembrar confirmação»."));
                yield new Resolved(List.of(r), 0, r.name() + " (individual)");
            }
            case ALL_STUDENTS -> optIn(directory.activeStudents(), "Todos os estudantes activos");
            case COURSE -> {
                if (a.courseId() == null) throw DomainException.badRequest("COURSE_REQUIRED", "Escolhe o curso.");
                List<Recipient> all = directory.activeStudents().stream()
                        .filter(r -> a.courseId().equals(r.courseId()))
                        .filter(r -> a.year() == null || a.year().equals(r.year())).toList();
                String course = all.isEmpty() ? "do curso escolhido" : "de " + all.get(0).courseName();
                yield optIn(all, "Estudantes " + course + (a.year() == null ? "" : " · " + a.year() + ".º ano"));
            }
            case INACTIVE -> {
                if (a.inactiveDays() == null) throw DomainException.badRequest("DAYS_REQUIRED", "Escolhe há quantos dias estão sem entrar.");
                Instant since = now.minus(Duration.ofDays(a.inactiveDays()));
                List<Recipient> all = directory.activeStudents().stream()
                        .filter(r -> r.lastActivity() != null && r.lastActivity().isBefore(since)).toList();
                yield optIn(all, "Estudantes sem entrar há " + a.inactiveDays() + " dias ou mais");
            }
        };
    }

    private static Resolved optIn(List<Recipient> all, String label) {
        List<Recipient> in = all.stream().filter(Recipient::emailOptIn).toList();
        return new Resolved(in, all.size() - in.size(), label);
    }

    private Recipient self(UUID me) {
        return directory.active(me).orElseThrow(() -> DomainException.forbidden("FORBIDDEN", "A tua conta não está activa."));
    }

    private List<CampaignRow> rows(List<Campaign> list) {
        if (list.isEmpty()) return List.of();
        Map<UUID, Map<DeliveryStatus, Long>> counts = new HashMap<>();
        for (Object[] row : deliveries.countsByCampaign(list.stream().map(Campaign::getId).toList())) {
            counts.computeIfAbsent((UUID) row[0], k -> new EnumMap<>(DeliveryStatus.class)).put((DeliveryStatus) row[1], (Long) row[2]);
        }
        var names = users.summaries(new HashSet<>(list.stream().map(Campaign::getCreatedBy).toList()));
        return list.stream().map(c -> {
            Map<DeliveryStatus, Long> m = counts.getOrDefault(c.getId(), Map.of());
            long sent = m.getOrDefault(DeliveryStatus.SENT, 0L), failed = m.getOrDefault(DeliveryStatus.FAILED, 0L);
            long skipped = m.getOrDefault(DeliveryStatus.SKIPPED, 0L), pending = m.getOrDefault(DeliveryStatus.PENDING, 0L);
            var author = names.get(c.getCreatedBy());
            return new CampaignRow(c.getId(), c.getSubject(), c.getAudience().name(), c.getAudienceLabel(),
                    author == null ? "—" : author.shortName(), c.getCreatedAt(), sent + failed + skipped + pending,
                    sent, failed, skipped, pending);
        }).toList();
    }
}
