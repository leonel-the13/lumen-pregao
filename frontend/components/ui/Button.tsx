import Link from "next/link";
import type { ButtonHTMLAttributes, ReactNode } from "react";
import { Icon } from "./Icon";

type Variant = "pri" | "sec" | "out" | "ghost" | "danger";

interface Common {
  variant?: Variant;
  size?: "md" | "sm";
  icon?: string;
  iconRight?: string;
  loading?: boolean;
  children?: ReactNode;
  className?: string;
}

function cls(variant: Variant, size: string, extra?: string, iconOnly?: boolean) {
  return ["btn", variant !== "sec" && variant, size === "sm" && "sm", iconOnly && "icon-btn", extra].filter(Boolean).join(" ");
}

/**
 * Botão do design: primário (1 por área), secundário, contorno, fantasma, destrutivo (pede confirmação).
 * Por defeito é `type="button"`: dentro de um <form> não o envia sem querer. Para enviar, passar `type="submit"`.
 */
export function Button({ variant = "sec", size = "md", icon, iconRight, loading, children, className, disabled, type = "button", ...rest }:
  Common & ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button type={type} className={cls(variant, size, className, !children)} disabled={disabled || loading} aria-busy={loading || undefined} {...rest}>
      {loading ? <Icon name="loader" className="icon spin" /> : icon && <Icon name={icon} />}
      {children}
      {iconRight && <Icon name={iconRight} />}
    </button>
  );
}

export function LinkButton({ href, variant = "sec", size = "md", icon, iconRight, children, className, ...rest }:
  Common & { href: string; "aria-label"?: string; prefetch?: boolean }) {
  return (
    <Link href={href} className={cls(variant, size, className, !children)} {...rest}>
      {icon && <Icon name={icon} />}
      {children}
      {iconRight && <Icon name={iconRight} />}
    </Link>
  );
}
