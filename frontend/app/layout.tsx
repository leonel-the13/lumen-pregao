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

export const metadata: Metadata = {
  title: { default: "Lúmen Pregão", template: "%s · Lúmen Pregão" },
  description: "Laboratório de competências do DCSA: trilhas, salas práticas, exames e a Maratona de casos.",
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
