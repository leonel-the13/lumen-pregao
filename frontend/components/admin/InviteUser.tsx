"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Field, Input, Select } from "@/components/ui/Field";
import { Modal } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";
import { post } from "@/lib/api-client";
import { isApiError } from "@/lib/errors";
import { ROLE_LABEL } from "./UsersTable";

const ASSIGNABLE = ["ADMIN", "REVIEWER", "AUTHOR", "CONTEST_ORGANIZER", "JURY", "STUDENT"];

/** Convida uma pessoa: recebe um email e só entra depois de definir a palavra-passe. */
export function InviteUser({ courses }: { courses: { id: string; name: string }[] }) {
  const router = useRouter();
  const toast = useToast();
  const [open, setOpen] = useState(false);
  const [f, setF] = useState({ name: "", email: "", roles: ["ADMIN"] as string[], courseId: "", year: "" });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const submit = async () => {
    setBusy(true); setErrors({});
    try {
      await post("/admin/users/invite", { name: f.name, email: f.email, roles: f.roles, courseId: f.courseId || null, year: f.year ? Number(f.year) : null });
      toast({ tone: "pos", title: "Convite enviado", body: `${f.email} recebe um email para definir a palavra-passe (válido 72 h).` });
      setOpen(false);
      setF({ name: "", email: "", roles: ["ADMIN"], courseId: "", year: "" });
      router.refresh();
    } catch (e) {
      if (isApiError(e)) setErrors(Object.keys(e.fields).length ? e.fields : { general: e.message });
    } finally { setBusy(false); }
  };
  return (
    <>
      <Button variant="pri" icon="plus" onClick={() => setOpen(true)}>Convidar</Button>
      <Modal open={open} onClose={() => setOpen(false)} title="Convidar pessoa"
        actions={<><Button variant="out" onClick={() => setOpen(false)}>Cancelar</Button>
          <Button variant="pri" loading={busy} onClick={submit} disabled={!f.name || !f.email || f.roles.length === 0}>Enviar convite</Button></>}>
        <div className="stack" style={{ color: "var(--text)" }}>
          <p className="small muted">A pessoa recebe um email e só consegue entrar depois de definir a palavra-passe. Perfis de administração,
            revisão, júri e organização configuram o 2.º factor no primeiro login.</p>
          <Field label="Nome completo" error={errors.name}>{({ id, invalid }) => <Input id={id} invalid={invalid} value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} />}</Field>
          <Field label="Email" error={errors.email}>{({ id, invalid }) => <Input id={id} invalid={invalid} type="email" value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} />}</Field>
          <fieldset className="stack" style={{ border: 0, padding: 0, margin: 0, gap: 2 }}>
            <legend className="small" style={{ fontWeight: 600, marginBottom: 6 }}>Perfis</legend>
            {ASSIGNABLE.map((r) => (
              <label key={r} className="check" style={{ paddingTop: 2, minHeight: 36 }}>
                <input type="checkbox" checked={f.roles.includes(r)} onChange={(e) => setF({ ...f, roles: e.target.checked ? [...f.roles, r] : f.roles.filter((x) => x !== r) })} />
                {ROLE_LABEL[r]}
              </label>
            ))}
            {errors.roles && <span className="err small" style={{ color: "var(--neg)" }}>{errors.roles}</span>}
          </fieldset>
          <div className="grid g2" style={{ gap: 10 }}>
            <Field label="Curso (opcional)">{({ id }) => (
              <Select id={id} value={f.courseId} onChange={(e) => setF({ ...f, courseId: e.target.value })}>
                <option value="">—</option>{courses.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </Select>)}</Field>
            <Field label="Ano (opcional)">{({ id }) => (
              <Select id={id} value={f.year} onChange={(e) => setF({ ...f, year: e.target.value })}>
                <option value="">—</option>{[1, 2, 3, 4, 5].map((y) => <option key={y} value={y}>{y}.º ano</option>)}
              </Select>)}</Field>
          </div>
          {errors.general && <div className="alert neg" role="alert">{errors.general}</div>}
        </div>
      </Modal>
    </>
  );
}
