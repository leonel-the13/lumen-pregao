"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { ConfirmDialog } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";
import { post } from "@/lib/api-client";

export function StartExamButton({ examId, durationMin, resumeId }: { examId: string; durationMin: number; resumeId?: string }) {
  const router = useRouter();
  const toast = useToast();
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  if (resumeId) return <Button variant="pri" onClick={() => router.push(`/exames/sessao/${resumeId}`)}>Continuar exame</Button>;
  return (
    <>
      <Button variant="pri" onClick={() => setOpen(true)}>Começar exame</Button>
      <ConfirmDialog open={open} onCancel={() => setOpen(false)} title="Começar o exame?" confirmLabel="Começar" busy={busy}
        body={`Tens ${durationMin} minutos, contados pelo relógio do servidor. No fim do tempo a submissão é automática.`}
        onConfirm={async () => {
          setBusy(true);
          try {
            const r = await post<{ sessionId: string }>(`/exams/${examId}/sessions`);
            router.push(`/exames/sessao/${r.sessionId}`);
          } catch (e) {
            toast({ tone: "neg", title: "Não foi possível começar", body: (e as Error).message });
            setBusy(false);
            setOpen(false);
          }
        }} />
    </>
  );
}
