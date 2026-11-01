package ao.lumen.pregao.common.web;

import java.util.List;
import java.util.function.Function;

/** Página por cursor (`?cursor=&limit=`, limit ≤ 100). */
public record CursorPage<T>(List<T> items, String nextCursor) {

    public static final int MAX_LIMIT = 100;

    public static int clamp(Integer limit) {
        if (limit == null || limit <= 0) return 20;
        return Math.min(limit, MAX_LIMIT);
    }

    /** `rows` deve ter sido pedido com limit + 1 para saber se há mais. */
    public static <E, T> CursorPage<T> of(List<E> rows, int limit, Function<E, T> map, Function<E, String> cursorOf) {
        boolean more = rows.size() > limit;
        List<E> page = more ? rows.subList(0, limit) : rows;
        String next = more ? cursorOf.apply(page.get(page.size() - 1)) : null;
        return new CursorPage<>(page.stream().map(map).toList(), next);
    }
}
