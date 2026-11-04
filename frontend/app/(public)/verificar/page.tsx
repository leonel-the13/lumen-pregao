import { AuthCard } from "@/components/auth/AuthCard";
import { VerifyEmail } from "@/components/auth/VerifyEmail";
import { getI18n } from "@/lib/i18n/server";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("auth.pages.verifyMeta") };
}

export default async function Verificar({ searchParams }: { searchParams: Promise<{ token?: string; email?: string }> }) {
  const [sp, { t }] = await Promise.all([searchParams, getI18n()]);
  return (
    <AuthCard step={t("auth.pages.verify")} title={t("auth.pages.confirmEmail")}>
      <VerifyEmail token={sp.token} email={sp.email} />
    </AuthCard>
  );
}
