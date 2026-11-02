"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type ReactNode } from "react";
import { Alert } from "@/components/ui/Alert";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Field, Input, Select, TextArea } from "@/components/ui/Field";
import { ConfirmDialog, Modal } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";
import { del, patch, post, put } from "@/lib/api-client";
import { STATUS_LABEL, STATUS_TONE, type EditorialStatus } from "@/lib/authoring-types";
import { isApiError } from "@/lib/errors";

interface TaskAdmin { id: string; position: number; title: string; points: number; exerciseVersionId: string; exerciseCode?: string; exerciseTitle?: string; exerciseType?: string; editorialStatus?: EditorialStatus; attempts: number }
interface RoomAdmin { id: string; position: number; title: string; status: string; difficulty: number; estMinutes: number; description?: string; objectives?: string[]; context?: string; tasks: TaskAdmin[] }
interface ModuleAdmin { id: string; position: number; title: string; summary?: string; rooms: RoomAdmin[] }
export interface TrackAdmin { id: string; slug: string; title: string; status: string; summary?: string; description?: string; difficulty?: number; topic?: string; estHours?: number; modules: ModuleAdmin[] }
export interface ExamAdmin { id: string; slug: string; title: string; durationMin: number; topic: string; count: number; passMark: number; status: string }
export interface ExerciseOption { versionId: string; label: string; status: EditorialStatus }

const CONTENT_STATUS: Record<string, [string, "pos" | "neutral" | "info"]> = { PUBLISHED: ["Publicada", "pos"], DRAFT: ["Rascunho", "neutral"], COMING_SOON: ["Fase posterior", "info"] };

type Dialog =
  | { kind: "track" } | { kind: "module"; trackId: string } | { kind: "room"; moduleId: string }
  | { kind: "task"; roomId: string } | { kind: "exam" } | { kind: "remove"; task: TaskAdmin }
  | { kind: "editTrack"; track: TrackAdmin } | { kind: "editRoom"; room: RoomAdmin } | { kind: "editTask"; task: TaskAdmin } | null;

export function ContentAdmin({ tracks, exams, exercises, inReview }: { tracks: TrackAdmin[]; exams: ExamAdmin[]; exercises: ExerciseOption[]; inReview: number }) {
  const router = useRouter();
  const toast = useToast();
  const [dialog, setDialog] = useState<Dialog>(null);
  const [form, setForm] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const set = (k: string) => (v: string) => setForm((f) => ({ ...f, [k]: v }));
  const open = (d: Dialog, defaults: Record<string, string> = {}) => { setForm(defaults); setError(null); setDialog(d); };
  const run = async (fn: () => Promise<unknown>, ok: string) => {
    setBusy(true); setError(null);
    try { await fn(); toast({ tone: "pos", title: ok }); setDialog(null); router.refresh(); }
    catch (e) { setError(isApiError(e) ? (Object.values(e.fields)[0] ?? e.message) : "Tenta de novo."); }
    finally { setBusy(false); }
  };
  const status = (url: string, value: string) => run(() => patch(url, { status: value }), "Estado actualizado");

  const field = (label: string, key: string, el: "input" | "textarea" = "input", type = "text") => (
    <Field label={label}>{({ id }) => el === "textarea"
      ? <TextArea id={id} value={form[key] ?? ""} onChange={(e) => set(key)(e.target.value)} />
      : <Input id={id} type={type} value={form[key] ?? ""} onChange={(e) => set(key)(e.target.value)} />}</Field>
  );

  const dialogBody: Record<string, { title: string; body: ReactNode; submit: () => void }> = {
    track: { title: "Nova trilha", body: <>{field("Título", "title")}{field("Identificador (minúsculas e -)", "slug")}{field("Resumo", "summary")}
      {field("Descrição", "description", "textarea")}{field("Tema (ex.: Contabilidade, Fiscalidade)", "topic")}
      {field("Dificuldade (1–4)", "difficulty", "input", "number")}</>,
      submit: () => run(() => post("/admin/content/tracks", { ...form, difficulty: Number(form.difficulty || 1), status: "DRAFT", courses: ["CONT", "ECON", "GEST"] }), "Trilha criada em rascunho") },
    module: { title: "Novo módulo", body: <>{field("Título", "title")}{field("Resumo", "summary")}</>,
      submit: () => dialog?.kind === "module" && run(() => post(`/admin/content/tracks/${dialog.trackId}/modules`, form), "Módulo criado") },
    room: { title: "Nova sala", body: <>{field("Título", "title")}{field("Descrição curta", "description")}{field("Contexto (markdown)", "context", "textarea")}
      {field("Dificuldade (1–4)", "difficulty", "input", "number")}{field("Duração estimada (min)", "estMinutes", "input", "number")}</>,
      submit: () => dialog?.kind === "room" && run(() => post(`/admin/content/modules/${dialog.moduleId}/rooms`, {
        ...form, difficulty: Number(form.difficulty || 1), estMinutes: Number(form.estMinutes || 20), status: "DRAFT" }), "Sala criada em rascunho") },
    task: { title: "Nova tarefa", body: <>
      <Field label="Exercício">{({ id }) => (
        <Select id={id} value={form.exerciseVersionId ?? ""} onChange={(e) => set("exerciseVersionId")(e.target.value)}>
          <option value="">Escolhe…</option>
          {exercises.map((x) => <option key={x.versionId} value={x.versionId}>{x.label} · {STATUS_LABEL[x.status]}</option>)}
        </Select>)}</Field>
      {field("Título da tarefa", "title")}{field("Pontos", "points", "input", "number")}
      <p className="small muted">Só os exercícios publicados ficam visíveis aos estudantes.</p></>,
      submit: () => dialog?.kind === "task" && run(() => post(`/admin/content/rooms/${dialog.roomId}/tasks`, { ...form, points: Number(form.points || 40) }), "Tarefa acrescentada") },
    editTrack: { title: "Editar trilha", body: <>{field("Título", "title")}{field("Resumo", "summary")}
      {field("Descrição", "description", "textarea")}{field("Tema", "topic")}{field("Dificuldade (1–4)", "difficulty", "input", "number")}
      {field("Horas estimadas", "estHours", "input", "number")}</>,
      submit: () => dialog?.kind === "editTrack" && run(() => put(`/admin/content/tracks/${dialog.track.id}`, {
        slug: dialog.track.slug, title: form.title, summary: form.summary, description: form.description, topic: form.topic,
        difficulty: Number(form.difficulty || 1), estHours: form.estHours ? Number(form.estHours) : null }), "Trilha actualizada") },
    editRoom: { title: "Editar sala", body: <>{field("Título", "title")}{field("Descrição curta", "description")}
      {field("Objectivos (um por linha)", "objectives", "textarea")}{field("Contexto (markdown)", "context", "textarea")}
      {field("Dificuldade (1–4)", "difficulty", "input", "number")}{field("Duração estimada (min)", "estMinutes", "input", "number")}</>,
      submit: () => dialog?.kind === "editRoom" && run(() => put(`/admin/content/rooms/${dialog.room.id}`, {
        title: form.title, description: form.description, context: form.context,
        objectives: (form.objectives ?? "").split("\n").map((x) => x.trim()).filter(Boolean),
        difficulty: Number(form.difficulty || 1), estMinutes: Number(form.estMinutes || 20) }), "Sala actualizada") },
    editTask: { title: "Editar tarefa", body: <>
      <Field label="Exercício (versão)">{({ id }) => (
        <Select id={id} value={form.exerciseVersionId ?? ""} onChange={(e) => set("exerciseVersionId")(e.target.value)}>
          {!exercises.some((x) => x.versionId === form.exerciseVersionId) && <option value={form.exerciseVersionId}>Versão actual</option>}
          {exercises.map((x) => <option key={x.versionId} value={x.versionId}>{x.label} · {STATUS_LABEL[x.status]}</option>)}
        </Select>)}</Field>
      {field("Título da tarefa", "title")}{field("Pontos", "points", "input", "number")}
      <p className="small muted">Mudar a versão só afecta as tentativas novas; as tentativas já feitas mantêm a versão com que foram resolvidas.</p></>,
      submit: () => dialog?.kind === "editTask" && run(() => put(`/admin/content/tasks/${dialog.task.id}`, {
        exerciseVersionId: form.exerciseVersionId, title: form.title, points: Number(form.points || 0) }), "Tarefa actualizada") },
    exam: { title: "Novo exame", body: <>{field("Título", "title")}{field("Identificador (minúsculas e -)", "slug")}
      {field("Tema das perguntas (prefixo, ex.: IVA, PGC, Análise)", "topic")}{field("Número de perguntas", "count", "input", "number")}
      {field("Duração (min)", "durationMin", "input", "number")}{field("Nota mínima (0–20)", "passMark", "input", "number")}
      <p className="small muted">As perguntas são sorteadas entre exercícios publicados de escolha múltipla e resposta numérica com esse tema.</p></>,
      submit: () => run(() => post("/admin/content/exams", { ...form, count: Number(form.count || 10), durationMin: Number(form.durationMin || 30),
        passMark: Number(form.passMark || 10), status: "DRAFT" }), "Exame criado em rascunho") },
  };
  const current = dialog && dialog.kind !== "remove" ? dialogBody[dialog.kind] : null;

  return (
    <div className="stack" style={{ gap: 20 }}>
      <Card lit className="row" style={{ gap: 16 }}>
        <div style={{ flex: 1, minWidth: 260 }} className="stack">
          <h2 className="h2">Pacote de conteúdo</h2>
          <p className="small muted">Importa as trilhas, salas, exercícios e exames incluídos na plataforma (pacote v1). A importação é segura de repetir:
            só cria o que falta. Os exercícios entram <b>em revisão</b> e só ficam visíveis aos estudantes depois de aprovados e publicados.</p>
        </div>
        <Button variant="pri" icon="download" loading={busy && !dialog} onClick={() => run(async () => {
          const r = await post<{ exercisesCreated: number; roomsCreated: number; examsCreated: number }>("/admin/content/import");
          toast({ tone: "info", title: `${r.exercisesCreated} exercícios, ${r.roomsCreated} salas e ${r.examsCreated} exames criados` });
        }, "Importação concluída")}>Importar pacote</Button>
      </Card>
      {inReview > 0 && (
        <Alert tone="acc" title={`${inReview} exercícios à espera de revisão`}
          action={<Link className="btn sm pri" href="/autoria?tab=revisao">Rever e publicar</Link>}>
          As tarefas destes exercícios aparecem aos estudantes como «Em revisão» até serem publicados.
        </Alert>
      )}
      {error && !dialog && <Alert tone="neg" title="Não foi possível concluir">{error}</Alert>}

      <div className="row"><h2 className="h2">Trilhas</h2><div className="spacer" /><Button variant="out" icon="plus" onClick={() => open({ kind: "track" })}>Nova trilha</Button></div>
      {tracks.map((t) => (
        <Card key={t.id} className="stack">
          <div className="row">
            <h3 className="h2" style={{ flex: 1 }}>{t.title}</h3>
            <Badge tone={CONTENT_STATUS[t.status]?.[1] ?? "neutral"}>{CONTENT_STATUS[t.status]?.[0] ?? t.status}</Badge>
            <Select aria-label={`Estado da trilha ${t.title}`} style={{ width: 170 }} value={t.status} onChange={(e) => status(`/admin/content/tracks/${t.id}`, e.target.value)}>
              <option value="DRAFT">Rascunho</option><option value="PUBLISHED">Publicada</option><option value="COMING_SOON">Fase posterior</option>
            </Select>
            <Button size="sm" variant="ghost" icon="pen" onClick={() => open({ kind: "editTrack", track: t }, { title: t.title, summary: t.summary ?? "",
              description: t.description ?? "", topic: t.topic ?? "", difficulty: String(t.difficulty ?? 1), estHours: t.estHours != null ? String(t.estHours) : "" })}>Editar</Button>
            <Button size="sm" variant="ghost" icon="plus" onClick={() => open({ kind: "module", trackId: t.id })}>Módulo</Button>
          </div>
          {t.modules.map((m) => (
            <details key={m.id} className="card flat" style={{ padding: 14 }}>
              <summary className="row" style={{ cursor: "pointer", gap: 10 }}>
                <span className="mono muted">{String(m.position).padStart(2, "0")}</span><b style={{ flex: 1 }}>{m.title}</b>
                <span className="small muted">{m.rooms.length} salas</span>
              </summary>
              <div className="stack" style={{ marginTop: 12 }}>
                {m.rooms.map((r) => (
                  <div key={r.id} className="card" style={{ padding: 12 }}>
                    <div className="row" style={{ gap: 8 }}>
                      <b style={{ flex: 1 }}>{r.position}. {r.title}</b>
                      <Select aria-label={`Estado da sala ${r.title}`} style={{ width: 150 }} value={r.status} onChange={(e) => status(`/admin/content/rooms/${r.id}`, e.target.value)}>
                        <option value="DRAFT">Rascunho</option><option value="PUBLISHED">Publicada</option><option value="COMING_SOON">Fase posterior</option>
                      </Select>
                      <Button size="sm" variant="ghost" icon="pen" aria-label={`Editar sala ${r.title}`} onClick={() => open({ kind: "editRoom", room: r }, {
                        title: r.title, description: r.description ?? "", context: r.context ?? "", objectives: (r.objectives ?? []).join("\n"),
                        difficulty: String(r.difficulty), estMinutes: String(r.estMinutes) })} />
                      <Button size="sm" variant="ghost" icon="plus" onClick={() => open({ kind: "task", roomId: r.id }, { points: "40" })}>Tarefa</Button>
                    </div>
                    <ul className="plain stack small" style={{ marginTop: 8, gap: 4 }}>
                      {r.tasks.map((k) => (
                        <li key={k.id} className="row" style={{ gap: 8 }}>
                          <span className="mono muted">{k.position}.</span>
                          <span style={{ flex: 1 }}>{k.title} <span className="muted">· {k.exerciseCode} · tipo {k.exerciseType} · {k.points} pts</span></span>
                          {k.editorialStatus && <Badge tone={STATUS_TONE[k.editorialStatus]}>{STATUS_LABEL[k.editorialStatus]}</Badge>}
                          <span className="muted">{k.attempts} tentativas</span>
                          <Button size="sm" variant="ghost" icon="pen" aria-label={`Editar tarefa ${k.title}`} onClick={() => open({ kind: "editTask", task: k },
                            { title: k.title, points: String(k.points), exerciseVersionId: k.exerciseVersionId })} />
                          {k.attempts === 0 && <Button size="sm" variant="ghost" icon="trash" aria-label={`Remover tarefa ${k.title}`} onClick={() => setDialog({ kind: "remove", task: k })} />}
                        </li>
                      ))}
                      {r.tasks.length === 0 && <li className="muted">Sem tarefas.</li>}
                    </ul>
                  </div>
                ))}
                <div><Button size="sm" variant="ghost" icon="plus" onClick={() => open({ kind: "room", moduleId: m.id }, { difficulty: "1", estMinutes: "20" })}>Sala</Button></div>
              </div>
            </details>
          ))}
          {t.modules.length === 0 && <p className="small muted">Sem módulos.</p>}
        </Card>
      ))}

      <div className="row"><h2 className="h2">Exames</h2><div className="spacer" /><Button variant="out" icon="plus" onClick={() => open({ kind: "exam" }, { count: "10", durationMin: "30", passMark: "10" })}>Novo exame</Button></div>
      <div className="tbl-wrap">
        <table className="tbl">
          <thead><tr><th>Exame</th><th>Tema</th><th className="num">Perguntas</th><th className="num">Duração</th><th>Estado</th></tr></thead>
          <tbody>{exams.map((x) => (
            <tr key={x.id}>
              <td><b>{x.title}</b><div className="small muted mono">{x.slug}</div></td><td>{x.topic}</td>
              <td className="num">{x.count}</td><td className="num">{x.durationMin} min</td>
              <td><Select aria-label={`Estado do exame ${x.title}`} value={x.status} style={{ width: 150 }}
                onChange={(e) => run(() => patch(`/admin/content/exams/${x.id}`, { status: e.target.value }), "Estado actualizado")}>
                <option value="DRAFT">Rascunho</option><option value="PUBLISHED">Publicado</option></Select></td>
            </tr>
          ))}</tbody>
        </table>
      </div>

      <Modal open={!!current} onClose={() => setDialog(null)} title={current?.title ?? ""}
        actions={<><Button variant="out" onClick={() => setDialog(null)}>Cancelar</Button><Button variant="pri" loading={busy} onClick={() => current?.submit()}>Guardar</Button></>}>
        <div className="stack" style={{ color: "var(--text)" }}>{current?.body}{error && <div className="alert neg" role="alert">{error}</div>}</div>
      </Modal>
      <ConfirmDialog open={dialog?.kind === "remove"} onCancel={() => setDialog(null)} destructive confirmLabel="Remover tarefa"
        title={`Remover a tarefa «${dialog?.kind === "remove" ? dialog.task.title : ""}»?`} body="A tarefa ainda não tem tentativas de estudantes. Fica registado em auditoria."
        onConfirm={() => dialog?.kind === "remove" && run(() => del(`/admin/content/tasks/${dialog.task.id}`), "Tarefa removida")} />
    </div>
  );
}
