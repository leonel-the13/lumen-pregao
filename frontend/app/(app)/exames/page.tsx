import Link from "next/link";
import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { StartExamButton } from "@/components/exam/StartExamButton";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { EmptyState } from "@/components/ui/States";
import { serverFetchOrLogin } from "@/lib/api-server";
import { formatGrade } from "@/lib/format";
import type { ExamCard } from "@/lib/types";
import { getI18n } from "@/lib/i18n/server";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("exam.list.title") };
}

export default async function Exames() {
  const [exams, { t }] = await Promise.all([serverFetchOrLogin<ExamCard[]>("/exams", "/exames"), getI18n()]);
  return (
    <>
      <SetCrumbs items={[{ label: t("shell.nav.groups.learn") }, { label: t("exam.list.title") }]} />
      <div className="stack" style={{ gap: 6 }}><div className="label">{t("exam.list.title")}</div><h1 className="h1">{t("exam.list.heading")}</h1></div>
      <p className="muted">{t("exam.list.intro")}</p>
      {exams.length === 0 ? <EmptyState title={t("exam.list.empty")} body={t("exam.list.emptyBody")} action={<Link className="btn out" href="/trilhas">{t("learn.home.seeTracks")}</Link>} /> : (
        <div className="grid g2">
          {exams.map((e) => (
            <Card key={e.id} className="stack">
              <div className="row"><h2 className="h3">{e.title}</h2><div className="spacer" />
                {e.bestGrade !== undefined && e.bestGrade !== null && <Badge tone={e.bestGrade >= e.passMark ? "pos" : "neg"}>{t("exam.list.best", { grade: formatGrade(e.bestGrade) })}</Badge>}
              </div>
              <p className="small muted">{t("exam.list.meta", { n: e.questions, min: e.durationMin, pass: formatGrade(e.passMark) })}</p>
              <div className="row"><div className="spacer" /><StartExamButton examId={e.id} durationMin={e.durationMin} resumeId={e.openSessionId} /></div>
            </Card>
          ))}
        </div>
      )}
    </>
  );
}
