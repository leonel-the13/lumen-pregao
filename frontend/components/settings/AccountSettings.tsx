"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card, CardHeader } from "@/components/ui/Card";
import { ConfirmDialog } from "@/components/ui/Modal";
import { Seg } from "@/components/ui/Tabs";
import { useToast } from "@/components/ui/Toast";
import { patch, post } from "@/lib/api-client";
import { THEME_COOKIE } from "@/lib/theme";
import type { Me } from "@/lib/types";
import { RecoveryCodesView } from "@/components/auth/RecoveryCodesView";
import { Modal } from "@/components/ui/Modal";

export function AccountSettings({ me }: { me: Me }) {
  const router = useRouter();
  const toast = useToast();
  const [theme, setTheme] = useState(me.theme);
  const [emails, setEmails] = useState(me.emailOptIn);
  const [del, setDel] = useState(false);
  const [busy, setBusy] = useState(false);
  const [regen, setRegen] = useState(false);
  const [codes, setCodes] = useState<string[] | null>(null);
  return (
    <div className="grid g2" style={{ alignItems: "start" }}>
      <Card className="stack">
        <CardHeader title="Conta" />
        <dl className="stack small" style={{ margin: 0 }}>
          <div className="row"><dt className="muted" style={{ width: 140 }}>Nome</dt><dd style={{ margin: 0 }}>{me.name}</dd></div>
          <div className="row"><dt className="muted" style={{ width: 140 }}>Email</dt><dd style={{ margin: 0 }}>{me.email}</dd></div>
          <div className="row"><dt className="muted" style={{ width: 140 }}>Curso e ano</dt><dd style={{ margin: 0 }}>{[me.course?.name, me.year && `${me.year}.º ano`].filter(Boolean).join(" · ") || "—"}</dd></div>
          <div className="row"><dt className="muted" style={{ width: 140 }}>2.º factor</dt><dd style={{ margin: 0 }}>{me.mfaEnabled ? <Badge tone="pos">Activo</Badge> : <Badge>Não configurado</Badge>}</dd></div>
        </dl>
        {me.mfaEnabled && (
          <div className="row" style={{ gap: 8 }}>
            <Button variant="out" size="sm" icon="shield" onClick={() => setRegen(true)}>Gerar novos códigos de recuperação</Button>
          </div>
        )}
        <ConfirmDialog open={regen} onCancel={() => setRegen(false)} busy={busy} title="Gerar novos códigos de recuperação?"
          body="Os códigos anteriores deixam de funcionar. Os novos só são mostrados uma vez." confirmLabel="Gerar códigos"
          onConfirm={async () => {
            setBusy(true);
            try {
              const r = await post<{ recoveryCodes: string[] }>("/me/mfa/recovery-codes");
              setCodes(r.recoveryCodes);
              setRegen(false);
            } catch {
              toast({ tone: "neg", title: "Não foi possível gerar os códigos", body: "Tenta de novo dentro de momentos." });
            } finally { setBusy(false); }
          }} />
        <Modal open={!!codes} onClose={() => setCodes(null)} title="Novos códigos de recuperação" labelledBy="codes-title"
          actions={<Button variant="pri" onClick={() => setCodes(null)}>Já guardei</Button>}>
          {codes && <div style={{ color: "var(--text)" }}><RecoveryCodesView codes={codes} /></div>}
        </Modal>
      </Card>
      <Card className="stack">
        <CardHeader title="Aparência" />
        <p className="small muted">Escuro por defeito. A preferência fica guardada no teu perfil.</p>
        <Seg label="Tema" value={theme} options={[{ value: "DARK", label: "Escuro" }, { value: "LIGHT", label: "Claro" }]}
          onChange={async (t) => {
            setTheme(t);
            const attr = t === "LIGHT" ? "light" : "dark";
            document.documentElement.dataset.theme = attr;
            document.cookie = `${THEME_COOKIE}=${attr}; path=/; max-age=31536000; samesite=lax`;
            try { await patch("/me", { theme: t }); } catch { toast({ tone: "neg", title: "Não foi possível guardar o tema no perfil" }); }
          }} />
      </Card>
      <Card className="stack">
        <CardHeader title="Emails" />
        <p className="small muted" style={{ margin: 0 }}>
          Notificações importantes (convites de equipa, contests e resultados, exames corrigidos, revisão editorial), lembretes e
          novidades da Associação de Estudantes. Os avisos de segurança chegam sempre.
        </p>
        <Seg label="Receber emails" value={emails ? "ON" : "OFF"} options={[{ value: "ON", label: "Ligados" }, { value: "OFF", label: "Desligados" }]}
          onChange={async (v) => {
            const on = v === "ON";
            setEmails(on);
            try {
              await patch("/me", { emailOptIn: on });
              toast({ tone: "pos", title: on ? "Emails ligados" : "Emails desligados", body: on ? undefined : "Continuas a ver tudo no sino de notificações." });
            } catch { setEmails(!on); toast({ tone: "neg", title: "Não foi possível guardar a preferência", body: "Tenta de novo dentro de momentos." }); }
          }} />
      </Card>
      <Card className="stack">
        <CardHeader title="Os teus dados" />
        <p className="small muted">Descarrega uma cópia de todos os dados que guardamos sobre ti (JSON).</p>
        <div><a className="btn out" href="/api/v1/me/export" download>Exportar os meus dados</a></div>
      </Card>
      <Card className="stack">
        <CardHeader title="Eliminar conta" />
        <Alert tone="neg" title="Operação irreversível">Depois do pedido, a sessão termina e a administração trata a eliminação.</Alert>
        <div><Button variant="danger" icon="trash" onClick={() => setDel(true)}>Pedir eliminação da conta</Button></div>
      </Card>
      <ConfirmDialog open={del} onCancel={() => setDel(false)} destructive typeToConfirm="ELIMINAR" busy={busy}
        title="Pedir a eliminação da conta?" confirmLabel="Pedir eliminação"
        body="Perdes o acesso, o progresso e as inscrições em contests. Escreve ELIMINAR para confirmar."
        onConfirm={async () => {
          setBusy(true);
          try { await post("/me/deletion-request"); router.push("/"); router.refresh(); }
          catch (e) { toast({ tone: "neg", title: "Não foi possível concluir", body: (e as Error).message }); setBusy(false); }
        }} />
    </div>
  );
}
