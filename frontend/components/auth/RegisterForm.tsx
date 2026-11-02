"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Field, Input, Select } from "@/components/ui/Field";
import { post } from "@/lib/api-client";
import { isApiError } from "@/lib/errors";

export function RegisterForm({ courses }: { courses: { id: string; name: string }[] }) {
  const router = useRouter();
  const [form, setForm] = useState({ name: "", email: "", courseId: "", year: "", password: "", consent: false });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [general, setGeneral] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const set = (k: keyof typeof form) => (v: string | boolean) => setForm((f) => ({ ...f, [k]: v }));

  function validate() {
    const e: Record<string, string> = {};
    if (form.name.trim().length < 2) e.name = "Indica o teu nome completo.";
    if (!/^\S+@\S+\.\S+$/.test(form.email)) e.email = "Indica um email institucional válido.";
    else if (/@isptec\.co\.ao$/i.test(form.email.trim()) && !/^[0-9]{8}@/.test(form.email.trim()))
      e.email = "Usa o email de estudante, que começa pelo teu número de 8 dígitos (ex.: 20221987@isptec.co.ao).";
    if (!form.courseId) e.courseId = "Escolhe o curso.";
    if (!form.year) e.year = "Escolhe o ano.";
    if (form.password.length < 12) e.password = "A palavra-passe tem de ter pelo menos 12 caracteres.";
    if (!form.consent) e.consent = "Tens de aceitar os termos e a política de privacidade.";
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
      } else setGeneral("Não foi possível criar a conta. Tenta de novo.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="stack" style={{ gap: 16 }} onSubmit={submit} noValidate>
      <Field label="Nome completo" error={errors.name}>
        {({ id, describedBy, invalid }) => <Input id={id} aria-describedby={describedBy} invalid={invalid} autoComplete="name" value={form.name} onChange={(e) => set("name")(e.target.value)} />}
      </Field>
      <Field label="Email institucional" hint="O teu email de estudante do ISPTEC, ex.: 20221987@isptec.co.ao. Aberto a todos os cursos." error={errors.email}>
        {({ id, describedBy, invalid }) => <Input id={id} aria-describedby={describedBy} invalid={invalid} type="email" autoComplete="email" value={form.email} onChange={(e) => set("email")(e.target.value)} />}
      </Field>
      <div className="grid" style={{ gridTemplateColumns: "2fr 1fr", gap: 12 }}>
        <Field label="Curso" error={errors.courseId}>
          {({ id, describedBy, invalid }) => (
            <Select id={id} aria-describedby={describedBy} invalid={invalid} value={form.courseId} onChange={(e) => set("courseId")(e.target.value)}>
              <option value="">Escolhe…</option>
              {courses.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </Select>
          )}
        </Field>
        <Field label="Ano" error={errors.year}>
          {({ id, describedBy, invalid }) => (
            <Select id={id} aria-describedby={describedBy} invalid={invalid} value={form.year} onChange={(e) => set("year")(e.target.value)}>
              <option value="">—</option>
              {[1, 2, 3, 4, 5].map((y) => <option key={y} value={y}>{y}.º ano</option>)}
            </Select>
          )}
        </Field>
      </div>
      <Field label="Palavra-passe" hint="12+ caracteres. Evita palavras óbvias." error={errors.password}>
        {({ id, describedBy, invalid }) => <Input id={id} aria-describedby={describedBy} invalid={invalid} type="password" autoComplete="new-password" value={form.password} onChange={(e) => set("password")(e.target.value)} />}
      </Field>
      <label className="check">
        <input type="checkbox" checked={form.consent} onChange={(e) => set("consent")(e.target.checked)} aria-invalid={!!errors.consent} />
        <span>Li e aceito os <Link href="/termos">termos e condições</Link> e a <Link href="/privacidade">política de privacidade</Link>, incluindo o tratamento dos meus dados para usar a plataforma.</span>
      </label>
      {errors.consent && <span className="small" style={{ color: "var(--neg)" }} role="alert">{errors.consent}</span>}
      {general && <Alert tone="neg" title="Não foi possível criar a conta">{general}</Alert>}
      <Button variant="pri" type="submit" loading={busy}>Criar conta</Button>
      <p className="small muted">Já tens conta? <Link href="/entrar">Entra</Link></p>
    </form>
  );
}
