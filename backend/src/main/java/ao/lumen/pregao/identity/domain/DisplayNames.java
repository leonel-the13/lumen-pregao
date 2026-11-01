package ao.lumen.pregao.identity.domain;

/** Nome curto apresentado em rankings e equipas: «Kiala Afonso» → «Kiala A.». */
public final class DisplayNames {
    private DisplayNames() {}

    public static String shortName(String full) {
        if (full == null || full.isBlank()) return "—";
        String[] parts = full.strip().split("\\s+");
        if (parts.length == 1) return parts[0];
        return parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }

    public static String initials(String text) {
        if (text == null || text.isBlank()) return "—";
        String[] parts = text.strip().replace('_', ' ').split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
