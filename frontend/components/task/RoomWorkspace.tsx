"use client";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { Badge, Tbc } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Markdown } from "@/components/ui/Markdown";
import { ConfirmDialog } from "@/components/ui/Modal";
import { ErrorState, LoadingBlock } from "@/components/ui/States";
import { Tabs } from "@/components/ui/Tabs";
import { useToast } from "@/components/ui/Toast";
import { post, put } from "@/lib/api-client";
import { ApiError } from "@/lib/errors";
import { useT } from "@/lib/i18n/client";
import type { AttemptView, ReferenceView, TaskView } from "@/lib/types";
import { useSubmission } from "@/lib/use-submission";
import { AnswerEditor, buildPayload, emptyAnswer, validateAnswer, type AnswerState, type TaskType } from "./AnswerEditor";
import { StatementText } from "./StatementText";
import { SubmissionPanel } from "./SubmissionPanel";

function draftKey(attemptId: string) {
  return `lp:draft:${attemptId}`;
}

export function RoomWorkspace({ task, context, references }: { task: TaskView; context?: string; references: ReferenceView[] }) {
  const router = useRouter();
  const toast = useToast();
  const t = useT();
  const [attempt, setAttempt] = useState<AttemptView | null>(null);
  const [loadError, setLoadError] = useState<ApiError | null>(null);
  const [answer, setAnswer] = useState<AnswerState>(emptyAnswer);
  const [tab, setTab] = useState("contexto");
  const [formatError, setFormatError] = useState<string | null>(null);
  const [hintOpen, setHintOpen] = useState(false);
  const [savedAt, setSavedAt] = useState<string | null>(null);
  const dirty = useRef(false);

  const load = useCallback(async () => {
    try {
      const a = await post<AttemptView>(`/tasks/${task.id}/attempts`);
      setAttempt(a);
      const local = typeof window !== "undefined" ? localStorage.getItem(draftKey(a.attemptId)) : null;
      const draft = (local ? JSON.parse(local) : a.draft) as AnswerState | undefined;
      setAnswer(draft && typeof draft === "object" && "lines" in draft ? draft : emptyAnswer());
      return a;
    } catch (e) {
      setLoadError(e as ApiError);
      return null;
    }
  }, [task.id]);

  const sub = useSubmission(async (s) => {
    const a = await load();
    router.refresh();
    if (s.correct) {
      toast({ tone: "pos", title: `${t("task.done", { n: task.position })}${s.points ? ` · +${s.points} pts` : ""}` });
      if (a) localStorage.removeItem(draftKey(a.attemptId));
    }
  });

  useEffect(() => {
    load().then((a) => { if (a?.lastSubmission) sub.track(a.lastSubmission); });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [load]);

  // Rascunho: guardado localmente a cada alteração e no servidor a cada 3 s de inactividade.
  useEffect(() => {
    if (!attempt || attempt.status !== "OPEN" || !dirty.current) return;
    localStorage.setItem(draftKey(attempt.attemptId), JSON.stringify(answer));
    const id = setTimeout(async () => {
      try { await put(`/attempts/${attempt.attemptId}/draft`, answer); setSavedAt(new Date().toISOString()); } catch { /* fica local */ }
    }, 3000);
    return () => clearTimeout(id);
  }, [answer, attempt]);

  if (loadError) {
    return loadError.code === "ROOM_LOCKED" || loadError.code === "CONTENT_UNAVAILABLE"
      ? <ErrorState title={t("task.unavailable")} body={loadError.message} />
      : <ErrorState title={t("task.loadFailed")} code={loadError.code} action={<Button variant="out" onClick={() => { setLoadError(null); load(); }}>{t("pages.error.retry")}</Button>} />;
  }
  if (!attempt) return <LoadingBlock label={t("task.loading")} />;

  const type = attempt.type as TaskType;
  const presentation = attempt.presentation as Record<string, never>;
  const open = attempt.status === "OPEN";
  const pending = sub.current?.status === "QUEUED" || sub.current?.status === "PROCESSING";
  const lastResult = sub.current?.status === "GRADED" ? sub.current : attempt.lastSubmission?.status === "GRADED" ? attempt.lastSubmission : undefined;
  const attemptsLeft = attempt.maxAttempts - attempt.attemptsUsed;
  const company = attempt.params.find((p) => p.name === "empresa")?.display;

  const onChange = (a: AnswerState) => { dirty.current = true; setFormatError(null); setAnswer(a); };
  const submit = () => {
    const err = validateAnswer(type, presentation, answer, t);
    if (err) { setFormatError(err); return; }
    sub.submit(`/attempts/${attempt.attemptId}/submissions`, buildPayload(type, presentation, answer));
  };
  const saveDraft = async () => {
    localStorage.setItem(draftKey(attempt.attemptId), JSON.stringify(answer));
    try { await put(`/attempts/${attempt.attemptId}/draft`, answer); setSavedAt(new Date().toISOString()); toast({ tone: "info", title: t("task.draftSaved") }); }
    catch { toast({ tone: "info", title: t("task.draftSavedLocal"), body: t("task.draftSavedLocalBody") }); }
  };

  return (
    <div className="stack" style={{ gap: 20 }}>
      <Card className="stack">
        <Tabs label={t("task.infoTabs")} active={tab} onChange={setTab} items={[
          { key: "contexto", label: t("task.context") },
          { key: "documento", label: t("task.document") },
          { key: "dicas", label: t("task.hints"), count: attempt.hintsAvailable ? attempt.hintsUsed : undefined },
        ]} />
        {tab === "contexto" && (
          <div className="stack" role="tabpanel">
            <StatementText segments={attempt.statementSegments} fallback={attempt.statementRendered} />
            <p className="small muted">{t("task.uniqueValues")}</p>
          </div>
        )}
        {tab === "documento" && (
          <div className="stack" role="tabpanel">
            {context ? <Markdown text={context} /> : <p className="small muted">{t("task.noDocument")}</p>}
            {references.length > 0 && (
              <ul className="plain stack small">
                {references.map((r) => <li key={r.title + r.identifier}><b>{r.title}</b> · {r.identifier}{!r.confirmed && <Tbc>{t("learn.rooms.versionTbc")}</Tbc>}</li>)}
              </ul>
            )}
          </div>
        )}
        {tab === "dicas" && (
          <div className="stack" role="tabpanel">
            {attempt.revealedHints.length === 0 && <p className="small muted">{t("task.noHintsYet")}</p>}
            {attempt.revealedHints.map((h, i) => <div key={i} className="alert info"><b>{t("task.hintN", { n: i + 1 })}</b> {h}</div>)}
            {open && attempt.hintsUsed < attempt.hintsAvailable && <Button variant="out" size="sm" onClick={() => setHintOpen(true)}>{t("task.seeHint")}</Button>}
            {attempt.hintsAvailable === 0 && <p className="small muted">{t("task.noHints")}</p>}
          </div>
        )}
      </Card>

      <Card className="stack" style={{ gap: 18 }}>
        <div className="row">
          <h2 className="h2">{t("task.heading", { n: task.position, title: task.title })}</h2>
          <div className="spacer" />
          {open ? <Badge mono>{t("task.attemptOf", { n: attempt.attemptsUsed + 1, max: attempt.maxAttempts })}</Badge>
            : attempt.status === "PASSED" ? <Badge tone="pos">{t("task.completed", { score: attempt.score ?? 0, points: attempt.points })}</Badge>
            : <Badge tone="neg">{t("task.noAttemptsLeft")}</Badge>}
        </div>
        <AnswerEditor type={type} presentation={presentation} state={answer} onChange={onChange} result={lastResult}
          disabled={!open || pending} pgcVersion={attempt.pgcVersion} company={company} />
        {formatError && <div className="alert neg" role="alert"><b>{t("task.reviewAnswer")}</b> {formatError}</div>}
        <SubmissionPanel s={sub.current} error={sub.error} attemptsLeft={attemptsLeft} />
        {open && (
          <div className="row">
            <span className="small muted" aria-live="polite">{savedAt ? t("task.draftSaved") : ""}</span>
            <div className="spacer" />
            <Button variant="out" onClick={saveDraft} disabled={pending}>{t("task.saveDraft")}</Button>
            <Button variant="pri" onClick={submit} loading={sub.sending || pending}>{type in { A: 1, B: 1, C: 1, D: 1, E: 1, F: 1 } ? t(`task.submit.${type as "A" | "B" | "C" | "D" | "E" | "F"}`) : t("task.submit.default")}</Button>
          </div>
        )}
        <p className="spec">
          <b>{t("task.serverGrading")}</b> {t("task.serverGradingBody", { balance: type === "C" ? t("task.balanceCheck") : "" })}
        </p>
      </Card>

      <ConfirmDialog open={hintOpen} onCancel={() => setHintOpen(false)} title={t("task.hintConfirm")} confirmLabel={t("task.seeHint")}
        body={t("task.hintConfirmBody")}
        onConfirm={async () => {
          setHintOpen(false);
          try { await post(`/attempts/${attempt.attemptId}/hints`); await load(); setTab("dicas"); }
          catch (e) { toast({ tone: "neg", title: t("task.hintFailed"), body: (e as Error).message }); }
        }} />
    </div>
  );
}
