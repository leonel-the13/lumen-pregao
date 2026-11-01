import Link from "next/link";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { Bar, Difficulty } from "@/components/ui/Progress";
import { DIFFICULTY_LABEL } from "@/lib/format";
import type { TrackCard } from "@/lib/types";

export function TrackCardView({ t }: { t: TrackCard }) {
  const soon = t.status === "COMING_SOON";
  return (
    <Card className="stack" style={{ opacity: soon ? 0.7 : 1 }}>
      <div className="row" style={{ gap: 8 }}>
        <span className="label">{t.modulesCount} módulos · {t.roomsCount} salas{t.estHours ? ` · ≈${t.estHours} h` : ""}</span>
        <div className="spacer" />
        <Difficulty level={t.difficulty} />
      </div>
      <h3 className="h2"><Link href={`/trilhas/${t.slug}`} style={{ textDecoration: "none" }}>{t.title}</Link></h3>
      <p className="small muted">{t.summary}</p>
      {soon ? (
        <div className="row"><Badge>Fase posterior</Badge><span className="small muted">{DIFFICULTY_LABEL[t.difficulty]}</span></div>
      ) : (
        <>
          <Bar value={t.percent} label={`Progresso em ${t.title}`} thin />
          <div className="row">
            <span className="small muted mono">{t.percent} % concluída</span>
            <div className="spacer" />
            <Link className="btn sm out" href={t.nextRoomId ? `/salas/${t.nextRoomId}` : `/trilhas/${t.slug}`}>
              {t.percent > 0 ? "Continuar" : "Começar"}
            </Link>
          </div>
        </>
      )}
    </Card>
  );
}
