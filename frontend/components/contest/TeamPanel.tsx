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
import type { ContestDetail } from "@/lib/types";

/** Equipa no contest: inscrição, convites, aceitação e eliminação (antes do bloqueio). */
export function TeamPanel({ c }: { c: ContestDetail }) {
  const router = useRouter();
  const toast = useToast();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
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
        <h2 className="h2">Inscrição</h2>
        {(c.myInvites ?? []).map((i) => (
          <Alert key={i.teamId} tone="acc" title={`Convite da equipa ${i.teamName}`}
            action={<div className="row" style={{ gap: 8 }}>
              <Button size="sm" variant="out" onClick={() => run(() => post(`/teams/${i.teamId}/decline`))}>Recusar</Button>
              <Button size="sm" variant="pri" loading={busy} onClick={() => run(() => post(`/teams/${i.teamId}/join`), "Entraste na equipa")}>Aceitar</Button>
            </div>}>
            {i.invitedBy} convidou-te para a equipa.
          </Alert>
        ))}
        {!regOpen ? <p className="muted">As inscrições estão fechadas.</p> : c.mode === "TEAM" ? (
          <form className="stack" onSubmit={(e) => { e.preventDefault(); run(() => post(`/contests/${c.id}/teams`, { name }), "Equipa criada · és o capitão"); }}>
            <Field label="Nome da equipa" hint={`Equipas de ${c.minTeam} a ${c.maxTeam}. Quem cria a equipa é o capitão.`}>
              {({ id, describedBy }) => <Input id={id} aria-describedby={describedBy} value={name} maxLength={40} onChange={(e) => setName(e.target.value)} />}
            </Field>
            <Button variant="pri" type="submit" loading={busy} disabled={name.trim().length < 3}>Inscrever equipa</Button>
          </form>
        ) : (
          <Button variant="pri" loading={busy} onClick={() => run(() => post(`/contests/${c.id}/registrations`), "Inscrição confirmada")}>Inscrever-me</Button>
        )}
        {err && <Alert tone="neg" title="Não foi possível concluir">{err}</Alert>}
      </Card>
    );
  }

  const free = Math.max(0, t.maxTeam - t.members.length - t.pendingInvites.length);
  return (
    <Card className="stack">
      <div className="row"><h2 className="h2">{t.individual ? "A tua inscrição" : `A tua equipa · ${t.name}`}</h2><div className="spacer" />
        <Badge tone="pos">Inscrição confirmada</Badge></div>
      {!t.individual && (
        <ul className="plain stack" style={{ gap: 10 }}>
          {t.members.map((m) => (
            <li key={m.name} className="row" style={{ gap: 12 }}>
              <Avatar initials={m.initials} />
              <div style={{ flex: 1 }}><b>{m.name}{m.me && " (tu)"}</b><div className="small muted">{[m.course, m.year && `${m.year}.º`].filter(Boolean).join(" · ")}</div></div>
              {m.role === "CAPTAIN" && <Badge tone="acc">Capitão</Badge>}
              {m.confirmed ? <Badge tone="pos">Pronto</Badge> : <Badge>Por confirmar</Badge>}
            </li>
          ))}
          {t.pendingInvites.map((p) => <li key={p} className="row" style={{ gap: 12 }}><Avatar initials="—" /><span style={{ flex: 1 }}>{p}</span><Badge icon="clock">Convidado</Badge></li>)}
          {Array.from({ length: free }, (_, i) => (
            <li key={`free-${i}`} className="row" style={{ gap: 12 }}><Avatar initials="—" /><span className="muted" style={{ flex: 1 }}>Lugar livre · até {t.maxTeam} membros</span><Badge>Opcional</Badge></li>
          ))}
        </ul>
      )}
      {t.confirmed < t.minTeam && <Alert tone="acc" title="Equipa incompleta">São precisos pelo menos {t.minTeam} membros confirmados para participar.</Alert>}
      <p className="small muted">Os membros ficam bloqueados a {c.lockMinutes} min do início <span className="tbc">a confirmar</span>
        {t.locked ? " · a equipa já está bloqueada." : ` · bloqueio às ${formatTime(c.lockAt)}.`}</p>
      {t.captain && !t.locked && !t.individual && (
        <div className="stack">
          {free > 0 && (
            <form className="row" style={{ alignItems: "flex-end" }} onSubmit={(e) => { e.preventDefault(); run(() => post(`/teams/${t.id}/invites`, { email }), "Convite enviado"); setEmail(""); }}>
              <div style={{ flex: 1, minWidth: 220 }}>
                <Field label="Convidar por email institucional">{({ id }) => <Input id={id} type="email" value={email} onChange={(e) => setEmail(e.target.value)} />}</Field>
              </div>
              <Button type="submit" variant="out" loading={busy} disabled={!email}>Convidar</Button>
            </form>
          )}
          <div><Button variant="danger" size="sm" icon="trash" onClick={() => setConfirmDelete(true)}>Eliminar equipa</Button></div>
        </div>
      )}
      {err && <Alert tone="neg" title="Não foi possível concluir">{err}</Alert>}
      <ConfirmDialog open={confirmDelete} onCancel={() => setConfirmDelete(false)} destructive typeToConfirm="ELIMINAR" busy={busy}
        title={`Eliminar a equipa «${t.name}»?`} body="Os membros perdem a inscrição neste contest." confirmLabel="Eliminar equipa"
        onConfirm={() => run(async () => { await del(`/teams/${t.id}`, { confirmation: "ELIMINAR" }); setConfirmDelete(false); }, "Equipa eliminada")} />
    </Card>
  );
}
