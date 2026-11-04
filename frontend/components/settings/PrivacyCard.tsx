"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Card, CardHeader } from "@/components/ui/Card";
import { Field, Input } from "@/components/ui/Field";
import { Seg } from "@/components/ui/Tabs";
import { useToast } from "@/components/ui/Toast";
import { patch } from "@/lib/api-client";
import { isApiError } from "@/lib/errors";
import { useT } from "@/lib/i18n/client";
import type { Me } from "@/lib/types";

type Vis = Me["rankingVisibility"];

/** Privacidade no ranking: nome, pseudónimo ou oculto. A tua posição continua visível para ti. */
export function PrivacyCard({ me }: { me: Me }) {
  const router = useRouter();
  const toast = useToast();
  const t = useT();
  const [pseudonym, setPseudonym] = useState(me.pseudonym ?? "");
  const [vis, setVis] = useState<Vis>(me.rankingVisibility);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const save = async () => {
    setBusy(true); setError(null);
    try {
      await patch("/me", { pseudonym, rankingVisibility: vis });
      toast({ tone: "pos", title: t("settings.privacy.saved") });
      router.refresh();
    } catch (e) {
      setError(isApiError(e) ? (e.fields.pseudonym ?? e.message) : t("common.couldNotSave"));
    } finally { setBusy(false); }
  };
  return (
    <Card className="stack">
      <CardHeader title={t("settings.privacy.title")} />
      <p className="small muted">{t("settings.privacy.intro")}</p>
      <Field label={t("settings.privacy.pseudonym")} hint={t("settings.privacy.pseudonymHint")} error={error ?? undefined}>
        {({ id, describedBy, invalid }) => <Input id={id} aria-describedby={describedBy} invalid={invalid} value={pseudonym} maxLength={24}
          onChange={(e) => setPseudonym(e.target.value.toLowerCase())} />}
      </Field>
      <Seg label={t("settings.privacy.visibility")} value={vis} onChange={setVis}
        options={[{ value: "NAME", label: t("settings.privacy.name") }, { value: "PSEUDONYM", label: t("settings.privacy.pseudonymOpt") }, { value: "HIDDEN", label: t("settings.privacy.hidden") }]} />
      <div><Button variant="out" onClick={save} loading={busy}>{t("common.save")}</Button></div>
    </Card>
  );
}
