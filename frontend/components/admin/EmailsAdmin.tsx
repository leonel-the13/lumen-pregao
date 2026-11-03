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
  const [confirm, setConfirm] = useState<"unverified" | "inactive" | null>(null);
  const [busy, setBusy] = useState(false);
  const oldest = daysSince(stats.oldestUnverifiedAt);
  const run = async () => {
    const kind = confirm;
    if (!kind) return;
    setBusy(true);
    try {
      const r = await post<{ sent: number }>(`/admin/emails/reminders/${kind}`);
      toast({ tone: "pos", title: r.sent === 1 ? "1 lembrete enviado" : `${r.sent} lembretes enviados`,
        body: r.sent === 0 ? "Ninguém precisava de lembrete agora (quem foi lembrado há pouco fica de fora)." : "Os emails saem nos próximos minutos." });
      setConfirm(null);
      router.refresh();
    } catch (e) { toast({ tone: "neg", title: "Não foi possível enviar", body: (e as Error).message }); }
    finally { setBusy(false); }
  };
  return (
    <section className="stack" aria-labelledby="rem-h">
      <h2 className="h2" id="rem-h">Lembretes</h2>
      {!stats.automatic && <Alert tone="info" title="Lembretes automáticos desligados">Só saem quando carregas em «Lembrar agora».</Alert>}
      <div className="grid g2">
        <Card className="stack">
          <CardHeader title="Contas por confirmar" />
          <div className="row" style={{ gap: 10, alignItems: "baseline" }}>
            <span className="mono" style={{ fontSize: 32, fontWeight: 700 }}>{stats.unverified}</span>
            {oldest !== null && <span className="small muted">a mais antiga há {oldest} {oldest === 1 ? "dia" : "dias"}</span>}
          </div>
          <p className="small muted" style={{ margin: 0 }}>
            Automático: um lembrete 3 dias depois do registo e outro uma semana depois (no máximo 2). Cada lembrete leva um link de confirmação novo.
          </p>
          <div><Button variant="out" icon="mail" disabled={stats.unverified === 0} onClick={() => setConfirm("unverified")}>Lembrar agora</Button></div>
        </Card>
        <Card className="stack">
          <CardHeader title="Estudantes inactivos" />
          <div className="row" style={{ gap: 10, alignItems: "baseline" }}>
            <span className="mono" style={{ fontSize: 32, fontWeight: 700 }}>{stats.inactive}</span>
            <span className="small muted">sem entrar há {stats.inactiveDays} dias ou mais</span>
          </div>
          <p className="small muted" style={{ margin: 0 }}>
            Automático: um lembrete leve depois de {stats.inactiveDays} dias sem actividade, no máximo um por mês. Quem desligou os emails não recebe.
          </p>
          <div><Button variant="out" icon="mail" disabled={stats.inactive === 0} onClick={() => setConfirm("inactive")}>Lembrar agora</Button></div>
        </Card>
      </div>
      <ConfirmDialog open={!!confirm} onCancel={() => setConfirm(null)} onConfirm={run} busy={busy} confirmLabel="Enviar lembretes"
        title={confirm === "unverified" ? "Lembrar as contas por confirmar?" : "Lembrar os estudantes inactivos?"}
        body={confirm === "unverified"
          ? `Até ${stats.unverified} pessoas recebem um email com um link de confirmação novo. Quem foi lembrado na última hora fica de fora.`
          : `Até ${stats.inactive} estudantes recebem um lembrete leve. Quem foi lembrado na última semana fica de fora.`} />
    </section>
  );
}

function Composer({ templates, courses, variables, preset }: {
  templates: EmailTemplateView[]; courses: { id: string; name: string }[]; variables: EmailVariable[]; preset?: Person;
}) {
  const router = useRouter();
  const toast = useToast();
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
    const t = setTimeout(async () => {
      try { setCount(await post("/admin/emails/audience", audience)); setCountErr(null); }
      catch (e) { setCount(null); setCountErr((e as Error).message); }
    }, 300);
    return () => clearTimeout(t);
  }, [audience, kind, person]);

  const selected = templates.find((t) => t.id === templateId);
  const content = { subject: c.subject, title: c.title, body: c.body, ctaLabel: c.ctaLabel || null, ctaPath: c.ctaPath || null };
  const set = (k: keyof Content) => (e: { target: { value: string } }) => setC({ ...c, [k]: e.target.value });

  const applyTemplate = (id: string) => {
    setTemplateId(id);
    const t = templates.find((x) => x.id === id);
    setC(t ? { subject: t.subject, title: t.title, body: t.body, ctaLabel: t.ctaLabel ?? "", ctaPath: t.ctaPath ?? "" } : EMPTY);
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
    toast({ tone: "pos", title: "Teste enviado para o teu email", body: "As variáveis usam os teus dados. Chega dentro de momentos." });
  });
  const doSend = () => act("send", async () => {
    try {
      const r = await post<CampaignRow>("/admin/emails/campaigns", { audience, content, templateId: templateId || null }, { idempotencyKey: key });
      toast({ tone: "pos", title: r.total === 1 ? "Email na fila" : `Email na fila para ${r.total} pessoas`, body: "Os emails saem aos poucos (cerca de 100 por minuto). Acompanha em «Enviados»." });
      setKey(newIdempotencyKey());
      setConfirmSend(false);
      router.refresh();
    } catch (e) { setConfirmSend(false); throw e; }
  });
  const doSaveTemplate = (name: string) => act("save", async () => {
    await post("/admin/email-templates", { name, content });
    toast({ tone: "pos", title: "Modelo guardado", body: name });
    setSaveAs(null);
    router.refresh();
  });
  const doUpdateTemplate = () => act("update", async () => {
    if (!selected) return;
    await put(`/admin/email-templates/${selected.id}`, { name: selected.name, content });
    toast({ tone: "pos", title: "Modelo actualizado", body: selected.name });
    router.refresh();
  });
  const doDeleteTemplate = () => act("delete", async () => {
    if (!delTpl) return;
    await del(`/admin/email-templates/${delTpl.id}`);
    toast({ tone: "pos", title: "Modelo apagado", body: delTpl.name });
    if (templateId === delTpl.id) applyTemplate("");
    setDelTpl(null);
    router.refresh();
  });

  const canSend = !!c.subject.trim() && !!c.body.trim() && !!count && count.recipients > 0;

  return (
    <section className="stack" aria-labelledby="compose-h">
      <h2 className="h2" id="compose-h">Escrever email</h2>
      <Card className="stack" style={{ gap: 16 }}>
        <div className="stack" style={{ gap: 10 }}>
          <Seg label="Destinatários" value={kind} onChange={(k) => setKind(k)} options={[
            { value: "ALL_STUDENTS", label: "Todos os estudantes" }, { value: "COURSE", label: "Por curso" },
            { value: "INACTIVE", label: "Inactivos" }, { value: "USER", label: "Uma pessoa" },
          ]} />
          {kind === "COURSE" && (
            <div className="row" style={{ gap: 12, flexWrap: "wrap" }}>
              <Field label="Curso">{({ id }) => (
                <Select id={id} value={courseId} onChange={(e) => setCourseId(e.target.value)}>
                  {courses.map((x) => <option key={x.id} value={x.id}>{x.name}</option>)}
                </Select>
              )}</Field>
              <Field label="Ano">{({ id }) => (
                <Select id={id} value={year} onChange={(e) => setYear(e.target.value)}>
                  <option value="">Todos os anos</option>
                  {[1, 2, 3, 4, 5].map((y) => <option key={y} value={y}>{y}.º ano</option>)}
                </Select>
              )}</Field>
            </div>
          )}
          {kind === "INACTIVE" && (
            <Field label="Sem entrar há (dias)" hint="Conta a última visita ou login.">{({ id, describedBy }) => (
              <Input id={id} aria-describedby={describedBy} type="number" min={1} max={365} value={days} onChange={(e) => setDays(e.target.value)} style={{ maxWidth: 140 }} />
            )}</Field>
          )}
          {kind === "USER" && <PersonPicker person={person} onChange={setPerson} />}
          <p className="small" role="status" aria-live="polite" style={{ margin: 0 }}>
            {countErr ? <span className="err">{countErr}</span> : count ? (
              <>Vai para <b className="mono">{count.recipients}</b> {count.recipients === 1 ? "pessoa" : "pessoas"} · {count.label}
                {count.optedOut > 0 && <span className="muted"> · {count.optedOut} desligaram os emails e ficam de fora</span>}
                {kind === "USER" && <span className="muted"> · mensagens individuais chegam mesmo a quem desligou os emails</span>}</>
            ) : kind === "USER" ? <span className="muted">Escolhe a pessoa.</span> : <span className="muted">A contar…</span>}
          </p>
        </div>

        <div className="row" style={{ gap: 12, alignItems: "flex-end", flexWrap: "wrap" }}>
          <Field label="Modelo">{({ id }) => (
            <Select id={id} value={templateId} onChange={(e) => applyTemplate(e.target.value)} style={{ minWidth: 260 }}>
              <option value="">Escrever do zero</option>
              {templates.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
            </Select>
          )}</Field>
          {selected && <Button variant="ghost" size="sm" icon="trash" onClick={() => setDelTpl(selected)}>Apagar modelo</Button>}
        </div>

        <Field label="Assunto" error={errors.subject}>{({ id, describedBy, invalid }) => (
          <Input id={id} aria-describedby={describedBy} invalid={invalid} maxLength={120} value={c.subject} onChange={set("subject")} placeholder="{nome}, o teu Balancete está desequilibrado" />
        )}</Field>
        <Field label="Título" hint="O título grande no topo do email." error={errors.title}>{({ id, describedBy, invalid }) => (
          <Input id={id} aria-describedby={describedBy} invalid={invalid} maxLength={120} value={c.title} onChange={set("title")} />
        )}</Field>
        <Field label="Mensagem" hint="Texto simples: cada linha é um parágrafo." error={errors.body}>{({ id, describedBy, invalid }) => (
          <TextArea id={id} ref={bodyRef} aria-describedby={describedBy} invalid={invalid} rows={8} maxLength={5000} value={c.body} onChange={set("body")} />
        )}</Field>
        <div className="row" style={{ gap: 6, flexWrap: "wrap" }} role="group" aria-label="Inserir variável na mensagem">
          <span className="small muted">Inserir:</span>
          {variables.map((v) => (
            <button key={v.key} type="button" className="btn sm ghost mono" title={v.label} aria-label={`Inserir ${v.label}`} onClick={() => insertVar(v.key)}>{`{${v.key}}`}</button>
          ))}
        </div>
        <div className="row" style={{ gap: 12, flexWrap: "wrap" }}>
          <Field label="Texto do botão (opcional)" error={errors.ctaLabel}>{({ id, describedBy, invalid }) => (
            <Input id={id} aria-describedby={describedBy} invalid={invalid} maxLength={40} value={c.ctaLabel} onChange={set("ctaLabel")} />
          )}</Field>
          <Field label="Página do botão" hint="Só páginas do Lúmen Lab, ex.: /trilhas" error={errors.ctaPath}>{({ id, describedBy, invalid }) => (
            <>
              <Input id={id} aria-describedby={describedBy} invalid={invalid} maxLength={200} list="email-pages" className="mono" value={c.ctaPath} onChange={set("ctaPath")} />
              <datalist id="email-pages">{PAGES.map((p) => <option key={p} value={p} />)}</datalist>
            </>
          )}</Field>
        </div>
        {errors.general && <div className="alert neg" role="alert">{errors.general}</div>}
        <div className="row" style={{ gap: 8, flexWrap: "wrap" }}>
          <Button variant="out" icon="eye" loading={busy === "preview"} disabled={!c.subject || !c.body} onClick={doPreview}>Pré-visualizar</Button>
          <Button variant="out" icon="mail" loading={busy === "test"} disabled={!c.subject || !c.body} onClick={doTest}>Enviar teste para mim</Button>
          <Button variant="ghost" icon="plus" disabled={!c.subject || !c.body} onClick={() => setSaveAs(selected ? `${selected.name} (cópia)` : "")}>Guardar como modelo</Button>
          {selected && <Button variant="ghost" icon="check" loading={busy === "update"} onClick={doUpdateTemplate}>Actualizar «{selected.name}»</Button>}
          <div className="spacer" />
          <Button variant="pri" icon="arrowRight" disabled={!canSend} onClick={() => setConfirmSend(true)}>
            {count && count.recipients > 0 ? `Enviar a ${count.recipients} ${count.recipients === 1 ? "pessoa" : "pessoas"}` : "Enviar"}
          </Button>
        </div>
      </Card>

      <ConfirmDialog open={confirmSend} onCancel={() => setConfirmSend(false)} onConfirm={doSend} busy={busy === "send"}
        confirmLabel="Enviar agora" title={`Enviar «${c.subject}»?`}
        body={`${count?.recipients ?? 0} ${count?.recipients === 1 ? "pessoa recebe" : "pessoas recebem"} este email (${count?.label ?? ""}). Depois de enviado não é possível cancelar. Fica registado em auditoria.`} />
      <ConfirmDialog open={!!delTpl} onCancel={() => setDelTpl(null)} onConfirm={doDeleteTemplate} busy={busy === "delete"} destructive
        confirmLabel="Apagar modelo" title={`Apagar o modelo «${delTpl?.name ?? ""}»?`} body="Os emails já enviados com este modelo não mudam." />
      <Modal open={saveAs !== null} onClose={() => setSaveAs(null)} title="Guardar como modelo"
        actions={<><Button variant="out" onClick={() => setSaveAs(null)}>Cancelar</Button>
          <Button variant="pri" loading={busy === "save"} disabled={!saveAs?.trim()} onClick={() => saveAs && doSaveTemplate(saveAs.trim())}>Guardar</Button></>}>
        <div className="stack" style={{ color: "var(--text)" }}>
          <Field label="Nome do modelo" error={errors.name}>{({ id, describedBy, invalid }) => (
            <Input id={id} aria-describedby={describedBy} invalid={invalid} maxLength={80} value={saveAs ?? ""} onChange={(e) => setSaveAs(e.target.value)} autoFocus />
          )}</Field>
          {errors.general && <div className="alert neg" role="alert">{errors.general}</div>}
        </div>
      </Modal>
      <Modal open={!!preview} onClose={() => setPreview(null)} title="Pré-visualização"
        actions={<Button variant="pri" onClick={() => setPreview(null)}>Fechar</Button>}>
        {preview && (
          <div className="stack" style={{ color: "var(--text)", gap: 8 }}>
            <p className="small" style={{ margin: 0 }}><span className="muted">Assunto:</span> <b>{preview.subject}</b></p>
            <p className="small muted" style={{ margin: 0 }}>As variáveis usam os teus dados como exemplo.</p>
            <iframe title="Pré-visualização do email" sandbox="" srcDoc={preview.html}
              style={{ width: "min(640px, 80vw)", height: "60vh", border: "1px solid var(--line)", borderRadius: 10, background: "#F6F6F4" }} />
          </div>
        )}
      </Modal>
    </section>
  );
}

function PersonPicker({ person, onChange }: { person: Person | null; onChange: (p: Person | null) => void }) {
  const [q, setQ] = useState("");
  const [results, setResults] = useState<Person[]>([]);
  useEffect(() => {
    if (q.trim().length < 2) { setResults([]); return; }
    const t = setTimeout(async () => {
      try {
        const r = await get<CursorPage<{ id: string; name: string; email: string; status: string }>>(`/admin/users?limit=8&q=${encodeURIComponent(q.trim())}`);
        setResults(r.items.filter((u) => u.status === "ACTIVE"));
      } catch { setResults([]); }
    }, 250);
    return () => clearTimeout(t);
  }, [q]);
  if (person) {
    return (
      <div className="row" style={{ gap: 8 }}>
        <Badge icon="user">{person.name}</Badge>{person.email && <span className="small muted mono">{person.email}</span>}
        <Button size="sm" variant="ghost" onClick={() => onChange(null)}>Trocar</Button>
      </div>
    );
  }
  return (
    <div className="stack" style={{ gap: 6 }}>
      <Field label="Pessoa" hint="Pesquisa por nome ou email (só contas activas).">{({ id, describedBy }) => (
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
      {q.trim().length >= 2 && results.length === 0 && <span className="small muted">Sem contas activas com esse nome ou email.</span>}
    </div>
  );
}

function History({ rows, nextCursor }: { rows: CampaignRow[]; nextCursor?: string }) {
  const router = useRouter();
  const toast = useToast();
  const [busy, setBusy] = useState<string | null>(null);
  const retry = async (id: string) => {
    setBusy(id);
    try {
      const r = await post<{ requeued: number }>(`/admin/emails/campaigns/${id}/retry`);
      toast({ tone: "pos", title: `${r.requeued} ${r.requeued === 1 ? "email voltou" : "emails voltaram"} à fila` });
      router.refresh();
    } catch (e) { toast({ tone: "neg", title: "Não foi possível repetir", body: (e as Error).message }); }
    finally { setBusy(null); }
  };
  return (
    <section className="stack" aria-labelledby="hist-h">
      <div className="row"><h2 className="h2" id="hist-h">Enviados</h2><div className="spacer" />
        <Button size="sm" variant="ghost" icon="refresh" onClick={() => router.refresh()}>Actualizar</Button></div>
      {rows.length === 0 ? <Card className="state-box"><p className="muted">Ainda não enviaste emails.</p></Card> : (
        <div className="tbl-wrap"><table className="tbl">
          <thead><tr><th>Data/hora</th><th>Assunto</th><th>Destinatários</th><th>Por</th><th>Estado</th><th><span className="sr-only">Acções</span></th></tr></thead>
          <tbody>{rows.map((r) => (
            <tr key={r.id}>
              <td className="mono small">{formatDateTime(r.createdAt)}</td>
              <td>{r.subject}</td>
              <td className="small">{r.audienceLabel}</td>
              <td className="small muted">{r.createdBy}</td>
              <td className="small">
                <span className="row" style={{ gap: 6, flexWrap: "wrap" }}>
                  <Badge tone={r.pending === 0 && r.failed === 0 ? "pos" : "neutral"} icon={r.pending === 0 && r.failed === 0 ? "check" : undefined}>
                    <span className="mono">{r.sent}/{r.total}</span> enviados
                  </Badge>
                  {r.pending > 0 && <Badge tone="info" icon="clock"><span className="mono">{r.pending}</span> na fila</Badge>}
                  {r.failed > 0 && <Badge tone="neg" icon="warn"><span className="mono">{r.failed}</span> falharam</Badge>}
                  {r.skipped > 0 && <span className="muted"><span className="mono">{r.skipped}</span> ignorados</span>}
                </span>
              </td>
              <td className="actions-cell">{r.failed > 0 && <Button size="sm" variant="ghost" icon="refresh" loading={busy === r.id} onClick={() => retry(r.id)}>Repetir falhados</Button>}</td>
            </tr>
          ))}</tbody>
        </table></div>
      )}
      {nextCursor && <div className="row"><div className="spacer" /><a className="btn sm out" href={`/admin?tab=emails&cursor=${nextCursor}`}>Mais antigos</a></div>}
    </section>
  );
}
