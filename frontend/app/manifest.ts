import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "Lúmen Pregão",
    short_name: "Lúmen Pregão",
    description: "Laboratório de competências para os estudantes do ISPTEC: trilhas, salas práticas, exames e a Maratona de casos.",
    lang: "pt-PT",
    start_url: "/inicio",
    display: "standalone",
    background_color: "#0A0A0A",
    theme_color: "#0A0A0A",
    icons: [
      { src: "/icon-192.png", sizes: "192x192", type: "image/png" },
      { src: "/icon-512.png", sizes: "512x512", type: "image/png" },
      { src: "/icon-512.png", sizes: "512x512", type: "image/png", purpose: "maskable" },
    ],
  };
}
