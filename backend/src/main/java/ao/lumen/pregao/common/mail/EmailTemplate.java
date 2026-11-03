package ao.lumen.pregao.common.mail;

import java.util.List;
import org.springframework.web.util.HtmlUtils;

/**
 * Email transaccional com a identidade do Lúmen Lab (tabelas e estilos em linha, compatível com Gmail e Outlook).
 * Todo o texto é escapado.
 */
public final class EmailTemplate {
    private EmailTemplate() {}

    public record Email(String subject, String text, String html) {}

    public static Email build(String subject, String preheader, String greeting, List<String> paragraphs,
                              String ctaLabel, String ctaUrl, String note) {
        return build(subject, preheader, greeting, paragraphs, ctaLabel, ctaUrl, note, null);
    }

    /** Com {@code preferencesUrl}, o rodapé diz como deixar de receber estes emails (lembretes, notificações, campanhas). */
    public static Email build(String subject, String preheader, String greeting, List<String> paragraphs,
                              String ctaLabel, String ctaUrl, String note, String preferencesUrl) {
        StringBuilder text = new StringBuilder(greeting).append("\n\n");
        paragraphs.forEach(p -> text.append(p).append("\n\n"));
        if (ctaUrl != null) text.append(ctaLabel).append(": ").append(ctaUrl).append("\n\n");
        if (note != null) text.append(note).append("\n\n");
        if (preferencesUrl != null) text.append("Não queres receber estes emails? Desliga-os nas definições: ").append(preferencesUrl).append("\n\n");
        text.append("Iniciativa da Associação de Estudantes · Não é um canal oficial do ISPTEC\n");
        String prefsHtml = preferencesUrl == null ? "" : "<br>Não queres receber estes emails? <a href=\"" + e(preferencesUrl)
                + "\" style=\"color:#7A5A00\">Desliga-os nas definições</a>.";

        StringBuilder body = new StringBuilder();
        for (String p : paragraphs) {
            body.append("<p style=\"margin:0 0 14px;font-size:15px;line-height:1.6;color:#1F1F1F\">").append(e(p)).append("</p>");
        }
        String button = ctaUrl == null ? "" : """
                <table role="presentation" cellpadding="0" cellspacing="0" style="margin:22px 0 18px"><tr>
                <td style="background:#FFC20E;border-radius:10px">
                <a href="%s" style="display:inline-block;padding:13px 22px;font:700 15px Arial,Helvetica,sans-serif;color:#0A0A0A;text-decoration:none">%s →</a>
                </td></tr></table>
                <p style="margin:0 0 6px;font-size:12px;color:#6E6E6E">Se o botão não funcionar, copia este endereço para o browser:</p>
                <p style="margin:0 0 14px;font-size:12px;word-break:break-all"><a href="%s" style="color:#7A5A00">%s</a></p>
                """.formatted(e(ctaUrl), e(ctaLabel), e(ctaUrl), e(ctaUrl));
        String noteHtml = note == null ? "" : "<p style=\"margin:16px 0 0;padding:12px 14px;border-radius:10px;background:#F6F6F4;"
                + "font-size:13px;line-height:1.5;color:#5E5E5E\">" + e(note) + "</p>";
        String html = """
                <!DOCTYPE html><html lang="pt"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width">
                <title>%s</title></head>
                <body style="margin:0;padding:0;background:#F6F6F4;font-family:Arial,Helvetica,sans-serif">
                <span style="display:none!important;opacity:0;color:transparent;height:0;width:0;overflow:hidden">%s</span>
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#F6F6F4;padding:28px 12px"><tr><td align="center">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:560px;background:#FFFFFF;border-radius:14px;overflow:hidden;border:1px solid #E2E2DE">
                <tr><td style="background:#0A0A0A;padding:22px 28px">
                  <span style="display:inline-block;width:10px;height:10px;border-radius:50%%;background:#FFC20E;margin-right:8px;vertical-align:middle"></span>
                  <span style="font:900 17px Arial,Helvetica,sans-serif;letter-spacing:.06em;color:#FFFFFF;vertical-align:middle">LÚMEN</span>
                  <span style="font:italic 18px Georgia,serif;color:#FFC20E;vertical-align:middle">Lab</span>
                </td></tr>
                <tr><td style="padding:30px 28px 26px">
                  <h1 style="margin:0 0 16px;font:800 22px Arial,Helvetica,sans-serif;color:#0A0A0A">%s</h1>
                  %s%s%s
                </td></tr>
                <tr><td style="padding:18px 28px;border-top:1px solid #E2E2DE;font-size:12px;line-height:1.5;color:#6E6E6E">
                  Iniciativa da Associação de Estudantes · Não é um canal oficial do ISPTEC.<br>
                  Recebeste este email porque há uma conta Lúmen Lab associada a este endereço.%s
                </td></tr></table></td></tr></table></body></html>
                """.formatted(e(subject), e(preheader), e(greeting), body, button, noteHtml, prefsHtml);
        return new Email(subject, text.toString(), html);
    }

    private static String e(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s, "UTF-8");
    }

    /** «Kiala Manuel Afonso» → «Kiala». */
    public static String firstName(String full) {
        if (full == null || full.isBlank()) return "";
        return full.strip().split("\\s+")[0];
    }
}
