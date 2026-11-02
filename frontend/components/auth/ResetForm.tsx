"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Field, Input } from "@/components/ui/Field";
import { post } from "@/lib/api-client";
import { isApiError } from "@/lib/errors";

/** Define a palavra-passe pelo link do email (convite ou «Esqueci-me»). */
export function ResetForm({ token, invite }: { token?: string; invite?: boolean }) {
  const router = useRouter();
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<{ field?: string; message: string; expired?: boolean } | null>(null);
  const [busy, setBusy] = useState(false);
  if (!token) {
    return <Alert tone="neg" title="Link incompleto">Abre o link exactamente como chegou no email, ou pede um novo em <Link href="/esqueci">Esqueci-me</Link>.</Alert>;
  }
  const mismatch = confirm.length > 0 && confirm !== password;
  return (
    <form className="stack" style={{ gap: 16 }} noValidate onSubmit={async (e) => {
      e.preventDefault();
      if (password.length < 12) { setError({ field: "password", message: "A palavra-passe tem de ter pelo menos 12 caracteres." }); return; }
      if (mismatch) return;
      setBusy(true); setError(null);
      try {
        await post("/auth/password/reset", { token, password });
        router.push("/entrar?definida=1");
      } catch (err) {
        if (isApiError(err)) {
          const expired = ["TOKEN_EXPIRED", "TOKEN_USED", "TOKEN_INVALID"].includes(err.code);
          setError({ field: err.fields.password ? "password" : expired ? undefined : "password", message: err.fields.password ?? err.message, expired });
        } else setError({ message: "Tenta de novo." });
      } finally { setBusy(false); }
    }}>
      <p className="muted">
        {invite ? "Bem-vindo ao Lúmen Lab. Escolhe a tua palavra-passe para activar a conta." : "Escolhe uma nova palavra-passe. As sessões abertas noutros dispositivos terminam."}
      </p>
      <Field label="Nova palavra-passe" hint="12+ caracteres. Evita palavras óbvias e o teu nome." error={error?.field === "password" ? error.message : undefined}>
        {({ id, describedBy, invalid }) => <Input id={id} aria-describedby={describedBy} invalid={invalid} type="password" autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} />}
      </Field>
      <Field label="Confirmar palavra-passe" error={mismatch ? "As palavras-passe não coincidem." : undefined}>
        {({ id, describedBy, invalid }) => <Input id={id} aria-describedby={describedBy} invalid={invalid} type="password" autoComplete="new-password" value={confirm} onChange={(e) => setConfirm(e.target.value)} />}
      </Field>
      {error && !error.field && (
        <Alert tone="neg" title={error.expired ? "Link inválido ou expirado" : "Não foi possível concluir"}>
          {error.message} {error.expired && (invite ? "Pede à administração que reenvie o convite." : <Link href="/esqueci">Pedir um novo link</Link>)}
        </Alert>
      )}
      <Button variant="pri" type="submit" loading={busy} disabled={!password || !confirm || mismatch}>{invite ? "Activar conta" : "Guardar palavra-passe"}</Button>
    </form>
  );
}
