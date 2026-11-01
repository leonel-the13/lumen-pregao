import Link from "next/link";
import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { PrivacyCard } from "@/components/settings/PrivacyCard";
import { Avatar } from "@/components/ui/Avatar";
import { Card, CardHeader } from "@/components/ui/Card";
import { Tabs } from "@/components/ui/Tabs";
import { EmptyState, ErrorState } from "@/components/ui/States";
import { Tbc } from "@/components/ui/Badge";
import { serverFetch, serverFetchOrLogin } from "@/lib/api-server";
import { ApiError } from "@/lib/errors";
import { formatPoints } from "@/lib/format";
import type { Me, RankingPage } from "@/lib/types";

export const metadata = { title: "Rankings" };

const SCOPES = [["GLOBAL", "Geral"], ["COURSE", "Por curso"], ["YEAR", "Por ano"], ["SEASON", "Por época"], ["TEAMS", "Equipas"]] as const;

function Move({ m }: { m?: number }) {
  if (m === undefined || m === null) return <span className="small muted">—</span>;
  if (m === 0) return <span className="small muted">—</span>;
  return m > 0 ? <span className="move-up">▲{m}</span> : <span className="move-dn">▼{Math.abs(m)}</span>;
}

export default async function Rankings({ searchParams }: { searchParams: Promise<{ scope?: string; cursor?: string }> }) {
  const sp = await searchParams;
  const scope = SCOPES.some(([s]) => s === sp.scope) ? sp.scope! : "GLOBAL";
  const me = await serverFetchOrLogin<Me>("/me", "/rankings");
  let page: RankingPage | null = null;
  let error: ApiError | null = null;
  try {
    page = await serverFetch<RankingPage>(`/rankings?scope=${scope}&limit=20${sp.cursor ? `&cursor=${sp.cursor}` : ""}`);
  } catch (e) {
    error = e as ApiError;
  }
  const rules = await serverFetch<{ rules: { action: string; points: string }[]; note: string }>("/scoring-rules").catch(() => null);
  const podium = page && !sp.cursor ? page.entries.slice(0, 3) : [];
  return (
    <>
      <SetCrumbs items={[{ label: "Competir" }, { label: "Rankings" }]} />
      <div className="stack" style={{ gap: 6 }}><div className="label">{page?.label ?? "Rankings"}</div><h1 className="h1">Rankings</h1></div>
      <Tabs label="Âmbito do ranking" active={scope} items={SCOPES.map(([k, l]) => ({ key: k, label: l, href: `/rankings?scope=${k}` }))} />
      {error ? <ErrorState title="Não foi possível carregar o ranking" code={`RNK-${error.status || 503}`} body={error.message}
        action={<Link className="btn out" href={`/rankings?scope=${scope}`}>Tentar novamente</Link>} /> : page && (
        <div className="ex-grid">
          <div className="stack" style={{ gap: 16 }}>
            {podium.length > 0 && (
              <div className="grid g3">
                {podium.map((e) => (
                  <Card key={e.position} lit={e.position === 1} className="row" style={{ gap: 12 }}>
                    <span className="mono h2 accent">#{e.position}</span><Avatar initials={e.initials} />
                    <div style={{ minWidth: 0 }}><b>{e.name}</b><div className="small muted">{[e.course, e.year && `${e.year}.º`].filter(Boolean).join(" · ")}</div></div>
                    <div className="spacer" /><b className="mono">{formatPoints(e.points)}</b>
                  </Card>
                ))}
              </div>
            )}
            {page.entries.length === 0 ? <EmptyState title="Ainda não há pontos neste ranking" body="Resolve tarefas para aparecer aqui." /> : (
              <div className="tbl-wrap">
                <table className="tbl">
                  <caption className="sr-only">Classificação · {page.label}</caption>
                  <thead><tr><th>#</th><th>{scope === "TEAMS" ? "Equipa" : "Estudante"}</th><th className="num">Pontos</th><th className="num">Semana</th></tr></thead>
                  <tbody>
                    {page.entries.map((e) => (
                      <tr key={e.position} className={e.me ? "me" : undefined}>
                        <td className="mono">{e.position}</td>
                        <td><span className="row" style={{ gap: 10 }}><Avatar initials={e.initials} />
                          <span>{e.portfolioHandle ? <Link href={`/p/${e.portfolioHandle}`}>{e.name}</Link> : <b>{e.name}</b>}
                            <div className="small muted">{[e.course, e.year && `${e.year}.º`].filter(Boolean).join(" · ")}</div></span></span></td>
                        <td className="num">{formatPoints(e.points)}</td>
                        <td className="num"><Move m={e.movement} /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <div className="row">
              <span className="small muted">{page.total} no total · ranking lido da projecção (reconstruível a partir dos pontos persistidos)</span>
              <div className="spacer" />
              {sp.cursor && <Link className="btn sm out" href={`/rankings?scope=${scope}`}>Início</Link>}
              {page.nextCursor && <Link className="btn sm out" href={`/rankings?scope=${scope}&cursor=${page.nextCursor}`}>Seguinte</Link>}
            </div>
          </div>
          <div className="stack" style={{ gap: 16 }}>
            {scope !== "TEAMS" && (
              <Card lit className="stack">
                <div className="label">A tua posição</div>
                <div className="kpi"><div className="v">{page.me.position ? `#${page.me.position}` : "—"}</div></div>
                <div className="small muted">{formatPoints(page.me.points)} pts{page.me.movement ? ` · ${page.me.movement > 0 ? "▲" : "▼"} ${Math.abs(page.me.movement)} esta semana` : ""}</div>
                {page.me.gapToNext !== undefined && page.me.gapToNext !== null && page.me.position && page.me.position > 1 &&
                  <div className="small">Faltam <b className="mono">{page.me.gapToNext}</b> pts para #{page.me.position - 1}</div>}
              </Card>
            )}
            {rules && (
              <Card>
                <CardHeader title="Regras de pontuação" />
                <ul className="plain stack small">{rules.rules.map((r) => <li key={r.action} className="row" style={{ justifyContent: "space-between" }}><span>{r.action}</span><span className="mono">{r.points}</span></li>)}</ul>
                <p className="small muted" style={{ marginTop: 10 }}>Valores ilustrativos <Tbc /> · regras finais publicadas antes da época.</p>
              </Card>
            )}
            <PrivacyCard me={me} />
          </div>
        </div>
      )}
    </>
  );
}
