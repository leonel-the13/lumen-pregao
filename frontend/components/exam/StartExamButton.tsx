"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { ConfirmDialog } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";
import { post } from "@/lib/api-client";
import { useT } from "@/lib/i18n/client";

export function StartExamButton({ examId, durationMin, resumeId }: { examId: string; durationMin: number; resumeId?: string }) {
  const router = useRouter();
  const toast = useToast();
  const t = useT();
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  if (resumeId) return <Button variant="pri" onClick={() => router.push(`/exames/sessao/${resumeId}`)}>{t("exam.start.resume")}</Button>;
  return (
    <>
      <Button variant="pri" onClick={() => setOpen(true)}>{t("exam.start.start")}</Button>
      <ConfirmDialog open={open} onCancel={() => setOpen(false)} title={t("exam.start.confirmTitle")} confirmLabel={t("exam.start.confirm")} busy={busy}
        body={t("exam.start.confirmBody", { n: durationMin })}
        onConfirm={async () => {
          setBusy(true);
          try {
            const r = await post<{ sessionId: string }>(`/exams/${examId}/sessions`);
            router.push(`/exames/sessao/${r.sessionId}`);
          } catch (e) {
            toast({ tone: "neg", title: t("exam.start.failed"), body: (e as Error).message });
            setBusy(false);
            setOpen(false);
          }
        }} />
    </>
  );
}
