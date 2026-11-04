import { Tbc } from "@/components/ui/Badge";
import { serverFetch } from "@/lib/api-server";
import { getI18n } from "@/lib/i18n/server";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("pages.rules.title") };
}

interface Rules { rules: { action: string; points: string }[]; badges: { code: string; title: string; description: string; points: number }[]; note: string }

export default async function Regras() {
  const [r, { t, locale }] = await Promise.all([serverFetch<Rules>("/scoring-rules").catch(() => null), getI18n()]);
  return (
    <article className="content stack" style={{ maxWidth: 860, margin: "0 auto", gap: 20 }}>
      <div className="label">{t("pages.rules.title")}</div>
      <h1 className="h1">{t("pages.rules.heading")}</h1>
      {locale !== "pt" && <p className="small muted">{t("pages.rules.serverLanguage")}</p>}
      {r ? (
        <>
          <div className="tbl-wrap">
            <table className="tbl">
              <thead><tr><th>{t("pages.rules.action")}</th><th className="num">{t("pages.rules.points")}</th></tr></thead>
              <tbody>{r.rules.map((x) => <tr key={x.action}><td>{x.action}</td><td className="num">{x.points}</td></tr>)}</tbody>
            </table>
          </div>
          <p className="small muted">{r.note} <Tbc /></p>
          <h2 className="h2">{t("pages.rules.badges")}</h2>
          <ul className="plain stack">{r.badges.map((b) => <li key={b.code}><b>{b.title}</b> · <span className="muted">{b.description}</span> · <span className="mono">+{b.points}</span></li>)}</ul>
        </>
      ) : <p className="muted">{t("pages.rules.failed")}</p>}
      <h2 className="h2">{t("pages.rules.sources")}</h2>
      <p>{t("pages.rules.sourcesPre")} <Tbc />. {t("pages.rules.sourcesPost")}</p>
    </article>
  );
}
