/** IP real do visitante segundo a plataforma: Cloudflare (CF-Connecting-IP) ou Vercel (x-real-ip / x-forwarded-for). */
export function clientIp(h: Headers): string | null {
  return h.get("cf-connecting-ip") ?? h.get("x-real-ip") ?? h.get("x-forwarded-for")?.split(",")[0]?.trim() ?? null;
}
