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
import type { AttemptView, ReferenceView, TaskView } from "@/lib/types";
import { useSubmission } from "@/lib/use-submission";
import { AnswerEditor, buildPayload, emptyAnswer, validateAnswer, type AnswerState, type TaskType } from "./AnswerEditor";
import { StatementText } from "./StatementText";
import { SubmissionPanel } from "./SubmissionPanel";

const SUBMIT_LABEL: Record<string, string> = { A: "Submeter resposta", B: "Submeter resposta", C: "Submeter lançamento", D: "Submeter balanço", E: "Verificar passo", F: "Submeter análise" };

function draftKey(attemptId: string) {
  return `lp:draft:${attemptId}`;
}

export function RoomWorkspace({ task, context, references }: { task: TaskView; context?: string; references: ReferenceView[] }) {
  const router = useRouter();
  const toast = useToast();
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
      toast({ tone: "pos", title: `Tarefa ${task.position} concluída${s.points ? ` · +${s.points} pts` : ""}` });
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
      ? <ErrorState title="Tarefa indisponível" body={loadError.message} />
      : <ErrorState title="Não foi possível carregar a tarefa" code={loadError.code} action={<Button variant="out" onClick={() => { setLoadError(null); load(); }}>Tentar novamente</Button>} />;
  }
  if (!attempt) return <LoadingBlock label="A carregar a tarefa…" />;

  const type = attempt.type as TaskType;
  const presentation = attempt.presentation as Record<string, never>;
  const open = attempt.status === "OPEN";
  const pending = sub.current?.status === "QUEUED" || sub.current?.status === "PROCESSING";
  const lastResult = sub.current?.status === "GRADED" ? sub.current : attempt.lastSubmission?.status === "GRADED" ? attempt.lastSubmission : undefined;
  const attemptsLeft = attempt.maxAttempts - attempt.attemptsUsed;
  const company = attempt.params.find((p) => p.name === "empresa")?.display;

  const onChange = (a: AnswerState) => { dirty.current = true; setFormatError(null); setAnswer(a); };
  const submit = () => {
    const err = validateAnswer(type, presentation, answer);
    if (err) { setFormatError(err); return; }
    sub.submit(`/attempts/${attempt.attemptId}/submissions`, buildPayload(type, presentation, answer));
  };
  const saveDraft = async () => {
    localStorage.setItem(draftKey(attempt.attemptId), JSON.stringify(answer));
    try { await put(`/attempts/${attempt.attemptId}/draft`, answer); setSavedAt(new Date().toISOString()); toast({ tone: "info", title: "Rascunho guardado" }); }
    catch { toast({ tone: "info", title: "Rascunho guardado neste dispositivo", body: "Será enviado quando voltar a ligação." }); }
  };

  return (
    <div className="stack" style={{ gap: 20 }}>
      <Card className="stack">
        <Tabs label="Informação da tarefa" active={tab} onChange={setTab} items={[
          { key: "contexto", label: "Contexto" },
          { key: "documento", label: "Documento" },
          { key: "dicas", label: `Dicas (−5 pts)`, count: attempt.hintsAvailable ? attempt.hintsUsed : undefined },
        ]} />
        {tab === "contexto" && (
          <div className="stack" role="tabpanel">
            <StatementText segments={attempt.statementSegments} fallback={attempt.statementRendered} />
            <p className="small muted">Os valores destacados são únicos para esta tentativa.</p>
          </div>
        )}
        {tab === "documento" && (
          <div className="stack" role="tabpanel">
            {context ? <Markdown text={context} /> : <p className="small muted">Sem documento de apoio.</p>}
            {references.length > 0 && (
              <ul className="plain stack small">
                {references.map((r) => <li key={r.title + r.identifier}><b>{r.title}</b> · {r.identifier}{!r.confirmed && <Tbc>versão a confirmar</Tbc>}</li>)}
              </ul>
            )}
          </div>
        )}
        {tab === "dicas" && (
          <div className="stack" role="tabpanel">
            {attempt.revealedHints.length === 0 && <p className="small muted">Ainda não viste dicas nesta tarefa.</p>}
            {attempt.revealedHints.map((h, i) => <div key={i} className="alert info"><b>Dica {i + 1}</b> {h}</div>)}
            {open && attempt.hintsUsed < attempt.hintsAvailable && <Button variant="out" size="sm" onClick={() => setHintOpen(true)}>Ver dica · −5 pts</Button>}
            {attempt.hintsAvailable === 0 && <p className="small muted">Esta tarefa não tem dicas.</p>}
          </div>
        )}
      </Card>

      <Card className="stack" style={{ gap: 18 }}>
        <div className="row">
          <h2 className="h2">Tarefa {task.position} · {task.title}</h2>
          <div className="spacer" />
          {open ? <Badge mono>Tentativa {attempt.attemptsUsed + 1} de {attempt.maxAttempts}</Badge>
            : attempt.status === "PASSED" ? <Badge tone="pos">Concluída · {attempt.score} / {attempt.points} pts</Badge>
            : <Badge tone="neg">Tentativas esgotadas</Badge>}
        </div>
        <AnswerEditor type={type} presentation={presentation} state={answer} onChange={onChange} result={lastResult}
          disabled={!open || pending} pgcVersion={attempt.pgcVersion} company={company} />
        {formatError && <div className="alert neg" role="alert"><b>Revê a resposta</b> {formatError}</div>}
        <SubmissionPanel s={sub.current} error={sub.error} attemptsLeft={attemptsLeft} />
        {open && (
          <div className="row">
            <span className="small muted" aria-live="polite">{savedAt ? "Rascunho guardado" : ""}</span>
            <div className="spacer" />
            <Button variant="out" onClick={saveDraft} disabled={pending}>Guardar rascunho</Button>
            <Button variant="pri" onClick={submit} loading={sub.sending || pending}>{SUBMIT_LABEL[type] ?? "Submeter"}</Button>
          </div>
        )}
        <p className="spec">
          <b>Correcção no servidor.</b> Antes de submeter, a página só verifica o formato{type === "C" ? " e o equilíbrio (débito = crédito)" : ""}.
          A correcção oficial e os pontos vêm do servidor; os parâmetros desta tentativa ficam guardados para auditoria.
        </p>
      </Card>

      <ConfirmDialog open={hintOpen} onCancel={() => setHintOpen(false)} title="Ver dica?" confirmLabel="Ver dica · −5 pts"
        body="Cada dica usada retira 5 pts à pontuação desta tarefa."
        onConfirm={async () => {
          setHintOpen(false);
          try { await post(`/attempts/${attempt.attemptId}/hints`); await load(); setTab("dicas"); }
          catch (e) { toast({ tone: "neg", title: "Não foi possível mostrar a dica", body: (e as Error).message }); }
        }} />
    </div>
  );
}
