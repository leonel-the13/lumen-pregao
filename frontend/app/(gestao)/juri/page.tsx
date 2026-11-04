import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { AccessDenied, LaterPhase } from "@/components/ui/States";
import { hasAny, requireMe } from "@/lib/session";
import { getI18n } from "@/lib/i18n/server";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("authoring.jury.title") };
}

export default async function Juri() {
  const [me, { t }] = await Promise.all([requireMe("/juri"), getI18n()]);
  return (
    <>
      <SetCrumbs items={[{ label: t("admin.page.group") }, { label: t("authoring.jury.title") }]} />
      {hasAny(me, "JURY") ? (
        <LaterPhase title={t("authoring.jury.heading")} body={t("authoring.jury.body")} />
      ) : <AccessDenied />}
    </>
  );
}
