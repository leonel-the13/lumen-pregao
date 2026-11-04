import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { Card } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Icon } from "@/components/ui/Icon";
import { getI18n } from "@/lib/i18n/server";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("learn.sims.title") };
}

const ITEMS = ["company", "market", "hiring", "data"] as const;

export default async function Simulacoes() {
  const { t } = await getI18n();
  return (
    <>
      <SetCrumbs items={[{ label: t("shell.nav.groups.learn") }, { label: t("learn.sims.title") }]} />
      <div className="stack" style={{ gap: 6 }}><div className="label">{t("learn.sims.title")}</div><h1 className="h1">{t("learn.sims.heading")}<span className="serif">{t("learn.sims.headingEm")}</span></h1></div>
      <div className="sim-ribbon"><Icon name="building" />{t("learn.sims.ribbon")}</div>
      <div className="grid g2">
        {ITEMS.map((k) => (
          <Card key={k} className="stack"><div className="row"><h2 className="h3">{t(`learn.sims.${k}`)}</h2><div className="spacer" /><Badge>{t("learn.sims.later")}</Badge></div><p className="small muted">{t(`learn.sims.${k}Body`)}</p></Card>
        ))}
      </div>
    </>
  );
}
