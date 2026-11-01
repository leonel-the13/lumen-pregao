import Link from "next/link";
import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { RoomLink } from "@/components/learning/RoomLink";
import { Card } from "@/components/ui/Card";
import { Tabs } from "@/components/ui/Tabs";
import { EmptyState } from "@/components/ui/States";
import { serverFetchOrLogin } from "@/lib/api-server";
import type { RoomCard } from "@/lib/types";

export const metadata = { title: "Salas" };

export default async function Salas({ searchParams }: { searchParams: Promise<{ q?: string }> }) {
  const { q } = await searchParams;
  const rooms = await serverFetchOrLogin<RoomCard[]>(`/rooms${q ? `?q=${encodeURIComponent(q)}` : ""}`, "/salas");
  const byTrack = rooms.reduce<Record<string, RoomCard[]>>((acc, r) => ((acc[r.trackTitle] ??= []).push(r), acc), {});
  return (
    <>
      <SetCrumbs items={[{ label: "Aprender" }, { label: "Salas" }]} />
      <div className="stack" style={{ gap: 6 }}><div className="label">Catálogo</div><h1 className="h1">Trilhas e salas</h1></div>
      <Tabs label="Catálogo" active="salas" items={[{ key: "trilhas", label: "Trilhas", href: "/trilhas" }, { key: "salas", label: "Todas as salas", href: "/salas" }]} />
      <form className="row" role="search">
        <label className="sr-only" htmlFor="q">Pesquisar salas</label>
        <input id="q" name="q" className="input" style={{ maxWidth: 420 }} placeholder="Pesquisar por título ou tema…" defaultValue={q} />
        <button className="btn out" type="submit">Pesquisar</button>
      </form>
      {rooms.length === 0 ? (
        <EmptyState title={q ? `Nenhuma sala corresponde a «${q}»` : "Ainda não há salas"} body="Experimenta outro termo ou consulta o catálogo de trilhas."
          action={<Link className="btn out" href="/salas">Limpar pesquisa</Link>} />
      ) : (
        Object.entries(byTrack).map(([track, rs]) => (
          <Card key={track}>
            <h2 className="h2" style={{ marginBottom: 12 }}>{track}</h2>
            <div className="stack" style={{ gap: 4 }}>{rs.map((r) => <RoomLink key={r.id} r={r} showTrack={false} />)}</div>
          </Card>
        ))
      )}
    </>
  );
}
