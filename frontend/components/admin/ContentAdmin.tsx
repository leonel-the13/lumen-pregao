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
import { STATUS_TONE, type EditorialStatus } from "@/lib/authoring-types";
import { isApiError } from "@/lib/errors";
import { useT } from "@/lib/i18n/client";
import type { MessageKey } from "@/lib/i18n/translate";

interface TaskAdmin { id: string; position: number; title: string; points: number; exerciseVersionId: string; exerciseCode?: string; exerciseTitle?: string; exerciseType?: string; editorialStatus?: EditorialStatus; attempts: number }
interface RoomAdmin { id: string; position: number; title: string; status: string; difficulty: number; estMinutes: number; description?: string; objectives?: string[]; context?: string; tasks: TaskAdmin[] }
interface ModuleAdmin { id: string; position: number; title: string; summary?: string; rooms: RoomAdmin[] }
export interface TrackAdmin { id: string; slug: string; title: string; status: string; summary?: string; description?: string; difficulty?: number; topic?: string; estHours?: number; modules: ModuleAdmin[] }
export interface ExamAdmin { id: string; slug: string; title: string; durationMin: number; topic: string; count: number; passMark: number; status: string }
export interface ExerciseOption { versionId: string; label: string; status: EditorialStatus }

const CONTENT_TONE: Record<string, "pos" | "neutral" | "info"> = { PUBLISHED: "pos", DRAFT: "neutral", COMING_SOON: "info" };
type ContentStatus = "PUBLISHED" | "DRAFT" | "COMING_SOON";

type Dialog =
  | { kind: "track" } | { kind: "module"; trackId: string } | { kind: "room"; moduleId: string }
  | { kind: "task"; roomId: string } | { kind: "exam" } | { kind: "remove"; task: TaskAdmin }
  | { kind: "editTrack"; track: TrackAdmin } | { kind: "editRoom"; room: RoomAdmin } | { kind: "editTask"; task: TaskAdmin } | null;

export function ContentAdmin({ tracks, exams, exercises, inReview }: { tracks: TrackAdmin[]; exams: ExamAdmin[]; exercises: ExerciseOption[]; inReview: number }) {
  const router = useRouter();
  const toast = useToast();
  const t = useT();
  const [dialog, setDialog] = useState<Dialog>(null);
  const [form, setForm] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const set = (k: string) => (v: string) => setForm((f) => ({ ...f, [k]: v }));
  const open = (d: Dialog, defaults: Record<string, string> = {}) => { setForm(defaults); setError(null); setDialog(d); };
  const run = async (fn: () => Promise<unknown>, ok: string) => {
    setBusy(true); setError(null);
    try { await fn(); toast({ tone: "pos", title: ok }); setDialog(null); router.refresh(); }
    catch (e) { setError(isApiError(e) ? (Object.values(e.fields)[0] ?? e.message) : t("admin.content.tryAgain")); }
    finally { setBusy(false); }
  };
  const status = (url: string, value: string) => run(() => patch(url, { status: value }), t("admin.content.statusUpdated"));

  const field = (label: MessageKey, key: string, el: "input" | "textarea" = "input", type = "text") => (
    <Field label={t(label)}>{({ id }) => el === "textarea"
      ? <TextArea id={id} value={form[key] ?? ""} onChange={(e) => set(key)(e.target.value)} />
      : <Input id={id} type={type} value={form[key] ?? ""} onChange={(e) => set(key)(e.target.value)} />}</Field>
  );

  const dialogBody: Record<string, { title: string; body: ReactNode; submit: () => void }> = {
    track: { title: t("admin.content.newTrack"), body: <>{field("admin.content.f.title", "title")}{field("admin.content.f.slug", "slug")}{field("admin.content.f.summary", "summary")}
      {field("admin.content.f.description", "description", "textarea")}{field("admin.content.f.topicTrack", "topic")}
      {field("admin.content.f.difficulty", "difficulty", "input", "number")}</>,
      submit: () => run(() => post("/admin/content/tracks", { ...form, difficulty: Number(form.difficulty || 1), status: "DRAFT", courses: ["CONT", "ECON", "GEST"] }), t("admin.content.trackCreated")) },
    module: { title: t("admin.content.newModule"), body: <>{field("admin.content.f.title", "title")}{field("admin.content.f.summary", "summary")}</>,
      submit: () => dialog?.kind === "module" && run(() => post(`/admin/content/tracks/${dialog.trackId}/modules`, form), t("admin.content.moduleCreated")) },
    room: { title: t("admin.content.newRoom"), body: <>{field("admin.content.f.title", "title")}{field("admin.content.f.shortDescription", "description")}{field("admin.content.f.context", "context", "textarea")}
      {field("admin.content.f.difficulty", "difficulty", "input", "number")}{field("admin.content.f.estMinutes", "estMinutes", "input", "number")}</>,
      submit: () => dialog?.kind === "room" && run(() => post(`/admin/content/modules/${dialog.moduleId}/rooms`, {
        ...form, difficulty: Number(form.difficulty || 1), estMinutes: Number(form.estMinutes || 20), status: "DRAFT" }), t("admin.content.roomCreated")) },
    task: { title: t("admin.content.newTask"), body: <>
      <Field label={t("admin.content.f.exercise")}>{({ id }) => (
        <Select id={id} value={form.exerciseVersionId ?? ""} onChange={(e) => set("exerciseVersionId")(e.target.value)}>
          <option value="">{t("admin.content.choose")}</option>
          {exercises.map((x) => <option key={x.versionId} value={x.versionId}>{x.label} · {t(`authoring.status.${x.status}`)}</option>)}
        </Select>)}</Field>
      {field("admin.content.f.taskTitle", "title")}{field("admin.content.f.points", "points", "input", "number")}
      <p className="small muted">{t("admin.content.onlyPublished")}</p></>,
      submit: () => dialog?.kind === "task" && run(() => post(`/admin/content/rooms/${dialog.roomId}/tasks`, { ...form, points: Number(form.points || 40) }), t("admin.content.taskAdded")) },
    editTrack: { title: t("admin.content.editTrack"), body: <>{field("admin.content.f.title", "title")}{field("admin.content.f.summary", "summary")}
      {field("admin.content.f.description", "description", "textarea")}{field("admin.content.f.topic", "topic")}{field("admin.content.f.difficulty", "difficulty", "input", "number")}
      {field("admin.content.f.estHours", "estHours", "input", "number")}</>,
      submit: () => dialog?.kind === "editTrack" && run(() => put(`/admin/content/tracks/${dialog.track.id}`, {
        slug: dialog.track.slug, title: form.title, summary: form.summary, description: form.description, topic: form.topic,
        difficulty: Number(form.difficulty || 1), estHours: form.estHours ? Number(form.estHours) : null }), t("admin.content.trackUpdated")) },
    editRoom: { title: t("admin.content.editRoom"), body: <>{field("admin.content.f.title", "title")}{field("admin.content.f.shortDescription", "description")}
      {field("admin.content.f.objectives", "objectives", "textarea")}{field("admin.content.f.context", "context", "textarea")}
      {field("admin.content.f.difficulty", "difficulty", "input", "number")}{field("admin.content.f.estMinutes", "estMinutes", "input", "number")}</>,
      submit: () => dialog?.kind === "editRoom" && run(() => put(`/admin/content/rooms/${dialog.room.id}`, {
        title: form.title, description: form.description, context: form.context,
        objectives: (form.objectives ?? "").split("\n").map((x) => x.trim()).filter(Boolean),
        difficulty: Number(form.difficulty || 1), estMinutes: Number(form.estMinutes || 20) }), t("admin.content.roomUpdated")) },
    editTask: { title: t("admin.content.editTask"), body: <>
      <Field label={t("admin.content.f.exerciseVersion")}>{({ id }) => (
        <Select id={id} value={form.exerciseVersionId ?? ""} onChange={(e) => set("exerciseVersionId")(e.target.value)}>
          {!exercises.some((x) => x.versionId === form.exerciseVersionId) && <option value={form.exerciseVersionId}>{t("admin.content.currentVersion")}</option>}
          {exercises.map((x) => <option key={x.versionId} value={x.versionId}>{x.label} · {t(`authoring.status.${x.status}`)}</option>)}
        </Select>)}</Field>
      {field("admin.content.f.taskTitle", "title")}{field("admin.content.f.points", "points", "input", "number")}
      <p className="small muted">{t("admin.content.versionNote")}</p></>,
      submit: () => dialog?.kind === "editTask" && run(() => put(`/admin/content/tasks/${dialog.task.id}`, {
        exerciseVersionId: form.exerciseVersionId, title: form.title, points: Number(form.points || 0) }), t("admin.content.taskUpdated")) },
    exam: { title: t("admin.content.newExam"), body: <>{field("admin.content.f.title", "title")}{field("admin.content.f.slug", "slug")}
      {field("admin.content.f.examTopic", "topic")}{field("admin.content.f.count", "count", "input", "number")}
      {field("admin.content.f.duration", "durationMin", "input", "number")}{field("admin.content.f.passMark", "passMark", "input", "number")}
      <p className="small muted">{t("admin.content.examNote")}</p></>,
      submit: () => run(() => post("/admin/content/exams", { ...form, count: Number(form.count || 10), durationMin: Number(form.durationMin || 30),
        passMark: Number(form.passMark || 10), status: "DRAFT" }), t("admin.content.examCreated")) },
  };
  const current = dialog && dialog.kind !== "remove" ? dialogBody[dialog.kind] : null;

  return (
    <div className="stack" style={{ gap: 20 }}>
      <Card lit className="row" style={{ gap: 16 }}>
        <div style={{ flex: 1, minWidth: 260 }} className="stack">
          <h2 className="h2">{t("admin.content.pack")}</h2>
          <p className="small muted">{t("admin.content.packBody")}</p>
        </div>
        <Button variant="pri" icon="download" loading={busy && !dialog} onClick={() => run(async () => {
          const r = await post<{ exercisesCreated: number; roomsCreated: number; examsCreated: number }>("/admin/content/import");
          toast({ tone: "info", title: t("admin.content.imported", { ex: r.exercisesCreated, rooms: r.roomsCreated, exams: r.examsCreated }) });
        }, t("admin.content.importDone"))}>{t("admin.content.import")}</Button>
      </Card>
      {inReview > 0 && (
        <Alert tone="acc" title={t("admin.content.awaiting", { n: inReview })}
          action={<Link className="btn sm pri" href="/autoria?tab=revisao">{t("admin.content.review")}</Link>}>
          {t("admin.content.awaitingBody")}
        </Alert>
      )}
      {error && !dialog && <Alert tone="neg" title={t("admin.content.failed")}>{error}</Alert>}

      <div className="row"><h2 className="h2">{t("admin.content.tracks")}</h2><div className="spacer" /><Button variant="out" icon="plus" onClick={() => open({ kind: "track" })}>{t("admin.content.newTrack")}</Button></div>
      {tracks.map((tr) => (
        <Card key={tr.id} className="stack">
          <div className="row">
            <h3 className="h2" style={{ flex: 1 }}>{tr.title}</h3>
            <Badge tone={CONTENT_TONE[tr.status] ?? "neutral"}>{tr.status in CONTENT_TONE ? t(`admin.content.status.${tr.status as ContentStatus}`) : tr.status}</Badge>
            <Select aria-label={t("admin.content.trackState", { title: tr.title })} style={{ width: 170 }} value={tr.status} onChange={(e) => status(`/admin/content/tracks/${tr.id}`, e.target.value)}>
              <option value="DRAFT">{t("admin.content.status.DRAFT")}</option><option value="PUBLISHED">{t("admin.content.status.PUBLISHED")}</option><option value="COMING_SOON">{t("admin.content.status.COMING_SOON")}</option>
            </Select>
            <Button size="sm" variant="ghost" icon="pen" onClick={() => open({ kind: "editTrack", track: tr }, { title: tr.title, summary: tr.summary ?? "",
              description: tr.description ?? "", topic: tr.topic ?? "", difficulty: String(tr.difficulty ?? 1), estHours: tr.estHours != null ? String(tr.estHours) : "" })}>{t("common.edit")}</Button>
            <Button size="sm" variant="ghost" icon="plus" onClick={() => open({ kind: "module", trackId: tr.id })}>{t("admin.content.module")}</Button>
          </div>
          {tr.modules.map((m) => (
            <details key={m.id} className="card flat" style={{ padding: 14 }}>
              <summary className="row" style={{ cursor: "pointer", gap: 10 }}>
                <span className="mono muted">{String(m.position).padStart(2, "0")}</span><b style={{ flex: 1 }}>{m.title}</b>
                <span className="small muted">{t("admin.content.rooms", { n: m.rooms.length })}</span>
              </summary>
              <div className="stack" style={{ marginTop: 12 }}>
                {m.rooms.map((r) => (
                  <div key={r.id} className="card" style={{ padding: 12 }}>
                    <div className="row" style={{ gap: 8 }}>
                      <b style={{ flex: 1 }}>{r.position}. {r.title}</b>
                      <Select aria-label={t("admin.content.roomState", { title: r.title })} style={{ width: 150 }} value={r.status} onChange={(e) => status(`/admin/content/rooms/${r.id}`, e.target.value)}>
                        <option value="DRAFT">{t("admin.content.status.DRAFT")}</option><option value="PUBLISHED">{t("admin.content.status.PUBLISHED")}</option><option value="COMING_SOON">{t("admin.content.status.COMING_SOON")}</option>
                      </Select>
                      <Button size="sm" variant="ghost" icon="pen" aria-label={t("admin.content.editRoomAria", { title: r.title })} onClick={() => open({ kind: "editRoom", room: r }, {
                        title: r.title, description: r.description ?? "", context: r.context ?? "", objectives: (r.objectives ?? []).join("\n"),
                        difficulty: String(r.difficulty), estMinutes: String(r.estMinutes) })} />
                      <Button size="sm" variant="ghost" icon="plus" onClick={() => open({ kind: "task", roomId: r.id }, { points: "40" })}>{t("admin.content.task")}</Button>
                    </div>
                    <ul className="plain stack small" style={{ marginTop: 8, gap: 4 }}>
                      {r.tasks.map((k) => (
                        <li key={k.id} className="row" style={{ gap: 8 }}>
                          <span className="mono muted">{k.position}.</span>
                          <span style={{ flex: 1 }}>{k.title} <span className="muted">{t("admin.content.taskMeta", { code: k.exerciseCode ?? "", type: k.exerciseType ?? "", points: k.points })}</span></span>
                          {k.editorialStatus && <Badge tone={STATUS_TONE[k.editorialStatus]}>{t(`authoring.status.${k.editorialStatus}`)}</Badge>}
                          <span className="muted">{t("admin.content.attempts", { n: k.attempts })}</span>
                          <Button size="sm" variant="ghost" icon="pen" aria-label={t("admin.content.editTaskAria", { title: k.title })} onClick={() => open({ kind: "editTask", task: k },
                            { title: k.title, points: String(k.points), exerciseVersionId: k.exerciseVersionId })} />
                          {k.attempts === 0 && <Button size="sm" variant="ghost" icon="trash" aria-label={t("admin.content.removeTaskAria", { title: k.title })} onClick={() => setDialog({ kind: "remove", task: k })} />}
                        </li>
                      ))}
                      {r.tasks.length === 0 && <li className="muted">{t("admin.content.noTasks")}</li>}
                    </ul>
                  </div>
                ))}
                <div><Button size="sm" variant="ghost" icon="plus" onClick={() => open({ kind: "room", moduleId: m.id }, { difficulty: "1", estMinutes: "20" })}>{t("admin.content.room")}</Button></div>
              </div>
            </details>
          ))}
          {tr.modules.length === 0 && <p className="small muted">{t("admin.content.noModules")}</p>}
        </Card>
      ))}

      <div className="row"><h2 className="h2">{t("admin.content.exams")}</h2><div className="spacer" /><Button variant="out" icon="plus" onClick={() => open({ kind: "exam" }, { count: "10", durationMin: "30", passMark: "10" })}>{t("admin.content.newExam")}</Button></div>
      <div className="tbl-wrap">
        <table className="tbl">
          <thead><tr><th>{t("admin.content.exam")}</th><th>{t("admin.content.topic")}</th><th className="num">{t("admin.content.questions")}</th><th className="num">{t("admin.content.duration")}</th><th>{t("admin.content.state")}</th></tr></thead>
          <tbody>{exams.map((x) => (
            <tr key={x.id}>
              <td><b>{x.title}</b><div className="small muted mono">{x.slug}</div></td><td>{x.topic}</td>
              <td className="num">{x.count}</td><td className="num">{x.durationMin} min</td>
              <td><Select aria-label={t("admin.content.examState", { title: x.title })} value={x.status} style={{ width: 150 }}
                onChange={(e) => run(() => patch(`/admin/content/exams/${x.id}`, { status: e.target.value }), t("admin.content.statusUpdated"))}>
                <option value="DRAFT">{t("admin.content.statusExam.DRAFT")}</option><option value="PUBLISHED">{t("admin.content.statusExam.PUBLISHED")}</option></Select></td>
            </tr>
          ))}</tbody>
        </table>
      </div>

      <Modal open={!!current} onClose={() => setDialog(null)} title={current?.title ?? ""}
        actions={<><Button variant="out" onClick={() => setDialog(null)}>{t("common.cancel")}</Button><Button variant="pri" loading={busy} onClick={() => current?.submit()}>{t("common.save")}</Button></>}>
        <div className="stack" style={{ color: "var(--text)" }}>{current?.body}{error && <div className="alert neg" role="alert">{error}</div>}</div>
      </Modal>
      <ConfirmDialog open={dialog?.kind === "remove"} onCancel={() => setDialog(null)} destructive confirmLabel={t("admin.content.removeTask")}
        title={t("admin.content.removeTitle", { title: dialog?.kind === "remove" ? dialog.task.title : "" })} body={t("admin.content.removeBody")}
        onConfirm={() => dialog?.kind === "remove" && run(() => del(`/admin/content/tasks/${dialog.task.id}`), t("admin.content.taskRemoved"))} />
    </div>
  );
}
