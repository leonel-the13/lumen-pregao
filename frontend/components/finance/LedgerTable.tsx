"use client";
import { formatNumber } from "@/lib/format";
import { useT } from "@/lib/i18n/client";

export interface LedgerRow { account: string; label: string; value: { value: number; display: string } | number; side: "D" | "C" }

/** Balancete (excerto): contas e saldos D/C, valores alinhados à direita. */
export function LedgerTable({ rows, caption }: { rows: LedgerRow[]; caption?: string }) {
  const t = useT();
  return (
    <div className="tbl-wrap">
      <table className="tbl" style={{ minWidth: 0 }}>
        <caption className="sr-only">{caption ?? t("task.ledger.caption")}</caption>
        <thead><tr><th>{t("task.ledger.account")}</th><th className="num">{t("task.ledger.balance")}</th></tr></thead>
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
