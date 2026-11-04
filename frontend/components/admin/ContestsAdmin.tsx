"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Field, Input, Select, TextArea } from "@/components/ui/Field";
import { ConfirmDialog, Modal } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";
import { post } from "@/lib/api-client";
import { formatDateTime } from "@/lib/format";
import { useLocale, useT } from "@/lib/i18n/client";

export interface AdminContestRow { id: string; slug: string; title: string; mode: string; state: string; startsAt: string; endsAt: string; teams: number; problems: number; pendingSubmissions: number }
interface Published { versionId: string; label: string }

const STATES = ["DRAFT", "REGISTRATION", "WAITING_ROOM", "RUNNING", "FROZEN", "SUBMISSIONS_CLOSED", "FINAL_EVALUATION", "RESULTS_PUBLISHED", "CLOSED"] as const;
const isState = (s: string): s is (typeof STATES)[number] => (STATES as readonly string[]).includes(s);

function local(d: Date) {
  const p = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`;
}

/** Gestão de contests (organizador). As transições temporais são do servidor; aqui só as manuais. */
export function ContestsAdmin({ rows, exercises }: { rows: AdminContestRow[]; exercises: Published[] }) {
  const router = useRouter();
  const toast = useToast();
  const t = useT();
  const locale = useLocale();
  const [creating, setCreating] = useState(false);
  const [confirm, setConfirm] = useState<{ row: AdminContestRow; action: "publish" | "publish-results" | "close" } | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const base = new Date(Date.now() + 86400000);
  const [f, setF] = useState({ slug: "", title: "", description: "", mode: "TEAM", minTeam: 2, maxTeam: 5,
    registrationOpensAt: local(new Date()), registrationClosesAt: local(base), startsAt: local(new Date(base.getTime() + 3600000)),
    freezeAt: local(new Date(base.getTime() + 3 * 3600000)), endsAt: local(new Date(base.getTime() + 4 * 3600000)), lockMinutes: 30, wrongPenaltyMinutes: 10,
    problems: [{ label: "A", title: "", companyName: "", exerciseVersionId: "", points: 100 }] });
  const set = (k: string, v: unknown) => setF((x) => ({ ...x, [k]: v }));
  const create = async () => {
    setErr(null);
    try {
      const iso = (s: string) => new Date(s).toISOString();
      await post("/admin/contests", { ...f, registrationOpensAt: iso(f.registrationOpensAt), registrationClosesAt: iso(f.registrationClosesAt),
        startsAt: iso(f.startsAt), freezeAt: iso(f.freezeAt), endsAt: iso(f.endsAt), problems: f.problems.filter((p) => p.exerciseVersionId) });
      toast({ tone: "pos", title: t("admin.contests.created") });
      setCreating(false);
      router.refresh();
    } catch (e) { setErr((e as Error).message); }
  };
  const act = async () => {
    if (!confirm) return;
    try {
      await post(`/admin/contests/${confirm.row.id}/${confirm.action}`);
      toast({ tone: "pos", title: t("admin.contests.updated") });
      router.refresh();
    } catch (e) { toast({ tone: "neg", title: t("admin.contests.failed"), body: (e as Error).message }); }
    setConfirm(null);
  };
  return (
    <div className="stack">
      <div><Button variant="pri" icon="plus" onClick={() => setCreating(true)}>{t("admin.contests.new")}</Button></div>
      <div className="tbl-wrap">
        <table className="tbl">
          <thead><tr><th>{t("admin.contests.contest")}</th><th>{t("admin.contests.state_")}</th><th>{t("admin.contests.start")}</th><th className="num">{t("admin.contests.teams")}</th><th className="num">{t("admin.contests.problems")}</th><th><span className="sr-only">{t("admin.contests.actions")}</span></th></tr></thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.id}>
                <td><b>{r.title}</b><div className="small muted mono">{r.slug} · {r.mode === "TEAM" ? t("admin.contests.teamsMode") : t("admin.contests.individualMode")}</div></td>
                <td><Badge tone={r.state === "RUNNING" || r.state === "FROZEN" ? "acc" : "neutral"}>{isState(r.state) ? t(`admin.contests.state.${r.state}`) : r.state}</Badge></td>
                <td className="small mono">{formatDateTime(r.startsAt, locale)}</td>
                <td className="num">{r.teams}</td><td className="num">{r.problems}</td>
                <td className="row" style={{ gap: 6 }}>
                  {r.state === "DRAFT" && <Button size="sm" variant="out" onClick={() => setConfirm({ row: r, action: "publish" })}>{t("admin.contests.publish")}</Button>}
                  {(r.state === "SUBMISSIONS_CLOSED" || r.state === "FINAL_EVALUATION") && <Button size="sm" variant="pri" onClick={() => setConfirm({ row: r, action: "publish-results" })}>{t("admin.contests.publishResults")}</Button>}
                  {r.state === "RESULTS_PUBLISHED" && <Button size="sm" variant="out" onClick={() => setConfirm({ row: r, action: "close" })}>{t("admin.contests.close")}</Button>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <ConfirmDialog open={!!confirm} onCancel={() => setConfirm(null)} onConfirm={act}
        title={confirm?.action === "publish" ? t("admin.contests.openRegistration") : confirm?.action === "publish-results" ? t("admin.contests.publishResultsQ") : t("admin.contests.closeQ")}
        confirmLabel={t("common.confirm")}
        body={confirm?.action === "publish-results" ? t("admin.contests.resultsBody") : t("admin.contests.auditBody")} />
      <Modal open={creating} onClose={() => setCreating(false)} title={t("admin.contests.new")}
        actions={<><Button variant="out" onClick={() => setCreating(false)}>{t("common.cancel")}</Button><Button variant="pri" onClick={create}>{t("admin.contests.createDraft")}</Button></>}>
        <div className="stack" style={{ color: "var(--text)", maxHeight: "65vh", overflowY: "auto", paddingRight: 4 }}>
          <div className="grid g2" style={{ gap: 10 }}>
            <Field label={t("admin.contests.title")}>{({ id }) => <Input id={id} value={f.title} onChange={(e) => set("title", e.target.value)} />}</Field>
            <Field label={t("admin.contests.slug")} hint={t("admin.contests.slugHint")}>{({ id, describedBy }) => <Input id={id} aria-describedby={describedBy} className="mono" value={f.slug} onChange={(e) => set("slug", e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, ""))} />}</Field>
          </div>
          <Field label={t("admin.contests.description")}>{({ id }) => <TextArea id={id} value={f.description} onChange={(e) => set("description", e.target.value)} />}</Field>
          <div className="grid g3" style={{ gap: 10 }}>
            <Field label={t("admin.contests.mode")}>{({ id }) => <Select id={id} value={f.mode} onChange={(e) => set("mode", e.target.value)}><option value="TEAM">{t("admin.contests.modeTeam")}</option><option value="INDIVIDUAL">{t("admin.contests.modeIndividual")}</option></Select>}</Field>
            <Field label={t("admin.contests.minTeam")}>{({ id }) => <Input id={id} value={f.minTeam} onChange={(e) => set("minTeam", Number(e.target.value) || 1)} />}</Field>
            <Field label={t("admin.contests.maxTeam")}>{({ id }) => <Input id={id} value={f.maxTeam} onChange={(e) => set("maxTeam", Number(e.target.value) || 1)} />}</Field>
          </div>
          <div className="grid g2" style={{ gap: 10 }}>
            {(["registrationOpensAt", "registrationClosesAt", "startsAt", "freezeAt", "endsAt"] as const).map((k) => (
              <Field key={k} label={t(`admin.contests.times.${k}`)}>{({ id }) => <Input id={id} type="datetime-local" value={f[k]} onChange={(e) => set(k, e.target.value)} />}</Field>
            ))}
            <Field label={t("admin.contests.lock")}>{({ id }) => <Input id={id} value={f.lockMinutes} onChange={(e) => set("lockMinutes", Number(e.target.value) || 0)} />}</Field>
          </div>
          <b className="small">{t("admin.contests.problemsTitle")}</b>
          {f.problems.map((p, i) => (
            <div key={i} className="grid" style={{ gridTemplateColumns: "60px 1fr 1fr 90px", gap: 8 }}>
              <Input aria-label={t("admin.contests.letter")} value={p.label} onChange={(e) => set("problems", f.problems.map((x, j) => (j === i ? { ...x, label: e.target.value.toUpperCase().slice(0, 2) } : x)))} />
              <Select aria-label={t("admin.contests.exercise")} value={p.exerciseVersionId} onChange={(e) => set("problems", f.problems.map((x, j) => (j === i ? { ...x, exerciseVersionId: e.target.value, title: x.title || exercises.find((ex) => ex.versionId === e.target.value)?.label || "" } : x)))}>
                <option value="">{t("admin.contests.exerciseChoose")}</option>{exercises.map((ex) => <option key={ex.versionId} value={ex.versionId}>{ex.label}</option>)}
              </Select>
              <Input aria-label={t("admin.contests.company")} placeholder={t("admin.contests.company")} value={p.companyName} onChange={(e) => set("problems", f.problems.map((x, j) => (j === i ? { ...x, companyName: e.target.value } : x)))} />
              <Input aria-label={t("admin.contests.points")} value={p.points} onChange={(e) => set("problems", f.problems.map((x, j) => (j === i ? { ...x, points: Number(e.target.value) || 0 } : x)))} />
            </div>
          ))}
          <div><Button size="sm" variant="ghost" icon="plus" onClick={() => set("problems", [...f.problems, { label: String.fromCharCode(65 + f.problems.length), title: "", companyName: "", exerciseVersionId: "", points: 100 }])}>{t("admin.contests.problem")}</Button></div>
          {err && <div className="alert neg" role="alert">{err}</div>}
        </div>
      </Modal>
    </div>
  );
}
