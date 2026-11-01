import Link from "next/link";
import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { TrackCardView } from "@/components/learning/TrackCardView";
import { Tabs } from "@/components/ui/Tabs";
import { EmptyState } from "@/components/ui/States";
import { serverFetchOrLogin } from "@/lib/api-server";
import type { TrackCard } from "@/lib/types";

export const metadata = { title: "Trilhas" };

const COURSES = [["CONT", "Contabilidade"], ["ECON", "Economia"], ["GEST", "Gestão"]];
const TOPICS = ["Fiscalidade", "Análise financeira", "Power BI", "Mercado"];
const LEVELS = [["1", "Iniciação"], ["2", "Intermédio"], ["3", "Avançado"]];

export default async function Trilhas({ searchParams }: { searchParams: Promise<Record<string, string | undefined>> }) {
  const sp = await searchParams;
  const qs = new URLSearchParams(Object.entries(sp).filter(([, v]) => v) as [string, string][]);
  const tracks = await serverFetchOrLogin<TrackCard[]>(`/tracks?${qs}`, "/trilhas");
  const chip = (key: string, value: string, label: string) => {
    const next = new URLSearchParams(qs);
    if (sp[key] === value) next.delete(key); else next.set(key, value);
    return <Link key={value} className={["chip", sp[key] === value && "on"].filter(Boolean).join(" ")} href={`/trilhas?${next}`} aria-pressed={sp[key] === value}>{label}</Link>;
  };
  return (
    <>
      <SetCrumbs items={[{ label: "Aprender" }, { label: "Trilhas" }]} />
      <div className="stack" style={{ gap: 6 }}><div className="label">Catálogo</div><h1 className="h1">Trilhas e salas</h1></div>
      <Tabs label="Catálogo" active="trilhas" items={[{ key: "trilhas", label: "Trilhas", href: "/trilhas" }, { key: "salas", label: "Todas as salas", href: "/salas" }]} />
      <section aria-label="Filtros" className="stack" style={{ gap: 10 }}>
        <div className="row" style={{ gap: 8 }}>{COURSES.map(([v, l]) => chip("course", v, l))}</div>
        <div className="row" style={{ gap: 8 }}>{TOPICS.map((t) => chip("topic", t, t))}</div>
        <div className="row" style={{ gap: 8 }}>
          {LEVELS.map(([v, l]) => chip("difficulty", v, l))}
          {qs.toString() && <Link className="btn sm ghost" href="/trilhas">Limpar</Link>}
        </div>
      </section>
      {tracks.length === 0 ? (
        <EmptyState title="Nenhuma trilha corresponde aos filtros" body="Remove um dos filtros para ver mais trilhas." action={<Link className="btn out" href="/trilhas">Limpar filtros</Link>} />
      ) : (
        <div className="grid g3">{tracks.map((t) => <TrackCardView key={t.slug} t={t} />)}</div>
      )}
    </>
  );
}
