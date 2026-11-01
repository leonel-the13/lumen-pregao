"use client";
import { Badge } from "@/components/ui/Badge";
import { MoneyInput } from "@/components/ui/MoneyInput";

export interface Step { key: string; label: string; formula?: string; help?: string }
export type StepStatus = "OK" | "WRONG" | "PENDING" | "LOCKED";

/** Calculadora fiscal passo a passo: cada passo é corrigido no servidor antes de desbloquear o seguinte. */
export function TaxStepper({ steps, values, onChange, statuses, disabled }:
  { steps: Step[]; values: Record<string, string>; onChange: (v: Record<string, string>) => void; statuses: Record<string, StepStatus>; disabled?: boolean }) {
  const firstOpen = steps.findIndex((s) => statuses[s.key] !== "OK");
  return (
    <ol className="plain stack" style={{ gap: 12 }}>
      {steps.map((s, i) => {
        const st = statuses[s.key];
        const unlocked = i <= (firstOpen === -1 ? steps.length : firstOpen);
        const current = i === firstOpen;
        return (
          <li key={s.key} className={["card", current && "lit"].filter(Boolean).join(" ")} style={{ padding: 16, opacity: unlocked ? 1 : 0.55 }}>
            <div className="row" style={{ gap: 14, alignItems: "flex-start" }}>
              <span className="avatar mono" aria-hidden="true">{i + 1}</span>
              <div style={{ flex: 1, minWidth: 200 }} className="stack">
                <div><b>{s.label}</b>{s.formula && <div className="small muted">{s.formula}</div>}</div>
                {s.help && unlocked && <div className="small muted">{s.help}</div>}
                <div style={{ maxWidth: 280 }}>
                  <MoneyInput aria-label={s.label} value={values[s.key] ?? ""} disabled={disabled || !unlocked || st === "OK"}
                    onChange={(v) => onChange({ ...values, [s.key]: v })} invalid={st === "WRONG"} ok={st === "OK"} />
                </div>
              </div>
              {st === "OK" ? <Badge tone="pos">Correcto</Badge> : st === "WRONG" ? <Badge tone="neg">Incorrecto</Badge>
                : current ? <Badge tone="acc">A preencher</Badge> : <Badge icon="lock">Seguinte</Badge>}
            </div>
          </li>
        );
      })}
    </ol>
  );
}
