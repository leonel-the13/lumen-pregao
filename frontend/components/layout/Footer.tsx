import Link from "next/link";
import { getI18n } from "@/lib/i18n/server";
import { LanguageSwitcher } from "./LanguageSwitcher";

/** Rodapé institucional obrigatório em todas as páginas (spec §1.7). */
export async function Footer() {
  const { t } = await getI18n();
  return (
    <footer className="foot">
      <span>{t("shell.footer.initiative")}</span>
      <nav aria-label={t("shell.footer.info")} className="row" style={{ gap: 16 }}>
        <Link href="/regras">{t("shell.footer.rules")}</Link>
        <Link href="/termos">{t("shell.footer.terms")}</Link>
        <Link href="/privacidade">{t("shell.footer.privacy")}</Link>
        <LanguageSwitcher />
        <span>Lúmen Lab</span>
      </nav>
    </footer>
  );
}
