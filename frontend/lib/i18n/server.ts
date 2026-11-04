import "server-only";
import { cookies, headers } from "next/headers";
import { LOCALE_COOKIE, fromAcceptLanguage, isLocale, type Locale } from "./config";
import { MESSAGES } from "./index";
import { translator } from "./translate";

export async function getLocale(): Promise<Locale> {
  const c = (await cookies()).get(LOCALE_COOKIE)?.value;
  if (isLocale(c)) return c;
  return fromAcceptLanguage((await headers()).get("accept-language"));
}

/** Para componentes de servidor: `const { t, locale } = await getI18n();` */
export async function getI18n() {
  const locale = await getLocale();
  return { locale, t: translator(MESSAGES[locale]) };
}
