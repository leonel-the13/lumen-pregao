import type { Segment } from "@/lib/types";

/** Enunciado com os valores próprios da tentativa destacados; constantes por confirmar levam a marca. */
export function StatementText({ segments, fallback }: { segments?: Segment[]; fallback?: string }) {
  if (!segments?.length) return <p>{fallback}</p>;
  return (
    <p style={{ lineHeight: 1.7 }}>
      {segments.map((s, i) =>
        s.param ? (
          <span key={i}>
            <span className="param">{s.text}</span>
            {s.unconfirmed && <span className="tbc">parâmetro · confirmar com o revisor</span>}
          </span>
        ) : (
          <span key={i}>{s.text}</span>
        ),
      )}
    </p>
  );
}
