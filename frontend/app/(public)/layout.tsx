import Link from "next/link";
import { cookies } from "next/headers";
import type { ReactNode } from "react";
import { Brand } from "@/components/layout/Brand";
import { Footer } from "@/components/layout/Footer";
import { LanguageSwitcher } from "@/components/layout/LanguageSwitcher";
import { ThemeToggle } from "@/components/layout/ThemeToggle";
import { getI18n } from "@/lib/i18n/server";
import { THEME_COOKIE, toAttr } from "@/lib/theme";

export default async function PublicLayout({ children }: { children: ReactNode }) {
  const theme = toAttr((await cookies()).get(THEME_COOKIE)?.value);
  const { t } = await getI18n();
  return (
    <div style={{ minHeight: "100vh", display: "flex", flexDirection: "column" }}>
      <header className="topbar public-top" style={{ padding: "14px clamp(18px,4vw,56px)" }}>
        <Brand />
        <nav aria-label={t("landing.nav.sections")} className="row public-nav" style={{ gap: 4 }}>
          <Link className="btn ghost sm" href="/#como-funciona">{t("landing.nav.how")}</Link>
          <Link className="btn ghost sm" href="/#trilhas">{t("landing.nav.tracks")}</Link>
          <Link className="btn ghost sm" href="/#maratona">{t("landing.nav.marathon")}</Link>
          <Link className="btn ghost sm" href="/regras">{t("landing.nav.rules")}</Link>
        </nav>
        <div className="spacer" />
        <div className="row" style={{ gap: 8 }}>
          <span className="public-lang"><LanguageSwitcher /></span>
          <ThemeToggle initial={theme} persist={false} />
          <Link className="btn ghost" href="/entrar">{t("landing.nav.signIn")}</Link>
          <Link className="btn pri" href="/registo">{t("landing.nav.signUp")}</Link>
        </div>
      </header>
      <main id="conteudo" tabIndex={-1} style={{ flex: 1 }}>{children}</main>
      <Footer />
    </div>
  );
}
