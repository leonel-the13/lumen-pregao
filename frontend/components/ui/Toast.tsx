"use client";
import { createContext, useCallback, useContext, useState, type ReactNode } from "react";
import { Icon } from "./Icon";

type Tone = "pos" | "neg" | "info" | "acc";
interface ToastItem { id: number; tone: Tone; title: string; body?: string }

const Ctx = createContext<(t: Omit<ToastItem, "id">) => void>(() => {});

export function useToast() {
  return useContext(Ctx);
}

const ICON: Record<Tone, string> = { pos: "check", neg: "x", info: "info", acc: "spark" };

/** Notificações (região aria-live). Desaparecem ao fim de 5 s. */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([]);
  const push = useCallback((t: Omit<ToastItem, "id">) => {
    const id = Date.now() + Math.random();
    setItems((xs) => [...xs, { ...t, id }]);
    setTimeout(() => setItems((xs) => xs.filter((x) => x.id !== id)), 5000);
  }, []);
  return (
    <Ctx.Provider value={push}>
      {children}
      <div className="toast-region" role="status" aria-live="polite">
        {items.map((t) => (
          <div key={t.id} className="toast">
            <span className="ic" style={{ background: `var(--${t.tone}-soft)`, color: `var(--${t.tone === "acc" ? "accent" : t.tone})` }}>
              <Icon name={ICON[t.tone]} />
            </span>
            <div><b>{t.title}</b>{t.body && <div className="small muted">{t.body}</div>}</div>
          </div>
        ))}
      </div>
    </Ctx.Provider>
  );
}
