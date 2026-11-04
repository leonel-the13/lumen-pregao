"use client";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useRef, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card, CardHeader } from "@/components/ui/Card";
import { Field, Input, Select, TextArea } from "@/components/ui/Field";
import { ConfirmDialog, Modal } from "@/components/ui/Modal";
import { Seg } from "@/components/ui/Tabs";
import { useToast } from "@/components/ui/Toast";
import { del, get, newIdempotencyKey, post, put } from "@/lib/api-client";
import { isApiError } from "@/lib/errors";
import { formatDateTime } from "@/lib/format";
import { useLocale, useT } from "@/lib/i18n/client";
import type { CursorPage } from "@/lib/types";

export interface EmailTemplateView { id: string; name: string; subject: string; title: string; body: string; ctaLabel?: string; ctaPath?: string; builtIn: boolean; updatedAt: string }
export interface ReminderStats { unverified: number; oldestUnverifiedAt?: string; dueVerification: number; inactive: number; inactiveDays: number; automatic: boolean }
export interface CampaignRow { id: string; subject: string; audience: string; audienceLabel: string; createdBy: string; createdAt: string; total: number; sent: number; failed: number; skipped: number; pending: number }
export interface EmailVariable { key: string; label: string }

type AudienceKind = "ALL_STUDENTS" | "COURSE" | "INACTIVE" | "USER";
interface Content { subject: string; title: string; body: string; ctaLabel: string; ctaPath: string }
interface Person { id: string; name: string; email?: string }

const EMPTY: Content = { subject: "", title: "Olá, {nome}!", body: "", ctaLabel: "Entrar no Lúmen Lab", ctaPath: "/inicio" };
const PAGES = ["/inicio", "/trilhas", "/salas", "/exames", "/competicoes", "/rankings", "/portefolio", "/definicoes"];

function daysSince(iso?: string) {
  if (!iso) return null;
  return Math.max(0, Math.floor((Date.now() - new Date(iso).getTime()) / 86_400_000));
}

function fieldErrors(e: unknown): Record<string, string> {
  if (!isApiError(e)) return { general: (e as Error).message };
  const out: Record<string, string> = {};
  for (const [k, v] of Object.entries(e.fields)) out[k.split(".").pop() ?? k] = v;
  return Object.keys(out).length ? out : { general: e.message };
}

export function EmailsAdmin({ templates, stats, campaigns, nextCursor, courses, variables, preset }: {
  templates: EmailTemplateView[]; stats: ReminderStats; campaigns: CampaignRow[]; nextCursor?: string;
  courses: { id: string; name: string }[]; variables: EmailVariable[]; preset?: Person;
}) {
  return (
    <div className="stack" style={{ gap: 24 }}>
      <Reminders stats={stats} />
      <Composer templates={templates} courses={courses} variables={variables} preset={preset} />
      <History rows={campaigns} nextCursor={nextCursor} />
    </div>
  );
}

function Reminders({ stats }: { stats: ReminderStats }) {
  const router = useRouter();
  const toast = useToast();
  const t = useT();
  const [confirm, setConfirm] = useState<"unverified" | "inactive" | null>(null);
  const [busy, setBusy] = useState(false);
  const oldest = daysSince(stats.oldestUnverifiedAt);
  const run = async () => {
    const kind = confirm;
    if (!kind) return;
    setBusy(true);
    try {
      const r = await post<{ sent: number }>(`/admin/emails/reminders/${kind}`);
      toast({ tone: "pos", title: r.sent === 1 ? t("admin.emails.remindersSentOne") : t("admin.emails.remindersSent", { n: r.sent }),
        body: r.sent === 0 ? t("admin.emails.nobody") : t("admin.emails.goingOut") });
      setConfirm(null);
      router.refresh();
    } catch (e) { toast({ tone: "neg", title: t("admin.emails.couldNotSend"), body: (e as Error).message }); }
    finally { setBusy(false); }
  };
  return (
    <section className="stack" aria-labelledby="rem-h">
      <h2 className="h2" id="rem-h">{t("admin.emails.reminders")}</h2>
      {!stats.automatic && <Alert tone="info" title={t("admin.emails.autoOff")}>{t("admin.emails.autoOffBody")}</Alert>}
      <div className="grid g2">
        <Card className="stack">
          <CardHeader title={t("admin.emails.unverified")} />
          <div className="row" style={{ gap: 10, alignItems: "baseline" }}>
            <span className="mono" style={{ fontSize: 32, fontWeight: 700 }}>{stats.unverified}</span>
            {oldest !== null && <span className="small muted">{t(oldest === 1 ? "admin.emails.oldestDay" : "admin.emails.oldestDays", { n: oldest })}</span>}
          </div>
          <p className="small muted" style={{ margin: 0 }}>{t("admin.emails.unverifiedAuto")}</p>
          <div><Button variant="out" icon="mail" disabled={stats.unverified === 0} onClick={() => setConfirm("unverified")}>{t("admin.emails.remindNow")}</Button></div>
        </Card>
        <Card className="stack">
          <CardHeader title={t("admin.emails.inactive")} />
          <div className="row" style={{ gap: 10, alignItems: "baseline" }}>
            <span className="mono" style={{ fontSize: 32, fontWeight: 700 }}>{stats.inactive}</span>
            <span className="small muted">{t("admin.emails.inactiveFor", { n: stats.inactiveDays })}</span>
          </div>
          <p className="small muted" style={{ margin: 0 }}>{t("admin.emails.inactiveAuto", { n: stats.inactiveDays })}</p>
          <div><Button variant="out" icon="mail" disabled={stats.inactive === 0} onClick={() => setConfirm("inactive")}>{t("admin.emails.remindNow")}</Button></div>
        </Card>
      </div>
      <ConfirmDialog open={!!confirm} onCancel={() => setConfirm(null)} onConfirm={run} busy={busy} confirmLabel={t("admin.emails.sendReminders")}
        title={confirm === "unverified" ? t("admin.emails.remindUnverifiedQ") : t("admin.emails.remindInactiveQ")}
        body={confirm === "unverified"
          ? t("admin.emails.remindUnverifiedBody", { n: stats.unverified })
          : t("admin.emails.remindInactiveBody", { n: stats.inactive })} />
    </section>
  );
}

function Composer({ templates, courses, variables, preset }: {
  templates: EmailTemplateView[]; courses: { id: string; name: string }[]; variables: EmailVariable[]; preset?: Person;
}) {
  const router = useRouter();
  const toast = useToast();
  const t = useT();
  const [kind, setKind] = useState<AudienceKind>(preset ? "USER" : "ALL_STUDENTS");
  const [courseId, setCourseId] = useState(courses[0]?.id ?? "");
  const [year, setYear] = useState("");
  const [days, setDays] = useState("14");
  const [person, setPerson] = useState<Person | null>(preset ?? null);
  const [templateId, setTemplateId] = useState("");
  const [c, setC] = useState<Content>(EMPTY);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [count, setCount] = useState<{ recipients: number; optedOut: number; label: string } | null>(null);
  const [countErr, setCountErr] = useState<string | null>(null);
  const [preview, setPreview] = useState<{ subject: string; html: string } | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [confirmSend, setConfirmSend] = useState(false);
  const [saveAs, setSaveAs] = useState<string | null>(null);
  const [delTpl, setDelTpl] = useState<EmailTemplateView | null>(null);
  const [key, setKey] = useState(() => newIdempotencyKey());
  const bodyRef = useRef<HTMLTextAreaElement>(null);

  const audience = useMemo(() => ({
    audience: kind,
    ...(kind === "COURSE" ? { courseId, year: year ? Number(year) : null } : {}),
    ...(kind === "INACTIVE" ? { inactiveDays: Number(days) || 0 } : {}),
    ...(kind === "USER" ? { userId: person?.id ?? null } : {}),
  }), [kind, courseId, year, days, person]);

  useEffect(() => {
    if (kind === "USER" && !person) { setCount(null); setCountErr(null); return; }
    const timer = setTimeout(async () => {
      try { setCount(await post("/admin/emails/audience", audience)); setCountErr(null); }
      catch (e) { setCount(null); setCountErr((e as Error).message); }
    }, 300);
    return () => clearTimeout(timer);
  }, [audience, kind, person]);

  const selected = templates.find((x) => x.id === templateId);
  const content = { subject: c.subject, title: c.title, body: c.body, ctaLabel: c.ctaLabel || null, ctaPath: c.ctaPath || null };
  const set = (k: keyof Content) => (e: { target: { value: string } }) => setC({ ...c, [k]: e.target.value });

  const applyTemplate = (id: string) => {
    setTemplateId(id);
    const tpl = templates.find((x) => x.id === id);
    setC(tpl ? { subject: tpl.subject, title: tpl.title, body: tpl.body, ctaLabel: tpl.ctaLabel ?? "", ctaPath: tpl.ctaPath ?? "" } : EMPTY);
    setErrors({});
  };

  const insertVar = (v: string) => {
    const el = bodyRef.current;
    const token = `{${v}}`;
    if (!el) { setC({ ...c, body: c.body + token }); return; }
    const start = el.selectionStart ?? c.body.length, end = el.selectionEnd ?? start;
    const body = c.body.slice(0, start) + token + c.body.slice(end);
    setC({ ...c, body });
    requestAnimationFrame(() => { el.focus(); el.setSelectionRange(start + token.length, start + token.length); });
  };

  const act = async (name: string, fn: () => Promise<void>) => {
    setBusy(name); setErrors({});
    try { await fn(); } catch (e) { setErrors(fieldErrors(e)); } finally { setBusy(null); }
  };

  const doPreview = () => act("preview", async () => setPreview(await post("/admin/emails/preview", { content })));
  const doTest = () => act("test", async () => {
    await post("/admin/emails/test", { content });
    toast({ tone: "pos", title: t("admin.emails.testSent"), body: t("admin.emails.testSentBody") });
  });
  const doSend = () => act("send", async () => {
    try {
      const r = await post<CampaignRow>("/admin/emails/campaigns", { audience, content, templateId: templateId || null }, { idempotencyKey: key });
      toast({ tone: "pos", title: r.total === 1 ? t("admin.emails.queuedOne") : t("admin.emails.queued", { n: r.total }), body: t("admin.emails.queuedBody") });
      setKey(newIdempotencyKey());
      setConfirmSend(false);
      router.refresh();
    } catch (e) { setConfirmSend(false); throw e; }
  });
  const doSaveTemplate = (name: string) => act("save", async () => {
    await post("/admin/email-templates", { name, content });
    toast({ tone: "pos", title: t("admin.emails.templateSaved"), body: name });
    setSaveAs(null);
    router.refresh();
  });
  const doUpdateTemplate = () => act("update", async () => {
    if (!selected) return;
    await put(`/admin/email-templates/${selected.id}`, { name: selected.name, content });
    toast({ tone: "pos", title: t("admin.emails.templateUpdated"), body: selected.name });
    router.refresh();
  });
  const doDeleteTemplate = () => act("delete", async () => {
    if (!delTpl) return;
    await del(`/admin/email-templates/${delTpl.id}`);
    toast({ tone: "pos", title: t("admin.emails.templateDeleted"), body: delTpl.name });
    if (templateId === delTpl.id) applyTemplate("");
    setDelTpl(null);
    router.refresh();
  });

  const canSend = !!c.subject.trim() && !!c.body.trim() && !!count && count.recipients > 0;

  return (
    <section className="stack" aria-labelledby="compose-h">
      <h2 className="h2" id="compose-h">{t("admin.emails.compose")}</h2>
      <Card className="stack" style={{ gap: 16 }}>
        <div className="stack" style={{ gap: 10 }}>
          <Seg label={t("admin.emails.recipients")} value={kind} onChange={(k) => setKind(k)}
            options={(["ALL_STUDENTS", "COURSE", "INACTIVE", "USER"] as const).map((v) => ({ value: v, label: t(`admin.emails.audience.${v}`) }))} />
          {kind === "COURSE" && (
            <div className="row" style={{ gap: 12, flexWrap: "wrap" }}>
              <Field label={t("admin.emails.course")}>{({ id }) => (
                <Select id={id} value={courseId} onChange={(e) => setCourseId(e.target.value)}>
                  {courses.map((x) => <option key={x.id} value={x.id}>{x.name}</option>)}
                </Select>
              )}</Field>
              <Field label={t("admin.emails.year")}>{({ id }) => (
                <Select id={id} value={year} onChange={(e) => setYear(e.target.value)}>
                  <option value="">{t("admin.emails.allYears")}</option>
                  {[1, 2, 3, 4, 5].map((y) => <option key={y} value={y}>{t("common.yearN", { n: y })}</option>)}
                </Select>
              )}</Field>
            </div>
          )}
          {kind === "INACTIVE" && (
            <Field label={t("admin.emails.inactiveDays")} hint={t("admin.emails.inactiveDaysHint")}>{({ id, describedBy }) => (
              <Input id={id} aria-describedby={describedBy} type="number" min={1} max={365} value={days} onChange={(e) => setDays(e.target.value)} style={{ maxWidth: 140 }} />
            )}</Field>
          )}
          {kind === "USER" && <PersonPicker person={person} onChange={setPerson} />}
          <p className="small" role="status" aria-live="polite" style={{ margin: 0 }}>
            {countErr ? <span className="err">{countErr}</span> : count ? (
              <>{t("admin.emails.goesTo")} <b className="mono">{count.recipients}</b> {count.recipients === 1 ? t("admin.emails.person") : t("admin.emails.people")} · {count.label}
                {count.optedOut > 0 && <span className="muted">{t("admin.emails.optedOut", { n: count.optedOut })}</span>}
                {kind === "USER" && <span className="muted">{t("admin.emails.individualNote")}</span>}</>
            ) : kind === "USER" ? <span className="muted">{t("admin.emails.choosePerson")}</span> : <span className="muted">{t("admin.emails.counting")}</span>}
          </p>
        </div>

        <div className="row" style={{ gap: 12, alignItems: "flex-end", flexWrap: "wrap" }}>
          <Field label={t("admin.emails.template")}>{({ id }) => (
            <Select id={id} value={templateId} onChange={(e) => applyTemplate(e.target.value)} style={{ minWidth: 260 }}>
              <option value="">{t("admin.emails.fromScratch")}</option>
              {templates.map((tpl) => <option key={tpl.id} value={tpl.id}>{tpl.name}</option>)}
            </Select>
          )}</Field>
          {selected && <Button variant="ghost" size="sm" icon="trash" onClick={() => setDelTpl(selected)}>{t("admin.emails.deleteTemplate")}</Button>}
        </div>

        <Field label={t("admin.emails.subject")} error={errors.subject}>{({ id, describedBy, invalid }) => (
          <Input id={id} aria-describedby={describedBy} invalid={invalid} maxLength={120} value={c.subject} onChange={set("subject")} placeholder="{nome}, o teu Balancete está desequilibrado" />
        )}</Field>
        <Field label={t("admin.emails.title")} hint={t("admin.emails.titleHint")} error={errors.title}>{({ id, describedBy, invalid }) => (
          <Input id={id} aria-describedby={describedBy} invalid={invalid} maxLength={120} value={c.title} onChange={set("title")} />
        )}</Field>
        <Field label={t("admin.emails.message")} hint={t("admin.emails.messageHint")} error={errors.body}>{({ id, describedBy, invalid }) => (
          <TextArea id={id} ref={bodyRef} aria-describedby={describedBy} invalid={invalid} rows={8} maxLength={5000} value={c.body} onChange={set("body")} />
        )}</Field>
        <div className="row" style={{ gap: 6, flexWrap: "wrap" }} role="group" aria-label={t("admin.emails.insertGroup")}>
          <span className="small muted">{t("admin.emails.insert")}</span>
          {variables.map((v) => (
            <button key={v.key} type="button" className="btn sm ghost mono" title={v.label} aria-label={t("admin.emails.insertAria", { label: v.label })} onClick={() => insertVar(v.key)}>{`{${v.key}}`}</button>
          ))}
        </div>
        <div className="row" style={{ gap: 12, flexWrap: "wrap" }}>
          <Field label={t("admin.emails.ctaLabel")} error={errors.ctaLabel}>{({ id, describedBy, invalid }) => (
            <Input id={id} aria-describedby={describedBy} invalid={invalid} maxLength={40} value={c.ctaLabel} onChange={set("ctaLabel")} />
          )}</Field>
          <Field label={t("admin.emails.ctaPath")} hint={t("admin.emails.ctaPathHint")} error={errors.ctaPath}>{({ id, describedBy, invalid }) => (
            <>
              <Input id={id} aria-describedby={describedBy} invalid={invalid} maxLength={200} list="email-pages" className="mono" value={c.ctaPath} onChange={set("ctaPath")} />
              <datalist id="email-pages">{PAGES.map((p) => <option key={p} value={p} />)}</datalist>
            </>
          )}</Field>
        </div>
        {errors.general && <div className="alert neg" role="alert">{errors.general}</div>}
        <div className="row" style={{ gap: 8, flexWrap: "wrap" }}>
          <Button variant="out" icon="eye" loading={busy === "preview"} disabled={!c.subject || !c.body} onClick={doPreview}>{t("admin.emails.preview")}</Button>
          <Button variant="out" icon="mail" loading={busy === "test"} disabled={!c.subject || !c.body} onClick={doTest}>{t("admin.emails.sendTest")}</Button>
          <Button variant="ghost" icon="plus" disabled={!c.subject || !c.body} onClick={() => setSaveAs(selected ? t("admin.emails.copy", { name: selected.name }) : "")}>{t("admin.emails.saveAsTemplate")}</Button>
          {selected && <Button variant="ghost" icon="check" loading={busy === "update"} onClick={doUpdateTemplate}>{t("admin.emails.updateTemplate", { name: selected.name })}</Button>}
          <div className="spacer" />
          <Button variant="pri" icon="arrowRight" disabled={!canSend} onClick={() => setConfirmSend(true)}>
            {count && count.recipients > 0 ? t("admin.emails.sendTo", { n: count.recipients, who: count.recipients === 1 ? t("admin.emails.person") : t("admin.emails.people") }) : t("admin.emails.send")}
          </Button>
        </div>
      </Card>

      <ConfirmDialog open={confirmSend} onCancel={() => setConfirmSend(false)} onConfirm={doSend} busy={busy === "send"}
        confirmLabel={t("admin.emails.sendNow")} title={t("admin.emails.sendQ", { subject: c.subject })}
        body={t(count?.recipients === 1 ? "admin.emails.sendBodyOne" : "admin.emails.sendBody", { n: count?.recipients ?? 0, label: count?.label ?? "" })} />
      <ConfirmDialog open={!!delTpl} onCancel={() => setDelTpl(null)} onConfirm={doDeleteTemplate} busy={busy === "delete"} destructive
        confirmLabel={t("admin.emails.deleteTemplate")} title={t("admin.emails.deleteTemplateQ", { name: delTpl?.name ?? "" })} body={t("admin.emails.deleteTemplateBody")} />
      <Modal open={saveAs !== null} onClose={() => setSaveAs(null)} title={t("admin.emails.saveAsTemplate")}
        actions={<><Button variant="out" onClick={() => setSaveAs(null)}>{t("common.cancel")}</Button>
          <Button variant="pri" loading={busy === "save"} disabled={!saveAs?.trim()} onClick={() => saveAs && doSaveTemplate(saveAs.trim())}>{t("common.save")}</Button></>}>
        <div className="stack" style={{ color: "var(--text)" }}>
          <Field label={t("admin.emails.templateName")} error={errors.name}>{({ id, describedBy, invalid }) => (
            <Input id={id} aria-describedby={describedBy} invalid={invalid} maxLength={80} value={saveAs ?? ""} onChange={(e) => setSaveAs(e.target.value)} autoFocus />
          )}</Field>
          {errors.general && <div className="alert neg" role="alert">{errors.general}</div>}
        </div>
      </Modal>
      <Modal open={!!preview} onClose={() => setPreview(null)} title={t("admin.emails.previewTitle")}
        actions={<Button variant="pri" onClick={() => setPreview(null)}>{t("common.close")}</Button>}>
        {preview && (
          <div className="stack" style={{ color: "var(--text)", gap: 8 }}>
            <p className="small" style={{ margin: 0 }}><span className="muted">{t("admin.emails.previewSubject")}</span> <b>{preview.subject}</b></p>
            <p className="small muted" style={{ margin: 0 }}>{t("admin.emails.previewNote")}</p>
            <iframe title={t("admin.emails.previewFrame")} sandbox="" srcDoc={preview.html}
              style={{ width: "min(640px, 80vw)", height: "60vh", border: "1px solid var(--line)", borderRadius: 10, background: "#F6F6F4" }} />
          </div>
        )}
      </Modal>
    </section>
  );
}

function PersonPicker({ person, onChange }: { person: Person | null; onChange: (p: Person | null) => void }) {
  const t = useT();
  const [q, setQ] = useState("");
  const [results, setResults] = useState<Person[]>([]);
  useEffect(() => {
    if (q.trim().length < 2) { setResults([]); return; }
    const timer = setTimeout(async () => {
      try {
        const r = await get<CursorPage<{ id: string; name: string; email: string; status: string }>>(`/admin/users?limit=8&q=${encodeURIComponent(q.trim())}`);
        setResults(r.items.filter((u) => u.status === "ACTIVE"));
      } catch { setResults([]); }
    }, 250);
    return () => clearTimeout(timer);
  }, [q]);
  if (person) {
    return (
      <div className="row" style={{ gap: 8 }}>
        <Badge icon="user">{person.name}</Badge>{person.email && <span className="small muted mono">{person.email}</span>}
        <Button size="sm" variant="ghost" onClick={() => onChange(null)}>{t("admin.emails.change")}</Button>
      </div>
    );
  }
  return (
    <div className="stack" style={{ gap: 6 }}>
      <Field label={t("admin.emails.personLabel")} hint={t("admin.emails.personHint")}>{({ id, describedBy }) => (
        <Input id={id} aria-describedby={describedBy} value={q} onChange={(e) => setQ(e.target.value)} style={{ maxWidth: 360 }} autoComplete="off" />
      )}</Field>
      {results.length > 0 && (
        <ul className="plain stack" style={{ gap: 4, maxWidth: 420 }}>
          {results.map((u) => (
            <li key={u.id}><button type="button" className="option" onClick={() => onChange(u)}>
              <span><b>{u.name}</b> <span className="small muted mono">{u.email}</span></span>
            </button></li>
          ))}
        </ul>
      )}
      {q.trim().length >= 2 && results.length === 0 && <span className="small muted">{t("admin.emails.noPerson")}</span>}
    </div>
  );
}

function History({ rows, nextCursor }: { rows: CampaignRow[]; nextCursor?: string }) {
  const router = useRouter();
  const toast = useToast();
  const t = useT();
  const locale = useLocale();
  const [busy, setBusy] = useState<string | null>(null);
  const retry = async (id: string) => {
    setBusy(id);
    try {
      const r = await post<{ requeued: number }>(`/admin/emails/campaigns/${id}/retry`);
      toast({ tone: "pos", title: t(r.requeued === 1 ? "admin.emails.requeuedOne" : "admin.emails.requeued", { n: r.requeued }) });
      router.refresh();
    } catch (e) { toast({ tone: "neg", title: t("admin.emails.couldNotRetry"), body: (e as Error).message }); }
    finally { setBusy(null); }
  };
  return (
    <section className="stack" aria-labelledby="hist-h">
      <div className="row"><h2 className="h2" id="hist-h">{t("admin.emails.sentList")}</h2><div className="spacer" />
        <Button size="sm" variant="ghost" icon="refresh" onClick={() => router.refresh()}>{t("admin.emails.refresh")}</Button></div>
      {rows.length === 0 ? <Card className="state-box"><p className="muted">{t("admin.emails.noneSent")}</p></Card> : (
        <div className="tbl-wrap"><table className="tbl">
          <thead><tr><th>{t("admin.emails.when")}</th><th>{t("admin.emails.subject")}</th><th>{t("admin.emails.recipients")}</th><th>{t("admin.emails.by")}</th><th>{t("admin.emails.state")}</th><th><span className="sr-only">{t("admin.emails.actions")}</span></th></tr></thead>
          <tbody>{rows.map((r) => (
            <tr key={r.id}>
              <td className="mono small">{formatDateTime(r.createdAt, locale)}</td>
              <td>{r.subject}</td>
              <td className="small">{r.audienceLabel}</td>
              <td className="small muted">{r.createdBy}</td>
              <td className="small">
                <span className="row" style={{ gap: 6, flexWrap: "wrap" }}>
                  <Badge tone={r.pending === 0 && r.failed === 0 ? "pos" : "neutral"} icon={r.pending === 0 && r.failed === 0 ? "check" : undefined}>
                    <span className="mono">{r.sent}/{r.total}</span> {t("admin.emails.sent")}
                  </Badge>
                  {r.pending > 0 && <Badge tone="info" icon="clock"><span className="mono">{r.pending}</span> {t("admin.emails.pending")}</Badge>}
                  {r.failed > 0 && <Badge tone="neg" icon="warn"><span className="mono">{r.failed}</span> {t("admin.emails.failed")}</Badge>}
                  {r.skipped > 0 && <span className="muted"><span className="mono">{r.skipped}</span> {t("admin.emails.skipped")}</span>}
                </span>
              </td>
              <td className="actions-cell">{r.failed > 0 && <Button size="sm" variant="ghost" icon="refresh" loading={busy === r.id} onClick={() => retry(r.id)}>{t("admin.emails.retry")}</Button>}</td>
            </tr>
          ))}</tbody>
        </table></div>
      )}
      {nextCursor && <div className="row"><div className="spacer" /><a className="btn sm out" href={`/admin?tab=emails&cursor=${nextCursor}`}>{t("admin.emails.older")}</a></div>}
    </section>
  );
}
