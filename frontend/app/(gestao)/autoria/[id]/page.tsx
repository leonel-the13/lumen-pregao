import { notFound } from "next/navigation";
import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { ExerciseEditor } from "@/components/authoring/ExerciseEditor";
import { AccessDenied } from "@/components/ui/States";
import { serverFetch } from "@/lib/api-server";
import type { ExerciseDto, LegalSourceDto } from "@/lib/authoring-types";
import { ApiError } from "@/lib/errors";
import { hasAny, requireMe } from "@/lib/session";

export default async function Exercicio({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const me = await requireMe(`/autoria/${id}`);
  try {
    const [ex, sources] = await Promise.all([serverFetch<ExerciseDto>(`/authoring/exercises/${id}`), serverFetch<LegalSourceDto[]>("/legal-sources")]);
    return (
      <>
        <SetCrumbs items={[{ label: "Gestão" }, { label: "Autoria", href: "/autoria" }, { label: ex.code }]} />
        <ExerciseEditor key={ex.versions[0]?.id + ex.versions[0]?.status} exercise={ex} sources={sources}
          canReview={hasAny(me, "REVIEWER", "ADMIN")} isAuthor={ex.authorId === me.id} />
      </>
    );
  } catch (e) {
    if (e instanceof ApiError && e.status === 403) return <AccessDenied area="Este exercício" />;
    if (e instanceof ApiError && (e.status === 404 || e.status === 400)) notFound();
    throw e;
  }
}
