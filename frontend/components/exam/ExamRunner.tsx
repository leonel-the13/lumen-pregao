"use client";
import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import { AnswerEditor, buildPayload, emptyAnswer, type AnswerState, type TaskType } from "@/components/task/AnswerEditor";
import { StatementText } from "@/components/task/StatementText";
import { Brand } from "@/components/layout/Brand";
import { Alert } from "@/components/ui/Alert";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import { ConfirmDialog } from "@/components/ui/Modal";
import { Timer } from "@/components/ui/Timer";
import { get, post, put } from "@/lib/api-client";
import { ApiError } from "@/lib/errors";
import { formatTime } from "@/lib/format";
import { useLocale, useT } from "@/lib/i18n/client";
import { useServerNow, useServerOffset } from "@/lib/server-clock";
import type { ExamReport, ExamSessionView } from "@/lib/types";
import { ExamReportView } from "./ExamReportView";

/** Exame em modo concentração. O fim é decidido pelo servidor; o cronómetro só apresenta. */
export function ExamRunner({ initial }: { initial: ExamSessionView }) {
  const t = useT();
  const locale = useLocale();
  const offset = useServerOffset(initial.serverNow);
  const now = useServerNow(offset);
  const [n, setN] = useState(1);
  const [answers, setAnswers] = useState<Record<number, AnswerState>>(() => {
    const out: Record<number, AnswerState> = {};
    for (const [k, v] of Object.entries(initial.answers ?? {})) {
      const a = v as { selected?: string[]; value?: number };
      out[Number(k)] = { ...emptyAnswer(), selected: a.selected ?? [], value: a.value !== undefined && a.value !== null ? String(a.value).replace(".", ",") : "" };
    }
    return out;
  });
  const [marked, setMarked] = useState<Set<number>>(new Set(initial.marked ?? []));
  const [saving, setSaving] = useState<"idle" | "saving" | "saved" | "error">("idle");
  const [confirm, setConfirm] = useState(false);
  const [report, setReport] = useState<ExamReport | null>(null);
  const [error, setError] = useState<string | null>(null);
  const timers = useRef<Record<number, ReturnType<typeof setTimeout>>>({});
  const q = initial.questions[n - 1];
  const total = initial.questions.length;
  const answered = (k: number) => {
    const a = answers[k];
    return !!a && (a.selected.length > 0 || a.value.trim() !== "");
  };
  const unanswered = initial.questions.filter((x) => !answered(x.n)).length;
  const left = now === null ? Infinity : new Date(initial.endsAt).getTime() - now;

  const save = useCallback((k: number, a: AnswerState, mark?: boolean) => {
    clearTimeout(timers.current[k]);
    timers.current[k] = setTimeout(async () => {
      setSaving("saving");
      const type = initial.questions[k - 1].type as TaskType;
      const empty = a.selected.length === 0 && a.value.trim() === "";
      try {
        await put(`/exam-sessions/${initial.id}/answers/${k}`, { answer: empty ? null : buildPayload(type, {}, a), marked: mark });
        setSaving("saved");
      } catch (e) {
        setSaving("error");
        if (e instanceof ApiError && e.code === "SUBMISSION_AFTER_DEADLINE") setError(e.message);
      }
    }, 350);
  }, [initial]);

  const finish = useCallback(async () => {
    setConfirm(false);
    try {
      setReport(await post<ExamReport>(`/exam-sessions/${initial.id}/submit`));
    } catch {
      // Depois do prazo o servidor submete automaticamente: espera pelo relatório.
      for (let i = 0; i < 10; i++) {
        await new Promise((r) => setTimeout(r, 2000));
        try { setReport(await get<ExamReport>(`/exam-sessions/${initial.id}/report`)); return; } catch { /* ainda a corrigir */ }
      }
      setError(t("exam.runner.submittedLater"));
    }
  }, [initial.id, t]);

  useEffect(() => {
    if (initial.status !== "IN_PROGRESS") get<ExamReport>(`/exam-sessions/${initial.id}/report`).then(setReport).catch(() => {});
  }, [initial]);

  if (report) return <div className="content" style={{ margin: "0 auto" }}><ExamReportView r={report} /></div>;

  return (
    <div className="stack" style={{ gap: 0 }}>
      <header className="topbar">
        <Brand href="/exames" />
        <Badge icon="eye">{t("exam.runner.focus")}</Badge>
        <span className="small muted">{initial.title}</span>
        <div className="spacer" />
        <Timer target={initial.endsAt} offsetMs={offset} withDays={false} onElapsed={finish} />
        <Link className="btn ghost" href="/exames">{t("exam.runner.exit")}</Link>
      </header>
      <div className="content ex-grid" style={{ margin: "0 auto" }}>
        <div className="stack" style={{ gap: 18 }}>
          <Card className="stack" style={{ gap: 18 }}>
            <div className="label">{t("exam.runner.question", { n, total, type: q.type === "A" || q.type === "B" ? t(`exam.runner.type.${q.type}`) : q.type })}{q.type === "A" && !q.presentation.multiple ? t("exam.runner.oneAnswer") : ""}</div>
            <div className="h3" style={{ fontWeight: 600, fontSize: 18 }}><StatementText segments={q.segments} fallback={q.statement} /></div>
            <AnswerEditor type={q.type as TaskType} presentation={q.presentation} state={answers[n] ?? emptyAnswer()} disabled={left <= 0}
              onChange={(a) => { setAnswers((x) => ({ ...x, [n]: a })); save(n, a); }} />
            <div className="row">
              <Button variant="out" icon="arrowLeft" disabled={n === 1} onClick={() => setN(n - 1)}>{t("common.previous")}</Button>
              <div className="spacer" />
              <Button variant="ghost" icon="flag" aria-pressed={marked.has(n)}
                onClick={() => { const m = new Set(marked); if (m.has(n)) m.delete(n); else m.add(n); setMarked(m); save(n, answers[n] ?? emptyAnswer(), m.has(n)); }}>
                {marked.has(n) ? t("exam.runner.marked") : t("exam.runner.mark")}
              </Button>
              <Button variant="pri" iconRight="arrowRight" disabled={n === total} onClick={() => setN(n + 1)}>{t("common.next")}</Button>
            </div>
          </Card>
          {left <= 5 * 60_000 && left > 0 && (
            <Alert tone="acc" title={t("exam.runner.minutesLeft", { n: Math.ceil(left / 60_000) })}>
              {t("exam.runner.endsAt", { time: formatTime(initial.endsAt, locale) })}
            </Alert>
          )}
          {error && <Alert tone="neg" title={t("exam.runner.timeUp")}>{error}</Alert>}
          <span className="small muted" aria-live="polite">
            {saving === "saving" ? t("exam.runner.saving") : saving === "saved" ? t("exam.runner.saved") : saving === "error" ? t("exam.runner.saveFailed") : ""}
          </span>
        </div>
        <aside className="card stack" aria-label={t("exam.runner.nav")}>
          <b>{t("exam.runner.navTitle")}</b>
          <div className="qnav">
            {initial.questions.map((x) => (
              <button key={x.n} type="button" onClick={() => setN(x.n)} aria-current={x.n === n ? "step" : undefined}
                aria-label={`${t("exam.runner.qLabel", { n: x.n })}${answered(x.n) ? t("exam.runner.answeredSuffix") : ""}${marked.has(x.n) ? t("exam.runner.reviewSuffix") : ""}`}
                className={[answered(x.n) && "answered", marked.has(x.n) && "marked", x.n === n && "cur"].filter(Boolean).join(" ")}>{x.n}</button>
            ))}
          </div>
          <div className="stack small muted" style={{ gap: 4 }}>
            <span>{t("exam.runner.answered", { n: total - unanswered })}</span><span>{t("exam.runner.toReview", { n: marked.size })}</span><span>{t("exam.runner.unanswered", { n: unanswered })}</span>
          </div>
          <Button variant="pri" icon="check" onClick={() => setConfirm(true)}>{t("exam.runner.finish")}</Button>
        </aside>
      </div>
      <ConfirmDialog open={confirm} onCancel={() => setConfirm(false)} onConfirm={finish} confirmLabel={t("exam.runner.submit")}
        title={unanswered ? t(unanswered === 1 ? "exam.runner.submitWithOne" : "exam.runner.submitWithMany", { n: unanswered }) : t("exam.runner.submitExam")}
        body={<><Icon name="info" /> {t("exam.runner.noChanges")}</>} />
    </div>
  );
}
