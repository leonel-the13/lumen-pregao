"use client";
import type { ReactNode } from "react";
import { useT } from "@/lib/i18n/client";
import { Icon } from "./Icon";

export type Tone = "neutral" | "acc" | "pos" | "neg" | "info";

/** Estado nunca só por cor: badges positivos/negativos levam ícone e texto. */
export function Badge({ tone = "neutral", icon, mono, children, title }: { tone?: Tone; icon?: string; mono?: boolean; children: ReactNode; title?: string }) {
  const auto = icon ?? (tone === "pos" ? "check" : tone === "neg" ? "x" : undefined);
  return (
    <span className={["badge", tone !== "neutral" && tone, mono && "mono"].filter(Boolean).join(" ")} title={title}>
      {auto && <Icon name={auto} />}
      {children}
    </span>
  );
}

/** Marca de referência ou valor por confirmar (regra editorial: nunca inventar normas). */
export function Tbc({ children }: { children?: ReactNode }) {
  const t = useT();
  return <span className="tbc">{children ?? t("pages.tbc")}</span>;
}
