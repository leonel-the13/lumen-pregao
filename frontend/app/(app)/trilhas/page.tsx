import Link from "next/link";
import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { TrackCardView } from "@/components/learning/TrackCardView";
import { Tabs } from "@/components/ui/Tabs";
import { EmptyState } from "@/components/ui/States";
import { serverFetchOrLogin } from "@/lib/api-server";
import type { TrackCard } from "@/lib/types";
import { getI18n } from "@/lib/i18n/server";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("learn.tracks.title") };
}

// Os valores são os filtros da API (em português); só o rótulo é traduzido.
const COURSES = ["CONT", "ECON", "GEST"] as const;
const TOPICS = ["Fiscalidade", "Análise financeira", "Power BI", "Mercado"] as const;
const LEVELS = ["1", "2", "3"] as const;

export default async function Trilhas({ searchParams }: { searchParams: Promise<Record<string, string | undefined>> }) {
  const sp = await searchParams;
  const qs = new URLSearchParams(Object.entries(sp).filter(([, v]) => v) as [string, string][]);
  const { t } = await getI18n();
  const tracks = await serverFetchOrLogin<TrackCard[]>(`/tracks?${qs}`, "/trilhas");
  const chip = (key: string, value: string, label: string) => {
    const next = new URLSearchParams(qs);
    if (sp[key] === value) next.delete(key); else next.set(key, value);
    return <Link key={value} className={["chip", sp[key] === value && "on"].filter(Boolean).join(" ")} href={`/trilhas?${next}`} aria-pressed={sp[key] === value}>{label}</Link>;
  };
  return (
    <>
      <SetCrumbs items={[{ label: t("shell.nav.groups.learn") }, { label: t("learn.tracks.title") }]} />
      <div className="stack" style={{ gap: 6 }}><div className="label">{t("learn.tracks.catalog")}</div><h1 className="h1">{t("learn.tracks.heading")}</h1></div>
      <Tabs label={t("learn.tracks.catalog")} active="trilhas" items={[{ key: "trilhas", label: t("learn.tracks.tabTracks"), href: "/trilhas" }, { key: "salas", label: t("learn.tracks.tabRooms"), href: "/salas" }]} />
      <section aria-label={t("learn.tracks.filters")} className="stack" style={{ gap: 10 }}>
        <div className="row" style={{ gap: 8 }}>{COURSES.map((v) => chip("course", v, t(`learn.tracks.courses.${v}`)))}</div>
        <div className="row" style={{ gap: 8 }}>{TOPICS.map((v) => chip("topic", v, t(`learn.tracks.topics.${v}`)))}</div>
        <div className="row" style={{ gap: 8 }}>
          {LEVELS.map((v) => chip("difficulty", v, t(`learn.difficulty.${v}`)))}
          {qs.toString() && <Link className="btn sm ghost" href="/trilhas">{t("learn.tracks.clear")}</Link>}
        </div>
      </section>
      {tracks.length === 0 ? (
        <EmptyState title={t("learn.tracks.noMatch")} body={t("learn.tracks.noMatchBody")} action={<Link className="btn out" href="/trilhas">{t("learn.tracks.clearFilters")}</Link>} />
      ) : (
        <div className="grid g3">{tracks.map((tr) => <TrackCardView key={tr.slug} t={tr} />)}</div>
      )}
    </>
  );
}
