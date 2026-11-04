import Link from "next/link";
import { notFound } from "next/navigation";
import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { RoomWorkspace } from "@/components/task/RoomWorkspace";
import { RoomRating } from "@/components/learning/RoomRating";
import { Badge, Tbc } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import { Markdown } from "@/components/ui/Markdown";
import { serverFetchOrLogin } from "@/lib/api-server";
import { ApiError } from "@/lib/errors";
import type { RoomDetail } from "@/lib/types";
import { getI18n } from "@/lib/i18n/server";

export default async function Sala({ params, searchParams }: { params: Promise<{ id: string }>; searchParams: Promise<{ tarefa?: string }> }) {
  const { id } = await params;
  const { tarefa } = await searchParams;
  const { t: tr } = await getI18n();
  let room: RoomDetail;
  try {
    room = await serverFetchOrLogin<RoomDetail>(`/rooms/${id}`, `/salas/${id}`);
  } catch (e) {
    if (e instanceof ApiError && (e.status === 404 || e.status === 400)) notFound();
    throw e;
  }
  const b = room.breadcrumb;
  const done = room.tasks.filter((t) => t.state === "PASSED" || t.state === "FAILED").length;
  const requested = tarefa ? room.tasks.find((t) => t.position === Number(tarefa)) : undefined;
  const current = requested ?? room.tasks.find((t) => t.state === "IN_PROGRESS" || t.state === "AVAILABLE") ?? room.tasks[0];

  return (
    <>
      <SetCrumbs items={[{ label: tr("learn.tracks.title"), href: "/trilhas" }, { label: b.trackTitle, href: `/trilhas/${b.trackSlug}` },
        { label: tr("learn.rooms.module", { n: b.modulePosition }) }, { label: room.title }]} />
      <div className="row" style={{ alignItems: "flex-end" }}>
        <div className="stack" style={{ gap: 6 }}>
          <div className="label">{tr("learn.rooms.header", { track: b.trackTitle, module: b.modulePosition, room: b.roomPosition, min: room.estMinutes })}</div>
          <h1 className="h1">{room.title}</h1>
        </div>
        <div className="spacer" />
        <span className="badge mono">{room.obtained} / {room.possible} pts</span>
      </div>

      {room.state === "LOCKED" ? (
        <Card className="state-box">
          <div className="row" style={{ gap: 10 }}><Icon name="lock" /><h2 className="h3">{tr("learn.rooms.locked")}</h2></div>
          <p className="small muted">{room.lockedReason ?? tr("learn.rooms.needsPrevious")}</p>
          <Link className="btn out" href={`/trilhas/${b.trackSlug}`}>{tr("learn.rooms.seeTrack")}</Link>
        </Card>
      ) : (
        <div className="room-grid">
          <aside className="stack" style={{ gap: 16 }} aria-label={tr("learn.rooms.tasksLabel")}>
            <Card className="stack" style={{ gap: 6, padding: 14 }}>
              <div className="row" style={{ padding: "0 4px 6px" }}><b>{tr("learn.rooms.tasks")}</b><div className="spacer" /><span className="mono small muted">{done}/{room.tasks.length}</span></div>
              <ol className="plain stack" style={{ gap: 2 }}>
                {room.tasks.map((t) => {
                  const on = t.id === current?.id;
                  const cls = ["task", (t.state === "PASSED") && "done", on && "cur", t.state === "UNAVAILABLE" && "lock"].filter(Boolean).join(" ");
                  return (
                    <li key={t.id}>
                      <Link className={cls} href={`/salas/${room.id}?tarefa=${t.position}`} aria-current={on ? "step" : undefined}>
                        <span className="st" aria-hidden="true">{t.state === "PASSED" ? <Icon name="check" /> : t.state === "FAILED" ? <Icon name="x" /> : t.position}</span>
                        <span style={{ flex: 1 }}>{t.title}<span className="sr-only"> · {tr(`learn.rooms.taskState.${t.state}`)}</span></span>
                        <span className="mono small muted">{t.state === "PASSED" ? t.score : t.points}</span>
                      </Link>
                    </li>
                  );
                })}
              </ol>
            </Card>
            {room.objectives.length > 0 && (
              <Card className="stack" style={{ padding: 16 }}>
                <b>{tr("learn.rooms.objectives")}</b>
                <ul className="plain stack small">{room.objectives.map((o) => <li key={o} className="row" style={{ gap: 8 }}><Icon name="check" />{o}</li>)}</ul>
              </Card>
            )}
            {room.references.length > 0 && (
              <Card className="stack" style={{ padding: 16 }}>
                <b>{tr("learn.rooms.references")}</b>
                <ul className="plain stack small">
                  {room.references.map((r) => <li key={r.title + r.identifier}>{r.title} · {r.identifier}{!r.confirmed && <Tbc>{tr("learn.rooms.versionTbc")}</Tbc>}</li>)}
                </ul>
              </Card>
            )}
            {room.context && <Card className="small" style={{ padding: 16 }}><Markdown text={room.context} /></Card>}
            <RoomRating roomId={room.id} />
          </aside>
          <section aria-label={tr("learn.rooms.workspace")}>
            {current ? (
              current.state === "UNAVAILABLE"
                ? <Card className="state-box"><Badge>{tr("learn.rooms.inReview")}</Badge><p className="small muted">{tr("learn.rooms.inReviewBody")}</p></Card>
                : <RoomWorkspace key={current.id} task={current} context={room.context} references={room.references} />
            ) : <Card className="state-box"><p className="muted">{tr("learn.rooms.noTasks")}</p></Card>}
          </section>
        </div>
      )}
    </>
  );
}
