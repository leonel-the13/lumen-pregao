"use client";
import { Icon } from "@/components/ui/Icon";
import { HTML_LANG } from "@/lib/i18n/config";
import { useLocale, useT } from "@/lib/i18n/client";

/** Estrelas 1–5 como grupo de rádio (setas do teclado mudam o valor). Só leitura sem onChange. */
export function StarRating({ value, onChange, label, size = 28 }:
  { value: number | null; onChange?: (v: number) => void; label: string; size?: number }) {
  const t = useT();
  const locale = useLocale();
  if (!onChange) {
    const v = value ?? 0;
    return (
      <span className="stars" role="img" aria-label={t("learn.rating.outOf", { label, v: v.toLocaleString(HTML_LANG[locale], { maximumFractionDigits: 1 }) })}>
        {[1, 2, 3, 4, 5].map((i) => (
          <Icon key={i} name="star" className={`icon star ${i <= Math.round(v) ? "on" : ""}`} style={{ width: size * 0.6, height: size * 0.6 }} />
        ))}
      </span>
    );
  }
  return (
    <div className="stars" role="radiogroup" aria-label={label}>
      {[1, 2, 3, 4, 5].map((i) => (
        <button key={i} type="button" role="radio" aria-checked={value === i} aria-label={t(i === 1 ? "learn.rating.star" : "learn.rating.stars", { n: i })}
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
