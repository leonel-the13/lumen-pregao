import type { HTMLAttributes, ReactNode } from "react";

export function Card({ lit, flat, className, children, ...rest }: HTMLAttributes<HTMLDivElement> & { lit?: boolean; flat?: boolean }) {
  return (
    <div className={["card", lit && "lit", flat && "flat", className].filter(Boolean).join(" ")} {...rest}>
      {children}
    </div>
  );
}

export function CardHeader({ title, children, as: Tag = "h2" }: { title: ReactNode; children?: ReactNode; as?: "h2" | "h3" }) {
  return (
    <div className="card-h">
      <Tag className={Tag === "h2" ? "h2" : "h3"}>{title}</Tag>
      {children && <><div className="spacer" />{children}</>}
    </div>
  );
}

export function Kpi({ label, value, detail, accent }: { label: string; value: ReactNode; detail?: ReactNode; accent?: boolean }) {
  return (
    <div className="card kpi">
      <div className="label">{label}</div>
      <div className={["v", accent && "accent"].filter(Boolean).join(" ")}>{value}</div>
      {detail && <div className="d">{detail}</div>}
    </div>
  );
}
