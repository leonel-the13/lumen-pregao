import { AuthCard } from "@/components/auth/AuthCard";
import { ResetForm } from "@/components/auth/ResetForm";
import { getI18n } from "@/lib/i18n/server";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("auth.pages.resetMeta") };
}

export default async function Redefinir({ searchParams }: { searchParams: Promise<{ token?: string; convite?: string }> }) {
  const [sp, { t }] = await Promise.all([searchParams, getI18n()]);
  const invite = sp.convite === "1";
  return (
    <AuthCard step={invite ? t("auth.pages.invite") : t("auth.pages.reset")} title={invite ? t("auth.pages.activate") : t("auth.pages.newPassword")}>
      <ResetForm token={sp.token} invite={invite} />
    </AuthCard>
  );
}
