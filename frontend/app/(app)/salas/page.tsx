import Link from "next/link";
import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { RoomLink } from "@/components/learning/RoomLink";
import { Card } from "@/components/ui/Card";
import { Tabs } from "@/components/ui/Tabs";
import { EmptyState } from "@/components/ui/States";
import { serverFetchOrLogin } from "@/lib/api-server";
import type { RoomCard } from "@/lib/types";
import { getI18n } from "@/lib/i18n/server";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("learn.rooms.title") };
}

export default async function Salas({ searchParams }: { searchParams: Promise<{ q?: string }> }) {
  const { q } = await searchParams;
  const { t } = await getI18n();
  const rooms = await serverFetchOrLogin<RoomCard[]>(`/rooms${q ? `?q=${encodeURIComponent(q)}` : ""}`, "/salas");
  const byTrack = rooms.reduce<Record<string, RoomCard[]>>((acc, r) => ((acc[r.trackTitle] ??= []).push(r), acc), {});
  return (
    <>
      <SetCrumbs items={[{ label: t("shell.nav.groups.learn") }, { label: t("learn.rooms.title") }]} />
      <div className="stack" style={{ gap: 6 }}><div className="label">{t("learn.tracks.catalog")}</div><h1 className="h1">{t("learn.tracks.heading")}</h1></div>
      <Tabs label={t("learn.tracks.catalog")} active="salas" items={[{ key: "trilhas", label: t("learn.tracks.tabTracks"), href: "/trilhas" }, { key: "salas", label: t("learn.tracks.tabRooms"), href: "/salas" }]} />
      <form className="row" role="search">
        <label className="sr-only" htmlFor="q">{t("learn.rooms.searchLabel")}</label>
        <input id="q" name="q" className="input" style={{ maxWidth: 420 }} placeholder={t("learn.rooms.searchPlaceholder")} defaultValue={q} />
        <button className="btn out" type="submit">{t("common.search")}</button>
      </form>
      {rooms.length === 0 ? (
        <EmptyState title={q ? t("learn.rooms.noMatch", { q }) : t("learn.rooms.none")} body={t("learn.rooms.noMatchBody")}
          action={<Link className="btn out" href="/salas">{t("learn.rooms.clearSearch")}</Link>} />
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
