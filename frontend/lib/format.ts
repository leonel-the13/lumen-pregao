/**
 * Formatação de valores (design A5 · Dados financeiros): Kz depois do valor, espaço fino nos milhares,
 * vírgula decimal, números tabulares (a classe .mono/.num aplica a fonte).
 */
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

export function formatDate(iso: string | Date, opts: Intl.DateTimeFormatOptions = { day: "2-digit", month: "2-digit" }) {
  return new Intl.DateTimeFormat("pt-PT", { timeZone: ZONE, ...opts }).format(new Date(iso));
}

export function formatTime(iso: string | Date) {
  return new Intl.DateTimeFormat("pt-PT", { timeZone: ZONE, hour: "2-digit", minute: "2-digit", second: "2-digit" }).format(new Date(iso));
}

export function formatDateTime(iso: string | Date) {
  return new Intl.DateTimeFormat("pt-PT", {
    timeZone: ZONE, day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit",
  }).format(new Date(iso));
}

export function formatLongDate(iso: string | Date) {
  const s = new Intl.DateTimeFormat("pt-PT", { timeZone: ZONE, weekday: "long", day: "numeric", month: "long" }).format(new Date(iso));
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
