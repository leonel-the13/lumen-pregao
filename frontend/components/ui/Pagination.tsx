import Link from "next/link";
import { Icon } from "./Icon";

/** Paginação por cursor: «Seguinte» leva o cursor do servidor. */
export function CursorPager({ hrefFor, nextCursor, hasPrev, prevHref }: { hrefFor: (cursor: string) => string; nextCursor?: string; hasPrev?: boolean; prevHref?: string }) {
  return (
    <nav className="row" aria-label="Paginação" style={{ justifyContent: "flex-end", gap: 8 }}>
      {hasPrev && prevHref && <Link className="btn sm out" href={prevHref}><Icon name="arrowLeft" />Anterior</Link>}
      {nextCursor && <Link className="btn sm out" href={hrefFor(nextCursor)}>Seguinte<Icon name="arrowRight" /></Link>}
    </nav>
  );
}
