import { formatNumber } from "@/lib/format";

export interface LedgerRow { account: string; label: string; value: { value: number; display: string } | number; side: "D" | "C" }

/** Balancete (excerto): contas e saldos D/C, valores alinhados à direita. */
export function LedgerTable({ rows, caption = "Balancete final (excerto)" }: { rows: LedgerRow[]; caption?: string }) {
  return (
    <div className="tbl-wrap">
      <table className="tbl" style={{ minWidth: 0 }}>
        <caption className="sr-only">{caption}</caption>
        <thead><tr><th>Conta</th><th className="num">Saldo</th></tr></thead>
        <tbody>
          {rows.map((r) => {
            const v = typeof r.value === "number" ? r.value : r.value.value;
            return (
              <tr key={r.account}>
                <td><span className="acct">{r.account}</span> {r.label}</td>
                <td className="num">{formatNumber(v)} <span className="muted">{r.side}</span></td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
