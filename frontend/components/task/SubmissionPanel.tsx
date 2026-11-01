"use client";
import { rejectionText } from "@/components/contest/SubmissionStatus";
import { Alert } from "@/components/ui/Alert";
import type { ApiError } from "@/lib/errors";
import type { SubmissionView } from "@/lib/types";

/** Feedback de uma submissão, conforme a política do exercício (só certo/errado ou detalhe). */
export function SubmissionPanel({ s, error, attemptsLeft, penalty = 5, contest }:
  { s: SubmissionView | null; error?: ApiError | null; attemptsLeft?: number; penalty?: number; contest?: boolean }) {
  if (error) {
    return <Alert tone="neg" title={error.status === 0 ? "Sem ligação" : "Não foi possível submeter"}>
      {error.message}{error.status === 0 && " A tua resposta continua guardada aqui; tenta de novo quando voltar a ligação."}
    </Alert>;
  }
  if (!s) return null;
  if (s.status === "QUEUED" || s.status === "PROCESSING") {
    return <Alert tone="info" title="Em processamento" icon="loader">
      A tua submissão está na fila de correcção{s.queuePosition ? ` (posição ${s.queuePosition})` : ""}.{contest && " Podes continuar a trabalhar noutros problemas."}
    </Alert>;
  }
  if (s.status === "FAILED") {
    return <Alert tone="info" title="Falha de processamento">A submissão ficou guardada e será corrigida automaticamente. Não precisas de submeter de novo.</Alert>;
  }
  if (s.status === "REJECTED") return <Alert tone="neg" title="Submissão rejeitada">{rejectionText(s)}</Alert>;
  if (s.outcome === "STEP_OK") return <Alert tone="pos" title="Passo correcto">{s.summary}</Alert>;
  if (s.correct) return <Alert tone="pos" title={`${s.summary ?? "Correcto"}${s.points ? ` · +${s.points} pts` : ""}`} />;
  return (
    <Alert tone="neg" title={s.summary ?? "Resposta incorrecta"}>
      {s.hint && <>{s.hint} </>}
      {!contest && attemptsLeft !== undefined && (attemptsLeft > 0
        ? `Restam ${attemptsLeft} ${attemptsLeft === 1 ? "tentativa" : "tentativas"} · cada tentativa falhada retira ${penalty} pts.`
        : "Esgotaste as tentativas desta tarefa.")}
    </Alert>
  );
}
