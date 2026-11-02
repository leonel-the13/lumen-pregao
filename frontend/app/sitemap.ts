import type { MetadataRoute } from "next";

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "https://lab.lumenao.tech";

export default function sitemap(): MetadataRoute.Sitemap {
  return ["", "/registo", "/entrar", "/regras", "/termos", "/privacidade"].map((path) => ({
    url: `${SITE_URL}${path}`,
    changeFrequency: path === "" ? "weekly" : "monthly",
    priority: path === "" ? 1 : 0.5,
  }));
}
