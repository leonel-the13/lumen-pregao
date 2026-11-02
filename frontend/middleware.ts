import { NextResponse, type NextRequest } from "next/server";
import { clientIp } from "@/lib/client-ip";

const PROTECTED = ["/inicio", "/trilhas", "/salas", "/simulacoes", "/exames", "/competicoes", "/rankings", "/portefolio",
  "/definicoes", "/autoria", "/juri", "/admin"];

/** Conveniência de navegação: sem cookie de sessão, vai para /entrar. A segurança real está no backend. */
/** Endereço antigo (antes de o produto se chamar Lúmen Lab): redirecciona para o novo, mantendo o caminho. */
const OLD_HOSTS = ["pregao.lumenao.tech"];
const CANONICAL_HOST = "lab.lumenao.tech";

export function middleware(req: NextRequest) {
  const { pathname, search } = req.nextUrl;
  const host = req.headers.get("host")?.split(":")[0];
  if (host && OLD_HOSTS.includes(host)) {
    return NextResponse.redirect(`https://${CANONICAL_HOST}${pathname}${search}`, 308);
  }
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
