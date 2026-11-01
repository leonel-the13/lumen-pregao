"use client";
import Link from "next/link";
import { useCallback } from "react";
import { EChart } from "@/components/charts/EChart";
import { RoomLink } from "@/components/learning/RoomLink";
import { Badge } from "@/components/ui/Badge";
import { Card, CardHeader, Kpi } from "@/components/ui/Card";
import { formatDuration, formatGrade } from "@/lib/format";
import type { ExamReport } from "@/lib/types";

export function ExamReportView({ r }: { r: ExamReport }) {
  const option = useCallback((t: (n: string) => string) => ({
    grid: { left: 8, right: 16, top: 8, bottom: 8, containLabel: true },
    xAxis: { type: "value" as const, max: 100, axisLabel: { formatter: "{value} %", color: t("--muted") }, splitLine: { lineStyle: { color: t("--line") } } },
    yAxis: { type: "category" as const, data: r.topics.map((x) => x.topic), axisLabel: { color: t("--text-2") }, axisLine: { lineStyle: { color: t("--line") } } },
    series: [{ type: "bar" as const, data: r.topics.map((x) => Math.round((x.correct / Math.max(1, x.total)) * 100)), itemStyle: { color: t("--accent"), borderRadius: 4 }, barMaxWidth: 18 }],
    tooltip: { trigger: "axis" as const, valueFormatter: (v: unknown) => `${v} %` },
  }), [r.topics]);
  return (
    <div className="stack" style={{ gap: 20 }}>
      <div className="row"><h1 className="h1">{r.title}</h1><div className="spacer" />{r.passed ? <Badge tone="pos">Aprovado</Badge> : <Badge tone="neg">Não aprovado</Badge>}</div>
      {r.autoSubmitted && <div className="alert info"><b>Submissão automática</b> O tempo terminou e as respostas guardadas foram corrigidas.</div>}
      <div className="grid g4">
        <Kpi label="Nota" value={formatGrade(r.grade)} />
        <Kpi label="Respostas certas" value={`${r.correct}/${r.total}`} />
        <Kpi label="Tempo usado" value={formatDuration(r.secondsUsed * 1000, false).replace(/^00:/, "")} />
        <Kpi label="Aprovação" value={`≥ ${formatGrade(r.passMark)}`} />
      </div>
      <div className="grid g2">
        <Card><CardHeader title="Pontos fortes" />{r.strengths.length ? <p>{r.strengths.join(" · ")}</p> : <p className="small muted">—</p>}</Card>
        <Card><CardHeader title="A melhorar" />{r.weaknesses.length ? <p>{r.weaknesses.join(" · ")}</p> : <p className="small muted">Nada a assinalar.</p>}</Card>
      </div>
      {r.topics.length > 0 && <Card><CardHeader title="Resultado por tema" /><EChart option={option} label="Percentagem de respostas certas por tema" height={Math.max(160, r.topics.length * 44)} /></Card>}
      {r.recommendedRooms.length > 0 && (
        <Card><CardHeader title="Salas recomendadas para rever" /><div className="stack" style={{ gap: 4 }}>{r.recommendedRooms.map((x) => <RoomLink key={x.id} r={x} />)}</div></Card>
      )}
      <Link className="btn out" href="/exames">Voltar aos exames</Link>
    </div>
  );
}
