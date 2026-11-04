"use client";
import { Avatar } from "@/components/ui/Avatar";
import { Badge } from "@/components/ui/Badge";
import { Icon } from "@/components/ui/Icon";
import { formatPoints } from "@/lib/format";
import { useT } from "@/lib/i18n/client";
import type { ScoreboardView } from "@/lib/types";

/** Ranking do contest. As linhas deslizam para a nova posição; nunca piscam. */
export function LiveScoreboard({ board, connected, compact }: { board: ScoreboardView | null; connected?: boolean; compact?: boolean }) {
  const t = useT();
  return (
    <div className="card stack" style={{ padding: 0 }}>
      <div className="row" style={{ padding: "16px 16px 4px" }}>
        <h2 className="h2">{board?.state === "RESULTS_PUBLISHED" || board?.state === "CLOSED" ? t("compete.board.final") : t("compete.board.live")}</h2>
        <div className="spacer" />
        {board?.frozen ? <Badge tone="info" icon="snow">{t("compete.board.frozen")}</Badge> : connected !== undefined && (connected ? <Badge tone="acc">{t("compete.board.onAir")}</Badge> : <Badge icon="wifiOff">{t("compete.board.connecting")}</Badge>)}
      </div>
      {!board ? <p className="small muted" style={{ padding: 16 }}>{t("compete.board.loading")}</p> : board.rows.length === 0 ? (
        <p className="small muted" style={{ padding: 16 }}>{t("compete.board.empty")}</p>
      ) : (
        <ol className="plain rank-list" aria-label={t("compete.board.label")}>
          {board.rows.slice(0, compact ? 8 : 100).map((r) => (
            <li key={r.name + r.position} className={["rank-row", r.position <= 3 && "top", r.mine && "me"].filter(Boolean).join(" ")}>
              <span className="pos">{r.position}</span>
              <span className="row" style={{ gap: 10, minWidth: 0 }}><Avatar initials={r.initials} /><span style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{r.name}{r.mine && t("compete.board.you")}</span></span>
              <span className="small muted mono" title={t("compete.board.penalty")}>{t("compete.board.solvedPenalty", { n: r.solved, min: r.penaltyMinutes })}</span>
              <b className="mono">{formatPoints(r.points)}</b>
            </li>
          ))}
        </ol>
      )}
      {board?.frozen && (
        <div className="alert info" style={{ margin: 12 }}><Icon name="snow" /><div><b>{t("compete.board.frozenTitle")}</b>{t("compete.board.frozenBody")}</div></div>
      )}
    </div>
  );
}
