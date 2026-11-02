"use client";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Field, Select, TextArea } from "@/components/ui/Field";
import { Icon } from "@/components/ui/Icon";
import { Modal } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";
import { post } from "@/lib/api-client";
import { ApiError } from "@/lib/errors";
import { StarRating } from "./StarRating";

const CATEGORIES = [
  { value: "SUGGESTION", label: "Sugestão" },
  { value: "BUG", label: "Algo não funciona" },
  { value: "CONTENT", label: "Conteúdo (enunciado, correcção, referência)" },
  { value: "PRAISE", label: "Elogio" },
  { value: "OTHER", label: "Outro" },
];

/** Feedback sobre a plataforma: chega à administração com a página onde estavas. */
export function FeedbackButton({ compact }: { compact?: boolean }) {
  const pathname = usePathname();
  const toast = useToast();
  const [open, setOpen] = useState(false);
  const [category, setCategory] = useState("SUGGESTION");
  const [rating, setRating] = useState<number | null>(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function send() {
    if (message.trim().length < 5) { setError("Escreve pelo menos 5 caracteres."); return; }
    setBusy(true);
    setError(null);
    try {
      await post("/feedback", { category, rating, message: message.trim(), page: pathname });
      toast({ tone: "pos", title: "Obrigado pelo feedback!", body: "A equipa da AE lê todas as mensagens." });
      setOpen(false);
      setMessage("");
      setRating(null);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Não foi possível enviar. Tenta de novo.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <button type="button" className={compact ? "btn ghost icon-btn" : "btn ghost"} aria-label={compact ? "Dar feedback" : undefined}
        onClick={() => setOpen(true)}>
        <Icon name="chat" />{!compact && <span>Feedback</span>}
      </button>
      <Modal open={open} onClose={() => setOpen(false)} title="Dar feedback" labelledBy="feedback-title"
        actions={<>
          <Button variant="out" onClick={() => setOpen(false)}>Cancelar</Button>
          <Button variant="pri" onClick={send} loading={busy}>Enviar</Button>
        </>}>
        <div className="stack" style={{ color: "var(--text)" }}>
          <p className="muted small" style={{ margin: 0 }}>O que achas do Lúmen Lab? Sugestões, erros e elogios ajudam-nos a melhorar.</p>
          <Field label="Tipo">
            {({ id }) => (
              <Select id={id} value={category} onChange={(e) => setCategory(e.target.value)}>
                {CATEGORIES.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
              </Select>
            )}
          </Field>
          <div className="stack" style={{ gap: 6 }}>
            <span className="label">Avaliação geral (opcional)</span>
            <StarRating value={rating} onChange={setRating} label="Avaliação geral da plataforma" />
          </div>
          <Field label="Mensagem" error={error ?? undefined} hint="Não incluas palavras-passe nem dados pessoais de terceiros.">
            {({ id, describedBy }) => (
              <TextArea id={id} aria-describedby={describedBy} rows={5} maxLength={2000} value={message}
                invalid={!!error} onChange={(e) => setMessage(e.target.value)} />
            )}
          </Field>
        </div>
      </Modal>
    </>
  );
}
