import Link from "next/link";
import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { EmptyState } from "@/components/ui/States";
import { serverFetchOrLogin } from "@/lib/api-server";
import { formatDateTime } from "@/lib/format";
import type { ContestCard } from "@/lib/types";

export const metadata = { title: "Competições" };

const STATE: Record<string, [string, "acc" | "info" | "neutral" | "pos"]> = {
  REGISTRATION: ["Inscrições abertas", "acc"], WAITING_ROOM: ["Sala de espera", "info"], RUNNING: ["Ao vivo", "acc"], FROZEN: ["Ao vivo · congelado", "acc"],
  SUBMISSIONS_CLOSED: ["Submissões encerradas", "neutral"], FINAL_EVALUATION: ["Avaliação final", "neutral"], RESULTS_PUBLISHED: ["Resultados", "pos"], CLOSED: ["Encerrado", "neutral"],
};

export default async function Competicoes() {
  const list = await serverFetchOrLogin<ContestCard[]>("/contests", "/competicoes");
  return (
    <>
      <SetCrumbs items={[{ label: "Competir" }, { label: "Competições" }]} />
      <div className="stack" style={{ gap: 6 }}><div className="label">Competir</div><h1 className="h1">Competições</h1></div>
      {list.length === 0 ? (
        <EmptyState title="Ainda não participaste em competições" body="A próxima Maratona abre inscrições em breve." />
      ) : (
        <div className="grid g2">
          {list.map((c) => {
            const [label, tone] = STATE[c.state] ?? [c.state, "neutral"];
            return (
              <Card key={c.id} className="stack">
                <div className="row" style={{ gap: 8 }}><Badge tone={tone}>{label}</Badge><Badge>{c.mode === "TEAM" ? `Equipas de ${c.minTeam} a ${c.maxTeam}` : "Individual"}</Badge></div>
                <h2 className="h2">{c.title}</h2>
                <p className="small muted">Início {formatDateTime(c.startsAt)} · fim {formatDateTime(c.endsAt)} (hora de Luanda)</p>
                {c.myTeamName && <p className="small">{c.mode === "TEAM" ? <>Equipa <b>{c.myTeamName}</b> · {c.myTeamConfirmed} de {c.maxTeam} confirmados</> : "Inscrição confirmada"}</p>}
                <div className="row"><div className="spacer" /><Link className="btn pri" href={`/competicoes/${c.id}`}>Abrir</Link></div>
              </Card>
            );
          })}
        </div>
      )}
    </>
  );
}
