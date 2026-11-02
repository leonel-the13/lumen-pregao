import type { Metadata, Viewport } from "next";
import { Archivo, Instrument_Serif, JetBrains_Mono } from "next/font/google";
import { cookies } from "next/headers";
import type { ReactNode } from "react";
import { ToastProvider } from "@/components/ui/Toast";
import { THEME_COOKIE, toAttr } from "@/lib/theme";
import "@/styles/tokens.css";

const archivo = Archivo({ subsets: ["latin"], axes: ["wdth"], variable: "--font-archivo", display: "swap" });
const instrument = Instrument_Serif({ subsets: ["latin"], weight: "400", style: "italic", variable: "--font-instrument", display: "swap" });
const jetbrains = JetBrains_Mono({ subsets: ["latin"], variable: "--font-jetbrains", display: "swap" });

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "https://pregao.lumenao.tech";
const DESCRIPTION = "Trilhas, salas práticas, exames e a Maratona de casos para os estudantes do ISPTEC. Iniciativa da Associação de Estudantes.";

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: { default: "Lúmen Pregão · Aprender é saber aplicar", template: "%s · Lúmen Pregão" },
  description: DESCRIPTION,
  applicationName: "Lúmen Pregão",
  keywords: ["ISPTEC", "contabilidade", "PGC", "IVA", "análise financeira", "exames", "Maratona de casos", "Associação de Estudantes"],
  openGraph: {
    type: "website",
    locale: "pt_PT",
    url: "/",
    siteName: "Lúmen Pregão",
    title: "Lúmen Pregão · Aprender é saber aplicar",
    description: DESCRIPTION,
  },
  twitter: { card: "summary_large_image", title: "Lúmen Pregão · Aprender é saber aplicar", description: DESCRIPTION },
  appleWebApp: { title: "Lúmen Pregão", statusBarStyle: "black-translucent" },
  formatDetection: { telephone: false },
};

export const viewport: Viewport = { themeColor: "#0A0A0A", width: "device-width", initialScale: 1 };

export default async function RootLayout({ children }: { children: ReactNode }) {
  const theme = toAttr((await cookies()).get(THEME_COOKIE)?.value);
  return (
    <html lang="pt-PT" data-theme={theme} className={`${archivo.variable} ${instrument.variable} ${jetbrains.variable}`}>
      <body className="lp">
        <a className="skip-link" href="#conteudo">Saltar para o conteúdo</a>
        <ToastProvider>{children}</ToastProvider>
      </body>
    </html>
  );
}
