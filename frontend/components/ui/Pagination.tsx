"use client";
import Link from "next/link";
import { useT } from "@/lib/i18n/client";
import { Icon } from "./Icon";

/** Paginação por cursor: «Seguinte» leva o cursor do servidor. */
export function CursorPager({ hrefFor, nextCursor, hasPrev, prevHref }: { hrefFor: (cursor: string) => string; nextCursor?: string; hasPrev?: boolean; prevHref?: string }) {
  const t = useT();
  return (
    <nav className="row" aria-label={t("compete.pager.label")} style={{ justifyContent: "flex-end", gap: 8 }}>
      {hasPrev && prevHref && <Link className="btn sm out" href={prevHref}><Icon name="arrowLeft" />{t("common.previous")}</Link>}
      {nextCursor && <Link className="btn sm out" href={hrefFor(nextCursor)}>{t("common.next")}<Icon name="arrowRight" /></Link>}
    </nav>
  );
}
