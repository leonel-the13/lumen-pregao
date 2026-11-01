"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { post } from "@/lib/api-client";
import { isApiError } from "@/lib/errors";

export function VerifyEmail({ token, email }: { token?: string; email?: string }) {
  const router = useRouter();
  const [state, setState] = useState<"waiting" | "verifying" | "ok" | "expired" | "invalid">(token ? "verifying" : "waiting");
  const [message, setMessage] = useState("");
  const [cooldown, setCooldown] = useState(45);
  const started = useRef(false);

  useEffect(() => {
    if (!token || started.current) return;
    started.current = true;
    post<{ signedIn: boolean }>("/auth/verify", { token })
      .then((r) => {
        setState("ok");
        setTimeout(() => { router.push(r.signedIn ? "/inicio" : "/entrar"); router.refresh(); }, 1200);
      })
      .catch((e) => {
        setState(isApiError(e) && e.code === "TOKEN_EXPIRED" ? "expired" : "invalid");
        setMessage(isApiError(e) ? e.message : "O link é inválido.");
      });
  }, [token, router]);

  useEffect(() => {
    if (state !== "waiting" || cooldown <= 0) return;
    const id = setTimeout(() => setCooldown((c) => c - 1), 1000);
    return () => clearTimeout(id);
  }, [state, cooldown]);

  if (state === "verifying") return <p className="muted" aria-live="polite">A verificar o teu email…</p>;
  if (state === "ok") return <Alert tone="pos" title="Email verificado">A entrar na plataforma…</Alert>;
  if (state === "expired" || state === "invalid") {
    return (
      <div className="stack">
        <Alert tone="neg" title={state === "expired" ? "Link expirado" : "Link inválido"}>{message}</Alert>
        <Link className="btn out" href="/verificar">Pedir um novo link</Link>
      </div>
    );
  }
  return (
    <div className="stack" style={{ gap: 16 }}>
      <p className="muted">
        Enviámos um link para <b>{email ?? "o teu email institucional"}</b>. Abre-o neste dispositivo para activar a conta.
        Os links valem 24 h <span className="tbc">a confirmar</span>.
      </p>
      {email && (
        <Button variant="out" disabled={cooldown > 0}
          onClick={async () => { setCooldown(45); try { await post("/auth/resend-verification", { email }); } catch { /* resposta genérica */ } }}>
          {cooldown > 0 ? `Reenviar email (00:${String(cooldown).padStart(2, "0")})` : "Reenviar email"}
        </Button>
      )}
    </div>
  );
}
