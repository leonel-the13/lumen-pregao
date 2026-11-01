"use client";
import { useEffect, useState, type InputHTMLAttributes } from "react";
import { formatNumber, parseAmount } from "@/lib/format";

interface Props extends Omit<InputHTMLAttributes<HTMLInputElement>, "value" | "onChange"> {
  value: string;
  onChange: (raw: string) => void;
  unit?: string;
  decimals?: number;
  invalid?: boolean;
  ok?: boolean;
}

/** Campo monetário: aceita números e vírgula; formata ao sair do campo (design A4). */
export function MoneyInput({ value, onChange, unit = "Kz", decimals = 2, invalid, ok, className, onBlur, ...rest }: Props) {
  const [text, setText] = useState(value);
  useEffect(() => setText(value), [value]);
  const bad = text !== "" && Number.isNaN(parseAmount(text));
  return (
    <span className="money-wrap">
      {unit && <span className="cur" aria-hidden="true">{unit}</span>}
      <input
        {...rest}
        inputMode="decimal"
        autoComplete="off"
        className={["input money", (invalid || bad) && "is-err", ok && "is-ok", className].filter(Boolean).join(" ")}
        aria-invalid={invalid || bad || undefined}
        style={unit ? undefined : { paddingLeft: 14 }}
        value={text}
        onChange={(e) => {
          const v = e.target.value.replace(/[^0-9,.\s −-]/g, "");
          setText(v);
          onChange(v);
        }}
        onBlur={(e) => {
          const n = parseAmount(text);
          if (n !== null && !Number.isNaN(n)) {
            const f = formatNumber(n, decimals);
            setText(f);
            onChange(f);
          }
          onBlur?.(e);
        }}
      />
    </span>
  );
}
