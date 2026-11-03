package ao.lumen.pregao.messaging.app;

import ao.lumen.pregao.common.error.DomainException;
import ao.lumen.pregao.common.mail.EmailTemplate;
import ao.lumen.pregao.identity.app.MailingDirectory.Recipient;
import ao.lumen.pregao.messaging.domain.MessageContent;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Substitui as variáveis e monta o email com a identidade do Lúmen Lab. O texto é sempre escapado
 * (sem HTML do autor); cada linha do corpo é um parágrafo; o botão só aponta para páginas da plataforma.
 */
public final class MessageRenderer {
    private MessageRenderer() {}

    public static final Map<String, String> VARIABLES = Map.of(
            "nome", "Primeiro nome",
            "nome_completo", "Nome completo",
            "curso", "Curso",
            "ano", "Ano (ex.: 2.º ano)",
            "dias_sem_entrar", "Dias desde a última visita");

    private static final Pattern VAR = Pattern.compile("\\{([a-z_]+)}");
    private static final Pattern PATH = Pattern.compile("^/(?!/)[A-Za-z0-9/_\\-?=&.%#]*$");
    public static final String DEFAULT_TITLE = "Olá, {nome}!";

    /** Erro claro para variáveis desconhecidas (ex.: {nme}) e botões incompletos ou para fora da plataforma. */
    public static MessageContent validate(MessageContent c) {
        Set<String> unknown = new TreeSet<>();
        for (String s : Arrays.asList(c.subject(), c.title(), c.body(), c.ctaLabel())) {
            if (s == null) continue;
            Matcher m = VAR.matcher(s);
            while (m.find()) if (!VARIABLES.containsKey(m.group(1))) unknown.add("{" + m.group(1) + "}");
        }
        if (!unknown.isEmpty()) {
            throw DomainException.badRequest("UNKNOWN_VARIABLE", "Variáveis desconhecidas: " + String.join(", ", unknown)
                    + ". Usa " + String.join(", ", new TreeSet<>(VARIABLES.keySet()).stream().map(v -> "{" + v + "}").toList()) + ".");
        }
        String label = blankToNull(c.ctaLabel());
        String path = blankToNull(c.ctaPath());
        if ((label == null) != (path == null)) {
            throw DomainException.badRequest("CTA_INCOMPLETE", "O botão precisa de texto e de página. Preenche os dois ou deixa os dois vazios.");
        }
        if (path != null && !PATH.matcher(path).matches()) {
            throw DomainException.badRequest("CTA_PATH_INVALID", "O botão só pode abrir páginas do Lúmen Lab: escreve um caminho como /trilhas.");
        }
        String title = blankToNull(c.title());
        return new MessageContent(c.subject().strip(), title == null ? DEFAULT_TITLE : title.strip(), c.body().strip(), label, path);
    }

    public static EmailTemplate.Email render(MessageContent c, Recipient r, String baseUrl, Instant now, boolean preferencesFooter) {
        Map<String, String> v = values(r, now);
        String subject = fill(c.subject(), v);
        List<String> paragraphs = c.body().lines().map(String::strip).filter(s -> !s.isEmpty()).map(s -> fill(s, v)).toList();
        String preheader = paragraphs.isEmpty() ? subject : paragraphs.get(0);
        if (preheader.length() > 120) preheader = preheader.substring(0, 119) + "…";
        String url = c.ctaPath() == null ? null : baseUrl + c.ctaPath();
        return EmailTemplate.build(subject, preheader, fill(c.title(), v), paragraphs, c.ctaLabel() == null ? null : fill(c.ctaLabel(), v),
                url, null, preferencesFooter ? baseUrl + "/definicoes" : null);
    }

    static Map<String, String> values(Recipient r, Instant now) {
        long days = r.lastActivity() == null ? 0 : Math.max(0, Duration.between(r.lastActivity(), now).toDays());
        return Map.of(
                "nome", EmailTemplate.firstName(r.name()),
                "nome_completo", r.name() == null ? "" : r.name().strip(),
                "curso", r.courseName() == null ? "o teu curso" : r.courseName(),
                "ano", r.year() == null ? "o teu ano" : r.year() + ".º ano",
                "dias_sem_entrar", Long.toString(days));
    }

    static String fill(String s, Map<String, String> v) {
        if (s == null) return null;
        Matcher m = VAR.matcher(s);
        StringBuilder out = new StringBuilder();
        while (m.find()) m.appendReplacement(out, Matcher.quoteReplacement(v.getOrDefault(m.group(1), m.group())));
        m.appendTail(out);
        return out.toString();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
