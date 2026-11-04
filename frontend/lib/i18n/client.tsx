"use client";
import { useRouter } from "next/navigation";
import { createContext, useContext, useMemo, type ReactNode } from "react";
import { DEFAULT_LOCALE, LOCALE_COOKIE, type Locale } from "./config";
import type { Messages } from "./index";
import { translator, type T } from "./translate";

/**
 * Só o dicionário da língua activa chega ao browser: o layout raiz passa-o ao provider.
 * Os módulos fora de componentes (cliente da API) usam `clientT()`, que lê o mesmo dicionário.
 */
const Ctx = createContext<{ locale: Locale; t: T }>({ locale: DEFAULT_LOCALE, t: (k) => k });
let active: T = (k) => k;

export function I18nProvider({ locale, messages, children }: { locale: Locale; messages: Messages; children: ReactNode }) {
  const value = useMemo(() => ({ locale, t: translator(messages) }), [locale, messages]);
  active = value.t;
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

/** Para componentes de cliente: `const t = useT();` */
export function useT(): T {
  return useContext(Ctx).t;
}

export function useLocale(): Locale {
  return useContext(Ctx).locale;
}

/** Fora de componentes (ex.: mensagens de erro do cliente da API). */
export function clientT(): T {
  return active;
}

/** Muda a língua: grava o cookie (1 ano) e volta a pedir a página ao servidor. */
export function useSetLocale() {
  const router = useRouter();
  return (l: Locale) => {
    document.cookie = `${LOCALE_COOKIE}=${l}; path=/; max-age=31536000; samesite=lax`;
    router.refresh();
  };
}
