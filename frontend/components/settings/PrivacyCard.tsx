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
import type { Me } from "@/lib/types";

type Vis = Me["rankingVisibility"];

/** Privacidade no ranking: nome, pseudónimo ou oculto. A tua posição continua visível para ti. */
export function PrivacyCard({ me }: { me: Me }) {
  const router = useRouter();
  const toast = useToast();
  const [pseudonym, setPseudonym] = useState(me.pseudonym ?? "");
  const [vis, setVis] = useState<Vis>(me.rankingVisibility);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const save = async () => {
    setBusy(true); setError(null);
    try {
      await patch("/me", { pseudonym, rankingVisibility: vis });
      toast({ tone: "pos", title: "Privacidade actualizada" });
      router.refresh();
    } catch (e) {
      setError(isApiError(e) ? (e.fields.pseudonym ?? e.message) : "Não foi possível guardar.");
    } finally { setBusy(false); }
  };
  return (
    <Card className="stack">
      <CardHeader title="Privacidade no ranking" />
      <p className="small muted">Podes aparecer com um pseudónimo ou ocultar o nome. A tua posição continua visível para ti.</p>
      <Field label="Pseudónimo" hint="3 a 24 caracteres: letras minúsculas, números e _." error={error ?? undefined}>
        {({ id, describedBy, invalid }) => <Input id={id} aria-describedby={describedBy} invalid={invalid} value={pseudonym} maxLength={24}
          onChange={(e) => setPseudonym(e.target.value.toLowerCase())} />}
      </Field>
      <Seg label="Como apareces no ranking" value={vis} onChange={setVis}
        options={[{ value: "NAME", label: "Nome" }, { value: "PSEUDONYM", label: "Pseudónimo" }, { value: "HIDDEN", label: "Oculto" }]} />
      <div><Button variant="out" onClick={save} loading={busy}>Guardar</Button></div>
    </Card>
  );
}
