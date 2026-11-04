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
import { RecoveryCodesView } from "./RecoveryCodesView";

type Step = { status: "mfa_required" } | { status: "mfa_enrollment_required"; otpauthUri: string; secret: string };

function safeNext(next?: string) {
  return next && next.startsWith("/") && !next.startsWith("//") ? next : "/inicio";
}

export function LoginForm({ next, expired, passwordSet, verified }: { next?: string; expired?: boolean; passwordSet?: boolean; verified?: boolean }) {
  const router = useRouter();
  const t = useT();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [remember, setRemember] = useState(false);
  const [code, setCode] = useState("");
  const [step, setStep] = useState<Step | null>(null);
  const [error, setError] = useState<{ title: string; body: string } | null>(null);
  const [busy, setBusy] = useState(false);
  const [useRecovery, setUseRecovery] = useState(false);
  const [newCodes, setNewCodes] = useState<string[] | null>(null);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const r = await post<Step | undefined>("/auth/login", { email, password, remember });
      if (r && "status" in r) setStep(r);
      else { router.push(safeNext(next)); router.refresh(); }
    } catch (err) {
      if (isApiError(err) && err.status === 429) setError({ title: t("auth.login.tooMany"), body: err.message });
      else if (isApiError(err) && err.code === "EMAIL_NOT_VERIFIED") setError({ title: t("auth.login.notVerified"), body: err.message });
      else if (isApiError(err) && err.code === "INVITE_PENDING") setError({ title: t("auth.login.invitePending"), body: err.message });
      else if (isApiError(err) && err.status === 403) setError({ title: t("auth.login.unavailable"), body: err.message });
      else if (isApiError(err) && err.status === 0) setError({ title: t("auth.login.offline"), body: err.message });
      else setError({ title: t("auth.login.wrong"), body: t("auth.login.wrongBody") });
    } finally {
      setBusy(false);
    }
  }

  async function submitCode(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const r = await post<{ recoveryCodes?: string[] } | undefined>("/auth/mfa", { code });
      if (r?.recoveryCodes?.length) { setNewCodes(r.recoveryCodes); return; }
      router.push(safeNext(next));
      router.refresh();
    } catch (err) {
      if (isApiError(err) && err.code === "MFA_SESSION_EXPIRED") { setStep(null); setCode(""); }
      setError({ title: t("auth.login.badCode"), body: isApiError(err) ? err.message : t("auth.login.tryAgain") });
    } finally {
      setBusy(false);
    }
  }

  if (newCodes) {
    return (
      <div className="stack" style={{ gap: 16 }}>
        <Alert tone="pos" title={t("auth.login.mfaDone")}>{t("auth.login.mfaDoneBody")}</Alert>
        <RecoveryCodesView codes={newCodes} />
        <Button variant="pri" onClick={() => { router.push(safeNext(next)); router.refresh(); }}>{t("auth.login.codesSavedContinue")}</Button>
      </div>
    );
  }

  const codeValid = useRecovery ? /^[A-Za-z0-9]{4}-?[A-Za-z0-9]{4}$/.test(code.trim()) : code.length === 6;

  if (step) {
    return (
      <form className="stack" style={{ gap: 16 }} onSubmit={submitCode} noValidate>
        {step.status === "mfa_enrollment_required" ? (
          <Alert tone="info" title={t("auth.login.setupMfa")}>
            {t("auth.login.setupMfaBody")}
            <div className="mono small" style={{ marginTop: 8, wordBreak: "break-all" }}>{t("auth.login.key", { key: step.secret })}</div>
            <a className="small" href={step.otpauthUri}>{t("auth.login.openAuthApp")}</a>
          </Alert>
        ) : (
          <p className="muted">{useRecovery ? t("auth.login.recoveryPrompt") : t("auth.login.codePrompt")}</p>
        )}
        <Field label={useRecovery ? t("auth.login.recoveryCode") : t("auth.login.verificationCode")} error={error?.body}>
          {({ id, describedBy, invalid }) => useRecovery ? (
            <Input key="rec" id={id} aria-describedby={describedBy} invalid={invalid} autoComplete="off" autoCapitalize="characters"
              maxLength={9} value={code} onChange={(e) => setCode(e.target.value.toUpperCase())} autoFocus className="mono" />
          ) : (
            <Input key="otp" id={id} aria-describedby={describedBy} invalid={invalid} inputMode="numeric" autoComplete="one-time-code"
              maxLength={6} value={code} onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))} autoFocus />
          )}
        </Field>
        <Button variant="pri" type="submit" loading={busy} disabled={!codeValid}>{t("common.confirm")}</Button>
        {step.status === "mfa_required" && (
          <button type="button" className="btn ghost sm" onClick={() => { setUseRecovery((u) => !u); setCode(""); setError(null); }}>
            {useRecovery ? t("auth.login.useAppCode") : t("auth.login.lostPhone")}
          </button>
        )}
      </form>
    );
  }

  return (
    <form className="stack" style={{ gap: 16 }} onSubmit={submit} noValidate>
      {expired && <Alert tone="info" title={t("auth.login.expired")}>{t("auth.login.expiredBody")}</Alert>}
      {passwordSet && <Alert tone="pos" title={t("auth.login.passwordSet")}>{t("auth.login.passwordSetBody")}</Alert>}
      {verified && <Alert tone="pos" title={t("auth.login.verified")}>{t("auth.login.verifiedBody")}</Alert>}
      <Field label={t("auth.login.email")}>
        {({ id }) => <Input id={id} type="email" autoComplete="username" required value={email} onChange={(e) => setEmail(e.target.value)} />}
      </Field>
      <Field label={t("auth.login.password")}>
        {({ id }) => <PasswordInput id={id} autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />}
      </Field>
      <div className="row" style={{ justifyContent: "space-between" }}>
        <label className="check"><input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} />{t("auth.login.remember")}</label>
        <Link className="small" href="/esqueci">{t("auth.login.forgot")}</Link>
      </div>
      {error && <Alert tone="neg" title={error.title}>{error.body}</Alert>}
      <Button variant="pri" type="submit" loading={busy}>{t("auth.login.submit")}</Button>
      <p className="small muted">{t("auth.login.newHere")} <Link href="/registo">{t("auth.login.createAccount")}</Link></p>
    </form>
  );
}
