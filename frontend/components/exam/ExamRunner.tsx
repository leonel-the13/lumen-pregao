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
import { useServerNow, useServerOffset } from "@/lib/server-clock";
import type { ExamReport, ExamSessionView } from "@/lib/types";
import { ExamReportView } from "./ExamReportView";

const TYPE_LABEL: Record<string, string> = { A: "escolha múltipla", B: "resposta numérica" };

/** Exame em modo concentração. O fim é decidido pelo servidor; o cronómetro só apresenta. */
export function ExamRunner({ initial }: { initial: ExamSessionView }) {
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
      setError("O exame foi submetido. O relatório estará disponível dentro de instantes.");
    }
  }, [initial.id]);

  useEffect(() => {
    if (initial.status !== "IN_PROGRESS") get<ExamReport>(`/exam-sessions/${initial.id}/report`).then(setReport).catch(() => {});
  }, [initial]);

  if (report) return <div className="content" style={{ margin: "0 auto" }}><ExamReportView r={report} /></div>;

  return (
    <div className="stack" style={{ gap: 0 }}>
      <header className="topbar">
        <Brand href="/exames" />
        <Badge icon="eye">Modo concentração</Badge>
        <span className="small muted">{initial.title}</span>
        <div className="spacer" />
        <Timer target={initial.endsAt} offsetMs={offset} withDays={false} onElapsed={finish} />
        <Link className="btn ghost" href="/exames">Sair</Link>
      </header>
      <div className="content ex-grid" style={{ margin: "0 auto" }}>
        <div className="stack" style={{ gap: 18 }}>
          <Card className="stack" style={{ gap: 18 }}>
            <div className="label">Pergunta {n} de {total} · {TYPE_LABEL[q.type] ?? q.type}{q.type === "A" && !q.presentation.multiple ? " · 1 resposta" : ""}</div>
            <div className="h3" style={{ fontWeight: 600, fontSize: 18 }}><StatementText segments={q.segments} fallback={q.statement} /></div>
            <AnswerEditor type={q.type as TaskType} presentation={q.presentation} state={answers[n] ?? emptyAnswer()} disabled={left <= 0}
              onChange={(a) => { setAnswers((x) => ({ ...x, [n]: a })); save(n, a); }} />
            <div className="row">
              <Button variant="out" icon="arrowLeft" disabled={n === 1} onClick={() => setN(n - 1)}>Anterior</Button>
              <div className="spacer" />
              <Button variant="ghost" icon="flag" aria-pressed={marked.has(n)}
                onClick={() => { const m = new Set(marked); if (m.has(n)) m.delete(n); else m.add(n); setMarked(m); save(n, answers[n] ?? emptyAnswer(), m.has(n)); }}>
                {marked.has(n) ? "Marcada para rever" : "Marcar para rever"}
              </Button>
              <Button variant="pri" iconRight="arrowRight" disabled={n === total} onClick={() => setN(n + 1)}>Seguinte</Button>
            </div>
          </Card>
          {left <= 5 * 60_000 && left > 0 && (
            <Alert tone="acc" title={`Faltam ${Math.ceil(left / 60_000)} minutos`}>
              O exame termina automaticamente às {formatTime(initial.endsAt)} (hora do servidor). As respostas são guardadas a cada alteração.
            </Alert>
          )}
          {error && <Alert tone="neg" title="Tempo terminado">{error}</Alert>}
          <span className="small muted" aria-live="polite">
            {saving === "saving" ? "A guardar…" : saving === "saved" ? "Resposta guardada" : saving === "error" ? "Não foi possível guardar. Vamos tentar de novo na próxima alteração." : ""}
          </span>
        </div>
        <aside className="card stack" aria-label="Navegação entre perguntas">
          <b>Navegação</b>
          <div className="qnav">
            {initial.questions.map((x) => (
              <button key={x.n} type="button" onClick={() => setN(x.n)} aria-current={x.n === n ? "step" : undefined}
                aria-label={`Pergunta ${x.n}${answered(x.n) ? ", respondida" : ""}${marked.has(x.n) ? ", para rever" : ""}`}
                className={[answered(x.n) && "answered", marked.has(x.n) && "marked", x.n === n && "cur"].filter(Boolean).join(" ")}>{x.n}</button>
            ))}
          </div>
          <div className="stack small muted" style={{ gap: 4 }}>
            <span>Respondida ({total - unanswered})</span><span>Para rever ({marked.size})</span><span>Por responder ({unanswered})</span>
          </div>
          <Button variant="pri" icon="check" onClick={() => setConfirm(true)}>Terminar e submeter</Button>
        </aside>
      </div>
      <ConfirmDialog open={confirm} onCancel={() => setConfirm(false)} onConfirm={finish} confirmLabel="Submeter"
        title={unanswered ? `Submeter com ${unanswered} ${unanswered === 1 ? "pergunta" : "perguntas"} por responder?` : "Submeter o exame?"}
        body={<><Icon name="info" /> Depois de submeter não podes alterar respostas.</>} />
    </div>
  );
}
