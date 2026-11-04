"use client";
import { Badge } from "@/components/ui/Badge";
import { formatTime } from "@/lib/format";
import type { Locale } from "@/lib/i18n/config";
import { useT } from "@/lib/i18n/client";
import type { T } from "@/lib/i18n/translate";
import type { SubmissionView } from "@/lib/types";

/** Estados de submissão: Em fila → Em processamento → Corrigida/Rejeitada (sempre com texto). */
export function SubmissionBadge({ status, correct }: { status: string; correct?: boolean }) {
  const t = useT();
  switch (status) {
    case "QUEUED": return <Badge tone="info" icon="clock">{t("task.submission.badge.QUEUED")}</Badge>;
    case "PROCESSING": return <Badge tone="info" icon="loader">{t("task.submission.badge.PROCESSING")}</Badge>;
    case "GRADED": return correct ? <Badge tone="pos">{t("task.submission.badge.GRADED")}</Badge> : <Badge tone="neg">{t("task.submission.badge.INCORRECT")}</Badge>;
    case "REJECTED": return <Badge tone="neg">{t("task.submission.badge.REJECTED")}</Badge>;
    case "FAILED": return <Badge tone="info" icon="refresh">{t("task.submission.badge.FAILED")}</Badge>;
    case "SOLVED": return <Badge tone="pos">{t("task.submission.badge.GRADED")}</Badge>;
    case "UNSOLVED": return <Badge>{t("task.submission.badge.UNSOLVED")}</Badge>;
    default: return <Badge>{status}</Badge>;
  }
}

export function rejectionText(s: SubmissionView, t: T, locale: Locale): string {
  if (s.errorCode === "SUBMISSION_AFTER_DEADLINE") return t("task.submission.afterDeadline", { time: formatTime(s.receivedAt, locale) });
  if (s.errorCode === "ANSWER_FORMAT_INVALID") return s.summary ?? t("task.submission.invalidFormat");
  if (s.errorCode === "ATTEMPT_CLOSED" || s.errorCode === "ALREADY_SOLVED") return t("task.submission.alreadyDone");
  return s.summary ?? t("task.submission.wasRejected");
}
