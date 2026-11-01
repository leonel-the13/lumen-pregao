"use client";
import Link from "next/link";
import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Field, Input } from "@/components/ui/Field";
import { post } from "@/lib/api-client";
import { isApiError } from "@/lib/errors";

export function ForgotForm() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  if (sent) {
    return (
      <div className="stack">
        <Alert tone="pos" title="Verifica o teu email">
          Se existir uma conta com este email, enviámos um link para redefinir a palavra-passe. O link vale 1 hora.
        </Alert>
        <Link className="btn out" href="/entrar">Voltar a entrar</Link>
      </div>
    );
  }
  return (
    <form className="stack" style={{ gap: 16 }} noValidate onSubmit={async (e) => {
      e.preventDefault();
      setBusy(true); setError(null);
      try { await post("/auth/password/forgot", { email }); setSent(true); }
      catch (err) { setError(isApiError(err) ? (err.fields.email ?? err.message) : "Tenta de novo."); }
      finally { setBusy(false); }
    }}>
      <p className="muted">Indica o email da tua conta. Enviamos um link para escolheres uma nova palavra-passe.</p>
      <Field label="Email" error={error ?? undefined}>
        {({ id, describedBy, invalid }) => <Input id={id} aria-describedby={describedBy} invalid={invalid} type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} />}
      </Field>
      <Button variant="pri" type="submit" loading={busy} disabled={!email}>Enviar link</Button>
      <Link className="small" href="/entrar">Voltar a entrar</Link>
    </form>
  );
}
