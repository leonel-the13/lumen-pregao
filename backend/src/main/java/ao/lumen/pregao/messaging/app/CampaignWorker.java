package ao.lumen.pregao.messaging.app;

import ao.lumen.pregao.common.config.AppProperties;
import ao.lumen.pregao.common.mail.EmailTemplate;
import ao.lumen.pregao.common.mail.MailQueue;
import ao.lumen.pregao.identity.app.MailingDirectory;
import ao.lumen.pregao.messaging.domain.Audience;
import ao.lumen.pregao.messaging.domain.Campaign;
import ao.lumen.pregao.messaging.domain.Delivery;
import ao.lumen.pregao.messaging.domain.DeliveryStatus;
import ao.lumen.pregao.messaging.infra.CampaignRepository;
import ao.lumen.pregao.messaging.infra.DeliveryRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Envia as entregas PENDING, uma de cada vez e ao ritmo do fornecedor. O envio fica fora das transacções:
 * a entrega é lida numa transacção curta, o email sai, e o resultado é gravado noutra.
 */
@Component
public class CampaignWorker {
    private static final Logger log = LoggerFactory.getLogger(CampaignWorker.class);
    private static final int BATCH = 50;

    private final DeliveryRepository deliveries;
    private final CampaignRepository campaigns;
    private final MailingDirectory directory;
    private final MailQueue mail;
    private final TransactionTemplate tx;
    private final AppProperties props;
    private final Clock clock;

    public CampaignWorker(DeliveryRepository deliveries, CampaignRepository campaigns, MailingDirectory directory, MailQueue mail,
                          TransactionTemplate tx, AppProperties props, Clock clock) {
        this.deliveries = deliveries;
        this.campaigns = campaigns;
        this.directory = directory;
        this.mail = mail;
        this.tx = tx;
        this.props = props;
        this.clock = clock;
    }

    record Prepared(UUID deliveryId, UUID campaignId, String to, EmailTemplate.Email email) {}

    @Scheduled(fixedDelayString = "${app.mail.campaign-poll-ms:5000}", initialDelay = 5000)
    public void run() {
        List<UUID> ids = tx.execute(s -> deliveries.pendingIds(PageRequest.of(0, BATCH)));
        if (ids == null) return;
        for (UUID id : ids) {
            try {
                process(id);
            } catch (RuntimeException e) {
                log.error("Falha ao processar a entrega {}", id, e);
            }
        }
    }

    void process(UUID id) {
        Prepared p = tx.execute(s -> prepare(id));
        if (p == null) return;
        try {
            mail.sendNow(p.to(), p.email());
            tx.executeWithoutResult(s -> deliveries.findById(id).ifPresent(d -> d.sent(clock.instant())));
        } catch (RuntimeException e) {
            log.warn("Email da campanha {} não enviado: {}", p.campaignId(), e.getMessage());
            tx.executeWithoutResult(s -> deliveries.findById(id).ifPresent(d -> d.failed(e.getMessage())));
        }
    }

    private Prepared prepare(UUID id) {
        Delivery d = deliveries.findById(id).orElse(null);
        if (d == null || d.getStatus() != DeliveryStatus.PENDING) return null;
        Campaign c = campaigns.findById(d.getCampaignId()).orElse(null);
        var r = directory.active(d.getUserId()).orElse(null);
        boolean individual = c != null && c.getAudience() == Audience.USER;
        if (c == null || r == null || (!individual && !r.emailOptIn())) {
            d.skipped();
            return null;
        }
        // As mensagens individuais não levam o rodapé das preferências: chegam mesmo com os emails desligados.
        var email = MessageRenderer.render(c.content(), r, props.baseUrl(), clock.instant(), !individual);
        return new Prepared(id, c.getId(), r.email(), email);
    }
}
