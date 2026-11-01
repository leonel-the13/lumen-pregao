package ao.lumen.pregao.common.text;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Filtro simples de linguagem ofensiva para pseudónimos e nomes de equipa. */
@Component
public class OffensiveFilter {
    private final List<String> terms;

    public OffensiveFilter() {
        try (var in = new ClassPathResource("text/offensive-terms.txt").getInputStream();
             var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            terms = reader.lines().map(String::strip).filter(l -> !l.isEmpty() && !l.startsWith("#"))
                    .map(OffensiveFilter::normalize).toList();
        } catch (Exception e) {
            throw new IllegalStateException("Lista de termos ofensivos em falta", e);
        }
    }

    public boolean isOffensive(String text) {
        if (text == null) return false;
        String n = normalize(text).replace("_", "").replace("0", "o").replace("1", "i").replace("3", "e")
                .replace("4", "a").replace("5", "s");
        return terms.stream().anyMatch(n::contains);
    }

    static String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
    }
}
