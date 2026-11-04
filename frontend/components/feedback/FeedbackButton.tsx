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
import { useT } from "@/lib/i18n/client";
import { StarRating } from "./StarRating";

const CATEGORIES = ["SUGGESTION", "BUG", "CONTENT", "PRAISE", "OTHER"] as const;

/** Feedback sobre a plataforma: chega à administração com a página onde estavas. */
export function FeedbackButton({ compact }: { compact?: boolean }) {
  const pathname = usePathname();
  const toast = useToast();
  const t = useT();
  const [open, setOpen] = useState(false);
  const [category, setCategory] = useState("SUGGESTION");
  const [rating, setRating] = useState<number | null>(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function send() {
    if (message.trim().length < 5) { setError(t("shell.feedback.tooShort")); return; }
    setBusy(true);
    setError(null);
    try {
      await post("/feedback", { category, rating, message: message.trim(), page: pathname });
      toast({ tone: "pos", title: t("shell.feedback.thanks"), body: t("shell.feedback.thanksBody") });
      setOpen(false);
      setMessage("");
      setRating(null);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : t("shell.feedback.failed"));
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <button type="button" className={compact ? "btn ghost icon-btn" : "btn ghost"} aria-label={compact ? t("shell.feedback.open") : undefined}
        onClick={() => setOpen(true)}>
        <Icon name="chat" />{!compact && <span>{t("shell.feedback.button")}</span>}
      </button>
      <Modal open={open} onClose={() => setOpen(false)} title={t("shell.feedback.title")} labelledBy="feedback-title"
        actions={<>
          <Button variant="out" onClick={() => setOpen(false)}>{t("common.cancel")}</Button>
          <Button variant="pri" onClick={send} loading={busy}>{t("common.send")}</Button>
        </>}>
        <div className="stack" style={{ color: "var(--text)" }}>
          <p className="muted small" style={{ margin: 0 }}>{t("shell.feedback.intro")}</p>
          <Field label={t("shell.feedback.type")}>
            {({ id }) => (
              <Select id={id} value={category} onChange={(e) => setCategory(e.target.value)}>
                {CATEGORIES.map((c) => <option key={c} value={c}>{t(`shell.feedback.categories.${c}`)}</option>)}
              </Select>
            )}
          </Field>
          <div className="stack" style={{ gap: 6 }}>
            <span className="label">{t("shell.feedback.rating")}</span>
            <StarRating value={rating} onChange={setRating} label={t("shell.feedback.ratingLabel")} />
          </div>
          <Field label={t("shell.feedback.message")} error={error ?? undefined} hint={t("shell.feedback.messageHint")}>
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
