import type { ReactNode } from "react";
import { Icon } from "./Icon";

const ICON = { pos: "check", neg: "warn", info: "info", acc: "spark", neutral: "info" } as const;

/** Alerta com ícone + título + o que fazer. role=alert só para erros. */
export function Alert({ tone = "neutral", title, children, action, icon }:
  { tone?: keyof typeof ICON; title?: ReactNode; children?: ReactNode; action?: ReactNode; icon?: string }) {
  return (
    <div className={["alert", tone !== "neutral" && tone].filter(Boolean).join(" ")} role={tone === "neg" ? "alert" : undefined}>
      <Icon name={icon ?? ICON[tone]} />
      <div style={{ flex: 1 }}>
        {title && <b>{title}</b>}
        {children && <div>{children}</div>}
      </div>
      {action}
    </div>
  );
}
