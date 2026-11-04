import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { PrivacyCard } from "@/components/settings/PrivacyCard";
import { ProfileForm } from "@/components/settings/ProfileForm";
import { serverFetch, serverFetchOrLogin } from "@/lib/api-server";
import { getI18n } from "@/lib/i18n/server";
import type { Me } from "@/lib/types";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("settings.profile.title") };
}

export default async function Perfil() {
  const [{ t }, me, courses] = await Promise.all([
    getI18n(),
    serverFetchOrLogin<Me>("/me", "/perfil"),
    serverFetch<{ id: string; name: string }[]>("/courses").catch(() => []),
  ]);
  return (
    <>
      <SetCrumbs items={[{ label: t("settings.profile.eyebrow") }, { label: t("settings.profile.title") }]} />
      <div className="stack" style={{ gap: 6 }}><div className="label">{t("settings.profile.eyebrow")}</div><h1 className="h1">{t("settings.profile.title")}</h1></div>
      <div className="grid g2" style={{ alignItems: "start" }}>
        <ProfileForm me={me} courses={courses} />
        <PrivacyCard me={me} />
      </div>
    </>
  );
}
