"use client";
import Link from "next/link";
import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Field, Input } from "@/components/ui/Field";
import { post } from "@/lib/api-client";
import { isApiError } from "@/lib/errors";
import { useT } from "@/lib/i18n/client";

export function ForgotForm() {
  const t = useT();
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  if (sent) {
    return (
      <div className="stack">
        <Alert tone="pos" title={t("auth.forgot.sentTitle")}>{t("auth.forgot.sentBody")}</Alert>
        <Link className="btn out" href="/entrar">{t("auth.forgot.backToSignIn")}</Link>
      </div>
    );
  }
  return (
    <form className="stack" style={{ gap: 16 }} noValidate onSubmit={async (e) => {
      e.preventDefault();
      setBusy(true); setError(null);
      try { await post("/auth/password/forgot", { email }); setSent(true); }
      catch (err) { setError(isApiError(err) ? (err.fields.email ?? err.message) : t("auth.login.tryAgain")); }
      finally { setBusy(false); }
    }}>
      <p className="muted">{t("auth.forgot.intro")}</p>
      <Field label={t("auth.forgot.email")} error={error ?? undefined}>
        {({ id, describedBy, invalid }) => <Input id={id} aria-describedby={describedBy} invalid={invalid} type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} />}
      </Field>
      <Button variant="pri" type="submit" loading={busy} disabled={!email}>{t("auth.forgot.send")}</Button>
      <Link className="small" href="/entrar">{t("auth.forgot.backToSignIn")}</Link>
    </form>
  );
}
