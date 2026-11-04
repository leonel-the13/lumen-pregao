import Link from "next/link";
import { notFound } from "next/navigation";
import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { RoomLink } from "@/components/learning/RoomLink";
import { Badge, Tbc } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { Bar, Ring } from "@/components/ui/Progress";
import { serverFetchOrLogin } from "@/lib/api-server";
import { ApiError } from "@/lib/errors";
import type { TrackDetail } from "@/lib/types";
import { getI18n } from "@/lib/i18n/server";

const MODULE_TONE = { COMPLETED: "pos", IN_PROGRESS: "acc", AVAILABLE: "neutral", LOCKED: "neutral" } as const;

export default async function Trilha({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const { t: tr } = await getI18n();
  let t: TrackDetail;
  try {
    t = await serverFetchOrLogin<TrackDetail>(`/tracks/${encodeURIComponent(slug)}`, `/trilhas/${slug}`);
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  }
  const current = t.modules.find((m) => m.state === "IN_PROGRESS") ?? t.modules.find((m) => m.state === "AVAILABLE");
  return (
    <>
      <SetCrumbs items={[{ label: tr("learn.tracks.title"), href: "/trilhas" }, { label: t.title }]} />
      <Card lit className="row" style={{ gap: 24, alignItems: "flex-start" }}>
        <Ring value={t.percent} label={tr("learn.tracks.completion")} size={84} />
        <div className="stack" style={{ flex: 1, minWidth: 260 }}>
          <div className="label">{tr("learn.tracks.detail")}</div>
          <h1 className="h1">{t.title}</h1>
          <p className="muted">{t.description} {t.completionRule}</p>
          <div className="row" style={{ gap: 8 }}>
            {t.competencies.map((c) => <Badge key={c}>{c}</Badge>)}
            {t.references.map((r) => (
              <span key={r.title} className="small muted">{tr("learn.tracks.source")} {r.identifier}{!r.confirmed && <Tbc />}</span>
            ))}
          </div>
        </div>
        {t.nextRoomId && current && <Link className="btn pri" href={`/salas/${t.nextRoomId}`}>{tr("learn.tracks.continueModule", { n: current.position })}</Link>}
      </Card>
      <ol className="plain stack" style={{ gap: 12 }}>
        {t.modules.map((m) => {
          const tone = MODULE_TONE[m.state];
          const label = tr(`learn.tracks.moduleState.${m.state}`);
          return (
            <li key={m.id}>
              <Card className="stack" style={{ opacity: m.state === "LOCKED" ? 0.75 : 1 }}>
                <div className="row" style={{ gap: 16 }}>
                  <span className="mono h2" aria-hidden="true">{String(m.position).padStart(2, "0")}</span>
                  <div style={{ flex: 1, minWidth: 200 }}>
                    <h2 className="h3">{m.title}</h2>
                    <div className="small muted">{tr(m.roomsCount === 1 ? "learn.tracks.room" : "learn.tracks.rooms", { n: m.roomsCount })}{m.summary ? ` · ${m.summary}` : ""}</div>
                  </div>
                  <div style={{ width: 160 }}><Bar value={m.percent} label={tr("learn.tracks.moduleProgress", { n: m.position })} thin /></div>
                  <span className="mono small">{m.percent}%</span>
                  <Badge tone={tone} icon={m.state === "LOCKED" ? "lock" : undefined}>{label}</Badge>
                </div>
                {m.rooms.length > 0 && <div className="stack" style={{ gap: 4 }}>{m.rooms.map((r) => <RoomLink key={r.id} r={r} showTrack={false} />)}</div>}
                {m.rooms.length === 0 && <p className="small muted">{tr("learn.tracks.roomsSoon")}</p>}
              </Card>
            </li>
          );
        })}
      </ol>
    </>
  );
}
