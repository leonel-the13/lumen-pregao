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
import { getI18n } from "@/lib/i18n/server";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("compete.rankings.title") };
}

const SCOPES = ["GLOBAL", "COURSE", "YEAR", "SEASON", "TEAMS"] as const;

function Move({ m }: { m?: number }) {
  if (m === undefined || m === null) return <span className="small muted">—</span>;
  if (m === 0) return <span className="small muted">—</span>;
  return m > 0 ? <span className="move-up">▲{m}</span> : <span className="move-dn">▼{Math.abs(m)}</span>;
}

export default async function Rankings({ searchParams }: { searchParams: Promise<{ scope?: string; cursor?: string }> }) {
  const sp = await searchParams;
  const { t } = await getI18n();
  const scope = SCOPES.some((s) => s === sp.scope) ? sp.scope! : "GLOBAL";
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
      <SetCrumbs items={[{ label: t("compete.list.group") }, { label: t("compete.rankings.title") }]} />
      <div className="stack" style={{ gap: 6 }}><div className="label">{page?.label ?? t("compete.rankings.title")}</div><h1 className="h1">{t("compete.rankings.title")}</h1></div>
      <Tabs label={t("compete.rankings.scopeLabel")} active={scope} items={SCOPES.map((k) => ({ key: k, label: t(`compete.rankings.scopes.${k}`), href: `/rankings?scope=${k}` }))} />
      {error ? <ErrorState title={t("compete.rankings.failed")} code={`RNK-${error.status || 503}`} body={error.message}
        action={<Link className="btn out" href={`/rankings?scope=${scope}`}>{t("pages.error.retry")}</Link>} /> : page && (
        <div className="ex-grid">
          <div className="stack" style={{ gap: 16 }}>
            {podium.length > 0 && (
              <div className="grid g3">
                {podium.map((e) => (
                  <Card key={e.position} lit={e.position === 1} className="row" style={{ gap: 12 }}>
                    <span className="mono h2 accent">#{e.position}</span><Avatar initials={e.initials} />
                    <div style={{ minWidth: 0 }}><b>{e.name}</b><div className="small muted">{[e.course, e.year && t("compete.team.yearShort", { n: e.year })].filter(Boolean).join(" · ")}</div></div>
                    <div className="spacer" /><b className="mono">{formatPoints(e.points)}</b>
                  </Card>
                ))}
              </div>
            )}
            {page.entries.length === 0 ? <EmptyState title={t("compete.rankings.empty")} body={t("compete.rankings.emptyBody")} /> : (
              <div className="tbl-wrap">
                <table className="tbl">
                  <caption className="sr-only">{t("compete.rankings.caption", { label: page.label })}</caption>
                  <thead><tr><th>#</th><th>{scope === "TEAMS" ? t("compete.rankings.team") : t("compete.rankings.student")}</th><th className="num">{t("compete.rankings.points")}</th><th className="num">{t("compete.rankings.week")}</th></tr></thead>
                  <tbody>
                    {page.entries.map((e) => (
                      <tr key={e.position} className={e.me ? "me" : undefined}>
                        <td className="mono">{e.position}</td>
                        <td><span className="row" style={{ gap: 10 }}><Avatar initials={e.initials} />
                          <span>{e.portfolioHandle ? <Link href={`/p/${e.portfolioHandle}`}>{e.name}</Link> : <b>{e.name}</b>}
                            <div className="small muted">{[e.course, e.year && t("compete.team.yearShort", { n: e.year })].filter(Boolean).join(" · ")}</div></span></span></td>
                        <td className="num">{formatPoints(e.points)}</td>
                        <td className="num"><Move m={e.movement} /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <div className="row">
              <span className="small muted">{t("compete.rankings.total", { n: page.total })}</span>
              <div className="spacer" />
              {sp.cursor && <Link className="btn sm out" href={`/rankings?scope=${scope}`}>{t("compete.rankings.first")}</Link>}
              {page.nextCursor && <Link className="btn sm out" href={`/rankings?scope=${scope}&cursor=${page.nextCursor}`}>{t("common.next")}</Link>}
            </div>
          </div>
          <div className="stack" style={{ gap: 16 }}>
            {scope !== "TEAMS" && (
              <Card lit className="stack">
                <div className="label">{t("compete.rankings.yourPosition")}</div>
                <div className="kpi"><div className="v">{page.me.position ? `#${page.me.position}` : "—"}</div></div>
                <div className="small muted">{formatPoints(page.me.points)} pts{page.me.movement ? ` · ${page.me.movement > 0 ? "▲" : "▼"} ${Math.abs(page.me.movement)}${t("compete.rankings.thisWeek")}` : ""}</div>
                {page.me.gapToNext !== undefined && page.me.gapToNext !== null && page.me.position && page.me.position > 1 &&
                  <div className="small">{t("compete.rankings.gap", { n: page.me.gapToNext, pos: page.me.position - 1 })}</div>}
              </Card>
            )}
            {rules && (
              <Card>
                <CardHeader title={t("compete.rankings.rules")} />
                <ul className="plain stack small">{rules.rules.map((r) => <li key={r.action} className="row" style={{ justifyContent: "space-between" }}><span>{r.action}</span><span className="mono">{r.points}</span></li>)}</ul>
                <p className="small muted" style={{ marginTop: 10 }}>{t("compete.rankings.illustrative")} <Tbc />{t("compete.rankings.finalRules")}</p>
              </Card>
            )}
            <PrivacyCard me={me} />
          </div>
        </div>
      )}
    </>
  );
}
