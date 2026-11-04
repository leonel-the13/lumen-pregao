"use client";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { AnswerEditor, buildPayload, emptyAnswer, validateAnswer, type AnswerState, type TaskType } from "@/components/task/AnswerEditor";
import { StatementText } from "@/components/task/StatementText";
import { SubmissionPanel } from "@/components/task/SubmissionPanel";
import { Alert } from "@/components/ui/Alert";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import { Timer } from "@/components/ui/Timer";
import { useToast } from "@/components/ui/Toast";
import { get } from "@/lib/api-client";
import { formatDuration, formatTime } from "@/lib/format";
import { useLocale, useT } from "@/lib/i18n/client";
import { useServerNow, useServerOffset } from "@/lib/server-clock";
import { useContestEvents } from "@/lib/sse";
import type { ContestDetail, ProblemView, ScoreboardView } from "@/lib/types";
import { useSubmission } from "@/lib/use-submission";
import { BigCountdown } from "./Countdown";
import { LiveScoreboard } from "./LiveScoreboard";
import { SubmissionBadge } from "./SubmissionStatus";
import { TeamPanel } from "./TeamPanel";

const TYPES = ["A", "B", "C", "D", "E", "F"] as const;
const STATES = ["REGISTRATION", "WAITING_ROOM", "RUNNING", "FROZEN", "SUBMISSIONS_CLOSED", "FINAL_EVALUATION", "RESULTS_PUBLISHED", "CLOSED"] as const;
const isOneOf = <T extends string>(list: readonly T[], v: string): v is T => (list as readonly string[]).includes(v);

export function ContestView({ initial }: { initial: ContestDetail }) {
  const router = useRouter();
  const toast = useToast();
  const t = useT();
  const locale = useLocale();
  const [c, setC] = useState(initial);
  const [board, setBoard] = useState<ScoreboardView | null>(null);
  const [problems, setProblems] = useState<ProblemView[] | null>(null);
  const [sel, setSel] = useState<string | null>(null);
  const [answers, setAnswers] = useState<Record<string, AnswerState>>({});
  const [formatError, setFormatError] = useState<string | null>(null);
  const offset = useServerOffset(initial.serverNow);
  const now = useServerNow(offset);
  const live = c.state === "RUNNING" || c.state === "FROZEN";
  const registered = !!c.myTeam;

  const loadBoard = useCallback(() => get<ScoreboardView>(`/contests/${c.id}/scoreboard`).then(setBoard).catch(() => {}), [c.id]);
  const loadProblems = useCallback(() => {
    if (!registered) return;
    get<ProblemView[]>(`/contests/${c.id}/problems`).then((p) => { setProblems(p); setSel((s) => s ?? p.find((x) => x.status.state !== "SOLVED")?.label ?? p[0]?.label ?? null); }).catch(() => {});
  }, [c.id, registered]);
  const reloadDetail = useCallback(() => get<ContestDetail>(`/contests/${c.id}`).then(setC).catch(() => {}), [c.id]);

  useEffect(() => { loadBoard(); }, [loadBoard]);
  useEffect(() => { if (live || c.state === "SUBMISSIONS_CLOSED") loadProblems(); }, [live, c.state, loadProblems]);

  const { connected, lastEventAt } = useContestEvents(c.id, (e) => {
    if (e.type === "state") { reloadDetail(); loadBoard(); router.refresh(); }
    if (e.type === "scoreboard") loadBoard();
    if (e.type === "submission") {
      loadProblems();
      if (e.data.status === "GRADED") toast({ tone: e.data.correct ? "pos" : "neg", title: e.data.correct ? t("compete.view.solved") : t("compete.view.incorrect") });
    }
  });

  const sub = useSubmission(() => { loadProblems(); loadBoard(); });
  const p = problems?.find((x) => x.label === sel);

  const header = (
    <div className="row" style={{ alignItems: "flex-end" }}>
      <div className="stack" style={{ gap: 6 }}>
        <div className="row" style={{ gap: 8 }}>
          <Badge tone="acc">{isOneOf(STATES, c.state) ? t(`compete.stateBadge.${c.state}`) : c.state}</Badge>
          <Badge>{c.mode === "TEAM" ? t("compete.view.byTeams", { min: c.minTeam, max: c.maxTeam }) : t("compete.view.individual")}</Badge>
          <Badge>{t("compete.view.fictional")}</Badge>
        </div>
        <h1 className="h1">{c.title}</h1>
      </div>
      <div className="spacer" />
      {live && (
        <div className="stack" style={{ alignItems: "flex-end", gap: 6 }}>
          <Timer target={c.endsAt} offsetMs={offset} withDays={false} />
          {c.state === "RUNNING" && <span className="small muted"><Icon name="snow" /> {t("compete.view.freezesIn", { time: now === null ? "—" : formatDuration(new Date(c.freezeAt).getTime() - now, false) })}</span>}
        </div>
      )}
    </div>
  );

  if (c.state === "REGISTRATION" || c.state === "WAITING_ROOM") {
    return (
      <>
        {header}
        <p className="muted">{c.description}</p>
        <div className="grid g2" style={{ alignItems: "start" }}>
          <div className="stack" style={{ gap: 16 }}>
            <Card className="stack"><div className="label">{t("compete.view.startsIn")}</div>
              <BigCountdown target={c.startsAt} serverNow={c.serverNow} onElapsed={() => setTimeout(reloadDetail, 1500)} /></Card>
            <TeamPanel c={c} />
          </div>
          <div className="stack" style={{ gap: 16 }}>
            <Card className="stack">
              <h2 className="h2">{t("compete.view.rules")}</h2>
              <ul className="stack small" style={{ margin: 0, paddingLeft: 18 }}>
                <li>{t("compete.view.r1")}</li>
                <li>{t("compete.view.r2")}</li>
                <li>{t("compete.view.r3", { time: formatTime(c.freezeAt, locale) })} <span className="tbc">{t("pages.tbc")}</span>.</li>
                <li>{t("compete.view.r4", { n: c.wrongPenaltyMinutes })}</li>
                <li>{t("compete.view.r5")}</li>
              </ul>
            </Card>
            <Card className="stack">
              <h2 className="h2">{t("compete.view.schedule")}</h2>
              <ul className="plain stack small">
                <li className="row"><span className="mono" style={{ minWidth: 64, whiteSpace: "nowrap" }}>{formatTime(c.startsAt, locale).slice(0, 5)}</span>{t("compete.view.s1")}</li>
                <li className="row"><span className="mono" style={{ minWidth: 64, whiteSpace: "nowrap" }}>{formatTime(c.freezeAt, locale).slice(0, 5)}</span>{t("compete.view.s2")}</li>
                <li className="row"><span className="mono" style={{ minWidth: 64, whiteSpace: "nowrap" }}>{formatTime(c.endsAt, locale).slice(0, 5)}</span>{t("compete.view.s3")}</li>
                <li className="row"><span className="mono" style={{ minWidth: 64, whiteSpace: "nowrap" }}>{t("compete.view.tba")}</span>{t("compete.view.s4")}</li>
              </ul>
            </Card>
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      {header}
      {!registered && live && <Alert tone="info" title={t("compete.view.notRegistered")}>{t("compete.view.notRegisteredBody")}</Alert>}
      {c.state === "SUBMISSIONS_CLOSED" || c.state === "FINAL_EVALUATION" ? (
        <Alert tone="info" title={t("compete.view.closed")}>{t("compete.view.closedBody")}</Alert>
      ) : null}
      <div className="ex-grid" style={{ gridTemplateColumns: "minmax(0,1fr) 380px" }}>
        <div className="stack" style={{ gap: 16 }}>
          {problems && (
            <div className="tbl-wrap">
              <table className="tbl">
                <thead><tr><th>{t("compete.view.problem")}</th><th>{t("compete.view.type")}</th><th className="num">{t("compete.view.points")}</th><th>{t("compete.view.teamState")}</th><th><span className="sr-only">{t("compete.view.action")}</span></th></tr></thead>
                <tbody>
                  {problems.map((x) => (
                    <tr key={x.label} className={x.label === sel ? "me" : undefined}>
                      <td><b>{x.label} · {x.companyName}</b><div className="small muted">{x.title}</div></td>
                      <td className="small">{isOneOf(TYPES, x.type) ? t(`compete.type.${x.type}`) : x.type}</td>
                      <td className="num">{x.points}</td>
                      <td><SubmissionBadge status={x.status.state} />{x.status.wrongAttempts > 0 && x.status.state !== "SOLVED" && <span className="small muted"> · {t("compete.view.wrong", { n: x.status.wrongAttempts })}</span>}</td>
                      <td><Button size="sm" variant={x.label === sel ? "pri" : "out"} onClick={() => { setSel(x.label); sub.reset(); setFormatError(null); }}>{x.status.state === "SOLVED" ? t("compete.view.see") : t("compete.view.solve")}</Button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {p && (
            <Card className="stack" style={{ gap: 16 }}>
              <div className="row"><h2 className="h2">{t("compete.view.problemHeading", { label: p.label, state: p.status.state === "SOLVED" ? t("compete.view.resolved") : t("compete.view.submitAnswer") })}</h2><div className="spacer" /><Badge>{t("compete.view.variant")}</Badge></div>
              <StatementText segments={p.segments} fallback={p.statement} />
              <AnswerEditor type={p.type as TaskType} presentation={p.presentation} state={answers[p.label] ?? emptyAnswer()}
                disabled={!live || p.status.state === "SOLVED" || p.status.state === "QUEUED" || p.status.state === "PROCESSING"}
                result={sub.current?.status === "GRADED" ? sub.current : undefined}
                onChange={(a) => { setFormatError(null); setAnswers((x) => ({ ...x, [p.label]: a })); }} />
              {formatError && <div className="alert neg" role="alert"><b>{t("task.reviewAnswer")}</b> {formatError}</div>}
              <SubmissionPanel s={sub.current} error={sub.error} contest />
              {live && p.status.state !== "SOLVED" && (
                <div className="row">
                  <span className="small muted">{t("compete.view.uniqueKey")}</span>
                  <div className="spacer" />
                  <Button variant="pri" loading={sub.sending} onClick={() => {
                    const a = answers[p.label] ?? emptyAnswer();
                    const err = validateAnswer(p.type as TaskType, p.presentation, a, t);
                    if (err) { setFormatError(err); return; }
                    sub.submit(`/contests/${c.id}/problems/${p.label}/submissions`, buildPayload(p.type as TaskType, p.presentation, a));
                  }}>{t("compete.view.submit")}</Button>
                </div>
              )}
            </Card>
          )}
        </div>
        <div className="stack" style={{ gap: 12 }}>
          <LiveScoreboard board={board} connected={connected} />
          <span className="small muted" aria-live="off">{t("compete.view.sse")}{lastEventAt ? t("compete.view.lastAgo", { n: Math.max(0, Math.round((Date.now() - lastEventAt) / 1000)) }) : ""}</span>
        </div>
      </div>
    </>
  );
}
