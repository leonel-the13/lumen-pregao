import { AuthCard } from "@/components/auth/AuthCard";
import { RegisterForm } from "@/components/auth/RegisterForm";
import { serverFetch } from "@/lib/api-server";
import { getI18n } from "@/lib/i18n/server";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("auth.pages.signUp") };
}

export default async function Registo() {
  const [courses, { t }] = await Promise.all([serverFetch<{ id: string; name: string }[]>("/courses").catch(() => []), getI18n()]);
  return (
    <AuthCard step={t("auth.pages.signUp")} title={t("auth.pages.createYours")}>
      <RegisterForm courses={courses} />
    </AuthCard>
  );
}
