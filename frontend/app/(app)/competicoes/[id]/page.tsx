import { notFound } from "next/navigation";
import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { ContestView } from "@/components/contest/ContestView";
import { serverFetchOrLogin } from "@/lib/api-server";
import { ApiError } from "@/lib/errors";
import type { ContestDetail } from "@/lib/types";
import { getI18n } from "@/lib/i18n/server";

export default async function Contest({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const { t } = await getI18n();
  let c: ContestDetail;
  try {
    c = await serverFetchOrLogin<ContestDetail>(`/contests/${id}`, `/competicoes/${id}`);
  } catch (e) {
    if (e instanceof ApiError && (e.status === 404 || e.status === 400)) notFound();
    throw e;
  }
  return (
    <>
      <SetCrumbs items={[{ label: t("compete.list.title"), href: "/competicoes" }, { label: c.title }]} />
      <ContestView key={c.state} initial={c} />
    </>
  );
}
