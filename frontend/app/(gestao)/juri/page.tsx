import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { AccessDenied, LaterPhase } from "@/components/ui/States";
import { hasAny, requireMe } from "@/lib/session";

export const metadata = { title: "Júri" };

export default async function Juri() {
  const me = await requireMe("/juri");
  return (
    <>
      <SetCrumbs items={[{ label: "Gestão" }, { label: "Júri" }]} />
      {hasAny(me, "JURY") ? (
        <LaterPhase title="Avaliação por júri"
          body="A fila de casos atribuídos, a submissão anónima e a grelha pública (critérios, peso, nota 0–4) pertencem à fase posterior. A fase 1 do contest é corrigida automaticamente." />
      ) : <AccessDenied area="Esta área" />}
    </>
  );
}
