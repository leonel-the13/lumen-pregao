"use client";
import { useId } from "react";
import { JournalEditor, toPayload, validateJournal, type JournalLine, type LineStatus } from "@/components/finance/JournalEditor";
import { LedgerTable, type LedgerRow } from "@/components/finance/LedgerTable";
import { StatementEditor, toStatementPayload, type Section } from "@/components/finance/StatementEditor";
import { TaxStepper, type Step, type StepStatus } from "@/components/finance/TaxStepper";
import { Badge } from "@/components/ui/Badge";
import { Field, TextArea } from "@/components/ui/Field";
import { MoneyInput } from "@/components/ui/MoneyInput";
import { parseAmount } from "@/lib/format";

export type TaskType = "A" | "B" | "C" | "D" | "E" | "F" | "G";

export interface AnswerState {
  selected: string[];
  value: string;
  justification: string;
  lines: JournalLine[];
  items: Record<string, string>;
  steps: Record<string, string>;
}

export const emptyAnswer = (): AnswerState => ({ selected: [], value: "", justification: "", lines: [], items: {}, steps: {} });

type Presentation = Record<string, unknown> & {
  options?: { key: string; text: string }[]; multiple?: boolean; unit?: string; precision?: number;
  steps?: Step[]; sections?: Section[]; ledger?: LedgerRow[]; justification?: { minLength?: number };
};

/** Validação de formato no cliente (a correcção é sempre no servidor). */
export function validateAnswer(type: TaskType, p: Presentation, a: AnswerState): string | null {
  switch (type) {
    case "A": return a.selected.length ? null : "Escolhe uma opção.";
    case "B": return validNumber(a.value) ? null : "Indica um valor numérico.";
    case "C": return validateJournal(a.lines);
    case "D": return Object.values(a.items).some((v) => v) ? null : "Preenche as rubricas da demonstração.";
    case "E": return Object.values(a.steps).some((v) => v) ? null : "Preenche o passo actual.";
    case "F": {
      if (!validNumber(a.value)) return "Indica o valor do rácio.";
      const min = p.justification?.minLength ?? 0;
      return a.justification.trim().length < min ? `A justificação precisa de pelo menos ${min} caracteres.` : null;
    }
    default: return "Este tipo de tarefa é corrigido por júri (fase posterior).";
  }
}

function validNumber(v: string) {
  const n = parseAmount(v);
  return n !== null && !Number.isNaN(n);
}

/** Para o tipo E, envia só os passos até ao primeiro por preencher. */
export function buildPayload(type: TaskType, p: Presentation, a: AnswerState): unknown {
  switch (type) {
    case "A": return { selected: a.selected };
    case "B": return { value: parseAmount(a.value) };
    case "C": return toPayload(a.lines);
    case "D": return toStatementPayload(p.sections ?? [], a.items);
    case "E": {
      const steps: Record<string, number> = {};
      for (const s of p.steps ?? []) {
        const v = parseAmount(a.steps[s.key] ?? "");
        if (v === null || Number.isNaN(v)) break;
        steps[s.key] = v;
      }
      return { steps };
    }
    case "F": return { value: parseAmount(a.value), justification: a.justification };
    default: return {};
  }
}

export interface ResultView { details?: Record<string, unknown> }

export function AnswerEditor({ type, presentation, state, onChange, result, disabled, pgcVersion, company }:
  { type: TaskType; presentation: Presentation; state: AnswerState; onChange: (a: AnswerState) => void; result?: ResultView;
    disabled?: boolean; pgcVersion?: string; company?: string }) {
  const id = useId();
  const d = result?.details ?? {};
  switch (type) {
    case "A": {
      const multiple = !!presentation.multiple;
      return (
        <fieldset className="stack" style={{ border: 0, padding: 0, margin: 0, gap: 8 }}>
          <legend className="label" style={{ marginBottom: 8 }}>{multiple ? "Escolhe todas as que se aplicam" : "Escolhe uma opção"}</legend>
          {(presentation.options ?? []).map((o) => {
            const on = state.selected.includes(o.key);
            return (
              <label key={o.key} className={["option", on && "on"].filter(Boolean).join(" ")}>
                <input type={multiple ? "checkbox" : "radio"} name={id} className="sr-only" checked={on} disabled={disabled}
                  onChange={() => onChange({ ...state, selected: multiple ? (on ? state.selected.filter((k) => k !== o.key) : [...state.selected, o.key]) : [o.key] })} />
                <span className="k" aria-hidden="true">{o.key}</span><span>{o.text}</span>
              </label>
            );
          })}
        </fieldset>
      );
    }
    case "B":
      return (
        <Field label="Resposta" hint={`Tolerância definida no exercício · ${presentation.unit || "valor"}`}>
          {({ id: fid, describedBy }) => (
            <div style={{ maxWidth: 320 }}>
              <MoneyInput id={fid} aria-describedby={describedBy} unit={(presentation.unit as string) ?? ""} value={state.value} disabled={disabled}
                onChange={(v) => onChange({ ...state, value: v })} decimals={presentation.precision ?? 2}
                invalid={d.value === "WRONG"} ok={d.value === "OK"} />
            </div>
          )}
        </Field>
      );
    case "C":
      return <JournalEditor lines={state.lines} onChange={(lines) => onChange({ ...state, lines })} disabled={disabled} pgcVersion={pgcVersion}
        results={(d.lines as { status: LineStatus }[] | undefined)?.map((l) => l.status)} />;
    case "D":
      return (
        <div className="stmt-grid">
          <div className="stack">
            {presentation.ledger && <LedgerTable rows={presentation.ledger} />}
            <p className="small muted">Valores fictícios gerados para esta tentativa.</p>
            {Array.isArray(presentation.relations) && (
              <div className="card flat stack" style={{ padding: 14 }}>
                <b className="small">Relações validadas</b>
                {(presentation.relations as { label: string }[]).map((r) => <span key={r.label} className="small muted">{r.label}</span>)}
              </div>
            )}
          </div>
          <StatementEditor sections={presentation.sections ?? []} values={state.items} disabled={disabled} company={company}
            onChange={(items) => onChange({ ...state, items })} results={d.items as Record<string, string> | undefined} />
        </div>
      );
    case "E": {
      const statuses: Record<string, StepStatus> = {};
      ((d.steps as { key: string; status: StepStatus }[] | undefined) ?? []).forEach((s) => (statuses[s.key] = s.status));
      return <TaxStepper steps={presentation.steps ?? []} values={state.steps} statuses={statuses} disabled={disabled}
        onChange={(steps) => onChange({ ...state, steps })} />;
    }
    case "F": {
      const min = presentation.justification?.minLength ?? 0;
      return (
        <div className="stack">
          <Field label="Valor do rácio" hint="Duas casas decimais.">
            {({ id: fid, describedBy }) => (
              <div style={{ maxWidth: 240 }}>
                <MoneyInput id={fid} aria-describedby={describedBy} unit="" value={state.value} disabled={disabled}
                  onChange={(v) => onChange({ ...state, value: v })} invalid={d.value === "WRONG"} ok={d.value === "OK"} />
              </div>
            )}
          </Field>
          <Field label="Interpretação" hint={`${state.justification.trim().length} / ${min} caracteres no mínimo`}
            error={d.justification === "TOO_SHORT" ? "A justificação é demasiado curta." : d.justification === "MISSING_TERMS" ? "A justificação não interpreta o rácio." : undefined}>
            {({ id: fid, describedBy, invalid }) => (
              <TextArea id={fid} aria-describedby={describedBy} invalid={invalid} value={state.justification} disabled={disabled} maxLength={4000}
                onChange={(e) => onChange({ ...state, justification: e.target.value })} />
            )}
          </Field>
        </div>
      );
    }
    default:
      return <Badge>Caso aberto · avaliação por júri (fase posterior)</Badge>;
  }
}
