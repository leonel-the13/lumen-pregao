import Link from "next/link";
import { Difficulty } from "@/components/ui/Progress";
import { Icon } from "@/components/ui/Icon";
import type { RoomCard } from "@/lib/types";

const STATE_LABEL = { LOCKED: "Bloqueada", AVAILABLE: "Disponível", IN_PROGRESS: "Em curso", COMPLETED: "Concluída" } as const;

/** Linha de sala (design: .task) com estado em ícone + texto. */
export function RoomLink({ r, showTrack = true }: { r: RoomCard; showTrack?: boolean }) {
  const cls = ["task", r.state === "COMPLETED" && "done", r.state === "IN_PROGRESS" && "cur", r.state === "LOCKED" && "lock"].filter(Boolean).join(" ");
  const meta = [showTrack && r.trackTitle, `Módulo ${r.modulePosition}`, `${r.estMinutes} min`].filter(Boolean).join(" · ");
  const body = (
    <>
      <span className="st" aria-hidden="true">
        {r.state === "COMPLETED" ? <Icon name="check" /> : r.state === "LOCKED" ? <Icon name="lock" /> : r.position}
      </span>
      <div style={{ flex: 1, minWidth: 0 }}>
        <b style={{ fontWeight: 600 }}>{r.title}</b>
        <div className="small muted">{r.state === "LOCKED" && r.lockedReason ? r.lockedReason : meta}</div>
      </div>
      <span className="sr-only">{STATE_LABEL[r.state]}</span>
      <Difficulty level={r.difficulty} />
      <span className="mono small muted">{r.state === "COMPLETED" || r.obtained > 0 ? `${r.obtained} / ${r.possible}` : r.possible} pts</span>
    </>
  );
  return r.state === "LOCKED" ? <div className={cls} aria-disabled="true">{body}</div> : <Link className={cls} href={`/salas/${r.id}`}>{body}</Link>;
}
