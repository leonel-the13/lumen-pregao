"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Field, Input } from "@/components/ui/Field";
import { post } from "@/lib/api-client";
import { isApiError } from "@/lib/errors";

type Step = { status: "mfa_required" } | { status: "mfa_enrollment_required"; otpauthUri: string; secret: string };

function safeNext(next?: string) {
  return next && next.startsWith("/") && !next.startsWith("//") ? next : "/inicio";
}

export function LoginForm({ next, expired }: { next?: string; expired?: boolean }) {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [remember, setRemember] = useState(false);
  const [code, setCode] = useState("");
  const [step, setStep] = useState<Step | null>(null);
  const [error, setError] = useState<{ title: string; body: string } | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const r = await post<Step | undefined>("/auth/login", { email, password, remember });
      if (r && "status" in r) setStep(r);
      else { router.push(safeNext(next)); router.refresh(); }
    } catch (err) {
      if (isApiError(err) && err.status === 429) setError({ title: "Demasiadas tentativas", body: err.message });
      else if (isApiError(err) && err.code === "EMAIL_NOT_VERIFIED") setError({ title: "Email por confirmar", body: err.message });
      else if (isApiError(err) && err.status === 403) setError({ title: "Acesso indisponível", body: err.message });
      else if (isApiError(err) && err.status === 0) setError({ title: "Sem ligação", body: err.message });
      else setError({ title: "Dados incorrectos", body: "Verifica o email e a palavra-passe. Após 5 tentativas, espera 15 min." });
    } finally {
      setBusy(false);
    }
  }

  async function submitCode(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await post("/auth/mfa", { code });
      router.push(safeNext(next));
      router.refresh();
    } catch (err) {
      if (isApiError(err) && err.code === "MFA_SESSION_EXPIRED") { setStep(null); setCode(""); }
      setError({ title: "Código inválido", body: isApiError(err) ? err.message : "Tenta de novo." });
    } finally {
      setBusy(false);
    }
  }

  if (step) {
    return (
      <form className="stack" style={{ gap: 16 }} onSubmit={submitCode} noValidate>
        {step.status === "mfa_enrollment_required" ? (
          <Alert tone="info" title="Configura o segundo factor">
            O teu perfil exige um segundo factor. Adiciona esta conta a uma aplicação de autenticação (TOTP) e escreve o código de 6 dígitos.
            <div className="mono small" style={{ marginTop: 8, wordBreak: "break-all" }}>Chave: {step.secret}</div>
            <a className="small" href={step.otpauthUri}>Abrir na aplicação de autenticação</a>
          </Alert>
        ) : (
          <p className="muted">Escreve o código de 6 dígitos da tua aplicação de autenticação.</p>
        )}
        <Field label="Código de verificação" error={error?.body}>
          {({ id, describedBy, invalid }) => (
            <Input id={id} aria-describedby={describedBy} invalid={invalid} inputMode="numeric" autoComplete="one-time-code"
              maxLength={6} value={code} onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))} autoFocus />
          )}
        </Field>
        <Button variant="pri" type="submit" loading={busy} disabled={code.length !== 6}>Confirmar</Button>
      </form>
    );
  }

  return (
    <form className="stack" style={{ gap: 16 }} onSubmit={submit} noValidate>
      {expired && <Alert tone="info" title="A tua sessão expirou">O teu rascunho foi guardado. Entra de novo para continuar.</Alert>}
      <Field label="Email institucional">
        {({ id }) => <Input id={id} type="email" autoComplete="username" required value={email} onChange={(e) => setEmail(e.target.value)} />}
      </Field>
      <Field label="Palavra-passe">
        {({ id }) => <Input id={id} type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />}
      </Field>
      <label className="check"><input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} />Manter sessão neste dispositivo</label>
      {error && <Alert tone="neg" title={error.title}>{error.body}</Alert>}
      <Button variant="pri" type="submit" loading={busy}>Entrar</Button>
      <p className="small muted">Novo por aqui? <Link href="/registo">Cria conta</Link></p>
    </form>
  );
}
