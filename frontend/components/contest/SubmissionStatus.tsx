import { Badge } from "@/components/ui/Badge";
import { formatTime } from "@/lib/format";
import type { SubmissionView } from "@/lib/types";

/** Estados de submissão: Em fila → Em processamento → Corrigida/Rejeitada (sempre com texto). */
export function SubmissionBadge({ status, correct }: { status: string; correct?: boolean }) {
  switch (status) {
    case "QUEUED": return <Badge tone="info" icon="clock">Em fila</Badge>;
    case "PROCESSING": return <Badge tone="info" icon="loader">Em processamento</Badge>;
    case "GRADED": return correct ? <Badge tone="pos">Corrigida</Badge> : <Badge tone="neg">Incorrecta</Badge>;
    case "REJECTED": return <Badge tone="neg">Rejeitada</Badge>;
    case "FAILED": return <Badge tone="info" icon="refresh">Falha de processamento</Badge>;
    case "SOLVED": return <Badge tone="pos">Corrigida</Badge>;
    case "UNSOLVED": return <Badge>Por resolver</Badge>;
    default: return <Badge>{status}</Badge>;
  }
}

export function rejectionText(s: SubmissionView): string {
  if (s.errorCode === "SUBMISSION_AFTER_DEADLINE") return `Recebida às ${formatTime(s.receivedAt)}; o prazo do servidor já tinha terminado.`;
  if (s.errorCode === "ANSWER_FORMAT_INVALID") return s.summary ?? "A resposta tem um formato inválido.";
  if (s.errorCode === "ATTEMPT_CLOSED" || s.errorCode === "ALREADY_SOLVED") return "Esta tarefa já estava terminada.";
  return s.summary ?? "A submissão foi rejeitada.";
}
