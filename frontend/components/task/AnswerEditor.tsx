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
import { useT } from "@/lib/i18n/client";
import type { T } from "@/lib/i18n/translate";

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
export function validateAnswer(type: TaskType, p: Presentation, a: AnswerState, t: T): string | null {
  switch (type) {
    case "A": return a.selected.length ? null : t("task.validate.choose");
    case "B": return validNumber(a.value) ? null : t("task.validate.number");
    case "C": return validateJournal(a.lines, t);
    case "D": return Object.values(a.items).some((v) => v) ? null : t("task.validate.statement");
    case "E": return Object.values(a.steps).some((v) => v) ? null : t("task.validate.step");
    case "F": {
      if (!validNumber(a.value)) return t("task.validate.ratio");
      const min = p.justification?.minLength ?? 0;
      return a.justification.trim().length < min ? t("task.validate.justificationMin", { n: min }) : null;
    }
    default: return t("task.validate.jury");
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
  const t = useT();
  const d = result?.details ?? {};
  switch (type) {
    case "A": {
      const multiple = !!presentation.multiple;
      return (
        <fieldset className="stack" style={{ border: 0, padding: 0, margin: 0, gap: 8 }}>
          <legend className="label" style={{ marginBottom: 8 }}>{multiple ? t("task.answer.chooseAll") : t("task.answer.chooseOne")}</legend>
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
        <Field label={t("task.answer.answer")} hint={t("task.answer.tolerance", { unit: presentation.unit || t("task.answer.value") })}>
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
            <p className="small muted">{t("task.answer.fictionalValues")}</p>
            {Array.isArray(presentation.relations) && (
              <div className="card flat stack" style={{ padding: 14 }}>
                <b className="small">{t("task.answer.relations")}</b>
                {(presentation.relations as { label: string }[]).map((r) => <span key={r.label} className="small muted">{r.label}</span>)}
              </div>
            )}
          </div>
          <StatementEditor sections={presentation.sections ?? []} values={state.items} disabled={disabled} company={company}
            kind={presentation.kind as string | undefined}
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
          <Field label={t("task.answer.ratio")} hint={t("task.answer.twoDecimals")}>
            {({ id: fid, describedBy }) => (
              <div style={{ maxWidth: 240 }}>
                <MoneyInput id={fid} aria-describedby={describedBy} unit="" value={state.value} disabled={disabled}
                  onChange={(v) => onChange({ ...state, value: v })} invalid={d.value === "WRONG"} ok={d.value === "OK"} />
              </div>
            )}
          </Field>
          <Field label={t("task.answer.interpretation")} hint={t("task.answer.minChars", { n: state.justification.trim().length, min })}
            error={d.justification === "TOO_SHORT" ? t("task.answer.tooShort") : d.justification === "MISSING_TERMS" ? t("task.answer.missingTerms") : undefined}>
            {({ id: fid, describedBy, invalid }) => (
              <TextArea id={fid} aria-describedby={describedBy} invalid={invalid} value={state.justification} disabled={disabled} maxLength={4000}
                onChange={(e) => onChange({ ...state, justification: e.target.value })} />
            )}
          </Field>
        </div>
      );
    }
    default:
      return <Badge>{t("task.answer.openCase")}</Badge>;
  }
}
