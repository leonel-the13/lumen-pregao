"use client";
import { Alert } from "@/components/ui/Alert";
import { Badge } from "@/components/ui/Badge";
import { MoneyInput } from "@/components/ui/MoneyInput";
import { formatKz, formatNumber, parseAmount } from "@/lib/format";

interface Item { key: string; label: string }
interface Group { title: string; items: Item[] }
export interface Section { key: string; title: string; totalKey: string; totalLabel: string; groups: Group[] }

export type Values = Record<string, string>;

export function sectionTotal(s: Section, values: Values) {
  return s.groups.flatMap((g) => g.items).reduce((acc, i) => acc + (parseAmount(values[i.key] ?? "") || 0), 0);
}

/** Inclui os totais calculados no cliente (só para orientar; a correcção é do servidor). */
export function toStatementPayload(sections: Section[], values: Values) {
  const items: Record<string, number | null> = {};
  for (const s of sections) {
    for (const i of s.groups.flatMap((g) => g.items)) {
      const v = parseAmount(values[i.key] ?? "");
      items[i.key] = v === null || Number.isNaN(v) ? null : v;
    }
    items[s.totalKey] = Math.round(sectionTotal(s, values) * 100) / 100;
  }
  return { items };
}

export function StatementEditor({ sections, values, onChange, results, disabled, company, kind }:
  { sections: Section[]; values: Values; onChange: (v: Values) => void; results?: Record<string, string>; disabled?: boolean; company?: string; kind?: string }) {
  const totals = sections.map((s) => sectionTotal(s, values));
  const diff = sections.length === 2 ? Math.abs(totals[0] - totals[1]) : 0;
  const filled = Object.values(values).some((v) => v);
  return (
    <div className="stack">
      <div className="row"><h3 className="h3">{kind === "income" ? "Demonstração de resultados" : "Balanço"}{company ? ` · ${company}` : ""}</h3><span className="tbc">fictícia</span><div className="spacer" /><span className="label">Kz</span></div>
      <div className="grid" style={{ alignItems: "start", gridTemplateColumns: `repeat(auto-fit,minmax(min(100%,${sections.length > 2 ? 300 : 360}px),1fr))` }}>
        {sections.map((s, si) => (
          <div key={s.key} className="tbl-wrap">
            <table className="tbl" style={{ minWidth: 0 }}>
              <thead><tr><th>{s.title}</th><th className="num">Valor</th></tr></thead>
              <tbody>
                {s.groups.map((g) => [
                  <tr key={g.title}><td colSpan={2} className="label" style={{ background: "var(--surface-2)" }}>{g.title}</td></tr>,
                  ...g.items.map((i) => {
                    const r = results?.[i.key];
                    return (
                      <tr key={i.key}>
                        <td>
                          {i.label}
                          {r && r !== "OK" && <> <Badge tone="neg">{r === "MISSING" ? "Em falta" : "Incorrecto"}</Badge></>}
                          {r === "OK" && <> <Badge tone="pos">Certo</Badge></>}
                        </td>
                        <td style={{ width: 200 }}>
                          <MoneyInput aria-label={i.label} unit="" value={values[i.key] ?? ""} disabled={disabled}
                            onChange={(v) => onChange({ ...values, [i.key]: v })} invalid={!!r && r !== "OK"} ok={r === "OK"} />
                        </td>
                      </tr>
                    );
                  }),
                ])}
              </tbody>
              <tfoot><tr><td>{s.totalLabel}</td><td className="num">{formatNumber(totals[si])}</td></tr></tfoot>
            </table>
          </div>
        ))}
      </div>
      {filled && sections.length === 2 && (diff > 0.005
        ? <Alert tone="neg" title={`O balanço não fecha: diferença de ${formatKz(diff)}`}>Revê as rubricas: o Activo tem de ser igual ao Capital próprio + Passivo.</Alert>
        : <Alert tone="pos" title="O balanço fecha">Activo = Capital próprio + Passivo. A correcção das rubricas é feita no servidor.</Alert>)}
    </div>
  );
}
