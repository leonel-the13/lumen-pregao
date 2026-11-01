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

export interface AdminContestRow { id: string; slug: string; title: string; mode: string; state: string; startsAt: string; endsAt: string; teams: number; problems: number; pendingSubmissions: number }
interface Published { versionId: string; label: string }

const STATE_LABEL: Record<string, string> = {
  DRAFT: "Rascunho", REGISTRATION: "Inscrições", WAITING_ROOM: "Sala de espera", RUNNING: "Em curso", FROZEN: "Congelado",
  SUBMISSIONS_CLOSED: "Submissões encerradas", FINAL_EVALUATION: "Avaliação final", RESULTS_PUBLISHED: "Resultados publicados", CLOSED: "Encerrado",
};

function local(d: Date) {
  const p = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`;
}

/** Gestão de contests (organizador). As transições temporais são do servidor; aqui só as manuais. */
export function ContestsAdmin({ rows, exercises }: { rows: AdminContestRow[]; exercises: Published[] }) {
  const router = useRouter();
  const toast = useToast();
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
      toast({ tone: "pos", title: "Contest criado em rascunho" });
      setCreating(false);
      router.refresh();
    } catch (e) { setErr((e as Error).message); }
  };
  const act = async () => {
    if (!confirm) return;
    try {
      await post(`/admin/contests/${confirm.row.id}/${confirm.action}`);
      toast({ tone: "pos", title: "Contest actualizado" });
      router.refresh();
    } catch (e) { toast({ tone: "neg", title: "Não foi possível concluir", body: (e as Error).message }); }
    setConfirm(null);
  };
  return (
    <div className="stack">
      <div><Button variant="pri" icon="plus" onClick={() => setCreating(true)}>Novo contest</Button></div>
      <div className="tbl-wrap">
        <table className="tbl">
          <thead><tr><th>Contest</th><th>Estado</th><th>Início</th><th className="num">Equipas</th><th className="num">Problemas</th><th><span className="sr-only">Acções</span></th></tr></thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.id}>
                <td><b>{r.title}</b><div className="small muted mono">{r.slug} · {r.mode === "TEAM" ? "equipas" : "individual"}</div></td>
                <td><Badge tone={r.state === "RUNNING" || r.state === "FROZEN" ? "acc" : "neutral"}>{STATE_LABEL[r.state] ?? r.state}</Badge></td>
                <td className="small mono">{formatDateTime(r.startsAt)}</td>
                <td className="num">{r.teams}</td><td className="num">{r.problems}</td>
                <td className="row" style={{ gap: 6 }}>
                  {r.state === "DRAFT" && <Button size="sm" variant="out" onClick={() => setConfirm({ row: r, action: "publish" })}>Publicar</Button>}
                  {(r.state === "SUBMISSIONS_CLOSED" || r.state === "FINAL_EVALUATION") && <Button size="sm" variant="pri" onClick={() => setConfirm({ row: r, action: "publish-results" })}>Publicar resultados</Button>}
                  {r.state === "RESULTS_PUBLISHED" && <Button size="sm" variant="out" onClick={() => setConfirm({ row: r, action: "close" })}>Encerrar</Button>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <ConfirmDialog open={!!confirm} onCancel={() => setConfirm(null)} onConfirm={act}
        title={confirm?.action === "publish" ? "Abrir inscrições?" : confirm?.action === "publish-results" ? "Publicar resultados?" : "Encerrar o contest?"}
        confirmLabel="Confirmar"
        body={confirm?.action === "publish-results" ? "O ranking congelado é substituído pelo final e os pontos entram no ranking geral. Fica registado em auditoria." : "Esta operação fica registada em auditoria."} />
      <Modal open={creating} onClose={() => setCreating(false)} title="Novo contest"
        actions={<><Button variant="out" onClick={() => setCreating(false)}>Cancelar</Button><Button variant="pri" onClick={create}>Criar rascunho</Button></>}>
        <div className="stack" style={{ color: "var(--text)", maxHeight: "65vh", overflowY: "auto", paddingRight: 4 }}>
          <div className="grid g2" style={{ gap: 10 }}>
            <Field label="Título">{({ id }) => <Input id={id} value={f.title} onChange={(e) => set("title", e.target.value)} />}</Field>
            <Field label="Identificador" hint="minúsculas, números e -">{({ id, describedBy }) => <Input id={id} aria-describedby={describedBy} className="mono" value={f.slug} onChange={(e) => set("slug", e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, ""))} />}</Field>
          </div>
          <Field label="Descrição">{({ id }) => <TextArea id={id} value={f.description} onChange={(e) => set("description", e.target.value)} />}</Field>
          <div className="grid g3" style={{ gap: 10 }}>
            <Field label="Modo">{({ id }) => <Select id={id} value={f.mode} onChange={(e) => set("mode", e.target.value)}><option value="TEAM">Equipas</option><option value="INDIVIDUAL">Individual</option></Select>}</Field>
            <Field label="Mín. equipa">{({ id }) => <Input id={id} value={f.minTeam} onChange={(e) => set("minTeam", Number(e.target.value) || 1)} />}</Field>
            <Field label="Máx. equipa">{({ id }) => <Input id={id} value={f.maxTeam} onChange={(e) => set("maxTeam", Number(e.target.value) || 1)} />}</Field>
          </div>
          <div className="grid g2" style={{ gap: 10 }}>
            {([["registrationOpensAt", "Abertura das inscrições"], ["registrationClosesAt", "Fecho das inscrições"], ["startsAt", "Início"], ["freezeAt", "Congelamento"], ["endsAt", "Fim"]] as const).map(([k, l]) => (
              <Field key={k} label={l}>{({ id }) => <Input id={id} type="datetime-local" value={f[k]} onChange={(e) => set(k, e.target.value)} />}</Field>
            ))}
            <Field label="Bloqueio das equipas (min antes)">{({ id }) => <Input id={id} value={f.lockMinutes} onChange={(e) => set("lockMinutes", Number(e.target.value) || 0)} />}</Field>
          </div>
          <b className="small">Problemas (só exercícios publicados)</b>
          {f.problems.map((p, i) => (
            <div key={i} className="grid" style={{ gridTemplateColumns: "60px 1fr 1fr 90px", gap: 8 }}>
              <Input aria-label="Letra" value={p.label} onChange={(e) => set("problems", f.problems.map((x, j) => (j === i ? { ...x, label: e.target.value.toUpperCase().slice(0, 2) } : x)))} />
              <Select aria-label="Exercício" value={p.exerciseVersionId} onChange={(e) => set("problems", f.problems.map((x, j) => (j === i ? { ...x, exerciseVersionId: e.target.value, title: x.title || exercises.find((ex) => ex.versionId === e.target.value)?.label || "" } : x)))}>
                <option value="">Exercício…</option>{exercises.map((ex) => <option key={ex.versionId} value={ex.versionId}>{ex.label}</option>)}
              </Select>
              <Input aria-label="Empresa fictícia" placeholder="Empresa fictícia" value={p.companyName} onChange={(e) => set("problems", f.problems.map((x, j) => (j === i ? { ...x, companyName: e.target.value } : x)))} />
              <Input aria-label="Pontos" value={p.points} onChange={(e) => set("problems", f.problems.map((x, j) => (j === i ? { ...x, points: Number(e.target.value) || 0 } : x)))} />
            </div>
          ))}
          <div><Button size="sm" variant="ghost" icon="plus" onClick={() => set("problems", [...f.problems, { label: String.fromCharCode(65 + f.problems.length), title: "", companyName: "", exerciseVersionId: "", points: 100 }])}>Problema</Button></div>
          {err && <div className="alert neg" role="alert">{err}</div>}
        </div>
      </Modal>
    </div>
  );
}
