import type { ReactNode } from "react";

export function AuthCard({ step, title, children }: { step?: string; title: ReactNode; children: ReactNode }) {
  return (
    <div style={{ display: "grid", placeItems: "center", padding: "48px 16px" }}>
      <div className="card stack" style={{ width: "min(440px,100%)", gap: 18, padding: 28 }}>
        {step && <div className="label">{step}</div>}
        <h1 className="h1">{title}</h1>
        {children}
      </div>
    </div>
  );
}
