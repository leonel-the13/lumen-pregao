import type { MetadataRoute } from "next";

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "https://lab.lumenao.tech";

/** Só as páginas públicas são indexadas; a área autenticada e a gestão ficam de fora. */
export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: ["/", "/registo", "/entrar", "/regras", "/termos", "/privacidade", "/p/"],
      disallow: ["/api/", "/inicio", "/trilhas", "/salas", "/simulacoes", "/exames", "/competicoes", "/rankings",
        "/portefolio", "/definicoes", "/autoria", "/juri", "/admin", "/verificar", "/redefinir", "/esqueci"],
    },
    sitemap: `${SITE_URL}/sitemap.xml`,
  };
}
