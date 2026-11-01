"use client";
import { useEffect, useId, useRef, useState } from "react";
import { Icon } from "@/components/ui/Icon";
import { get } from "@/lib/api-client";

export interface Account { code: string; name: string; classCode: string; className: string; placeholder: boolean }

/** Pesquisa de contas do PGC por código ou designação, com sugestões (combobox acessível). */
export function AccountSearch({ version, onPick, label = "Adicionar conta do PGC" }: { version?: string; onPick: (a: Account) => void; label?: string }) {
  const id = useId();
  const [q, setQ] = useState("");
  const [items, setItems] = useState<Account[]>([]);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(0);
  const [error, setError] = useState(false);
  const timer = useRef<ReturnType<typeof setTimeout>>(undefined);

  useEffect(() => {
    clearTimeout(timer.current);
    if (!q.trim()) { setItems([]); return; }
    timer.current = setTimeout(async () => {
      try {
        const r = await get<Account[]>(`/pgc/accounts?q=${encodeURIComponent(q)}${version ? `&version=${encodeURIComponent(version)}` : ""}`);
        setItems(r); setActive(0); setOpen(true); setError(false);
      } catch { setError(true); }
    }, 180);
  }, [q, version]);

  const pick = (a: Account) => { onPick(a); setQ(""); setItems([]); setOpen(false); };

  return (
    <div className="field" style={{ position: "relative" }}>
      <label htmlFor={id}>{label}</label>
      <span className="money-wrap">
        <span className="cur" style={{ left: 12 }} aria-hidden="true"><Icon name="search" /></span>
        <input id={id} className="input" style={{ paddingLeft: 40 }} value={q} placeholder="Código ou designação (ex.: 32 ou fornecedores)"
          role="combobox" aria-expanded={open && items.length > 0} aria-controls={`${id}-list`} aria-autocomplete="list"
          aria-activedescendant={open && items[active] ? `${id}-o${active}` : undefined}
          onChange={(e) => setQ(e.target.value)} onFocus={() => items.length && setOpen(true)} onBlur={() => setTimeout(() => setOpen(false), 150)}
          onKeyDown={(e) => {
            if (e.key === "ArrowDown") { e.preventDefault(); setActive((a) => Math.min(a + 1, items.length - 1)); }
            if (e.key === "ArrowUp") { e.preventDefault(); setActive((a) => Math.max(a - 1, 0)); }
            if (e.key === "Enter" && items[active]) { e.preventDefault(); pick(items[active]); }
            if (e.key === "Escape") setOpen(false);
          }} />
      </span>
      {error && <span className="err">Não foi possível pesquisar contas. Tenta de novo.</span>}
      {open && q && (
        <ul id={`${id}-list`} role="listbox" className="card" style={{ position: "absolute", top: "100%", left: 0, right: 0, zIndex: 20, padding: 6, marginTop: 4, listStyle: "none", boxShadow: "var(--shadow-2)" }}>
          {items.length === 0 && <li className="small muted" style={{ padding: 10 }}>Nenhuma conta corresponde a «{q}».</li>}
          {items.map((a, i) => (
            <li key={a.code} id={`${id}-o${i}`} role="option" aria-selected={i === active}
              className={["task", i === active && "cur"].filter(Boolean).join(" ")} style={{ cursor: "pointer" }}
              onMouseDown={(e) => { e.preventDefault(); pick(a); }}>
              <span className="acct" style={{ minWidth: 56 }}>{a.code}</span>
              <span style={{ flex: 1 }}>{a.name}{a.placeholder && <span className="tbc">subconta a confirmar</span>}</span>
              <span className="small muted">Classe {a.classCode} · {a.className}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
