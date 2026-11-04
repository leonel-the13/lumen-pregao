"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { get, post } from "@/lib/api-client";
import { isApiError } from "@/lib/errors";
import { useT } from "@/lib/i18n/client";

export function VerifyEmail({ token, email }: { token?: string; email?: string }) {
  const router = useRouter();
  const t = useT();
  const [state, setState] = useState<"waiting" | "verifying" | "ok" | "expired" | "invalid">(token ? "verifying" : "waiting");
  const [message, setMessage] = useState("");
  const [cooldown, setCooldown] = useState(0);
  const [resendNote, setResendNote] = useState<string | null>(null);
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
        setMessage(isApiError(e) ? e.message : t("auth.verify.invalid"));
      });
  }, [token, router, t]);

  // Este browser fez o registo: pergunta ao servidor se a conta já foi confirmada (pode ser noutro dispositivo).
  // Quando foi, entra sozinho. A contagem do «Reenviar» também vem do servidor, por isso não recomeça com o refresh.
  useEffect(() => {
    if (state !== "waiting") return;
    let stop = false;
    const check = async () => {
      if (stop || document.visibilityState !== "visible") return;
      try {
        const r = await get<{ verified: boolean; signedIn: boolean; resendInSeconds: number }>("/auth/registration-status");
        if (stop) return;
        if (r.verified) {
          stop = true;
          setState("ok");
          setTimeout(() => { router.push(r.signedIn ? "/inicio" : "/entrar?verificado=1"); router.refresh(); }, 1200);
          return;
        }
        setCooldown((c) => (Math.abs(c - r.resendInSeconds) > 2 ? r.resendInSeconds : c));
      } catch { /* sem ligação: tenta na próxima volta */ }
    };
    check();
    const id = setInterval(check, 3000);
    const onVisible = () => { if (document.visibilityState === "visible") check(); };
    document.addEventListener("visibilitychange", onVisible);
    return () => { stop = true; clearInterval(id); document.removeEventListener("visibilitychange", onVisible); };
  }, [state, router]);

  useEffect(() => {
    if (state !== "waiting" || cooldown <= 0) return;
    const id = setTimeout(() => setCooldown((c) => c - 1), 1000);
    return () => clearTimeout(id);
  }, [state, cooldown]);

  if (state === "verifying") return <p className="muted" aria-live="polite">{t("auth.verify.verifying")}</p>;
  if (state === "ok") return <Alert tone="pos" title={t("auth.verify.ok")}>{t("auth.verify.entering")}</Alert>;
  if (state === "expired" || state === "invalid") {
    return (
      <div className="stack">
        <Alert tone="neg" title={state === "expired" ? t("auth.verify.expired") : t("auth.verify.invalidTitle")}>{message}</Alert>
        <Link className="btn out" href="/verificar">{t("auth.verify.newLink")}</Link>
      </div>
    );
  }
  return (
    <div className="stack" style={{ gap: 16 }}>
      <p className="muted">
        {t("auth.verify.sentPre")}<b>{email ?? t("auth.verify.yourEmail")}</b>{t("auth.verify.sentPost")}
      </p>
      <p className="small muted row" style={{ gap: 8 }} aria-live="polite"><Icon name="loader" className="icon spin" />{t("auth.verify.waiting")}</p>
      {email && (
        <Button variant="out" disabled={cooldown > 0}
          onClick={async () => {
            setCooldown(45);
            setResendNote(null);
            try {
              await post("/auth/resend-verification", { email });
              setResendNote(t("auth.verify.resent"));
            } catch (e) {
              setResendNote(isApiError(e) ? e.message : t("auth.verify.resendFailed"));
            }
          }}>
          {cooldown > 0 ? t("auth.verify.resendIn", { time: `00:${String(cooldown).padStart(2, "0")}` }) : t("auth.verify.resend")}
        </Button>
      )}
      {resendNote && <p className="small muted" role="status">{resendNote}</p>}
    </div>
  );
}
