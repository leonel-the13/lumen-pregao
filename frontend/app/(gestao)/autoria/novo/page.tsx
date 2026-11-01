import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { ExerciseEditor } from "@/components/authoring/ExerciseEditor";
import { AccessDenied } from "@/components/ui/States";
import { serverFetch } from "@/lib/api-server";
import type { LegalSourceDto } from "@/lib/authoring-types";
import { hasAny, requireMe } from "@/lib/session";

export const metadata = { title: "Novo exercício" };

export default async function Novo() {
  const me = await requireMe("/autoria/novo");
  if (!hasAny(me, "AUTHOR")) return <AccessDenied area="A criação de exercícios" />;
  const sources = await serverFetch<LegalSourceDto[]>("/legal-sources");
  return (
    <>
      <SetCrumbs items={[{ label: "Gestão" }, { label: "Autoria", href: "/autoria" }, { label: "Novo exercício" }]} />
      <ExerciseEditor sources={sources} canReview={false} isAuthor />
    </>
  );
}
