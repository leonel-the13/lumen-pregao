import Link from "next/link";
import { Footer } from "@/components/layout/Footer";
import { getI18n } from "@/lib/i18n/server";

export default async function NotFound() {
  const { t } = await getI18n();
  return (
    <>
    <div style={{ display: "grid", placeItems: "center", minHeight: "60vh", padding: 24 }}>
      <div className="card state-box center">
        <div className="label">{t("pages.notFound.eyebrow")}</div>
        <h1 className="h2">{t("pages.notFound.title")}</h1>
        <p className="small muted">{t("pages.notFound.body")}</p>
        <Link className="btn out" href="/inicio">{t("pages.notFound.home")}</Link>
      </div>
    </div>
    <Footer />
    </>
  );
}
