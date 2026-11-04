import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { AccountSettings } from "@/components/settings/AccountSettings";
import { serverFetchOrLogin } from "@/lib/api-server";
import { getI18n } from "@/lib/i18n/server";
import type { Me } from "@/lib/types";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("settings.settings.title") };
}

export default async function Definicoes() {
  const [{ t }, me] = await Promise.all([getI18n(), serverFetchOrLogin<Me>("/me", "/definicoes")]);
  return (
    <>
      <SetCrumbs items={[{ label: t("settings.settings.eyebrow") }, { label: t("settings.settings.title") }]} />
      <div className="stack" style={{ gap: 6 }}><div className="label">{t("settings.settings.eyebrow")}</div><h1 className="h1">{t("settings.settings.title")}</h1></div>
      <AccountSettings me={me} />
    </>
  );
}
