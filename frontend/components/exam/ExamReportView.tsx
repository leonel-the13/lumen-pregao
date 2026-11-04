"use client";
import Link from "next/link";
import { useCallback } from "react";
import { EChart } from "@/components/charts/EChart";
import { RoomLink } from "@/components/learning/RoomLink";
import { Badge } from "@/components/ui/Badge";
import { Card, CardHeader, Kpi } from "@/components/ui/Card";
import { formatDuration, formatGrade } from "@/lib/format";
import { useT } from "@/lib/i18n/client";
import type { ExamReport } from "@/lib/types";

export function ExamReportView({ r }: { r: ExamReport }) {
  const tr = useT();
  const option = useCallback((t: (n: string) => string) => ({
    grid: { left: 8, right: 16, top: 8, bottom: 8, containLabel: true },
    xAxis: { type: "value" as const, max: 100, axisLabel: { formatter: "{value} %", color: t("--muted") }, splitLine: { lineStyle: { color: t("--line") } } },
    yAxis: { type: "category" as const, data: r.topics.map((x) => x.topic), axisLabel: { color: t("--text-2") }, axisLine: { lineStyle: { color: t("--line") } } },
    series: [{ type: "bar" as const, data: r.topics.map((x) => Math.round((x.correct / Math.max(1, x.total)) * 100)), itemStyle: { color: t("--accent"), borderRadius: 4 }, barMaxWidth: 18 }],
    tooltip: { trigger: "axis" as const, valueFormatter: (v: unknown) => `${v} %` },
  }), [r.topics]);
  return (
    <div className="stack" style={{ gap: 20 }}>
      <div className="row"><h1 className="h1">{r.title}</h1><div className="spacer" />{r.passed ? <Badge tone="pos">{tr("exam.report.passed")}</Badge> : <Badge tone="neg">{tr("exam.report.failed")}</Badge>}</div>
      {r.autoSubmitted && <div className="alert info"><b>{tr("exam.report.auto")}</b> {tr("exam.report.autoBody")}</div>}
      <div className="grid g4">
        <Kpi label={tr("exam.report.grade")} value={formatGrade(r.grade)} />
        <Kpi label={tr("exam.report.correct")} value={`${r.correct}/${r.total}`} />
        <Kpi label={tr("exam.report.time")} value={formatDuration(r.secondsUsed * 1000, false).replace(/^00:/, "")} />
        <Kpi label={tr("exam.report.passMark")} value={`≥ ${formatGrade(r.passMark)}`} />
      </div>
      <div className="grid g2">
        <Card><CardHeader title={tr("exam.report.strengths")} />{r.strengths.length ? <p>{r.strengths.join(" · ")}</p> : <p className="small muted">—</p>}</Card>
        <Card><CardHeader title={tr("exam.report.weaknesses")} />{r.weaknesses.length ? <p>{r.weaknesses.join(" · ")}</p> : <p className="small muted">{tr("exam.report.nothing")}</p>}</Card>
      </div>
      {r.topics.length > 0 && <Card><CardHeader title={tr("exam.report.byTopic")} /><EChart option={option} label={tr("exam.report.byTopicLabel")} height={Math.max(160, r.topics.length * 44)} /></Card>}
      {r.recommendedRooms.length > 0 && (
        <Card><CardHeader title={tr("exam.report.review")} /><div className="stack" style={{ gap: 4 }}>{r.recommendedRooms.map((x) => <RoomLink key={x.id} r={x} />)}</div></Card>
      )}
      <Link className="btn out" href="/exames">{tr("exam.report.back")}</Link>
    </div>
  );
}
