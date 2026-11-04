"use client";
import { useRouter } from "next/navigation";
import { createContext, useContext, useMemo, type ReactNode } from "react";
import { LOCALE_COOKIE, type Locale } from "./config";
import { MESSAGES } from "./index";
import { translator, type T } from "./translate";

const Ctx = createContext<{ locale: Locale; t: T }>({ locale: "pt", t: translator(MESSAGES.pt) });

export function I18nProvider({ locale, children }: { locale: Locale; children: ReactNode }) {
  const value = useMemo(() => ({ locale, t: translator(MESSAGES[locale]) }), [locale]);
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

/** Para componentes de cliente: `const t = useT();` */
export function useT(): T {
  return useContext(Ctx).t;
}

export function useLocale(): Locale {
  return useContext(Ctx).locale;
}

/** Muda a língua: grava o cookie (1 ano) e volta a pedir a página ao servidor. */
export function useSetLocale() {
  const router = useRouter();
  return (l: Locale) => {
    document.cookie = `${LOCALE_COOKIE}=${l}; path=/; max-age=31536000; samesite=lax`;
    router.refresh();
  };
}
