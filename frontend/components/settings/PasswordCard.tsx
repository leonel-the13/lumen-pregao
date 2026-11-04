"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Field, PasswordInput } from "@/components/ui/Field";
import { useToast } from "@/components/ui/Toast";
import { post } from "@/lib/api-client";
import { isApiError } from "@/lib/errors";
import { useT } from "@/lib/i18n/client";

/** Mudar a palavra-passe com a sessão iniciada. O servidor termina todas as sessões, por isso volta-se a entrar. */
export function PasswordCard() {
  const router = useRouter();
  const toast = useToast();
  const t = useT();
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [confirm, setConfirm] = useState("");
  const [errors, setErrors] = useState<{ current?: string; next?: string; general?: string }>({});
  const [busy, setBusy] = useState(false);
  const mismatch = confirm.length > 0 && confirm !== next;
  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (next.length < 12) { setErrors({ next: t("settings.settings.tooShort") }); return; }
    setBusy(true); setErrors({});
    try {
      await post("/me/password", { currentPassword: current, newPassword: next });
      toast({ tone: "pos", title: t("settings.settings.passwordChanged"), body: t("settings.settings.passwordChangedBody") });
      router.push("/entrar");
      router.refresh();
    } catch (err) {
      if (isApiError(err) && err.code === "WRONG_PASSWORD") setErrors({ current: err.message });
      else if (isApiError(err) && (err.fields.newPassword || err.code?.startsWith("PASSWORD"))) setErrors({ next: err.fields.newPassword ?? err.message });
      else setErrors({ general: isApiError(err) ? err.message : t("common.couldNotSave") });
    } finally { setBusy(false); }
  };
  return (
    <form className="stack" onSubmit={submit} style={{ borderTop: "1px solid var(--line)", paddingTop: 14, marginTop: 4 }}>
      <b className="small">{t("settings.settings.password")}</b>
      <p className="small muted" style={{ margin: 0 }}>{t("settings.settings.passwordIntro")}</p>
      <Field label={t("settings.settings.currentPassword")} error={errors.current}>
        {({ id, describedBy, invalid }) => <PasswordInput id={id} aria-describedby={describedBy} invalid={invalid} autoComplete="current-password" value={current} onChange={(e) => setCurrent(e.target.value)} />}
      </Field>
      <Field label={t("settings.settings.newPassword")} hint={t("settings.settings.newPasswordHint")} error={errors.next}>
        {({ id, describedBy, invalid }) => <PasswordInput id={id} aria-describedby={describedBy} invalid={invalid} autoComplete="new-password" value={next} onChange={(e) => setNext(e.target.value)} />}
      </Field>
      <Field label={t("settings.settings.confirmPassword")} error={mismatch ? t("settings.settings.mismatch") : undefined}>
        {({ id, describedBy, invalid }) => <PasswordInput id={id} aria-describedby={describedBy} invalid={invalid} autoComplete="new-password" value={confirm} onChange={(e) => setConfirm(e.target.value)} />}
      </Field>
      {errors.general && <div className="alert neg" role="alert">{errors.general}</div>}
      <div><Button variant="out" type="submit" loading={busy} disabled={!current || !next || !confirm || mismatch}>{t("settings.settings.changePassword")}</Button></div>
    </form>
  );
}
