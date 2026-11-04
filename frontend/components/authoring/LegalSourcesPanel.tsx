"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Badge, Tbc } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Field, Input } from "@/components/ui/Field";
import { Modal } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";
import { patch, post } from "@/lib/api-client";
import type { LegalSourceDto } from "@/lib/authoring-types";
import { useT } from "@/lib/i18n/client";

type Form = { title: string; identifier: string; version: string; effectiveFrom: string; effectiveTo: string; url: string; confirmed: boolean };
const blank: Form = { title: "", identifier: "", version: "a confirmar", effectiveFrom: "", effectiveTo: "", url: "", confirmed: false };

/** Fontes normativas. Uma nova versão envia os exercícios ligados de volta a revisão (spec §4.14). */
export function LegalSourcesPanel({ sources, canEdit }: { sources: LegalSourceDto[]; canEdit: boolean }) {
  const router = useRouter();
  const toast = useToast();
  const t = useT();
  const [editing, setEditing] = useState<LegalSourceDto | "new" | null>(null);
  const [form, setForm] = useState<Form>(blank);
  const [err, setErr] = useState<string | null>(null);
  const open = (s: LegalSourceDto | "new") => {
    setErr(null);
    setEditing(s);
    setForm(s === "new" ? blank : { title: s.title, identifier: s.identifier, version: s.version, effectiveFrom: s.effectiveFrom ?? "",
      effectiveTo: s.effectiveTo ?? "", url: s.url ?? "", confirmed: s.confirmed });
  };
  const save = async () => {
    const body = { ...form, effectiveFrom: form.effectiveFrom || null, effectiveTo: form.effectiveTo || null };
    try {
      if (editing === "new") { await post("/legal-sources", body); toast({ tone: "pos", title: t("authoring.sources.created") }); }
      else if (editing) {
        const r = await patch<{ affectedExercises: number }>(`/legal-sources/${editing.id}`, body);
        toast({ tone: r.affectedExercises ? "acc" : "pos", title: t("authoring.sources.updated"), body: r.affectedExercises ? t("authoring.sources.backToReview", { n: r.affectedExercises }) : undefined });
      }
      setEditing(null);
      router.refresh();
    } catch (e) { setErr((e as Error).message); }
  };
  const set = (k: keyof Form) => (v: string | boolean) => setForm((f) => ({ ...f, [k]: v }));
  return (
    <div className="stack">
      {canEdit && <div><Button variant="out" icon="plus" onClick={() => open("new")}>{t("authoring.sources.new")}</Button></div>}
      <div className="grid g2">
        {sources.map((s) => (
          <Card key={s.id} className="stack">
            <div className="row"><b>{s.title}</b><div className="spacer" />{s.confirmed ? <Badge tone="pos">{t("authoring.sources.confirmed")}</Badge> : <Tbc />}</div>
            <div className="small muted">{t("authoring.sources.meta", { identifier: s.identifier, version: s.version })}{s.effectiveFrom ? t("authoring.sources.since", { date: s.effectiveFrom }) : ""}</div>
            {canEdit && <div><Button size="sm" variant="ghost" icon="pen" onClick={() => open(s)}>{t("common.edit")}</Button></div>}
          </Card>
        ))}
      </div>
      <Modal open={!!editing} onClose={() => setEditing(null)} title={editing === "new" ? t("authoring.sources.newTitle") : t("authoring.sources.editTitle")}
        actions={<><Button variant="out" onClick={() => setEditing(null)}>{t("common.cancel")}</Button><Button variant="pri" onClick={save}>{t("common.save")}</Button></>}>
        <div className="stack" style={{ color: "var(--text)" }}>
          <Field label={t("authoring.sources.title")}>{({ id }) => <Input id={id} value={form.title} onChange={(e) => set("title")(e.target.value)} />}</Field>
          <Field label={t("authoring.sources.identifier")} hint={t("authoring.sources.identifierHint")}>{({ id, describedBy }) => <Input id={id} aria-describedby={describedBy} value={form.identifier} onChange={(e) => set("identifier")(e.target.value)} />}</Field>
          <Field label={t("authoring.sources.version")} hint={t("authoring.sources.versionHint")}>{({ id, describedBy }) => <Input id={id} aria-describedby={describedBy} value={form.version} onChange={(e) => set("version")(e.target.value)} />}</Field>
          <div className="grid g2" style={{ gap: 12 }}>
            <Field label={t("authoring.sources.from")}>{({ id }) => <Input id={id} type="date" value={form.effectiveFrom} onChange={(e) => set("effectiveFrom")(e.target.value)} />}</Field>
            <Field label={t("authoring.sources.to")}>{({ id }) => <Input id={id} type="date" value={form.effectiveTo} onChange={(e) => set("effectiveTo")(e.target.value)} />}</Field>
          </div>
          <Field label={t("authoring.sources.url")}>{({ id }) => <Input id={id} value={form.url} onChange={(e) => set("url")(e.target.value)} />}</Field>
          <label className="check"><input type="checkbox" checked={form.confirmed} onChange={(e) => set("confirmed")(e.target.checked)} />{t("authoring.sources.confirmedCheck")}</label>
          {err && <div className="alert neg" role="alert">{err}</div>}
        </div>
      </Modal>
    </div>
  );
}
