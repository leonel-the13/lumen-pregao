import { AuthCard } from "@/components/auth/AuthCard";
import { ForgotForm } from "@/components/auth/ForgotForm";
import { getI18n } from "@/lib/i18n/server";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("auth.pages.forgotMeta") };
}

export default async function Esqueci() {
  const { t } = await getI18n();
  return <AuthCard step={t("auth.pages.recover")} title={t("auth.pages.forgotTitle")}><ForgotForm /></AuthCard>;
}
