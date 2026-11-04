"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Avatar } from "@/components/ui/Avatar";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Field, Input } from "@/components/ui/Field";
import { ConfirmDialog } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";
import { del, post } from "@/lib/api-client";
import { formatTime } from "@/lib/format";
import { useLocale, useT } from "@/lib/i18n/client";
import type { ContestDetail } from "@/lib/types";

/** Equipa no contest: inscrição, convites, aceitação e eliminação (antes do bloqueio). */
export function TeamPanel({ c }: { c: ContestDetail }) {
  const router = useRouter();
  const toast = useToast();
  const tr = useT();
  const locale = useLocale();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [confirmLeave, setConfirmLeave] = useState(false);
  const regOpen = c.state === "REGISTRATION";
  const run = async (fn: () => Promise<unknown>, ok?: string) => {
    setBusy(true); setErr(null);
    try { await fn(); if (ok) toast({ tone: "pos", title: ok }); router.refresh(); }
    catch (e) { setErr((e as Error).message); }
    finally { setBusy(false); }
  };
  const t = c.myTeam;

  if (!t) {
    return (
      <Card className="stack">
        <h2 className="h2">{tr("compete.team.registration")}</h2>
        {(c.myInvites ?? []).map((i) => (
          <Alert key={i.teamId} tone="acc" title={tr("compete.team.inviteFrom", { team: i.teamName })}
            action={<div className="row" style={{ gap: 8 }}>
              <Button size="sm" variant="out" onClick={() => run(() => post(`/teams/${i.teamId}/decline`))}>{tr("compete.team.decline")}</Button>
              <Button size="sm" variant="pri" loading={busy} onClick={() => run(() => post(`/teams/${i.teamId}/join`), tr("compete.team.joined"))}>{tr("compete.team.accept")}</Button>
            </div>}>
            {tr("compete.team.invitedYou", { who: i.invitedBy })}
          </Alert>
        ))}
        {!regOpen ? <p className="muted">{tr("compete.team.closed")}</p> : c.mode === "TEAM" ? (
          <form className="stack" onSubmit={(e) => { e.preventDefault(); run(() => post(`/contests/${c.id}/teams`, { name }), tr("compete.team.created")); }}>
            <Field label={tr("compete.team.name")} hint={tr("compete.team.nameHint", { min: c.minTeam, max: c.maxTeam })}>
              {({ id, describedBy }) => <Input id={id} aria-describedby={describedBy} value={name} maxLength={40} onChange={(e) => setName(e.target.value)} />}
            </Field>
            <Button variant="pri" type="submit" loading={busy} disabled={name.trim().length < 3}>{tr("compete.team.register")}</Button>
          </form>
        ) : (
          <Button variant="pri" loading={busy} onClick={() => run(() => post(`/contests/${c.id}/registrations`), tr("compete.team.confirmed"))}>{tr("compete.team.registerMe")}</Button>
        )}
        {err && <Alert tone="neg" title={tr("compete.team.failed")}>{err}</Alert>}
      </Card>
    );
  }

  const free = Math.max(0, t.maxTeam - t.members.length - t.pendingInvites.length);
  return (
    <Card className="stack">
      <div className="row"><h2 className="h2">{t.individual ? tr("compete.team.yours") : tr("compete.team.yourTeam", { name: t.name })}</h2><div className="spacer" />
        <Badge tone="pos">{tr("compete.team.confirmed")}</Badge></div>
      {!t.individual && (
        <ul className="plain stack" style={{ gap: 10 }}>
          {t.members.map((m) => (
            <li key={m.name} className="row" style={{ gap: 12 }}>
              <Avatar initials={m.initials} />
              <div style={{ flex: 1 }}><b>{m.name}{m.me && tr("compete.team.you")}</b><div className="small muted">{[m.course, m.year && tr("compete.team.yearShort", { n: m.year })].filter(Boolean).join(" · ")}</div></div>
              {m.role === "CAPTAIN" && <Badge tone="acc">{tr("compete.team.captain")}</Badge>}
              {m.confirmed ? <Badge tone="pos">{tr("compete.team.ready")}</Badge> : <Badge>{tr("compete.team.toConfirm")}</Badge>}
            </li>
          ))}
          {t.pendingInvites.map((p) => <li key={p} className="row" style={{ gap: 12 }}><Avatar initials="—" /><span style={{ flex: 1 }}>{p}</span><Badge icon="clock">{tr("compete.team.invited")}</Badge></li>)}
          {Array.from({ length: free }, (_, i) => (
            <li key={`free-${i}`} className="row" style={{ gap: 12 }}><Avatar initials="—" /><span className="muted" style={{ flex: 1 }}>{tr("compete.team.freeSlot", { n: t.maxTeam })}</span><Badge>{tr("compete.team.optional")}</Badge></li>
          ))}
        </ul>
      )}
      {t.confirmed < t.minTeam && <Alert tone="acc" title={tr("compete.team.incomplete")}>{tr("compete.team.incompleteBody", { n: t.minTeam })}</Alert>}
      <p className="small muted">{tr("compete.team.lockNote", { n: c.lockMinutes })} <span className="tbc">{tr("pages.tbc")}</span>
        {t.locked ? tr("compete.team.locked") : tr("compete.team.lockAt", { time: formatTime(c.lockAt, locale) })}</p>
      {t.captain && !t.locked && !t.individual && (
        <div className="stack">
          {free > 0 && (
            <form className="row" style={{ alignItems: "flex-end" }} onSubmit={(e) => { e.preventDefault(); run(() => post(`/teams/${t.id}/invites`, { email }), tr("compete.team.inviteSent")); setEmail(""); }}>
              <div style={{ flex: 1, minWidth: 220 }}>
                <Field label={tr("compete.team.inviteByEmail")}>{({ id }) => <Input id={id} type="email" value={email} onChange={(e) => setEmail(e.target.value)} />}</Field>
              </div>
              <Button type="submit" variant="out" loading={busy} disabled={!email}>{tr("compete.team.invite")}</Button>
            </form>
          )}
          <div><Button variant="danger" size="sm" icon="trash" onClick={() => setConfirmDelete(true)}>{tr("compete.team.delete")}</Button></div>
        </div>
      )}
      {!t.captain && !t.locked && !t.individual && (
        <div><Button variant="out" size="sm" icon="logout" onClick={() => setConfirmLeave(true)}>{tr("compete.team.leave")}</Button></div>
      )}
      {err && <Alert tone="neg" title={tr("compete.team.failed")}>{err}</Alert>}
      <ConfirmDialog open={confirmLeave} onCancel={() => setConfirmLeave(false)} busy={busy}
        title={tr("compete.team.leaveTitle", { name: t.name })} body={tr("compete.team.leaveBody")}
        confirmLabel={tr("compete.team.leave")}
        onConfirm={() => run(async () => { await post(`/teams/${t.id}/leave`); setConfirmLeave(false); }, tr("compete.team.left"))} />
      <ConfirmDialog open={confirmDelete} onCancel={() => setConfirmDelete(false)} destructive typeToConfirm="ELIMINAR" busy={busy}
        title={tr("compete.team.deleteTitle", { name: t.name })} body={tr("compete.team.deleteBody")} confirmLabel={tr("compete.team.delete")}
        onConfirm={() => run(async () => { await del(`/teams/${t.id}`, { confirmation: "ELIMINAR" }); setConfirmDelete(false); }, tr("compete.team.deleted"))} />
    </Card>
  );
}
