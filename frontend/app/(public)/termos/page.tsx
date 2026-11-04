import { Terms } from "@/components/legal/Terms";
import { getI18n } from "@/lib/i18n/server";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("pages.legal.terms") };
}

export default async function Termos() {
  const { locale } = await getI18n();
  return (
    <article className="content stack legal" style={{ maxWidth: 780, margin: "0 auto", gap: 14 }}>
      <Terms locale={locale} />
    </article>
  );
}
