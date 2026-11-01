import Link from "next/link";
import { cookies } from "next/headers";
import type { ReactNode } from "react";
import { Brand } from "@/components/layout/Brand";
import { Footer } from "@/components/layout/Footer";
import { ThemeToggle } from "@/components/layout/ThemeToggle";
import { THEME_COOKIE, toAttr } from "@/lib/theme";

export default async function PublicLayout({ children }: { children: ReactNode }) {
  const theme = toAttr((await cookies()).get(THEME_COOKIE)?.value);
  return (
    <div style={{ minHeight: "100vh", display: "flex", flexDirection: "column" }}>
      <header className="topbar public-top" style={{ padding: "14px clamp(18px,4vw,56px)" }}>
        <Brand />
        <nav aria-label="Secções" className="row public-nav" style={{ gap: 4 }}>
          <Link className="btn ghost sm" href="/#como-funciona">Como funciona</Link>
          <Link className="btn ghost sm" href="/#trilhas">Trilhas</Link>
          <Link className="btn ghost sm" href="/#maratona">A Maratona</Link>
          <Link className="btn ghost sm" href="/regras">Regras e fontes</Link>
        </nav>
        <div className="spacer" />
        <div className="row" style={{ gap: 8 }}>
          <ThemeToggle initial={theme} persist={false} />
          <Link className="btn ghost" href="/entrar">Entrar</Link>
          <Link className="btn pri" href="/registo">Criar conta</Link>
        </div>
      </header>
      <main id="conteudo" tabIndex={-1} style={{ flex: 1 }}>{children}</main>
      <Footer />
    </div>
  );
}
