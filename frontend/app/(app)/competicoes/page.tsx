import Link from "next/link";
import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { EmptyState } from "@/components/ui/States";
import { serverFetchOrLogin } from "@/lib/api-server";
import { formatDateTime } from "@/lib/format";
import type { ContestCard } from "@/lib/types";
import { getI18n } from "@/lib/i18n/server";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("compete.list.title") };
}

const TONE: Record<string, "acc" | "info" | "neutral" | "pos"> = {
  REGISTRATION: "acc", WAITING_ROOM: "info", RUNNING: "acc", FROZEN: "acc",
  SUBMISSIONS_CLOSED: "neutral", FINAL_EVALUATION: "neutral", RESULTS_PUBLISHED: "pos", CLOSED: "neutral",
};

export default async function Competicoes() {
  const [list, { t, locale }] = await Promise.all([serverFetchOrLogin<ContestCard[]>("/contests", "/competicoes"), getI18n()]);
  return (
    <>
      <SetCrumbs items={[{ label: t("compete.list.group") }, { label: t("compete.list.title") }]} />
      <div className="stack" style={{ gap: 6 }}><div className="label">{t("compete.list.group")}</div><h1 className="h1">{t("compete.list.title")}</h1></div>
      {list.length === 0 ? (
        <EmptyState title={t("compete.list.empty")} body={t("compete.list.emptyBody")} />
      ) : (
        <div className="grid g2">
          {list.map((c) => {
            const tone = TONE[c.state] ?? "neutral";
            const label = c.state in TONE ? t(`compete.state.${c.state as "REGISTRATION" | "WAITING_ROOM" | "RUNNING" | "FROZEN" | "SUBMISSIONS_CLOSED" | "FINAL_EVALUATION" | "RESULTS_PUBLISHED" | "CLOSED"}`) : c.state;
            return (
              <Card key={c.id} className="stack">
                <div className="row" style={{ gap: 8 }}><Badge tone={tone}>{label}</Badge><Badge>{c.mode === "TEAM" ? t("compete.list.teamsOf", { min: c.minTeam, max: c.maxTeam }) : t("compete.list.individual")}</Badge></div>
                <h2 className="h2">{c.title}</h2>
                <p className="small muted">{t("compete.list.when", { start: formatDateTime(c.startsAt, locale), end: formatDateTime(c.endsAt, locale) })}</p>
                {c.myTeamName && <p className="small">{c.mode === "TEAM" ? <>{t("compete.list.team")} <b>{c.myTeamName}</b> · {t("compete.list.confirmedOf", { n: c.myTeamConfirmed ?? 0, max: c.maxTeam })}</> : t("compete.list.registered")}</p>}
                <div className="row"><div className="spacer" /><Link className="btn pri" href={`/competicoes/${c.id}`}>{t("compete.list.open")}</Link></div>
              </Card>
            );
          })}
        </div>
      )}
    </>
  );
}
