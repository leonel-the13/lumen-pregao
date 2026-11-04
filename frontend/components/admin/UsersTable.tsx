"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Avatar } from "@/components/ui/Avatar";
import { Badge } from "@/components/ui/Badge";
import { Button, LinkButton } from "@/components/ui/Button";
import { Field, Input, Select } from "@/components/ui/Field";
import { ConfirmDialog, Modal } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";
import { patch, post } from "@/lib/api-client";
import { formatDateTime } from "@/lib/format";
import { useLocale, useT } from "@/lib/i18n/client";

export interface AdminUserRow { id: string; name: string; initials: string; email: string; course?: string; year?: number; roles: string[]; status: string; mfaEnabled: boolean; lastLoginAt?: string }

export const ROLES = ["STUDENT", "AUTHOR", "REVIEWER", "JURY", "CONTEST_ORGANIZER", "ADMIN", "TEAM_CAPTAIN"] as const;
export type RoleCode = (typeof ROLES)[number];
export const isRole = (r: string): r is RoleCode => (ROLES as readonly string[]).includes(r);
const STATUS_TONE: Record<string, "pos" | "neutral" | "neg" | "info"> = {
  ACTIVE: "pos", INVITED: "info", PENDING_VERIFICATION: "info", SUSPENDED: "neg", DELETION_REQUESTED: "neg", DELETED: "neutral",
};
type UserStatus = "ACTIVE" | "INVITED" | "PENDING_VERIFICATION" | "SUSPENDED" | "DELETION_REQUESTED" | "DELETED";
const ASSIGNABLE = ["STUDENT", "AUTHOR", "REVIEWER", "JURY", "CONTEST_ORGANIZER", "ADMIN"] as const;

export function UsersTable({ rows, me }: { rows: AdminUserRow[]; me: string }) {
  const router = useRouter();
  const toast = useToast();
  const t = useT();
  const locale = useLocale();
  const [edit, setEdit] = useState<AdminUserRow | null>(null);
  const [roles, setRoles] = useState<string[]>([]);
  const [status, setStatus] = useState("ACTIVE");
  const [email, setEmail] = useState("");
  const [err, setErr] = useState<string | null>(null);
  const [mfaTarget, setMfaTarget] = useState<AdminUserRow | null>(null);
  const [delTarget, setDelTarget] = useState<AdminUserRow | null>(null);
  const [delBusy, setDelBusy] = useState(false);
  const open = (u: AdminUserRow) => { setEdit(u); setRoles(u.roles.filter((r) => r !== "TEAM_CAPTAIN")); setStatus(u.status); setEmail(u.email); setErr(null); };
  const save = async () => {
    if (!edit) return;
    setErr(null);
    const newEmail = email.trim().toLowerCase();
    const emailChanged = newEmail !== edit.email;
    try {
      // O email vai primeiro: se já estiver em uso, nada mais muda.
      if (emailChanged) await patch(`/admin/users/${edit.id}/email`, { email: newEmail });
      await patch(`/admin/users/${edit.id}/roles`, { roles });
      if (status !== edit.status) await patch(`/admin/users/${edit.id}`, { status });
      toast({ tone: "pos", title: t("admin.users.updated", { name: edit.name }), body: emailChanged
        ? `${t("admin.users.movedTo", { email: newEmail })}${edit.status === "INVITED" ? t("admin.users.inviteResentTo") : t("admin.users.bothNotified")}.`
        : t("admin.users.sessionsEnded", { email: edit.email }) });
      setEdit(null);
      router.refresh();
    } catch (e) { setErr((e as Error).message); }
  };
  return (
    <>
      <div className="tbl-wrap">
        <table className="tbl">
          <thead><tr><th>{t("admin.users.user")}</th><th>{t("admin.users.roles")}</th><th>{t("admin.users.state")}</th><th>{t("admin.users.mfa")}</th><th>{t("admin.users.lastAccess")}</th><th><span className="sr-only">{t("admin.users.actions")}</span></th></tr></thead>
          <tbody>
            {rows.map((u) => {
              const st = STATUS_TONE[u.status] ?? "neutral";
              const sl = u.status in STATUS_TONE ? t(`admin.users.status.${u.status as UserStatus}`) : u.status;
              return (
                <tr key={u.id}>
                  <td><span className="row" style={{ gap: 10 }}><Avatar initials={u.initials} /><span><b>{u.name}</b><div className="small muted">{u.email}</div></span></span></td>
                  <td><span className="row" style={{ gap: 6 }}>{u.roles.map((r) => <Badge key={r}>{isRole(r) ? t(`common.roles.${r}`) : r}</Badge>)}</span></td>
                  <td><Badge tone={st}>{sl}</Badge></td>
                  <td>{u.mfaEnabled ? t("common.yes") : "—"}</td>
                  <td className="small mono">{u.lastLoginAt ? formatDateTime(u.lastLoginAt, locale) : "—"}</td>
                  <td className="actions-cell">
                    <div className="row" style={{ gap: 4, flexWrap: "nowrap", justifyContent: "flex-end" }}>
                    {u.status !== "DELETED" && <Button size="sm" variant="ghost" icon="pen" aria-label={t("admin.users.editAria", { name: u.name })} onClick={() => open(u)}>{t("common.edit")}</Button>}
                    {u.status === "DELETION_REQUESTED" && <Button size="sm" variant="ghost" icon="trash" onClick={() => setDelTarget(u)}>{t("admin.users.deleteData")}</Button>}
                    {u.status === "INVITED" && <Button size="sm" variant="ghost" icon="refresh" onClick={async () => {
                      try { await post(`/admin/users/${u.id}/resend-invite`); toast({ tone: "pos", title: t("admin.users.inviteResent") }); }
                      catch (e) { toast({ tone: "neg", title: t("admin.users.couldNotResend"), body: (e as Error).message }); }
                    }}>{t("admin.users.resendInvite")}</Button>}
                    {u.status === "ACTIVE" && u.id !== me && <LinkButton size="sm" variant="ghost" icon="mail"
                      href={`/admin?tab=emails&para=${u.id}&nome=${encodeURIComponent(u.name)}`} aria-label={t("admin.users.sendEmailAria", { name: u.name })}>{t("admin.users.sendEmail")}</LinkButton>}
                    {u.status === "PENDING_VERIFICATION" && <Button size="sm" variant="ghost" icon="mail" onClick={async () => {
                      try { await post(`/admin/users/${u.id}/remind-verification`); toast({ tone: "pos", title: t("admin.users.reminderSent"), body: t("admin.users.reminderSentBody", { email: u.email }) }); }
                      catch (e) { toast({ tone: "neg", title: t("admin.users.couldNotRemind"), body: (e as Error).message }); }
                    }}>{t("admin.users.remind")}</Button>}
                    {u.mfaEnabled && u.id !== me && <Button size="sm" variant="ghost" icon="shield" onClick={() => setMfaTarget(u)}>{t("admin.users.resetMfa")}</Button>}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <ConfirmDialog open={!!delTarget} onCancel={() => setDelTarget(null)} confirmLabel={t("admin.users.deleteConfirm")} destructive
        typeToConfirm="ELIMINAR" busy={delBusy} title={t("admin.users.deleteTitle", { name: delTarget?.name ?? "" })}
        body={t("admin.users.deleteBody")}
        onConfirm={async () => {
          const u = delTarget;
          if (!u) return;
          setDelBusy(true);
          try { await post(`/admin/users/${u.id}/anonymize`); toast({ tone: "pos", title: t("admin.users.deleted"), body: t("admin.users.deletedBody") }); setDelTarget(null); router.refresh(); }
          catch (e) { toast({ tone: "neg", title: t("admin.users.couldNotDelete"), body: (e as Error).message }); }
          finally { setDelBusy(false); }
        }} />
      <ConfirmDialog open={!!mfaTarget} onCancel={() => setMfaTarget(null)} confirmLabel={t("admin.users.resetMfa")}
        title={t("admin.users.resetMfaTitle", { name: mfaTarget?.name ?? "" })}
        body={t("admin.users.resetMfaBody")}
        onConfirm={async () => {
          const u = mfaTarget;
          setMfaTarget(null);
          if (!u) return;
          try { await post(`/admin/users/${u.id}/reset-mfa`); toast({ tone: "pos", title: t("admin.users.mfaReset") }); router.refresh(); }
          catch (e) { toast({ tone: "neg", title: t("admin.users.couldNotReset"), body: (e as Error).message }); }
        }} />
      <Modal open={!!edit} onClose={() => setEdit(null)} title={t("admin.users.editTitle", { name: edit?.name ?? "" })}
        actions={<><Button variant="out" onClick={() => setEdit(null)}>{t("common.cancel")}</Button><Button variant="pri" onClick={save}>{t("common.save")}</Button></>}>
        <div className="stack" style={{ color: "var(--text)" }}>
          <Field label={t("admin.users.email")} hint={t("admin.users.emailHint")}>{({ id }) => (
            <Input id={id} type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="off" />)}</Field>
          <fieldset className="stack" style={{ border: 0, padding: 0, margin: 0, gap: 4 }}>
            <legend className="small" style={{ fontWeight: 600, marginBottom: 6 }}>{t("admin.users.roles")}</legend>
            {ASSIGNABLE.map((r) => (
              <label key={r} className="check" style={{ paddingTop: 4, minHeight: 36 }}>
                <input type="checkbox" checked={roles.includes(r)} disabled={edit?.id === me && r === "ADMIN"}
                  onChange={(e) => setRoles(e.target.checked ? [...roles, r] : roles.filter((x) => x !== r))} />{t(`common.roles.${r}`)}
              </label>
            ))}
            <span className="small muted">{t("admin.users.privilegedNote")}</span>
          </fieldset>
          {edit?.id !== me && (
            <Field label={t("admin.users.accountState")}>{({ id }) => (
              <Select id={id} value={status} onChange={(e) => setStatus(e.target.value)}>
                {(["ACTIVE", "SUSPENDED", "INVITED", "PENDING_VERIFICATION", "DELETION_REQUESTED"] as const).map((s) => <option key={s} value={s}>{t(`admin.users.status.${s}`)}</option>)}
              </Select>
            )}</Field>
          )}
          {err && <div className="alert neg" role="alert">{err}</div>}
        </div>
      </Modal>
    </>
  );
}
