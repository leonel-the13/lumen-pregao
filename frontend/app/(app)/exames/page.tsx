import Link from "next/link";
import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { StartExamButton } from "@/components/exam/StartExamButton";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { EmptyState } from "@/components/ui/States";
import { serverFetchOrLogin } from "@/lib/api-server";
import { formatGrade } from "@/lib/format";
import type { ExamCard } from "@/lib/types";

export const metadata = { title: "Exames" };

export default async function Exames() {
  const exams = await serverFetchOrLogin<ExamCard[]>("/exams", "/exames");
  return (
    <>
      <SetCrumbs items={[{ label: "Aprender" }, { label: "Exames" }]} />
      <div className="stack" style={{ gap: 6 }}><div className="label">Exames</div><h1 className="h1">Exames de módulo</h1></div>
      <p className="muted">Modo concentração, sem navegação lateral. O início e o fim oficiais são do servidor; as respostas são guardadas a cada alteração.</p>
      {exams.length === 0 ? <EmptyState title="Ainda não há exames disponíveis" body="Os exames aparecem quando o revisor publicar as perguntas." action={<Link className="btn out" href="/trilhas">Ver trilhas</Link>} /> : (
        <div className="grid g2">
          {exams.map((e) => (
            <Card key={e.id} className="stack">
              <div className="row"><h2 className="h3">{e.title}</h2><div className="spacer" />
                {e.bestGrade !== undefined && e.bestGrade !== null && <Badge tone={e.bestGrade >= e.passMark ? "pos" : "neg"}>Melhor nota {formatGrade(e.bestGrade)}</Badge>}
              </div>
              <p className="small muted">{e.questions} perguntas · {e.durationMin} min · aprovação ≥ {formatGrade(e.passMark)}</p>
              <div className="row"><div className="spacer" /><StartExamButton examId={e.id} durationMin={e.durationMin} resumeId={e.openSessionId} /></div>
            </Card>
          ))}
        </div>
      )}
    </>
  );
}
