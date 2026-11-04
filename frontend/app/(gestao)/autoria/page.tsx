import Link from "next/link";
import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { LegalSourcesPanel } from "@/components/authoring/LegalSourcesPanel";
import { ReviewQueue } from "@/components/authoring/ReviewQueue";
import { Alert } from "@/components/ui/Alert";
import { Badge } from "@/components/ui/Badge";
import { Tabs } from "@/components/ui/Tabs";
import { AccessDenied, EmptyState } from "@/components/ui/States";
import { serverFetch } from "@/lib/api-server";
import { STATUS_TONE, isExerciseType, type ExerciseSummary, type LegalSourceDto } from "@/lib/authoring-types";
import { formatDate } from "@/lib/format";
import { hasAny, requireMe } from "@/lib/session";
import { getI18n } from "@/lib/i18n/server";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("authoring.page.title") };
}

export default async function Autoria({ searchParams }: { searchParams: Promise<{ tab?: string }> }) {
  const { tab = "exercicios" } = await searchParams;
  const [me, { t, locale }] = await Promise.all([requireMe("/autoria"), getI18n()]);
  if (!hasAny(me, "AUTHOR", "REVIEWER", "ADMIN")) return <AccessDenied area={t("authoring.page.area")} />;
  const reviewer = hasAny(me, "REVIEWER", "ADMIN");
  const [list, queue, sources, alerts] = await Promise.all([
    serverFetch<ExerciseSummary[]>("/authoring/exercises"),
    reviewer ? serverFetch<ExerciseSummary[]>("/authoring/review-queue") : Promise.resolve([]),
    serverFetch<LegalSourceDto[]>("/legal-sources"),
    serverFetch<{ affectedExercises: number }>("/legal-sources/alerts"),
  ]);
  const rows = tab === "revisao" ? queue : list;
  return (
    <>
      <SetCrumbs items={[{ label: t("admin.page.group") }, { label: t("authoring.page.title") }]} />
      <div className="row"><div className="stack" style={{ gap: 6 }}><div className="label">{t("admin.page.group")}</div><h1 className="h1">{t("authoring.page.title")}</h1></div>
        <div className="spacer" />{hasAny(me, "AUTHOR") && <Link className="btn pri" href="/autoria/novo">{t("authoring.page.newExercise")}</Link>}</div>
      {alerts.affectedExercises > 0 && (
        <Alert tone="neg" title={t("authoring.page.legalChanged", { n: alerts.affectedExercises })}>
          {t("authoring.page.legalChangedBody")}
        </Alert>
      )}
      <Tabs label={t("authoring.page.title")} active={tab} items={[
        { key: "exercicios", label: reviewer ? t("authoring.page.all") : t("authoring.page.mine"), href: "/autoria" },
        ...(reviewer ? [{ key: "revisao", label: t("authoring.page.queue"), href: "/autoria?tab=revisao", count: queue.length }] : []),
        { key: "fontes", label: t("authoring.page.sources"), href: "/autoria?tab=fontes" },
      ]} />
      {tab === "fontes" ? <LegalSourcesPanel sources={sources} canEdit={reviewer} /> : tab === "revisao" && rows.length > 0 ? (
        <ReviewQueue rows={rows} />
      ) : rows.length === 0 ? (
        <EmptyState title={tab === "revisao" ? t("authoring.page.queueEmpty") : t("authoring.page.none")} body={t("authoring.page.noneBody")} />
      ) : (
        <div className="tbl-wrap">
          <table className="tbl">
            <thead><tr><th>{t("authoring.page.code")}</th><th>{t("authoring.page.exercise")}</th><th>{t("authoring.page.type")}</th><th>{t("authoring.page.author")}</th><th>{t("authoring.page.state")}</th><th>{t("authoring.page.updated")}</th></tr></thead>
            <tbody>
              {rows.map((e) => (
                <tr key={e.id}>
                  <td className="mono"><Link href={`/autoria/${e.id}`}>{e.code}</Link></td>
                  <td><Link href={`/autoria/${e.id}`}><b>{e.title}</b></Link><div className="small muted">{e.topic} · v{e.latestVersion}</div></td>
                  <td className="small">{isExerciseType(e.type) ? t(`authoring.type.${e.type}`) : e.type}</td>
                  <td className="small">{e.authorName}</td>
                  <td><Badge tone={STATUS_TONE[e.status]}>{t(`authoring.status.${e.status}`)}</Badge>{e.legalAlertAt && <> <Badge tone="neg" icon="warn">{t("authoring.page.normChanged")}</Badge></>}</td>
                  <td className="small mono">{formatDate(e.updatedAt, undefined, locale)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
