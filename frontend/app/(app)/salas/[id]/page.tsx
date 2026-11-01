import Link from "next/link";
import { notFound } from "next/navigation";
import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { RoomWorkspace } from "@/components/task/RoomWorkspace";
import { Badge, Tbc } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import { Markdown } from "@/components/ui/Markdown";
import { serverFetchOrLogin } from "@/lib/api-server";
import { ApiError } from "@/lib/errors";
import type { RoomDetail, TaskView } from "@/lib/types";

const TASK_STATE_LABEL: Record<TaskView["state"], string> = {
  AVAILABLE: "Por resolver", IN_PROGRESS: "Em curso", PASSED: "Resolvida", FAILED: "Tentativas esgotadas", UNAVAILABLE: "Em revisão",
};

export default async function Sala({ params, searchParams }: { params: Promise<{ id: string }>; searchParams: Promise<{ tarefa?: string }> }) {
  const { id } = await params;
  const { tarefa } = await searchParams;
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
      <SetCrumbs items={[{ label: "Trilhas", href: "/trilhas" }, { label: b.trackTitle, href: `/trilhas/${b.trackSlug}` },
        { label: `Módulo ${b.modulePosition}` }, { label: room.title }]} />
      <div className="row" style={{ alignItems: "flex-end" }}>
        <div className="stack" style={{ gap: 6 }}>
          <div className="label">{b.trackTitle} · Módulo {b.modulePosition} · Sala {b.roomPosition} · ≈ {room.estMinutes} min</div>
          <h1 className="h1">{room.title}</h1>
        </div>
        <div className="spacer" />
        <span className="badge mono">{room.obtained} / {room.possible} pts</span>
      </div>

      {room.state === "LOCKED" ? (
        <Card className="state-box">
          <div className="row" style={{ gap: 10 }}><Icon name="lock" /><h2 className="h3">Sala bloqueada</h2></div>
          <p className="small muted">{room.lockedReason ?? "Requer a sala anterior."}</p>
          <Link className="btn out" href={`/trilhas/${b.trackSlug}`}>Ver a trilha</Link>
        </Card>
      ) : (
        <div className="room-grid">
          <aside className="stack" style={{ gap: 16 }} aria-label="Tarefas da sala">
            <Card className="stack" style={{ gap: 6, padding: 14 }}>
              <div className="row" style={{ padding: "0 4px 6px" }}><b>Tarefas</b><div className="spacer" /><span className="mono small muted">{done}/{room.tasks.length}</span></div>
              <ol className="plain stack" style={{ gap: 2 }}>
                {room.tasks.map((t) => {
                  const on = t.id === current?.id;
                  const cls = ["task", (t.state === "PASSED") && "done", on && "cur", t.state === "UNAVAILABLE" && "lock"].filter(Boolean).join(" ");
                  return (
                    <li key={t.id}>
                      <Link className={cls} href={`/salas/${room.id}?tarefa=${t.position}`} aria-current={on ? "step" : undefined}>
                        <span className="st" aria-hidden="true">{t.state === "PASSED" ? <Icon name="check" /> : t.state === "FAILED" ? <Icon name="x" /> : t.position}</span>
                        <span style={{ flex: 1 }}>{t.title}<span className="sr-only"> · {TASK_STATE_LABEL[t.state]}</span></span>
                        <span className="mono small muted">{t.state === "PASSED" ? t.score : t.points}</span>
                      </Link>
                    </li>
                  );
                })}
              </ol>
            </Card>
            {room.objectives.length > 0 && (
              <Card className="stack" style={{ padding: 16 }}>
                <b>Objectivos</b>
                <ul className="plain stack small">{room.objectives.map((o) => <li key={o} className="row" style={{ gap: 8 }}><Icon name="check" />{o}</li>)}</ul>
              </Card>
            )}
            {room.references.length > 0 && (
              <Card className="stack" style={{ padding: 16 }}>
                <b>Referências</b>
                <ul className="plain stack small">
                  {room.references.map((r) => <li key={r.title + r.identifier}>{r.title} · {r.identifier}{!r.confirmed && <Tbc>versão a confirmar</Tbc>}</li>)}
                </ul>
              </Card>
            )}
            {room.context && <Card className="small" style={{ padding: 16 }}><Markdown text={room.context} /></Card>}
          </aside>
          <section aria-label="Área de trabalho">
            {current ? (
              current.state === "UNAVAILABLE"
                ? <Card className="state-box"><Badge>Em revisão</Badge><p className="small muted">Esta tarefa voltou a revisão editorial e está temporariamente indisponível.</p></Card>
                : <RoomWorkspace key={current.id} task={current} context={room.context} references={room.references} />
            ) : <Card className="state-box"><p className="muted">Esta sala ainda não tem tarefas publicadas.</p></Card>}
          </section>
        </div>
      )}
    </>
  );
}
