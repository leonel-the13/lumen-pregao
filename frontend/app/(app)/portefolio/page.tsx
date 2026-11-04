import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { PortfolioView } from "@/components/portfolio/PortfolioView";
import { PortfolioActions } from "@/components/settings/PortfolioActions";
import { serverFetchOrLogin } from "@/lib/api-server";
import type { Portfolio } from "@/lib/types";
import { getI18n } from "@/lib/i18n/server";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("portfolio.title") };
}

export default async function Portefolio() {
  const [p, { t }] = await Promise.all([serverFetchOrLogin<Portfolio>("/me/portfolio", "/portefolio"), getI18n()]);
  return (
    <>
      <SetCrumbs items={[{ label: t("shell.nav.groups.me") }, { label: t("portfolio.title") }]} />
      <PortfolioView p={p} actions={<PortfolioActions isPublic={p.isPublic} handle={p.handle} />} />
    </>
  );
}
