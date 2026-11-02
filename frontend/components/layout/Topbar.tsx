"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Icon } from "@/components/ui/Icon";
import { post } from "@/lib/api-client";
import { formatPoints } from "@/lib/format";
import type { ThemeAttr } from "@/lib/theme";
import { useCrumbs } from "./CrumbsContext";
import { ThemeToggle } from "./ThemeToggle";
import { NotificationBell } from "./NotificationBell";
import { FeedbackButton } from "@/components/feedback/FeedbackButton";

export function Topbar({ streak, points, theme }: { streak?: number; points?: number; theme: ThemeAttr }) {
  const router = useRouter();
  const crumbs = useCrumbs();
  const [q, setQ] = useState("");
  return (
    <header className="topbar app-top">
      <Link href="/inicio" className="brand mobile-brand" aria-label="Lúmen Lab — início"><i className="dot" aria-hidden="true" /><b>LÚMEN</b></Link>
      <Crumbs items={crumbs} />
      <div className="spacer" />
      <form role="search" className="money-wrap top-search" onSubmit={(e) => { e.preventDefault(); router.push(`/salas?q=${encodeURIComponent(q)}`); }}>
        <span className="cur" style={{ left: 12 }}><Icon name="search" /></span>
        <input className="input" style={{ paddingLeft: 40 }} type="search" placeholder="Pesquisar salas e trilhas…" aria-label="Pesquisar"
          value={q} onChange={(e) => setQ(e.target.value)} />
      </form>
      <div className="row" style={{ gap: 8 }}>
        {streak !== undefined && (
          <span className="badge acc" title="Sequência de dias de prática"><Icon name="flame" /><span className="mono">{streak} {streak === 1 ? "dia" : "dias"}</span></span>
        )}
        {points !== undefined && <span className="badge mono">{formatPoints(points)} pts</span>}
        <FeedbackButton compact />
        <NotificationBell />
        <ThemeToggle initial={theme} />
        <button type="button" className="btn ghost icon-btn" aria-label="Terminar sessão"
          onClick={async () => { try { await post("/auth/logout"); } finally { router.push("/entrar"); router.refresh(); } }}>
          <Icon name="logout" />
        </button>
      </div>
    </header>
  );
}

export function Crumbs({ items }: { items: { label: string; href?: string }[] }) {
  return (
    <nav aria-label="Localização" className="crumbs">
      {items.map((c, i) => (
        <span key={i} className="row" style={{ gap: 8 }}>
          {i > 0 && <span aria-hidden="true">/</span>}
          {c.href && i < items.length - 1 ? <Link href={c.href}>{c.label}</Link> : <b aria-current={i === items.length - 1 ? "page" : undefined}>{c.label}</b>}
        </span>
      ))}
    </nav>
  );
}
