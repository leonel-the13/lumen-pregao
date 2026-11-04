"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Field, Input, PasswordInput } from "@/components/ui/Field";
import { post } from "@/lib/api-client";
import { isApiError } from "@/lib/errors";
import { useT } from "@/lib/i18n/client";

/** Define a palavra-passe pelo link do email (convite ou «Esqueci-me»). */
export function ResetForm({ token, invite }: { token?: string; invite?: boolean }) {
  const router = useRouter();
  const t = useT();
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<{ field?: string; message: string; expired?: boolean } | null>(null);
  const [busy, setBusy] = useState(false);
  if (!token) {
    return <Alert tone="neg" title={t("auth.reset.incompleteTitle")}>{t("auth.reset.incompletePre")}<Link href="/esqueci">{t("auth.reset.forgotLink")}</Link>{t("auth.reset.incompletePost")}</Alert>;
  }
  const mismatch = confirm.length > 0 && confirm !== password;
  return (
    <form className="stack" style={{ gap: 16 }} noValidate onSubmit={async (e) => {
      e.preventDefault();
      if (password.length < 12) { setError({ field: "password", message: t("auth.reset.tooShort") }); return; }
      if (mismatch) return;
      setBusy(true); setError(null);
      try {
        await post("/auth/password/reset", { token, password });
        router.push("/entrar?definida=1");
      } catch (err) {
        if (isApiError(err)) {
          const expired = ["TOKEN_EXPIRED", "TOKEN_USED", "TOKEN_INVALID"].includes(err.code);
          setError({ field: err.fields.password ? "password" : expired ? undefined : "password", message: err.fields.password ?? err.message, expired });
        } else setError({ message: t("auth.login.tryAgain") });
      } finally { setBusy(false); }
    }}>
      <p className="muted">
        {invite ? t("auth.reset.inviteIntro") : t("auth.reset.resetIntro")}
      </p>
      <Field label={t("auth.reset.newPassword")} hint={t("auth.reset.newPasswordHint")} error={error?.field === "password" ? error.message : undefined}>
        {({ id, describedBy, invalid }) => <PasswordInput id={id} aria-describedby={describedBy} invalid={invalid} autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} />}
      </Field>
      <Field label={t("auth.reset.confirm")} error={mismatch ? t("auth.reset.mismatch") : undefined}>
        {({ id, describedBy, invalid }) => <PasswordInput id={id} aria-describedby={describedBy} invalid={invalid} autoComplete="new-password" value={confirm} onChange={(e) => setConfirm(e.target.value)} />}
      </Field>
      {error && !error.field && (
        <Alert tone="neg" title={error.expired ? t("auth.reset.expiredTitle") : t("auth.reset.failedTitle")}>
          {error.message} {error.expired && (invite ? t("auth.reset.askResend") : <Link href="/esqueci">{t("auth.reset.newLink")}</Link>)}
        </Alert>
      )}
      <Button variant="pri" type="submit" loading={busy} disabled={!password || !confirm || mismatch}>{invite ? t("auth.reset.activate") : t("auth.reset.save")}</Button>
    </form>
  );
}
