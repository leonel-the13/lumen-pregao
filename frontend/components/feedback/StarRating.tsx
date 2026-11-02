"use client";
import { Icon } from "@/components/ui/Icon";

/** Estrelas 1–5 como grupo de rádio (setas do teclado mudam o valor). Só leitura sem onChange. */
export function StarRating({ value, onChange, label, size = 28 }:
  { value: number | null; onChange?: (v: number) => void; label: string; size?: number }) {
  if (!onChange) {
    const v = value ?? 0;
    return (
      <span className="stars" role="img" aria-label={`${label}: ${v.toLocaleString("pt-PT", { maximumFractionDigits: 1 })} de 5`}>
        {[1, 2, 3, 4, 5].map((i) => (
          <Icon key={i} name="star" className={`icon star ${i <= Math.round(v) ? "on" : ""}`} style={{ width: size * 0.6, height: size * 0.6 }} />
        ))}
      </span>
    );
  }
  return (
    <div className="stars" role="radiogroup" aria-label={label}>
      {[1, 2, 3, 4, 5].map((i) => (
        <button key={i} type="button" role="radio" aria-checked={value === i} aria-label={`${i} ${i === 1 ? "estrela" : "estrelas"}`}
          tabIndex={(value ?? 1) === i ? 0 : -1} className={`star-btn ${value !== null && i <= value ? "on" : ""}`}
          onClick={() => onChange(i)}
          onKeyDown={(e) => {
            if (e.key === "ArrowRight" || e.key === "ArrowUp") { e.preventDefault(); onChange(Math.min(5, (value ?? 0) + 1)); }
            if (e.key === "ArrowLeft" || e.key === "ArrowDown") { e.preventDefault(); onChange(Math.max(1, (value ?? 2) - 1)); }
          }}>
          <Icon name="star" />
        </button>
      ))}
    </div>
  );
}
