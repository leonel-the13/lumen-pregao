"use client";
import { createContext, useContext, useEffect, useState, type ReactNode } from "react";

export interface Crumb { label: string; href?: string }
const Ctx = createContext<{ items: Crumb[]; set: (c: Crumb[]) => void }>({ items: [], set: () => {} });

export function CrumbsProvider({ children }: { children: ReactNode }) {
  const [items, set] = useState<Crumb[]>([]);
  return <Ctx.Provider value={{ items, set }}>{children}</Ctx.Provider>;
}

export function useCrumbs() {
  return useContext(Ctx).items;
}

/** Cada página declara a sua localização; a barra superior mostra-a. */
export function SetCrumbs({ items }: { items: Crumb[] }) {
  const { set } = useContext(Ctx);
  const key = JSON.stringify(items);
  useEffect(() => { set(JSON.parse(key)); }, [key, set]);
  return null;
}
