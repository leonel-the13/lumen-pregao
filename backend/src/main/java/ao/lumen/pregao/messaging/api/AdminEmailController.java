package ao.lumen.pregao.messaging.api;

import ao.lumen.pregao.common.security.SessionUser;
import ao.lumen.pregao.common.web.CursorPage;
import ao.lumen.pregao.messaging.api.AdminEmailDtos.AudienceCount;
import ao.lumen.pregao.messaging.api.AdminEmailDtos.AudienceIn;
import ao.lumen.pregao.messaging.api.AdminEmailDtos.CampaignIn;
import ao.lumen.pregao.messaging.api.AdminEmailDtos.CampaignRow;
import ao.lumen.pregao.messaging.api.AdminEmailDtos.PreviewIn;
import ao.lumen.pregao.messaging.api.AdminEmailDtos.PreviewView;
import ao.lumen.pregao.messaging.api.AdminEmailDtos.RetryResult;
import ao.lumen.pregao.messaging.api.AdminEmailDtos.TemplateIn;
import ao.lumen.pregao.messaging.api.AdminEmailDtos.TemplateView;
import ao.lumen.pregao.messaging.api.AdminEmailDtos.VariableView;
import ao.lumen.pregao.messaging.app.CampaignService;
import ao.lumen.pregao.messaging.app.MessageRenderer;
import ao.lumen.pregao.messaging.app.MessageTemplateService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Emails da administração: modelos, pré-visualização, teste, envio a segmentos e histórico. */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEmailController {
    private final CampaignService campaigns;
    private final MessageTemplateService templates;

    public AdminEmailController(CampaignService campaigns, MessageTemplateService templates) {
        this.campaigns = campaigns;
        this.templates = templates;
    }

    @GetMapping("/email-templates")
    public List<TemplateView> templates() {
        return templates.list();
    }

    @PostMapping("/email-templates")
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateView createTemplate(@AuthenticationPrincipal SessionUser me, @Valid @RequestBody TemplateIn in) {
        return templates.create(me.id(), in);
    }

    @PutMapping("/email-templates/{id}")
    public TemplateView updateTemplate(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id, @Valid @RequestBody TemplateIn in) {
        return templates.update(me.id(), id, in);
    }

    @DeleteMapping("/email-templates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        templates.delete(me.id(), id);
    }

    @GetMapping("/emails/variables")
    public List<VariableView> variables() {
        return new TreeMap<>(MessageRenderer.VARIABLES).entrySet().stream().map(e -> new VariableView(e.getKey(), e.getValue())).toList();
    }

    @PostMapping("/emails/audience")
    public AudienceCount audience(@Valid @RequestBody AudienceIn in) {
        return campaigns.count(in);
    }

    @PostMapping("/emails/preview")
    public PreviewView preview(@AuthenticationPrincipal SessionUser me, @Valid @RequestBody PreviewIn in) {
        return campaigns.preview(me.id(), in.content());
    }

    @PostMapping("/emails/test")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void test(@AuthenticationPrincipal SessionUser me, @Valid @RequestBody PreviewIn in) {
        campaigns.sendTest(me.id(), in.content());
    }

    @GetMapping("/emails/campaigns")
    public CursorPage<CampaignRow> list(@RequestParam(required = false) String cursor, @RequestParam(required = false) Integer limit) {
        return campaigns.page(cursor, limit);
    }

    /** 202: a campanha fica na fila e os emails saem ao ritmo do fornecedor. */
    @PostMapping("/emails/campaigns")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CampaignRow send(@AuthenticationPrincipal SessionUser me, @RequestHeader(value = "Idempotency-Key", required = false) String key,
                            @Valid @RequestBody CampaignIn in) {
        return campaigns.create(me.id(), key, in);
    }

    @PostMapping("/emails/campaigns/{id}/retry")
    public RetryResult retry(@AuthenticationPrincipal SessionUser me, @PathVariable UUID id) {
        return new RetryResult(campaigns.retryFailed(me.id(), id));
    }
}
