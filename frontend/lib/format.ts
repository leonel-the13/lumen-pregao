/**
 * Formatação de valores (design A5 · Dados financeiros): Kz depois do valor, espaço fino nos milhares,
 * vírgula decimal, números tabulares (a classe .mono/.num aplica a fonte).
 */
import { HTML_LANG, type Locale } from "./i18n/config";

export const THIN = " ";
const ZONE = "Africa/Luanda";

function group(intPart: string): string {
  return intPart.replace(/\B(?=(\d{3})+(?!\d))/g, THIN);
}

export function formatNumber(value: number | string | null | undefined, decimals = 2): string {
  if (value === null || value === undefined || value === "") return "—";
  const n = typeof value === "string" ? Number(value) : value;
  if (!Number.isFinite(n)) return "—";
  const negative = n < 0;
  const [i, d] = Math.abs(n).toFixed(decimals).split(".");
  return (negative ? "− " : "") + group(i) + (decimals > 0 ? "," + d : "");
}

/** 1 250 000,00 Kz */
export function formatKz(value: number | string | null | undefined, withUnit = true): string {
  const s = formatNumber(value, 2);
  return withUnit && s !== "—" ? `${s} Kz` : s;
}

/** 14,0 % */
export function formatPercent(value: number | null | undefined, decimals = 1): string {
  if (value === null || value === undefined) return "—";
  return `${formatNumber(value, decimals)} %`;
}

/** «43.1 Depósitos à ordem» — código antes da designação */
export function formatAccount(code: string, name?: string): string {
  return name ? `${code} ${name}` : code;
}

export function formatPoints(n: number | null | undefined): string {
  return n === null || n === undefined ? "—" : formatNumber(n, 0);
}

/** Converte texto escrito pelo estudante («500 000,00») em número; vazio → null. */
export function parseAmount(text: string): number | null {
  const clean = text.replace(/[\s  ]/g, "").replace("−", "-");
  if (clean === "") return null;
  const normalized = clean.includes(",") ? clean.replace(/\./g, "").replace(",", ".") : clean;
  if (!/^-?\d+(\.\d+)?$/.test(normalized)) return NaN;
  return Number(normalized);
}

export function formatDate(iso: string | Date, opts: Intl.DateTimeFormatOptions = { day: "2-digit", month: "2-digit" }, locale: Locale = "pt") {
  return new Intl.DateTimeFormat(HTML_LANG[locale], { timeZone: ZONE, ...opts }).format(new Date(iso));
}

export function formatTime(iso: string | Date, locale: Locale = "pt") {
  return new Intl.DateTimeFormat(HTML_LANG[locale], { timeZone: ZONE, hour: "2-digit", minute: "2-digit", second: "2-digit" }).format(new Date(iso));
}

export function formatDateTime(iso: string | Date, locale: Locale = "pt") {
  return new Intl.DateTimeFormat(HTML_LANG[locale], {
    timeZone: ZONE, day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit",
  }).format(new Date(iso));
}

export function formatLongDate(iso: string | Date, locale: Locale = "pt") {
  const s = new Intl.DateTimeFormat(HTML_LANG[locale], { timeZone: ZONE, weekday: "long", day: "numeric", month: "long" }).format(new Date(iso));
  return s.charAt(0).toUpperCase() + s.slice(1);
}

/** 06d 04:12:30 · 01:42:10 */
export function formatDuration(ms: number, withDays = true): string {
  const total = Math.max(0, Math.floor(ms / 1000));
  const d = Math.floor(total / 86400);
  const h = Math.floor((total % 86400) / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  const p = (x: number) => String(x).padStart(2, "0");
  if (withDays && d > 0) return `${p(d)}d ${p(h)}:${p(m)}:${p(s)}`;
  return `${p(h + d * 24)}:${p(m)}:${p(s)}`;
}

export function formatGrade(g: number | string | null | undefined) {
  if (g === null || g === undefined) return "—";
  return `${formatNumber(Number(g), Number(g) % 1 === 0 ? 0 : 1)}/20`;
}

export const DIFFICULTY_LABEL: Record<number, string> = { 1: "Iniciação", 2: "Intermédio", 3: "Avançado", 4: "Avançado" };

/** «há 5 min», «há 3 h», «ontem», ou a data (Africa/Luanda), na língua da interface. */
export function formatRelative(iso: string | Date, now: number = Date.now(), locale: Locale = "pt"): string {
  const t = typeof iso === "string" ? new Date(iso).getTime() : iso.getTime();
  const min = Math.round((now - t) / 60_000);
  const rtf = new Intl.RelativeTimeFormat(HTML_LANG[locale], { numeric: "auto", style: "short" });
  if (min < 1) return rtf.format(0, "second");
  if (min < 60) return rtf.format(-min, "minute");
  const h = Math.round(min / 60);
  if (h < 24) return rtf.format(-h, "hour");
  if (h < 48) return rtf.format(-1, "day");
  return formatDate(iso, { day: "2-digit", month: "short" }, locale);
}
