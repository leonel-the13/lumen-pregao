import type { ReactNode } from "react";
import { Footer } from "@/components/layout/Footer";
import { GlobalWatchers } from "@/components/layout/GlobalWatchers";
import { ThemeSync } from "@/components/layout/ThemeSync";
import { requireMe } from "@/lib/session";

/** Modo concentração: sem navegação lateral (spec §4.6). */
export default async function FocoLayout({ children }: { children: ReactNode }) {
  const me = await requireMe("/exames");
  return (
    <div style={{ minHeight: "100vh", display: "flex", flexDirection: "column" }}>
      <ThemeSync theme={me.theme} />
      <GlobalWatchers />
      <main id="conteudo" tabIndex={-1} style={{ flex: 1 }}>{children}</main>
      <Footer />
    </div>
  );
}
