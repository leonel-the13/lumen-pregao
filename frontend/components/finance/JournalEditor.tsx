"use client";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { MoneyInput } from "@/components/ui/MoneyInput";
import { formatNumber, parseAmount } from "@/lib/format";
import { useT } from "@/lib/i18n/client";
import type { T } from "@/lib/i18n/translate";
import { AccountSearch } from "./AccountSearch";

export interface JournalLine { account: string; name: string; debit: string; credit: string }
export type LineStatus = "OK" | "WRONG_ACCOUNT" | "UNKNOWN_ACCOUNT" | "WRONG_SIDE" | "WRONG_AMOUNT";

const STATUS_TONE: Record<LineStatus, "pos" | "neg"> = { OK: "pos", WRONG_ACCOUNT: "neg", UNKNOWN_ACCOUNT: "neg", WRONG_SIDE: "neg", WRONG_AMOUNT: "neg" };

export function totals(lines: JournalLine[]) {
  let d = 0, c = 0;
  for (const l of lines) { d += parseAmount(l.debit) || 0; c += parseAmount(l.credit) || 0; }
  return { debit: Math.round(d * 100) / 100, credit: Math.round(c * 100) / 100 };
}

/** Só formato e equilíbrio no cliente; a correcção oficial é do servidor (design: Sala · editor de lançamentos). */
export function validateJournal(lines: JournalLine[], t: T): string | null {
  if (lines.length === 0) return t("task.journal.noLines");
  for (const [i, l] of lines.entries()) {
    const d = parseAmount(l.debit), c = parseAmount(l.credit);
    if (Number.isNaN(d) || Number.isNaN(c)) return t("task.journal.invalidValue", { n: i + 1 });
    if ((d ?? 0) > 0 === (c ?? 0) > 0) return t("task.journal.oneSide", { n: i + 1 });
  }
  const sum = totals(lines);
  if (sum.debit !== sum.credit) return t("task.journal.unbalanced");
  return null;
}

export function toPayload(lines: JournalLine[]) {
  return { lines: lines.map((l) => ({ account: l.account, debit: parseAmount(l.debit) || null, credit: parseAmount(l.credit) || null })) };
}

export function JournalEditor({ lines, onChange, results, pgcVersion, disabled }:
  { lines: JournalLine[]; onChange: (l: JournalLine[]) => void; results?: LineStatus[]; pgcVersion?: string; disabled?: boolean }) {
  const tr = useT();
  const t = totals(lines);
  const balanced = lines.length > 0 && t.debit === t.credit && t.debit > 0;
  const set = (i: number, patch: Partial<JournalLine>) => onChange(lines.map((l, j) => (j === i ? { ...l, ...patch } : l)));
  const remove = (i: number) => onChange(lines.filter((_, j) => j !== i));
  const add = (account = "", name = "") => onChange([...lines, { account, name, debit: "", credit: "" }]);
  const status = (i: number): [string, "pos" | "neg"] | undefined =>
    results?.[i] ? [tr(`task.journal.status.${results[i]}`), STATUS_TONE[results[i]]] : undefined;

  return (
    <div className="stack" style={{ gap: 14 }}>
      {!disabled && <AccountSearch version={pgcVersion} onPick={(a) => add(a.code, a.name)} />}

      <div className="tbl-wrap journal-table">
        <table className="tbl">
          <caption className="sr-only">{tr("task.journal.caption")}</caption>
          <thead>
            <tr><th>#</th><th>{tr("task.journal.account")}</th><th className="num">{tr("task.journal.debitKz")}</th><th className="num">{tr("task.journal.creditKz")}</th><th>{tr("task.journal.validation")}</th><th><span className="sr-only">{tr("task.journal.remove")}</span></th></tr>
          </thead>
          <tbody>
            {lines.map((l, i) => {
              const st = status(i);
              return (
                <tr key={i}>
                  <td className="mono muted">{i + 1}</td>
                  <td><span className="acct">{l.account}</span> {l.name}</td>
                  <td style={{ width: 190 }}>
                    <MoneyInput aria-label={tr("task.journal.debitOf", { n: i + 1 })} unit="" value={l.debit} disabled={disabled}
                      onChange={(v) => set(i, { debit: v })} invalid={st?.[1] === "neg" && !!l.debit} />
                  </td>
                  <td style={{ width: 190 }}>
                    <MoneyInput aria-label={tr("task.journal.creditOf", { n: i + 1 })} unit="" value={l.credit} disabled={disabled}
                      onChange={(v) => set(i, { credit: v })} invalid={st?.[1] === "neg" && !!l.credit}
                      onKeyDown={(e) => { if (e.key === "Enter" && i === lines.length - 1) { e.preventDefault(); add(); } }} />
                  </td>
                  <td>{st ? <Badge tone={st[1]}>{st[0]}</Badge> : <span className="small muted">—</span>}</td>
                  <td>{!disabled && <Button variant="ghost" size="sm" icon="trash" aria-label={tr("task.journal.removeLine", { n: i + 1 })} onClick={() => remove(i)} />}</td>
                </tr>
              );
            })}
            {lines.length === 0 && <tr><td colSpan={6} className="small muted">{tr("task.journal.empty")}</td></tr>}
          </tbody>
          <tfoot>
            <tr>
              <td colSpan={2}>{tr("task.journal.totals")}</td>
              <td className="num">{formatNumber(t.debit)}</td>
              <td className="num">{formatNumber(t.credit)}</td>
              <td colSpan={2}>{lines.length > 0 && (balanced ? <Badge tone="pos">{tr("task.journal.balanced")}</Badge> : <Badge tone="neg">{tr("task.journal.notBalanced")}</Badge>)}</td>
            </tr>
          </tfoot>
        </table>
      </div>

      {/* Telemóvel: uma linha de lançamento por cartão */}
      <div className="journal-cards">
        {lines.map((l, i) => {
          const st = status(i);
          return (
            <div key={i} className="card stack" style={{ padding: 14 }}>
              <div className="row"><span className="acct">{l.account}</span><span style={{ flex: 1 }}>{l.name}</span>{st && <Badge tone={st[1]}>{st[0]}</Badge>}</div>
              <div className="grid" style={{ gridTemplateColumns: "1fr 1fr", gap: 8 }}>
                <MoneyInput aria-label={tr("task.journal.debitOf", { n: i + 1 })} value={l.debit} disabled={disabled} onChange={(v) => set(i, { debit: v })} />
                <MoneyInput aria-label={tr("task.journal.creditOf", { n: i + 1 })} value={l.credit} disabled={disabled} onChange={(v) => set(i, { credit: v })} />
              </div>
              {!disabled && <Button variant="ghost" size="sm" icon="trash" onClick={() => remove(i)}>{tr("task.journal.remove")}</Button>}
            </div>
          );
        })}
        <div className="row" style={{ justifyContent: "space-between" }}>
          <b>{tr("task.journal.totals")}</b>
          {lines.length > 0 && (balanced ? <Badge tone="pos">{tr("task.journal.balanced")}</Badge> : <Badge tone="neg">{tr("task.journal.notBalanced")}</Badge>)}
        </div>
      </div>

      {!disabled && (
        <div className="row">
          <Button variant="ghost" size="sm" icon="plus" onClick={() => add()} disabled={lines.length === 0}>{tr("task.journal.addLine")}</Button>
          <span className="small muted">{tr("task.journal.shortcut")}</span>
        </div>
      )}
    </div>
  );
}
