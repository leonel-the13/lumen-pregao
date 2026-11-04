import type { Metadata, Viewport } from "next";
import { Archivo, Instrument_Serif, JetBrains_Mono } from "next/font/google";
import { cookies } from "next/headers";
import type { ReactNode } from "react";
import { ToastProvider } from "@/components/ui/Toast";
import { HTML_LANG } from "@/lib/i18n/config";
import { I18nProvider } from "@/lib/i18n/client";
import { getI18n } from "@/lib/i18n/server";
import { THEME_COOKIE, toAttr } from "@/lib/theme";
import "@/styles/tokens.css";

const archivo = Archivo({ subsets: ["latin"], axes: ["wdth"], variable: "--font-archivo", display: "swap" });
const instrument = Instrument_Serif({ subsets: ["latin"], weight: "400", style: "italic", variable: "--font-instrument", display: "swap" });
const jetbrains = JetBrains_Mono({ subsets: ["latin"], variable: "--font-jetbrains", display: "swap" });

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "https://lab.lumenao.tech";

export async function generateMetadata(): Promise<Metadata> {
  const { t, locale } = await getI18n();
  const title = t("pages.meta.title");
  const description = t("pages.meta.description");
  return {
    metadataBase: new URL(SITE_URL),
    title: { default: title, template: "%s · Lúmen Lab" },
    description,
    applicationName: "Lúmen Lab",
    keywords: ["ISPTEC", "contabilidade", "PGC", "IVA", "análise financeira", "exames", "Maratona de casos", "Associação de Estudantes"],
    openGraph: {
      type: "website",
      locale: HTML_LANG[locale].replace("-", "_"),
      url: "/",
      siteName: "Lúmen Lab",
      title,
      description,
    },
    twitter: { card: "summary_large_image", title, description },
    appleWebApp: { title: "Lúmen Lab", statusBarStyle: "black-translucent" },
    formatDetection: { telephone: false },
  };
}

export const viewport: Viewport = { themeColor: "#0A0A0A", width: "device-width", initialScale: 1 };

export default async function RootLayout({ children }: { children: ReactNode }) {
  const theme = toAttr((await cookies()).get(THEME_COOKIE)?.value);
  const { locale, t } = await getI18n();
  return (
    <html lang={HTML_LANG[locale]} data-theme={theme} className={`${archivo.variable} ${instrument.variable} ${jetbrains.variable}`}>
      <body className="lp">
        <a className="skip-link" href="#conteudo">{t("common.skipToContent")}</a>
        <I18nProvider locale={locale}><ToastProvider>{children}</ToastProvider></I18nProvider>
      </body>
    </html>
  );
}
