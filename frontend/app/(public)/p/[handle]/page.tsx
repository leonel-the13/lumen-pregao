import { notFound } from "next/navigation";
import { PortfolioView } from "@/components/portfolio/PortfolioView";
import { serverFetchOptional } from "@/lib/api-server";
import { getI18n } from "@/lib/i18n/server";
import type { Portfolio } from "@/lib/types";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("pages.meta.portfolio"), robots: { index: false } };
}

export default async function PublicPortfolio({ params }: { params: Promise<{ handle: string }> }) {
  const { handle } = await params;
  const p = await serverFetchOptional<Portfolio>(`/public/portfolios/${encodeURIComponent(handle)}`);
  if (!p) notFound();
  return <div className="content" style={{ margin: "0 auto" }}><PortfolioView p={p} /></div>;
}
