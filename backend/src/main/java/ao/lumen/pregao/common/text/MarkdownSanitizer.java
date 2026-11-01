package ao.lumen.pregao.common.text;

import java.util.regex.Pattern;

/**
 * Sanitização de markdown à entrada: remove HTML bruto e ligações com esquemas perigosos.
 * O frontend renderiza o markdown sem HTML (SEC-08).
 */
public final class MarkdownSanitizer {
    private static final Pattern TAG = Pattern.compile("<[^>]*>");
    private static final Pattern DANGEROUS_LINK = Pattern.compile("\\]\\(\\s*(javascript|data|vbscript):[^)]*\\)",
            Pattern.CASE_INSENSITIVE);

    private MarkdownSanitizer() {}

    public static String clean(String md) {
        if (md == null) return null;
        String out = TAG.matcher(md).replaceAll("");
        out = DANGEROUS_LINK.matcher(out).replaceAll("](#)");
        return out.strip();
    }
}
