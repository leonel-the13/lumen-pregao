import { notFound } from "next/navigation";
import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { ContestView } from "@/components/contest/ContestView";
import { serverFetchOrLogin } from "@/lib/api-server";
import { ApiError } from "@/lib/errors";
import type { ContestDetail } from "@/lib/types";

export default async function Contest({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  let c: ContestDetail;
  try {
    c = await serverFetchOrLogin<ContestDetail>(`/contests/${id}`, `/competicoes/${id}`);
  } catch (e) {
    if (e instanceof ApiError && (e.status === 404 || e.status === 400)) notFound();
    throw e;
  }
  return (
    <>
      <SetCrumbs items={[{ label: "Competições", href: "/competicoes" }, { label: c.title }]} />
      <ContestView key={c.state} initial={c} />
    </>
  );
}
