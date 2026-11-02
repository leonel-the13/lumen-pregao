"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Avatar } from "@/components/ui/Avatar";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Field, Select } from "@/components/ui/Field";
import { ConfirmDialog, Modal } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";
import { patch, post } from "@/lib/api-client";
import { formatDateTime } from "@/lib/format";

export interface AdminUserRow { id: string; name: string; initials: string; email: string; course?: string; year?: number; roles: string[]; status: string; mfaEnabled: boolean; lastLoginAt?: string }

export const ROLE_LABEL: Record<string, string> = { STUDENT: "Estudante", AUTHOR: "Autor", REVIEWER: "Revisor", JURY: "Júri", CONTEST_ORGANIZER: "Organizador", ADMIN: "Administrador", TEAM_CAPTAIN: "Capitão" };
const STATUS_LABEL: Record<string, [string, "pos" | "neutral" | "neg" | "info"]> = {
  ACTIVE: ["Activo", "pos"], INVITED: ["Convite pendente", "info"], PENDING_VERIFICATION: ["Por verificar", "info"], SUSPENDED: ["Suspenso", "neg"], DELETION_REQUESTED: ["Eliminação pedida", "neg"], DELETED: ["Eliminada", "neutral"],
};
const ASSIGNABLE = ["STUDENT", "AUTHOR", "REVIEWER", "JURY", "CONTEST_ORGANIZER", "ADMIN"];

export function UsersTable({ rows, me }: { rows: AdminUserRow[]; me: string }) {
  const router = useRouter();
  const toast = useToast();
  const [edit, setEdit] = useState<AdminUserRow | null>(null);
  const [roles, setRoles] = useState<string[]>([]);
  const [status, setStatus] = useState("ACTIVE");
  const [err, setErr] = useState<string | null>(null);
  const [mfaTarget, setMfaTarget] = useState<AdminUserRow | null>(null);
  const [delTarget, setDelTarget] = useState<AdminUserRow | null>(null);
  const [delBusy, setDelBusy] = useState(false);
  const open = (u: AdminUserRow) => { setEdit(u); setRoles(u.roles.filter((r) => r !== "TEAM_CAPTAIN")); setStatus(u.status); setErr(null); };
  const save = async () => {
    if (!edit) return;
    setErr(null);
    try {
      await patch(`/admin/users/${edit.id}/roles`, { roles });
      if (status !== edit.status) await patch(`/admin/users/${edit.id}`, { status });
      toast({ tone: "pos", title: `${edit.name} actualizado`, body: `${edit.email} · as sessões activas desta pessoa foram terminadas.` });
      setEdit(null);
      router.refresh();
    } catch (e) { setErr((e as Error).message); }
  };
  return (
    <>
      <div className="tbl-wrap">
        <table className="tbl">
          <thead><tr><th>Utilizador</th><th>Perfis</th><th>Estado</th><th>2.º factor</th><th>Último acesso</th><th><span className="sr-only">Acções</span></th></tr></thead>
          <tbody>
            {rows.map((u) => {
              const [sl, st] = STATUS_LABEL[u.status] ?? [u.status, "neutral"];
              return (
                <tr key={u.id}>
                  <td><span className="row" style={{ gap: 10 }}><Avatar initials={u.initials} /><span><b>{u.name}</b><div className="small muted">{u.email}</div></span></span></td>
                  <td><span className="row" style={{ gap: 6 }}>{u.roles.map((r) => <Badge key={r}>{ROLE_LABEL[r] ?? r}</Badge>)}</span></td>
                  <td><Badge tone={st}>{sl}</Badge></td>
                  <td>{u.mfaEnabled ? "Sim" : "—"}</td>
                  <td className="small mono">{u.lastLoginAt ? formatDateTime(u.lastLoginAt) : "—"}</td>
                  <td className="actions-cell">
                    <div className="row" style={{ gap: 4, flexWrap: "nowrap", justifyContent: "flex-end" }}>
                    {u.status !== "DELETED" && <Button size="sm" variant="ghost" icon="pen" aria-label={`Editar ${u.name}`} onClick={() => open(u)}>Editar</Button>}
                    {u.status === "DELETION_REQUESTED" && <Button size="sm" variant="ghost" icon="trash" onClick={() => setDelTarget(u)}>Eliminar dados</Button>}
                    {u.status === "INVITED" && <Button size="sm" variant="ghost" icon="refresh" onClick={async () => {
                      try { await post(`/admin/users/${u.id}/resend-invite`); toast({ tone: "pos", title: "Convite reenviado" }); }
                      catch (e) { toast({ tone: "neg", title: "Não foi possível reenviar", body: (e as Error).message }); }
                    }}>Reenviar convite</Button>}
                    {u.mfaEnabled && u.id !== me && <Button size="sm" variant="ghost" icon="shield" onClick={() => setMfaTarget(u)}>Repor 2.º factor</Button>}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <ConfirmDialog open={!!delTarget} onCancel={() => setDelTarget(null)} confirmLabel="Eliminar definitivamente" destructive
        typeToConfirm="ELIMINAR" busy={delBusy} title={`Eliminar os dados de ${delTarget?.name ?? ""}?`}
        body="Executa o pedido de eliminação: nome, email, pseudónimo, perfis e 2.º factor são apagados e a conta sai dos rankings. Os pontos e a auditoria ficam ligados a uma conta anónima. Não é reversível."
        onConfirm={async () => {
          const u = delTarget;
          if (!u) return;
          setDelBusy(true);
          try { await post(`/admin/users/${u.id}/anonymize`); toast({ tone: "pos", title: "Conta eliminada", body: "Os dados pessoais foram apagados." }); setDelTarget(null); router.refresh(); }
          catch (e) { toast({ tone: "neg", title: "Não foi possível eliminar", body: (e as Error).message }); }
          finally { setDelBusy(false); }
        }} />
      <ConfirmDialog open={!!mfaTarget} onCancel={() => setMfaTarget(null)} confirmLabel="Repor 2.º factor"
        title={`Repor o 2.º factor de ${mfaTarget?.name ?? ""}?`}
        body="As sessões desta pessoa terminam e, no próximo login, terá de configurar de novo a aplicação de autenticação. Fica registado em auditoria."
        onConfirm={async () => {
          const u = mfaTarget;
          setMfaTarget(null);
          if (!u) return;
          try { await post(`/admin/users/${u.id}/reset-mfa`); toast({ tone: "pos", title: "2.º factor reposto" }); router.refresh(); }
          catch (e) { toast({ tone: "neg", title: "Não foi possível repor", body: (e as Error).message }); }
        }} />
      <Modal open={!!edit} onClose={() => setEdit(null)} title={`Editar ${edit?.name ?? ""}`}
        actions={<><Button variant="out" onClick={() => setEdit(null)}>Cancelar</Button><Button variant="pri" onClick={save}>Guardar</Button></>}>
        <div className="stack" style={{ color: "var(--text)" }}>
          <p className="small muted mono" style={{ margin: 0 }}>{edit?.email}</p>
          <fieldset className="stack" style={{ border: 0, padding: 0, margin: 0, gap: 4 }}>
            <legend className="small" style={{ fontWeight: 600, marginBottom: 6 }}>Perfis</legend>
            {ASSIGNABLE.map((r) => (
              <label key={r} className="check" style={{ paddingTop: 4, minHeight: 36 }}>
                <input type="checkbox" checked={roles.includes(r)} disabled={edit?.id === me && r === "ADMIN"}
                  onChange={(e) => setRoles(e.target.checked ? [...roles, r] : roles.filter((x) => x !== r))} />{ROLE_LABEL[r]}
              </label>
            ))}
            <span className="small muted">Revisor, júri, organizador e administrador exigem 2.º factor. Capitão é atribuído dentro de cada equipa.</span>
          </fieldset>
          {edit?.id !== me && (
            <Field label="Estado da conta">{({ id }) => (
              <Select id={id} value={status} onChange={(e) => setStatus(e.target.value)}>
                <option value="ACTIVE">Activo</option><option value="SUSPENDED">Suspenso</option><option value="INVITED">Convite pendente</option>
                <option value="PENDING_VERIFICATION">Por verificar</option><option value="DELETION_REQUESTED">Eliminação pedida</option>
              </Select>
            )}</Field>
          )}
          {err && <div className="alert neg" role="alert">{err}</div>}
        </div>
      </Modal>
    </>
  );
}
