"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Avatar } from "@/components/ui/Avatar";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card, CardHeader } from "@/components/ui/Card";
import { Field, Input, Select } from "@/components/ui/Field";
import { useToast } from "@/components/ui/Toast";
import { patch } from "@/lib/api-client";
import { isApiError } from "@/lib/errors";
import { useT } from "@/lib/i18n/client";
import type { Me } from "@/lib/types";

/** Editar perfil: nome, curso e ano. O email só muda pela administração (fica ligado ao convite e ao login). */
export function ProfileForm({ me, courses }: { me: Me; courses: { id: string; name: string }[] }) {
  const router = useRouter();
  const toast = useToast();
  const t = useT();
  const [name, setName] = useState(me.name);
  const [courseId, setCourseId] = useState(me.course?.id ?? "");
  const [year, setYear] = useState(me.year ? String(me.year) : "");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const dirty = name.trim() !== me.name || courseId !== (me.course?.id ?? "") || year !== (me.year ? String(me.year) : "");
  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) { setErrors({ name: t("settings.profile.nameRequired") }); return; }
    setBusy(true); setErrors({});
    try {
      await patch("/me", { name: name.trim(), courseId: courseId || null, year: year ? Number(year) : null });
      toast({ tone: "pos", title: t("settings.profile.saved") });
      router.refresh();
    } catch (err) {
      if (isApiError(err)) setErrors(Object.keys(err.fields).length ? err.fields : { general: err.message });
      else setErrors({ general: t("common.couldNotSave") });
    } finally { setBusy(false); }
  };
  return (
    <Card className="stack">
      <div className="row" style={{ gap: 14 }}>
        <Avatar initials={me.initials} />
        <div className="stack" style={{ gap: 4, minWidth: 0 }}>
          <b>{me.name}</b>
          <span className="row" style={{ gap: 6 }}>{me.roles.map((r) => <Badge key={r}>{t(`common.roles.${r}`)}</Badge>)}</span>
        </div>
        <div className="spacer" />
        <Link className="btn ghost sm" href="/portefolio">{t("settings.profile.viewPortfolio")}</Link>
      </div>
      <CardHeader title={t("settings.profile.identity")} />
      <form className="stack" onSubmit={submit}>
        <Field label={t("settings.profile.name")} error={errors.name}>
          {({ id, describedBy, invalid }) => <Input id={id} aria-describedby={describedBy} invalid={invalid} autoComplete="name" maxLength={120} value={name} onChange={(e) => setName(e.target.value)} />}
        </Field>
        <Field label={t("settings.profile.email")} hint={t("settings.profile.emailHint")}>
          {({ id, describedBy }) => <Input id={id} aria-describedby={describedBy} value={me.email} readOnly disabled />}
        </Field>
        <div className="grid g2" style={{ gap: 10 }}>
          <Field label={t("settings.profile.course")} error={errors.courseId}>
            {({ id }) => (
              <Select id={id} value={courseId} onChange={(e) => setCourseId(e.target.value)}>
                {!courseId && <option value="">{t("settings.profile.noCourse")}</option>}
                {courses.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </Select>)}
          </Field>
          <Field label={t("settings.profile.year")} error={errors.year}>
            {({ id }) => (
              <Select id={id} value={year} onChange={(e) => setYear(e.target.value)}>
                {!year && <option value="">—</option>}
                {[1, 2, 3, 4, 5].map((y) => <option key={y} value={y}>{t("common.yearN", { n: y })}</option>)}
              </Select>)}
          </Field>
        </div>
        {errors.general && <div className="alert neg" role="alert">{errors.general}</div>}
        <div><Button variant="pri" type="submit" loading={busy} disabled={!dirty}>{t("common.save")}</Button></div>
      </form>
    </Card>
  );
}
