"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { useToast } from "@/components/ui/Toast";
import { post } from "@/lib/api-client";
import { STATUS_TONE, isExerciseType, type ExerciseSummary } from "@/lib/authoring-types";
import { useT } from "@/lib/i18n/client";

interface BulkResult { ok: number; failed: { versionId: string; code: string; detail: string }[] }

/** Fila de revisão com acções em lote. As regras editoriais aplicam-se a cada exercício (o autor nunca aprova o seu). */
export function ReviewQueue({ rows }: { rows: ExerciseSummary[] }) {
  const router = useRouter();
  const toast = useToast();
  const t = useT();
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [checked, setChecked] = useState(false);
  const [busy, setBusy] = useState(false);
  const [failures, setFailures] = useState<BulkResult["failed"]>([]);
  const all = rows.map((r) => r.latestVersionId);
  const pick = (ids: string[]) => ids.filter((id) => selected.has(id));
  const inReview = rows.filter((r) => r.status === "IN_REVIEW").map((r) => r.latestVersionId);
  const approved = rows.filter((r) => r.status === "APPROVED").map((r) => r.latestVersionId);

  const run = async (steps: { ids: string[]; to: string }[]) => {
    setBusy(true); setFailures([]);
    let ok = 0;
    const failed: BulkResult["failed"] = [];
    let carry: string[] = [];
    for (const step of steps) {
      const ids = [...new Set([...step.ids, ...carry])];
      if (!ids.length) continue;
      const r = await post<BulkResult>("/authoring/versions/bulk-transition", { versionIds: ids, to: step.to, referencesChecked: checked });
      ok = r.ok;
      failed.push(...r.failed);
      carry = ids.filter((id) => !r.failed.some((f) => f.versionId === id));
    }
    setFailures(failed);
    setBusy(false);
    setSelected(new Set());
    toast({ tone: failed.length ? "acc" : "pos", title: `${t("authoring.queue.updated", { n: ok })}${failed.length ? t("authoring.queue.withErrors", { n: failed.length }) : ""}` });
    router.refresh();
  };

  return (
    <div className="stack">
      <div className="card row" style={{ gap: 12 }}>
        <label className="check" style={{ paddingTop: 0 }}>
          <input type="checkbox" checked={selected.size === all.length && all.length > 0} onChange={(e) => setSelected(new Set(e.target.checked ? all : []))} />
          {t("authoring.queue.selectAll", { n: rows.length })}
        </label>
        <label className="check" style={{ paddingTop: 0 }}>
          <input type="checkbox" checked={checked} onChange={(e) => setChecked(e.target.checked)} />
          {t("authoring.queue.checked")}
        </label>
        <div className="spacer" />
        <Button variant="out" loading={busy} disabled={!checked || pick(inReview).length === 0}
          onClick={() => run([{ ids: pick(inReview), to: "APPROVED" }])}>{t("authoring.queue.approve", { n: pick(inReview).length })}</Button>
        <Button variant="pri" loading={busy} disabled={!checked || pick(all).length === 0}
          onClick={() => run([{ ids: pick(inReview), to: "APPROVED" }, { ids: pick(approved), to: "PUBLISHED" }])}>
          {t("authoring.queue.approvePublish", { n: pick(all).length })}
        </Button>
      </div>
      {failures.length > 0 && (
        <Alert tone="neg" title={t("authoring.queue.someFailed")}>
          <ul className="plain small">{failures.slice(0, 8).map((f) => <li key={f.versionId + f.code}>{f.detail}</li>)}</ul>
        </Alert>
      )}
      <div className="tbl-wrap">
        <table className="tbl">
          <thead><tr><th><span className="sr-only">{t("authoring.queue.select")}</span></th><th>{t("authoring.page.code")}</th><th>{t("authoring.page.exercise")}</th><th>{t("authoring.page.type")}</th><th>{t("authoring.page.author")}</th><th>{t("authoring.page.state")}</th></tr></thead>
          <tbody>
            {rows.map((e) => (
              <tr key={e.id} className={selected.has(e.latestVersionId) ? "me" : undefined}>
                <td><input type="checkbox" aria-label={t("authoring.queue.selectOne", { code: e.code })} checked={selected.has(e.latestVersionId)}
                  onChange={(ev) => { const s = new Set(selected); if (ev.target.checked) s.add(e.latestVersionId); else s.delete(e.latestVersionId); setSelected(s); }} /></td>
                <td className="mono"><Link href={`/autoria/${e.id}`}>{e.code}</Link></td>
                <td><Link href={`/autoria/${e.id}`}><b>{e.title}</b></Link><div className="small muted">{e.topic}</div></td>
                <td className="small">{isExerciseType(e.type) ? t(`authoring.type.${e.type}`) : e.type}</td>
                <td className="small">{e.authorName}</td>
                <td><Badge tone={STATUS_TONE[e.status]}>{t(`authoring.status.${e.status}`)}</Badge></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="small muted">{t("authoring.queue.note")}</p>
    </div>
  );
}
