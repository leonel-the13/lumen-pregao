import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { PortfolioView } from "@/components/portfolio/PortfolioView";
import { PortfolioActions } from "@/components/settings/PortfolioActions";
import { serverFetchOrLogin } from "@/lib/api-server";
import type { Portfolio } from "@/lib/types";

export const metadata = { title: "Portefólio" };

export default async function Portefolio() {
  const p = await serverFetchOrLogin<Portfolio>("/me/portfolio", "/portefolio");
  return (
    <>
      <SetCrumbs items={[{ label: "Eu" }, { label: "Portefólio" }]} />
      <PortfolioView p={p} actions={<PortfolioActions isPublic={p.isPublic} handle={p.handle} />} />
    </>
  );
}
