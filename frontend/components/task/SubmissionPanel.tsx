"use client";
import { rejectionText } from "@/components/contest/SubmissionStatus";
import { Alert } from "@/components/ui/Alert";
import type { ApiError } from "@/lib/errors";
import { useLocale, useT } from "@/lib/i18n/client";
import type { SubmissionView } from "@/lib/types";

/** Feedback de uma submissão, conforme a política do exercício (só certo/errado ou detalhe). */
export function SubmissionPanel({ s, error, attemptsLeft, penalty = 5, contest }:
  { s: SubmissionView | null; error?: ApiError | null; attemptsLeft?: number; penalty?: number; contest?: boolean }) {
  const t = useT();
  const locale = useLocale();
  if (error) {
    return <Alert tone="neg" title={error.status === 0 ? t("task.submission.offline") : t("task.submission.failed")}>
      {error.message}{error.status === 0 && t("task.submission.offlineBody")}
    </Alert>;
  }
  if (!s) return null;
  if (s.status === "QUEUED" || s.status === "PROCESSING") {
    return <Alert tone="info" title={t("task.submission.processing")} icon="loader">
      {t("task.submission.queued", { pos: s.queuePosition ? t("task.submission.queuePos", { n: s.queuePosition }) : "" })}{contest && t("task.submission.keepWorking")}
    </Alert>;
  }
  if (s.status === "FAILED") {
    return <Alert tone="info" title={t("task.submission.processingFailed")}>{t("task.submission.processingFailedBody")}</Alert>;
  }
  if (s.status === "REJECTED") return <Alert tone="neg" title={t("task.submission.rejected")}>{rejectionText(s, t, locale)}</Alert>;
  if (s.outcome === "STEP_OK") return <Alert tone="pos" title={t("task.submission.stepOk")}>{s.summary}</Alert>;
  if (s.correct) return <Alert tone="pos" title={`${s.summary ?? t("task.submission.correct")}${s.points ? ` · +${s.points} pts` : ""}`} />;
  return (
    <Alert tone="neg" title={s.summary ?? t("task.submission.incorrect")}>
      {s.hint && <>{s.hint} </>}
      {!contest && attemptsLeft !== undefined && (attemptsLeft > 0
        ? t(attemptsLeft === 1 ? "task.submission.attemptLeft" : "task.submission.attemptsLeft", { n: attemptsLeft, p: penalty })
        : t("task.submission.exhausted"))}
    </Alert>
  );
}
