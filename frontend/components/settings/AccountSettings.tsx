"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card, CardHeader } from "@/components/ui/Card";
import { ConfirmDialog, Modal } from "@/components/ui/Modal";
import { Seg } from "@/components/ui/Tabs";
import { useToast } from "@/components/ui/Toast";
import { RecoveryCodesView } from "@/components/auth/RecoveryCodesView";
import { patch, post } from "@/lib/api-client";
import { LOCALES, LOCALE_NAME, type Locale } from "@/lib/i18n/config";
import { useLocale, useSetLocale, useT } from "@/lib/i18n/client";
import { THEME_COOKIE } from "@/lib/theme";
import type { Me } from "@/lib/types";
import { PasswordCard } from "./PasswordCard";

/** Definições: aparência e língua, emails, segurança, dados e eliminação da conta. */
export function AccountSettings({ me }: { me: Me }) {
  const router = useRouter();
  const toast = useToast();
  const t = useT();
  const locale = useLocale();
  const setLocale = useSetLocale();
  const [theme, setTheme] = useState(me.theme);
  const [emails, setEmails] = useState(me.emailOptIn);
  const [del, setDel] = useState(false);
  const [busy, setBusy] = useState(false);
  const [regen, setRegen] = useState(false);
  const [codes, setCodes] = useState<string[] | null>(null);
  return (
    <div className="grid g2" style={{ alignItems: "start" }}>
      <Card className="stack">
        <CardHeader title={t("settings.settings.appearance")} />
        <p className="small muted" style={{ margin: 0 }}>{t("settings.settings.appearanceIntro")}</p>
        <Seg label={t("settings.settings.theme")} value={theme} options={[{ value: "DARK", label: t("settings.settings.dark") }, { value: "LIGHT", label: t("settings.settings.light") }]}
          onChange={async (v) => {
            setTheme(v);
            const attr = v === "LIGHT" ? "light" : "dark";
            document.documentElement.dataset.theme = attr;
            document.cookie = `${THEME_COOKIE}=${attr}; path=/; max-age=31536000; samesite=lax`;
            try { await patch("/me", { theme: v }); } catch { toast({ tone: "neg", title: t("settings.settings.themeFailed") }); }
          }} />
        <div className="stack" style={{ gap: 8, marginTop: 8 }}>
          <b className="small">{t("settings.settings.language")}</b>
          <p className="small muted" style={{ margin: 0 }}>{t("settings.settings.languageIntro")}</p>
          <Seg<Locale> label={t("settings.settings.language")} value={locale} onChange={setLocale}
            options={LOCALES.map((l) => ({ value: l, label: LOCALE_NAME[l] }))} />
        </div>
      </Card>
      <Card className="stack">
        <CardHeader title={t("settings.settings.emails")} />
        <p className="small muted" style={{ margin: 0 }}>{t("settings.settings.emailsIntro")}</p>
        <Seg label={t("settings.settings.receiveEmails")} value={emails ? "ON" : "OFF"}
          options={[{ value: "ON", label: t("settings.settings.on") }, { value: "OFF", label: t("settings.settings.off") }]}
          onChange={async (v) => {
            const on = v === "ON";
            setEmails(on);
            try {
              await patch("/me", { emailOptIn: on });
              toast({ tone: "pos", title: on ? t("settings.settings.emailsOn") : t("settings.settings.emailsOff"), body: on ? undefined : t("settings.settings.emailsOffBody") });
            } catch { setEmails(!on); toast({ tone: "neg", title: t("settings.settings.emailsFailed"), body: t("common.tryAgainSoon") }); }
          }} />
      </Card>
      <Card className="stack">
        <CardHeader title={t("settings.settings.security")} />
        <div className="row small" style={{ justifyContent: "space-between" }}>
          <span className="muted">{t("settings.settings.mfa")}</span>
          {me.mfaEnabled ? <Badge tone="pos">{t("settings.settings.mfaOn")}</Badge> : <Badge>{t("settings.settings.mfaOff")}</Badge>}
        </div>
        {me.mfaEnabled && (
          <div><Button variant="out" size="sm" icon="shield" onClick={() => setRegen(true)}>{t("settings.settings.newCodes")}</Button></div>
        )}
        <PasswordCard />
      </Card>
      <Card className="stack">
        <CardHeader title={t("settings.settings.data")} />
        <p className="small muted">{t("settings.settings.dataIntro")}</p>
        <div><a className="btn out" href="/api/v1/me/export" download>{t("settings.settings.export")}</a></div>
      </Card>
      <Card className="stack">
        <CardHeader title={t("settings.settings.deleteTitle")} />
        <Alert tone="neg" title={t("settings.settings.deleteAlertTitle")}>{t("settings.settings.deleteAlert")}</Alert>
        <div><Button variant="danger" icon="trash" onClick={() => setDel(true)}>{t("settings.settings.deleteButton")}</Button></div>
      </Card>
      <ConfirmDialog open={regen} onCancel={() => setRegen(false)} busy={busy} title={t("settings.settings.newCodesTitle")}
        body={t("settings.settings.newCodesBody")} confirmLabel={t("settings.settings.newCodesConfirm")}
        onConfirm={async () => {
          setBusy(true);
          try {
            const r = await post<{ recoveryCodes: string[] }>("/me/mfa/recovery-codes");
            setCodes(r.recoveryCodes);
            setRegen(false);
          } catch {
            toast({ tone: "neg", title: t("settings.settings.newCodesFailed"), body: t("common.tryAgainSoon") });
          } finally { setBusy(false); }
        }} />
      <Modal open={!!codes} onClose={() => setCodes(null)} title={t("settings.settings.codesTitle")} labelledBy="codes-title"
        actions={<Button variant="pri" onClick={() => setCodes(null)}>{t("settings.settings.codesSaved")}</Button>}>
        {codes && <div style={{ color: "var(--text)" }}><RecoveryCodesView codes={codes} /></div>}
      </Modal>
      <ConfirmDialog open={del} onCancel={() => setDel(false)} destructive typeToConfirm="ELIMINAR" busy={busy}
        title={t("settings.settings.deleteConfirmTitle")} confirmLabel={t("settings.settings.deleteConfirm")}
        body={t("settings.settings.deleteConfirmBody")}
        onConfirm={async () => {
          setBusy(true);
          try { await post("/me/deletion-request"); router.push("/"); router.refresh(); }
          catch (e) { toast({ tone: "neg", title: t("settings.settings.couldNotFinish"), body: (e as Error).message }); setBusy(false); }
        }} />
    </div>
  );
}
