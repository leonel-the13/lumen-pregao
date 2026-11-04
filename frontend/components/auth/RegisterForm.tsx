"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Field, Input, PasswordInput, Select } from "@/components/ui/Field";
import { post } from "@/lib/api-client";
import { isApiError } from "@/lib/errors";
import { useT } from "@/lib/i18n/client";

export function RegisterForm({ courses }: { courses: { id: string; name: string }[] }) {
  const router = useRouter();
  const t = useT();
  const [form, setForm] = useState({ name: "", email: "", courseId: "", year: "", password: "", consent: false });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [general, setGeneral] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const set = (k: keyof typeof form) => (v: string | boolean) => setForm((f) => ({ ...f, [k]: v }));

  function validate() {
    const e: Record<string, string> = {};
    if (form.name.trim().length < 2) e.name = t("auth.register.nameRequired");
    if (!/^\S+@\S+\.\S+$/.test(form.email)) e.email = t("auth.register.emailInvalid");
    else if (/@isptec\.co\.ao$/i.test(form.email.trim()) && !/^[0-9]{8}@/.test(form.email.trim()))
      e.email = t("auth.register.emailStudent");
    if (!form.courseId) e.courseId = t("auth.register.courseRequired");
    if (!form.year) e.year = t("auth.register.yearRequired");
    if (form.password.length < 12) e.password = t("auth.register.passwordShort");
    if (!form.consent) e.consent = t("auth.register.consentRequired");
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  async function submit(ev: React.FormEvent) {
    ev.preventDefault();
    setGeneral(null);
    if (!validate()) return;
    setBusy(true);
    try {
      await post("/auth/register", { ...form, year: Number(form.year) });
      router.push(`/verificar?email=${encodeURIComponent(form.email)}`);
    } catch (err) {
      if (isApiError(err)) {
        if (err.code === "EMAIL_DOMAIN_NOT_ALLOWED" || err.code === "EMAIL_NOT_STUDENT") setErrors({ email: err.message });
        else if (err.code === "PASSWORD_WEAK" || err.code === "PASSWORD_TOO_SHORT") setErrors({ password: err.message });
        else if (Object.keys(err.fields).length) setErrors(err.fields);
        else setGeneral(err.message);
      } else setGeneral(t("auth.register.failed"));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="stack" style={{ gap: 16 }} onSubmit={submit} noValidate>
      <Field label={t("auth.register.name")} error={errors.name}>
        {({ id, describedBy, invalid }) => <Input id={id} aria-describedby={describedBy} invalid={invalid} autoComplete="name" value={form.name} onChange={(e) => set("name")(e.target.value)} />}
      </Field>
      <Field label={t("auth.register.email")} hint={t("auth.register.emailHint")} error={errors.email}>
        {({ id, describedBy, invalid }) => <Input id={id} aria-describedby={describedBy} invalid={invalid} type="email" autoComplete="email" value={form.email} onChange={(e) => set("email")(e.target.value)} />}
      </Field>
      <div className="grid" style={{ gridTemplateColumns: "2fr 1fr", gap: 12 }}>
        <Field label={t("auth.register.course")} error={errors.courseId}>
          {({ id, describedBy, invalid }) => (
            <Select id={id} aria-describedby={describedBy} invalid={invalid} value={form.courseId} onChange={(e) => set("courseId")(e.target.value)}>
              <option value="">{t("auth.register.choose")}</option>
              {courses.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </Select>
          )}
        </Field>
        <Field label={t("auth.register.year")} error={errors.year}>
          {({ id, describedBy, invalid }) => (
            <Select id={id} aria-describedby={describedBy} invalid={invalid} value={form.year} onChange={(e) => set("year")(e.target.value)}>
              <option value="">—</option>
              {[1, 2, 3, 4, 5].map((y) => <option key={y} value={y}>{t("common.yearN", { n: y })}</option>)}
            </Select>
          )}
        </Field>
      </div>
      <Field label={t("auth.register.password")} hint={t("auth.register.passwordHint")} error={errors.password}>
        {({ id, describedBy, invalid }) => <PasswordInput id={id} aria-describedby={describedBy} invalid={invalid} autoComplete="new-password" value={form.password} onChange={(e) => set("password")(e.target.value)} />}
      </Field>
      <label className="check">
        <input type="checkbox" checked={form.consent} onChange={(e) => set("consent")(e.target.checked)} aria-invalid={!!errors.consent} />
        <span>{t("auth.register.consentPre")}<Link href="/termos">{t("auth.register.terms")}</Link>{t("auth.register.consentMid")}<Link href="/privacidade">{t("auth.register.privacy")}</Link>{t("auth.register.consentPost")}</span>
      </label>
      {errors.consent && <span className="small" style={{ color: "var(--neg)" }} role="alert">{errors.consent}</span>}
      {general && <Alert tone="neg" title={t("auth.register.failedTitle")}>{general}</Alert>}
      <Button variant="pri" type="submit" loading={busy}>{t("auth.register.submit")}</Button>
      <p className="small muted">{t("auth.register.haveAccount")} <Link href="/entrar">{t("auth.register.signIn")}</Link></p>
    </form>
  );
}
