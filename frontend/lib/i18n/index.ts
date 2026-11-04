import type { Locale } from "./config";
import { common } from "./messages/common";
import { shell } from "./messages/shell";
import { settings } from "./messages/settings";
import { landing } from "./messages/landing";
import { auth } from "./messages/auth";
import { pages } from "./messages/pages";

function build(l: Locale) {
  return { common: common[l], shell: shell[l], settings: settings[l], landing: landing[l], auth: auth[l], pages: pages[l] };
}

export type Messages = ReturnType<typeof build>;
export const MESSAGES: Record<Locale, Messages> = { pt: build("pt"), en: build("en"), fr: build("fr") };
