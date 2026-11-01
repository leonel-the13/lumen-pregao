"use client";
import Link from "next/link";
import { useRef, type KeyboardEvent } from "react";

export interface TabItem { key: string; label: string; href?: string; count?: number }

/** Separadores com navegação por setas (WAI-ARIA tabs). Com href, são ligações. */
export function Tabs({ items, active, onChange, label }: { items: TabItem[]; active: string; onChange?: (k: string) => void; label: string }) {
  const refs = useRef<(HTMLButtonElement | HTMLAnchorElement | null)[]>([]);
  const onKey = (e: KeyboardEvent, i: number) => {
    if (e.key !== "ArrowRight" && e.key !== "ArrowLeft") return;
    e.preventDefault();
    const n = (i + (e.key === "ArrowRight" ? 1 : -1) + items.length) % items.length;
    refs.current[n]?.focus();
    if (!items[n].href) onChange?.(items[n].key);
  };
  return (
    <div className="tabs" role="tablist" aria-label={label}>
      {items.map((t, i) =>
        t.href ? (
          <Link key={t.key} href={t.href} ref={(el) => { refs.current[i] = el; }} className={["tab", active === t.key && "on"].filter(Boolean).join(" ")}
            role="tab" aria-selected={active === t.key} onKeyDown={(e) => onKey(e, i)}>
            {t.label}{t.count !== undefined && <span className="mono small muted">{t.count}</span>}
          </Link>
        ) : (
          <button key={t.key} type="button" ref={(el) => { refs.current[i] = el; }} className={["tab", active === t.key && "on"].filter(Boolean).join(" ")}
            role="tab" aria-selected={active === t.key} tabIndex={active === t.key ? 0 : -1}
            onClick={() => onChange?.(t.key)} onKeyDown={(e) => onKey(e, i)}>
            {t.label}{t.count !== undefined && <span className="mono small muted">{t.count}</span>}
          </button>
        ),
      )}
    </div>
  );
}

/** Controlo segmentado (ex.: Geral · Curso · Ano). */
export function Seg<T extends string>({ options, value, onChange, label }: { options: { value: T; label: string }[]; value: T; onChange: (v: T) => void; label: string }) {
  return (
    <div className="seg" role="radiogroup" aria-label={label}>
      {options.map((o) => (
        <button key={o.value} type="button" role="radio" aria-checked={value === o.value} className={value === o.value ? "on" : undefined} onClick={() => onChange(o.value)}>
          {o.label}
        </button>
      ))}
    </div>
  );
}
