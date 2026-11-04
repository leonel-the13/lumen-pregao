import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { ExerciseEditor } from "@/components/authoring/ExerciseEditor";
import { AccessDenied } from "@/components/ui/States";
import { serverFetch } from "@/lib/api-server";
import type { LegalSourceDto } from "@/lib/authoring-types";
import { hasAny, requireMe } from "@/lib/session";
import { getI18n } from "@/lib/i18n/server";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("authoring.page.newExercise") };
}

export default async function Novo() {
  const [me, { t }] = await Promise.all([requireMe("/autoria/novo"), getI18n()]);
  if (!hasAny(me, "AUTHOR")) return <AccessDenied area={t("authoring.page.createArea")} />;
  const sources = await serverFetch<LegalSourceDto[]>("/legal-sources");
  return (
    <>
      <SetCrumbs items={[{ label: t("admin.page.group") }, { label: t("authoring.page.title"), href: "/autoria" }, { label: t("authoring.page.newExercise") }]} />
      <ExerciseEditor sources={sources} canReview={false} isAuthor />
    </>
  );
}
