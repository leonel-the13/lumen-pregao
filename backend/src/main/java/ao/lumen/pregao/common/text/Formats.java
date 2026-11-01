package ao.lumen.pregao.common.text;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Formatação de valores no servidor (enunciados): espaço fino nos milhares, vírgula decimal, Kz depois. */
public final class Formats {
    public static final char THIN = ' ';

    private Formats() {}

    public static String money(BigDecimal v) {
        return number(v.setScale(2, RoundingMode.HALF_UP), 2);
    }

    public static String kz(BigDecimal v) {
        return money(v) + " Kz";
    }

    public static String percent(BigDecimal v) {
        return plain(v) + " %";
    }

    public static String plain(BigDecimal v) {
        BigDecimal s = v.stripTrailingZeros();
        int scale = Math.max(0, s.scale());
        return number(s.setScale(scale, RoundingMode.HALF_UP), scale);
    }

    private static final java.time.format.DateTimeFormatter TIME =
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss").withZone(java.time.ZoneId.of("Africa/Luanda"));

    /** Hora apresentada em Africa/Luanda. */
    public static String time(java.time.Instant at) {
        return TIME.format(at);
    }

    private static String number(BigDecimal v, int scale) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.ROOT);
        sym.setGroupingSeparator(THIN);
        sym.setDecimalSeparator(',');
        sym.setMinusSign('−');
        DecimalFormat f = new DecimalFormat(scale > 0 ? "#,##0." + "0".repeat(scale) : "#,##0", sym);
        f.setRoundingMode(RoundingMode.HALF_UP);
        return f.format(v);
    }
}
