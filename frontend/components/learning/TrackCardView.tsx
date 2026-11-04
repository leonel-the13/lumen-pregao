"use client";
import Link from "next/link";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { Bar, Difficulty } from "@/components/ui/Progress";
import { useT } from "@/lib/i18n/client";
import type { TrackCard } from "@/lib/types";

export function TrackCardView({ t }: { t: TrackCard }) {
  const tr = useT();
  const soon = t.status === "COMING_SOON";
  return (
    <Card className="stack" style={{ opacity: soon ? 0.7 : 1 }}>
      <div className="row" style={{ gap: 8 }}>
        <span className="label">{tr("learn.tracks.card", { modules: t.modulesCount, rooms: t.roomsCount })}{t.estHours ? ` · ≈${t.estHours} h` : ""}</span>
        <div className="spacer" />
        <Difficulty level={t.difficulty} />
      </div>
      <h3 className="h2"><Link href={`/trilhas/${t.slug}`} style={{ textDecoration: "none" }}>{t.title}</Link></h3>
      <p className="small muted">{t.summary}</p>
      {soon ? (
        <div className="row"><Badge>{tr("learn.tracks.later")}</Badge><span className="small muted">{tr(`learn.difficulty.${t.difficulty as 1 | 2 | 3 | 4}`)}</span></div>
      ) : (
        <>
          <Bar value={t.percent} label={tr("learn.tracks.progressIn", { title: t.title })} thin />
          <div className="row">
            <span className="small muted mono">{tr("learn.tracks.percentDone", { n: t.percent })}</span>
            <div className="spacer" />
            <Link className="btn sm out" href={t.nextRoomId ? `/salas/${t.nextRoomId}` : `/trilhas/${t.slug}`}>
              {t.percent > 0 ? tr("learn.tracks.continue") : tr("learn.tracks.start")}
            </Link>
          </div>
        </>
      )}
    </Card>
  );
}
