import { Avatar } from "@/components/ui/Avatar";
import { Badge } from "@/components/ui/Badge";
import { Card, CardHeader, Kpi } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import { formatDate, formatNumber } from "@/lib/format";
import type { Portfolio } from "@/lib/types";

/** Portefólio (próprio ou público). Nunca mostra email nem dados de contacto. */
export function PortfolioView({ p, actions }: { p: Portfolio; actions?: React.ReactNode }) {
  const earned = new Set(p.badges.map((b) => b.code));
  return (
    <div className="stack" style={{ gap: 24 }}>
      <div className="row" style={{ alignItems: "center", gap: 18 }}>
        <Avatar initials={p.initials} size={64} />
        <div className="stack" style={{ gap: 4 }}>
          <div className="label">{p.isPublic ? "Portefólio público" : "Portefólio privado"}</div>
          <h1 className="h1">{p.name}</h1>
          <div className="muted">{[p.course, p.year && `${p.year}.º ano`, "ISPTEC"].filter(Boolean).join(" · ")}</div>
        </div>
        <div className="spacer" />
        {actions}
      </div>
      <div className="grid g4">
        <Kpi label="Trilhas concluídas" value={p.tracksCompleted} />
        <Kpi label="Salas concluídas" value={p.roomsCompleted} />
        <Kpi label="Média em exames" value={p.examAverage === undefined || p.examAverage === null ? "—" : formatNumber(p.examAverage, 1)} />
        <Kpi label="Melhor posição" value={p.bestPosition ? `#${p.bestPosition}` : "—"} detail={p.bestPositionLabel} />
      </div>
      <div className="grid g2">
        <Card>
          <CardHeader title="Competências demonstradas" />
          {p.competencies.length === 0 ? <p className="small muted">Ainda sem salas concluídas.</p> : (
            <ul className="plain stack">
              {p.competencies.map((c) => (
                <li key={c.topic} className="row" style={{ justifyContent: "space-between" }}>
                  <span>{c.topic}</span><span className="small muted mono">{c.rooms} {c.rooms === 1 ? "sala" : "salas"}</span>
                </li>
              ))}
            </ul>
          )}
        </Card>
        <Card>
          <CardHeader title="Conquistas" />
          <div className="grid g3" style={{ gap: 10 }}>
            {p.badgeCatalog.map((b) => (
              <div key={b.code} className="card flat" style={{ padding: 12, opacity: earned.has(b.code) ? 1 : 0.45 }}>
                <div className="row" style={{ gap: 8 }}><Icon name={earned.has(b.code) ? "trophy" : "lock"} /><b className="small">{b.title}</b></div>
                <div className="small muted">{b.description}</div>
                <span className="sr-only">{earned.has(b.code) ? "Obtida" : "Por obter"}</span>
              </div>
            ))}
          </div>
        </Card>
      </div>
      <div className="grid g2">
        <Card>
          <CardHeader title="Certificados"><Badge>Fase posterior</Badge></CardHeader>
          <p className="small muted">Emissão sujeita a acordo institucional · enquanto não existir, o certificado indica «iniciativa da Associação de Estudantes».</p>
        </Card>
        <Card>
          <CardHeader title="Histórico" />
          {p.history.length === 0 ? <p className="small muted">Sem exames nem contests concluídos.</p> : (
            <ul className="plain stack">
              {p.history.map((h, i) => (
                <li key={i} className="row" style={{ gap: 12 }}>
                  <span className="mono small muted">{formatDate(h.at)}</span><span style={{ flex: 1 }}>{h.label}</span><b className="mono">{h.result}</b>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </div>
  );
}
