import { Tbc } from "@/components/ui/Badge";
import { serverFetch } from "@/lib/api-server";

export const metadata = { title: "Regras e fontes" };

interface Rules { rules: { action: string; points: string }[]; badges: { code: string; title: string; description: string; points: number }[]; note: string }

export default async function Regras() {
  const r = await serverFetch<Rules>("/scoring-rules").catch(() => null);
  return (
    <article className="content stack" style={{ maxWidth: 860, margin: "0 auto", gap: 20 }}>
      <div className="label">Regras e fontes</div>
      <h1 className="h1">Regras de pontuação</h1>
      {r ? (
        <>
          <div className="tbl-wrap">
            <table className="tbl">
              <thead><tr><th>Acção</th><th className="num">Pontos</th></tr></thead>
              <tbody>{r.rules.map((x) => <tr key={x.action}><td>{x.action}</td><td className="num">{x.points}</td></tr>)}</tbody>
            </table>
          </div>
          <p className="small muted">{r.note} <Tbc /></p>
          <h2 className="h2">Conquistas</h2>
          <ul className="plain stack">{r.badges.map((b) => <li key={b.code}><b>{b.title}</b> · <span className="muted">{b.description}</span> · <span className="mono">+{b.points}</span></li>)}</ul>
        </>
      ) : <p className="muted">Não foi possível carregar as regras. Tenta de novo dentro de instantes.</p>}
      <h2 className="h2">Fontes normativas</h2>
      <p>Cada exercício indica a fonte, os artigos e a versão da norma. Enquanto um revisor não confirmar, aparecem como <Tbc />.
        Os exercícios são originais e todas as empresas são fictícias.</p>
    </article>
  );
}
