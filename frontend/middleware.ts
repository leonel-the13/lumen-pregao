import { NextResponse, type NextRequest } from "next/server";
import { clientIp } from "@/lib/client-ip";

const PROTECTED = ["/inicio", "/trilhas", "/salas", "/simulacoes", "/exames", "/competicoes", "/rankings", "/portefolio",
  "/definicoes", "/autoria", "/juri", "/admin"];

/** Conveniência de navegação: sem cookie de sessão, vai para /entrar. A segurança real está no backend. */
export function middleware(req: NextRequest) {
  const { pathname, search } = req.nextUrl;
  if (PROTECTED.some((p) => pathname === p || pathname.startsWith(p + "/")) && !req.cookies.has("LP_SESSION")) {
    const url = req.nextUrl.clone();
    url.pathname = "/entrar";
    url.search = `?next=${encodeURIComponent(pathname + search)}`;
    return NextResponse.redirect(url);
  }
  const headers = new Headers(req.headers);
  headers.set("x-pathname", pathname + search);
  // IP real (Cloudflare ou Vercel) para rate limiting e auditoria; o backend só confia com o segredo partilhado.
  headers.delete("x-client-ip");
  headers.delete("x-proxy-secret");
  const ip = clientIp(req.headers);
  if (ip && process.env.PROXY_SECRET) {
    headers.set("x-client-ip", ip);
    headers.set("x-proxy-secret", process.env.PROXY_SECRET);
  }
  return NextResponse.next({ request: { headers } });
}

export const config = {
  matcher: ["/((?!_next|favicon.ico|icon.svg|apple-icon.png|opengraph-image|twitter-image|manifest.webmanifest|robots.txt|sitemap.xml|icon-|img/).*)"],
};
