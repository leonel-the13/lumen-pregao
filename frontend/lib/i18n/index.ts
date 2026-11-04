import type { Locale } from "./config";
import { common } from "./messages/common";
import { shell } from "./messages/shell";
import { settings } from "./messages/settings";

function build(l: Locale) {
  return { common: common[l], shell: shell[l], settings: settings[l] };
}

export type Messages = ReturnType<typeof build>;
export const MESSAGES: Record<Locale, Messages> = { pt: build("pt"), en: build("en"), fr: build("fr") };
