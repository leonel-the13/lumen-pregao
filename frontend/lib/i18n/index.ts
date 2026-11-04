import type { Locale } from "./config";
import { common } from "./messages/common";
import { shell } from "./messages/shell";
import { settings } from "./messages/settings";
import { landing } from "./messages/landing";
import { auth } from "./messages/auth";
import { pages } from "./messages/pages";
import { learn } from "./messages/learn";
import { task } from "./messages/task";
import { compete } from "./messages/compete";
import { exam } from "./messages/exam";
import { portfolio } from "./messages/portfolio";

function build(l: Locale) {
  return { common: common[l], shell: shell[l], settings: settings[l], landing: landing[l], auth: auth[l], pages: pages[l], learn: learn[l], task: task[l], compete: compete[l], exam: exam[l], portfolio: portfolio[l] };
}

export type Messages = ReturnType<typeof build>;
export const MESSAGES: Record<Locale, Messages> = { pt: build("pt"), en: build("en"), fr: build("fr") };
