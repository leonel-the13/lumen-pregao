"use client";
import { Avatar } from "@/components/ui/Avatar";
import { Badge } from "@/components/ui/Badge";
import { Icon } from "@/components/ui/Icon";
import { formatPoints } from "@/lib/format";
import type { ScoreboardView } from "@/lib/types";

/** Ranking do contest. As linhas deslizam para a nova posição; nunca piscam. */
export function LiveScoreboard({ board, connected, compact }: { board: ScoreboardView | null; connected?: boolean; compact?: boolean }) {
  return (
    <div className="card stack" style={{ padding: 0 }}>
      <div className="row" style={{ padding: "16px 16px 4px" }}>
        <h2 className="h2">Ranking {board?.state === "RESULTS_PUBLISHED" || board?.state === "CLOSED" ? "final" : "ao vivo"}</h2>
        <div className="spacer" />
        {board?.frozen ? <Badge tone="info" icon="snow">Congelado</Badge> : connected !== undefined && (connected ? <Badge tone="acc">Em directo</Badge> : <Badge icon="wifiOff">A ligar…</Badge>)}
      </div>
      {!board ? <p className="small muted" style={{ padding: 16 }}>A carregar o ranking…</p> : board.rows.length === 0 ? (
        <p className="small muted" style={{ padding: 16 }}>Ainda não há participantes inscritos.</p>
      ) : (
        <ol className="plain rank-list" aria-label="Classificação">
          {board.rows.slice(0, compact ? 8 : 100).map((r) => (
            <li key={r.name + r.position} className={["rank-row", r.position <= 3 && "top", r.mine && "me"].filter(Boolean).join(" ")}>
              <span className="pos">{r.position}</span>
              <span className="row" style={{ gap: 10, minWidth: 0 }}><Avatar initials={r.initials} /><span style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{r.name}{r.mine && " (tu)"}</span></span>
              <span className="small muted mono" title="Penalização acumulada">{r.solved} res. · {r.penaltyMinutes} min</span>
              <b className="mono">{formatPoints(r.points)}</b>
            </li>
          ))}
        </ol>
      )}
      {board?.frozen && (
        <div className="alert info" style={{ margin: 12 }}><Icon name="snow" /><div><b>Ranking congelado</b>Continuas a ver os teus resultados; as outras posições ficam ocultas até ao anúncio.</div></div>
      )}
    </div>
  );
}
