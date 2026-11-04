import Link from "next/link";
import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { RoomLink } from "@/components/learning/RoomLink";
import { Badge } from "@/components/ui/Badge";
import { Card, CardHeader, Kpi } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import { Ring } from "@/components/ui/Progress";
import { EmptyState } from "@/components/ui/States";
import { ServerCountdown } from "@/components/contest/Countdown";
import { CountUp } from "@/components/ui/CountUp";
import { serverFetchOrLogin } from "@/lib/api-server";
import { formatLongDate, formatPoints } from "@/lib/format";
import type { Activity, Dashboard } from "@/lib/types";
import { getI18n } from "@/lib/i18n/server";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("learn.home.title") };
}

const ACTIVITY_TONE: Record<Activity["kind"], "pos" | "neg" | "info" | "acc" | "neutral"> = {
  CORRECT: "pos", INCORRECT: "neg", STEP: "info", REJECTED: "neg", PENDING: "info",
};

const CONTEST_STATES = ["REGISTRATION", "WAITING_ROOM", "RUNNING", "FROZEN"] as const;

function movement(m?: number) {
  if (m === undefined || m === null || m === 0) return null;
  return m > 0 ? <span className="move-up">▲ {m}</span> : <span className="move-dn">▼ {Math.abs(m)}</span>;
}

export default async function Inicio() {
  const [d, { t, locale }] = await Promise.all([serverFetchOrLogin<Dashboard>("/me/dashboard", "/inicio"), getI18n()]);
  const c = d.nextContest;
  const isNew = d.points === 0 && d.activity.length === 0;
  return (
    <>
      <SetCrumbs items={[{ label: t("learn.home.title") }]} />
      <div className="row" style={{ justifyContent: "space-between", alignItems: "flex-end" }}>
        <div className="stack" style={{ gap: 6 }}>
          <div className="label">{formatLongDate(d.serverNow, locale)}</div>
          <h1 className="d2">{isNew ? t("learn.home.welcome") : t("learn.home.welcomeBack")}, <span className="serif">{d.firstName}.</span></h1>
        </div>
        {d.continueRoomId && (
          <Link className="btn pri" href={`/salas/${d.continueRoomId}`}>
            {isNew ? t("learn.home.start") : t("learn.home.continue")} «{d.continueRoomTitle}» <Icon name="arrowRight" />
          </Link>
        )}
      </div>

      <div className="grid g4">
        <Kpi label={t("learn.home.score")} value={<CountUp value={d.points} />} detail={<><span className="move-up">▲ {formatPoints(d.weekPoints)}</span> {t("learn.home.thisWeek")}</>} />
        <Kpi label={t("learn.home.streak")} value={t(d.streak === 1 ? "common.day" : "common.days", { n: d.streak })} accent
          detail={d.practicedToday ? t("learn.home.practicedToday") : t("learn.home.practiceToday")} />
        <Kpi label={t("learn.home.ranking", { scope: d.courseName ?? t("learn.home.general") })} value={d.coursePosition.position ? `#${d.coursePosition.position}` : "—"}
          detail={movement(d.coursePosition.movement) ?? (d.coursePosition.position ? t("learn.home.noChange") : t("learn.home.solveToEnter"))} />
        <Kpi label={t("learn.home.roomsDone")} value={d.roomsCompleted} detail={d.roomsInActiveTrack ? t("learn.home.ofActiveTrack", { n: d.roomsInActiveTrack }) : undefined} />
      </div>

      {isNew && (
        <EmptyState title={t("learn.home.firstRoom")} icon="route"
          body={t("learn.home.firstRoomBody")}
          action={<Link className="btn out" href="/trilhas">{t("learn.home.seeTracks")}</Link>} />
      )}

      <div className="grid" style={{ gridTemplateColumns: "repeat(auto-fit,minmax(min(100%,520px),1fr))" }}>
        <Card>
          <CardHeader title={t("learn.home.tracksInProgress")}><Link className="btn sm ghost" href="/trilhas">{t("learn.home.seeAll")}</Link></CardHeader>
          {d.tracksInProgress.length === 0 ? <p className="small muted">{t("learn.home.noTracks")}</p> : (
            <div className="stack" style={{ gap: 16 }}>
              {d.tracksInProgress.map((tr, i) => (
                <div key={tr.slug} className="stack" style={{ gap: 16 }}>
                  {i > 0 && <hr className="divider" />}
                  <div className="row" style={{ gap: 16 }}>
                    <Ring value={tr.percent} label={tr.title} />
                    <div style={{ flex: 1, minWidth: 200 }}><div className="h3">{tr.title}</div><div className="small muted">{tr.summary}</div></div>
                    <Link className="btn sm out" href={`/trilhas/${tr.slug}`}>{t("learn.home.open")}</Link>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
        <Card>
          <CardHeader title={t("learn.home.nextContest")}>{c && <Badge tone="acc">{(CONTEST_STATES as readonly string[]).includes(c.state) ? t(`learn.home.contestState.${c.state as (typeof CONTEST_STATES)[number]}`) : c.state}</Badge>}</CardHeader>
          {!c ? (
            <EmptyState title={t("learn.home.noContests")} body={t("learn.home.noContestsBody")}
              action={<Link className="btn out" href="/competicoes">{t("learn.home.seeContests")}</Link>} />
          ) : (
            <>
              <div className="h3">{c.title}</div>
              <p className="small muted" style={{ margin: "4px 0 14px" }}>
                {c.mode === "TEAM" ? t("learn.home.teamsOf", { min: c.minTeam, max: c.maxTeam }) : t("learn.home.individual")} · {t("learn.home.fictional")}
              </p>
              <div className="row">
                <ServerCountdown target={c.state === "REGISTRATION" || c.state === "WAITING_ROOM" ? c.startsAt : c.endsAt} serverNow={d.serverNow} />
                <div className="spacer" />
                <Link className="btn pri" href={`/competicoes/${c.id}`}>{c.myTeamName ? t("learn.home.open") : c.mode === "TEAM" ? t("learn.home.registerTeam") : t("learn.home.registerMe")}</Link>
              </div>
              {c.myTeamName && c.mode === "TEAM" && (
                <p className="small muted" style={{ margin: "12px 0 0" }}>{t("learn.home.yourTeam", { team: c.myTeamName, n: c.myTeamConfirmed ?? 0, max: c.maxTeam })}</p>
              )}
            </>
          )}
        </Card>
      </div>

      <div className="grid" style={{ gridTemplateColumns: "repeat(auto-fit,minmax(min(100%,520px),1fr))" }}>
        <Card>
          <CardHeader title={t("learn.home.recommended")}><span className="small muted">{t("learn.home.basedOnProgress")}</span></CardHeader>
          {d.recommended.length === 0 ? <p className="small muted">{t("learn.home.noRecommendations")}</p>
            : <div className="stack" style={{ gap: 4 }}>{d.recommended.map((r) => <RoomLink key={r.id} r={r} />)}</div>}
        </Card>
        <Card>
          <CardHeader title={t("learn.home.recentActivity")} />
          {d.activity.length === 0 ? <p className="small muted">{t("learn.home.noActivity")}</p> : (
            <ul className="plain stack" style={{ gap: 12 }}>
              {d.activity.map((a, i) => (
                <li key={i} className="row" style={{ gap: 12 }}>
                  <Badge tone={ACTIVITY_TONE[a.kind]}>{t(`learn.home.activity.${a.kind}`)}</Badge>
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
