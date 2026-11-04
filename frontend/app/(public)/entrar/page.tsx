import { AuthCard } from "@/components/auth/AuthCard";
import { LoginForm } from "@/components/auth/LoginForm";
import { getI18n } from "@/lib/i18n/server";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("auth.pages.signIn") };
}

export default async function Entrar({ searchParams }: { searchParams: Promise<{ next?: string; expirada?: string; definida?: string; verificado?: string }> }) {
  const [sp, { t }] = await Promise.all([searchParams, getI18n()]);
  return (
    <AuthCard step={t("auth.pages.signIn")} title={t("auth.pages.welcomeBack")}>
      <LoginForm next={sp.next} expired={sp.expirada === "1"} passwordSet={sp.definida === "1"} verified={sp.verificado === "1"} />
    </AuthCard>
  );
}
