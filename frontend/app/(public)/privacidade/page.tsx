import { Privacy } from "@/components/legal/Privacy";
import { getI18n } from "@/lib/i18n/server";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("pages.legal.privacy") };
}

export default async function Privacidade() {
  const { locale } = await getI18n();
  return (
    <article className="content stack legal" style={{ maxWidth: 780, margin: "0 auto", gap: 14 }}>
      <Privacy locale={locale} />
    </article>
  );
}
