/** Línguas da interface. A escolha vive num cookie (sem prefixo no URL); sem cookie, segue o browser e cai no português. */
export const LOCALES = ["pt", "en", "fr"] as const;
export type Locale = (typeof LOCALES)[number];
export const DEFAULT_LOCALE: Locale = "pt";
export const LOCALE_COOKIE = "lp_lang";

/** Nome de cada língua na própria língua (o seletor mostra sempre assim). */
export const LOCALE_NAME: Record<Locale, string> = { pt: "Português", en: "English", fr: "Français" };
/** Atributo lang do <html> e locale do Intl. */
export const HTML_LANG: Record<Locale, string> = { pt: "pt-PT", en: "en-GB", fr: "fr-FR" };

export function isLocale(v: unknown): v is Locale {
  return typeof v === "string" && (LOCALES as readonly string[]).includes(v);
}

/** Primeira língua suportada do cabeçalho Accept-Language (ex.: «fr-FR,fr;q=0.9,en;q=0.8» → fr). */
export function fromAcceptLanguage(header: string | null | undefined): Locale {
  for (const part of (header ?? "").split(",")) {
    const base = part.split(";")[0].trim().slice(0, 2).toLowerCase();
    if (isLocale(base)) return base;
  }
  return DEFAULT_LOCALE;
}

/** No browser, fora de componentes (ex.: cliente da API): a língua com que o servidor desenhou a página (<html lang>). */
export function cookieLocale(): Locale {
  if (typeof document === "undefined") return DEFAULT_LOCALE;
  const lang = document.documentElement.lang.slice(0, 2);
  return isLocale(lang) ? lang : DEFAULT_LOCALE;
}
