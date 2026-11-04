"use client";
import { Icon } from "@/components/ui/Icon";
import { LOCALES, LOCALE_NAME, isLocale } from "@/lib/i18n/config";
import { useLocale, useSetLocale, useT } from "@/lib/i18n/client";

/** Seletor de língua compacto (rodapé e barra pública). Cada língua aparece no próprio nome. */
export function LanguageSwitcher() {
  const t = useT();
  const locale = useLocale();
  const setLocale = useSetLocale();
  return (
    <label className="lang-switch">
      <Icon name="globe" />
      <span className="sr-only">{t("shell.footer.language")}</span>
      <select value={locale} onChange={(e) => { if (isLocale(e.target.value)) setLocale(e.target.value); }}>
        {LOCALES.map((l) => <option key={l} value={l} lang={l}>{LOCALE_NAME[l]}</option>)}
      </select>
    </label>
  );
}
