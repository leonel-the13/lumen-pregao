"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Field, Input, Select } from "@/components/ui/Field";
import { Modal } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";
import { post } from "@/lib/api-client";
import { isApiError } from "@/lib/errors";
import { useT } from "@/lib/i18n/client";

const ASSIGNABLE = ["ADMIN", "REVIEWER", "AUTHOR", "CONTEST_ORGANIZER", "JURY", "STUDENT"] as const;

/** Convida uma pessoa: recebe um email e só entra depois de definir a palavra-passe. */
export function InviteUser({ courses }: { courses: { id: string; name: string }[] }) {
  const router = useRouter();
  const toast = useToast();
  const t = useT();
  const [open, setOpen] = useState(false);
  const [f, setF] = useState({ name: "", email: "", roles: ["STUDENT"] as string[], courseId: "", year: "" });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const submit = async () => {
    setBusy(true); setErrors({});
    try {
      await post("/admin/users/invite", { name: f.name, email: f.email, roles: f.roles, courseId: f.courseId || null, year: f.year ? Number(f.year) : null });
      toast({ tone: "pos", title: t("admin.invite.sent"), body: t("admin.invite.sentBody", { email: f.email }) });
      setOpen(false);
      setF({ name: "", email: "", roles: ["STUDENT"], courseId: "", year: "" });
      router.refresh();
    } catch (e) {
      if (isApiError(e)) setErrors(Object.keys(e.fields).length ? e.fields : { general: e.message });
    } finally { setBusy(false); }
  };
  return (
    <>
      <Button variant="pri" icon="plus" onClick={() => setOpen(true)}>{t("admin.invite.open")}</Button>
      <Modal open={open} onClose={() => setOpen(false)} title={t("admin.invite.title")}
        actions={<><Button variant="out" onClick={() => setOpen(false)}>{t("common.cancel")}</Button>
          <Button variant="pri" loading={busy} onClick={submit} disabled={!f.name || !f.email || f.roles.length === 0}>{t("admin.invite.sendInvite")}</Button></>}>
        <div className="stack" style={{ color: "var(--text)" }}>
          <p className="small muted">{t("admin.invite.intro")}</p>
          <Field label={t("admin.invite.name")} error={errors.name}>{({ id, invalid }) => <Input id={id} invalid={invalid} value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} />}</Field>
          <Field label={t("admin.invite.email")} hint={t("admin.invite.emailHint")} error={errors.email}>{({ id, invalid }) => <Input id={id} invalid={invalid} type="email" value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} />}</Field>
          <fieldset className="stack" style={{ border: 0, padding: 0, margin: 0, gap: 2 }}>
            <legend className="small" style={{ fontWeight: 600, marginBottom: 6 }}>{t("admin.invite.roles")}</legend>
            {ASSIGNABLE.map((r) => (
              <label key={r} className="check" style={{ paddingTop: 2, minHeight: 36 }}>
                <input type="checkbox" checked={f.roles.includes(r)} onChange={(e) => setF({ ...f, roles: e.target.checked ? [...f.roles, r] : f.roles.filter((x) => x !== r) })} />
                {t(`common.roles.${r}`)}
              </label>
            ))}
            {errors.roles && <span className="err small" style={{ color: "var(--neg)" }}>{errors.roles}</span>}
          </fieldset>
          <div className="grid g2" style={{ gap: 10 }}>
            <Field label={t("admin.invite.course")}>{({ id }) => (
              <Select id={id} value={f.courseId} onChange={(e) => setF({ ...f, courseId: e.target.value })}>
                <option value="">—</option>{courses.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </Select>)}</Field>
            <Field label={t("admin.invite.year")}>{({ id }) => (
              <Select id={id} value={f.year} onChange={(e) => setF({ ...f, year: e.target.value })}>
                <option value="">—</option>{[1, 2, 3, 4, 5].map((y) => <option key={y} value={y}>{t("common.yearN", { n: y })}</option>)}
              </Select>)}</Field>
          </div>
          {errors.general && <div className="alert neg" role="alert">{errors.general}</div>}
        </div>
      </Modal>
    </>
  );
}
