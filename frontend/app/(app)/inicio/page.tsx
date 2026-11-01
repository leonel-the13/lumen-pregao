import Link from "next/link";
import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { RoomLink } from "@/components/learning/RoomLink";
import { Badge } from "@/components/ui/Badge";
import { Card, CardHeader, Kpi } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import { Ring } from "@/components/ui/Progress";
import { EmptyState } from "@/components/ui/States";
import { ServerCountdown } from "@/components/contest/Countdown";
import { serverFetchOrLogin } from "@/lib/api-server";
import { formatLongDate, formatPoints } from "@/lib/format";
import type { Activity, Dashboard } from "@/lib/types";

export const metadata = { title: "Início" };

const ACTIVITY: Record<Activity["kind"], { tone: "pos" | "neg" | "info" | "acc" | "neutral"; label: string }> = {
  CORRECT: { tone: "pos", label: "Correcta" },
  INCORRECT: { tone: "neg", label: "Incorrecta" },
  STEP: { tone: "info", label: "Passo certo" },
  REJECTED: { tone: "neg", label: "Rejeitada" },
  PENDING: { tone: "info", label: "Em processamento" },
};

const CONTEST_STATE: Record<string, string> = {
  REGISTRATION: "Inscrições abertas", WAITING_ROOM: "Sala de espera", RUNNING: "Em curso", FROZEN: "Em curso · congelado",
};

function movement(m?: number) {
  if (m === undefined || m === null || m === 0) return null;
  return m > 0 ? <span className="move-up">▲ {m}</span> : <span className="move-dn">▼ {Math.abs(m)}</span>;
}

export default async function Inicio() {
  const d = await serverFetchOrLogin<Dashboard>("/me/dashboard", "/inicio");
  const c = d.nextContest;
  const isNew = d.points === 0 && d.activity.length === 0;
  return (
    <>
      <SetCrumbs items={[{ label: "Início" }]} />
      <div className="row" style={{ justifyContent: "space-between", alignItems: "flex-end" }}>
        <div className="stack" style={{ gap: 6 }}>
          <div className="label">{formatLongDate(d.serverNow)}</div>
          <h1 className="d2">{isNew ? "Bem-vindo" : "Bom regresso"}, <span className="serif">{d.firstName}.</span></h1>
        </div>
        {d.continueRoomId && (
          <Link className="btn pri" href={`/salas/${d.continueRoomId}`}>
            {isNew ? "Começar" : "Continuar"} «{d.continueRoomTitle}» <Icon name="arrowRight" />
          </Link>
        )}
      </div>

      <div className="grid g4">
        <Kpi label="Pontuação" value={formatPoints(d.points)} detail={<><span className="move-up">▲ {formatPoints(d.weekPoints)}</span> esta semana</>} />
        <Kpi label="Sequência" value={`${d.streak} ${d.streak === 1 ? "dia" : "dias"}`} accent
          detail={d.practicedToday ? "Já praticaste hoje" : "Pratica hoje para manter"} />
        <Kpi label={`Ranking · ${d.courseName ?? "Geral"}`} value={d.coursePosition.position ? `#${d.coursePosition.position}` : "—"}
          detail={movement(d.coursePosition.movement) ?? (d.coursePosition.position ? "sem alteração desde segunda" : "Resolve uma tarefa para entrar")} />
        <Kpi label="Salas concluídas" value={d.roomsCompleted} detail={d.roomsInActiveTrack ? `de ${d.roomsInActiveTrack} na trilha activa` : undefined} />
      </div>

      {isNew && (
        <EmptyState title="Começa pela tua primeira sala" icon="route"
          body="Ainda não tens actividade. Escolhe uma trilha recomendada e resolve a primeira tarefa: a correcção é feita no servidor."
          action={<Link className="btn out" href="/trilhas">Ver trilhas</Link>} />
      )}

      <div className="grid" style={{ gridTemplateColumns: "repeat(auto-fit,minmax(min(100%,520px),1fr))" }}>
        <Card>
          <CardHeader title="Trilhas em curso"><Link className="btn sm ghost" href="/trilhas">Ver todas</Link></CardHeader>
          {d.tracksInProgress.length === 0 ? <p className="small muted">Ainda não começaste nenhuma trilha.</p> : (
            <div className="stack" style={{ gap: 16 }}>
              {d.tracksInProgress.map((t, i) => (
                <div key={t.slug} className="stack" style={{ gap: 16 }}>
                  {i > 0 && <hr className="divider" />}
                  <div className="row" style={{ gap: 16 }}>
                    <Ring value={t.percent} label={t.title} />
                    <div style={{ flex: 1, minWidth: 200 }}><div className="h3">{t.title}</div><div className="small muted">{t.summary}</div></div>
                    <Link className="btn sm out" href={`/trilhas/${t.slug}`}>Abrir</Link>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
        <Card>
          <CardHeader title="Próximo contest">{c && <Badge tone="acc">{CONTEST_STATE[c.state] ?? c.state}</Badge>}</CardHeader>
          {!c ? (
            <EmptyState title="Ainda não participaste em competições" body="A próxima Maratona abre inscrições em breve."
              action={<Link className="btn out" href="/competicoes">Ver competições</Link>} />
          ) : (
            <>
              <div className="h3">{c.title}</div>
              <p className="small muted" style={{ margin: "4px 0 14px" }}>
                {c.mode === "TEAM" ? `Equipas de ${c.minTeam} a ${c.maxTeam}` : "Individual"} · casos com empresas fictícias
              </p>
              <div className="row">
                <ServerCountdown target={c.state === "REGISTRATION" || c.state === "WAITING_ROOM" ? c.startsAt : c.endsAt} serverNow={d.serverNow} />
                <div className="spacer" />
                <Link className="btn pri" href={`/competicoes/${c.id}`}>{c.myTeamName ? "Abrir" : c.mode === "TEAM" ? "Inscrever equipa" : "Inscrever-me"}</Link>
              </div>
              {c.myTeamName && c.mode === "TEAM" && (
                <p className="small muted" style={{ margin: "12px 0 0" }}>A tua equipa <b>{c.myTeamName}</b>: {c.myTeamConfirmed} de {c.maxTeam} membros confirmados.</p>
              )}
            </>
          )}
        </Card>
      </div>

      <div className="grid" style={{ gridTemplateColumns: "repeat(auto-fit,minmax(min(100%,520px),1fr))" }}>
        <Card>
          <CardHeader title="Salas recomendadas"><span className="small muted">com base no teu progresso</span></CardHeader>
          {d.recommended.length === 0 ? <p className="small muted">Sem recomendações por agora.</p>
            : <div className="stack" style={{ gap: 4 }}>{d.recommended.map((r) => <RoomLink key={r.id} r={r} />)}</div>}
        </Card>
        <Card>
          <CardHeader title="Actividade recente" />
          {d.activity.length === 0 ? <p className="small muted">Ainda sem actividade.</p> : (
            <ul className="plain stack" style={{ gap: 12 }}>
              {d.activity.map((a, i) => (
                <li key={i} className="row" style={{ gap: 12 }}>
                  <Badge tone={ACTIVITY[a.kind].tone}>{ACTIVITY[a.kind].label}</Badge>
                  <span className="small" style={{ flex: 1 }}>{a.label}</span>
                  <span className={["mono small", a.points ? "accent" : "muted"].join(" ")}>{a.points ? `+${a.points}` : "0"}</span>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </>
  );
}
