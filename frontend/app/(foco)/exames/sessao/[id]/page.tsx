import { notFound } from "next/navigation";
import { ExamRunner } from "@/components/exam/ExamRunner";
import { serverFetchOrLogin } from "@/lib/api-server";
import { ApiError } from "@/lib/errors";
import type { ExamSessionView } from "@/lib/types";
import { getI18n } from "@/lib/i18n/server";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("exam.runner.meta") };
}

export default async function Sessao({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  try {
    const s = await serverFetchOrLogin<ExamSessionView>(`/exam-sessions/${id}`, `/exames/sessao/${id}`);
    return <ExamRunner initial={s} />;
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  }
}
