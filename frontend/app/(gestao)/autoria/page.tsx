import Link from "next/link";
import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { LegalSourcesPanel } from "@/components/authoring/LegalSourcesPanel";
import { ReviewQueue } from "@/components/authoring/ReviewQueue";
import { Alert } from "@/components/ui/Alert";
import { Badge } from "@/components/ui/Badge";
import { Tabs } from "@/components/ui/Tabs";
import { AccessDenied, EmptyState } from "@/components/ui/States";
import { serverFetch } from "@/lib/api-server";
import { STATUS_LABEL, STATUS_TONE, TYPE_LABEL, type ExerciseSummary, type LegalSourceDto } from "@/lib/authoring-types";
import { formatDate } from "@/lib/format";
import { hasAny, requireMe } from "@/lib/session";

export const metadata = { title: "Autoria" };

export default async function Autoria({ searchParams }: { searchParams: Promise<{ tab?: string }> }) {
  const { tab = "exercicios" } = await searchParams;
  const me = await requireMe("/autoria");
  if (!hasAny(me, "AUTHOR", "REVIEWER", "ADMIN")) return <AccessDenied area="A autoria" />;
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
      <SetCrumbs items={[{ label: "Gestão" }, { label: "Autoria" }]} />
      <div className="row"><div className="stack" style={{ gap: 6 }}><div className="label">Gestão</div><h1 className="h1">Autoria</h1></div>
        <div className="spacer" />{hasAny(me, "AUTHOR") && <Link className="btn pri" href="/autoria/novo">Novo exercício</Link>}</div>
      {alerts.affectedExercises > 0 && (
        <Alert tone="neg" title={`Norma alterada · ${alerts.affectedExercises} exercícios afectados`}>
          Um revisor marcou nova versão de uma referência. Os exercícios ligados voltaram a revisão.
        </Alert>
      )}
      <Tabs label="Autoria" active={tab} items={[
        { key: "exercicios", label: reviewer ? "Todos os exercícios" : "Os meus exercícios", href: "/autoria" },
        ...(reviewer ? [{ key: "revisao", label: "Fila de revisão", href: "/autoria?tab=revisao", count: queue.length }] : []),
        { key: "fontes", label: "Fontes normativas", href: "/autoria?tab=fontes" },
      ]} />
      {tab === "fontes" ? <LegalSourcesPanel sources={sources} canEdit={reviewer} /> : tab === "revisao" && rows.length > 0 ? (
        <ReviewQueue rows={rows} />
      ) : rows.length === 0 ? (
        <EmptyState title={tab === "revisao" ? "A fila de revisão está vazia" : "Ainda não há exercícios"} body="Os exercícios aparecem aqui depois de criados." />
      ) : (
        <div className="tbl-wrap">
          <table className="tbl">
            <thead><tr><th>Código</th><th>Exercício</th><th>Tipo</th><th>Autor</th><th>Estado</th><th>Actualizado</th></tr></thead>
            <tbody>
              {rows.map((e) => (
                <tr key={e.id}>
                  <td className="mono"><Link href={`/autoria/${e.id}`}>{e.code}</Link></td>
                  <td><Link href={`/autoria/${e.id}`}><b>{e.title}</b></Link><div className="small muted">{e.topic} · v{e.latestVersion}</div></td>
                  <td className="small">{TYPE_LABEL[e.type]}</td>
                  <td className="small">{e.authorName}</td>
                  <td><Badge tone={STATUS_TONE[e.status]}>{STATUS_LABEL[e.status]}</Badge>{e.legalAlertAt && <> <Badge tone="neg" icon="warn">Norma alterada</Badge></>}</td>
                  <td className="small mono">{formatDate(e.updatedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
