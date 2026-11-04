"use client";
import type { ReactNode } from "react";
import { useT } from "@/lib/i18n/client";
import { Icon } from "./Icon";
import { LinkButton } from "./Button";

/** Estados globais (design A3 · 13): vazio, erro com código, acesso negado, sessão expirada, loading. */
export function EmptyState({ title, body, action, icon = "info" }: { title: string; body?: ReactNode; action?: ReactNode; icon?: string }) {
  return (
    <div className="card state-box center" role="status">
      <span className="avatar" style={{ width: 48, height: 48 }}><Icon name={icon} /></span>
      <h2 className="h3">{title}</h2>
      {body && <p className="small muted" style={{ maxWidth: 440 }}>{body}</p>}
      {action}
    </div>
  );
}

export function ErrorState({ title, body, code, action }: { title: string; body?: ReactNode; code?: string; action?: ReactNode }) {
  const t = useT();
  return (
    <div className="card state-box" role="alert">
      <div className="row" style={{ gap: 10 }}><span style={{ color: "var(--neg)" }}><Icon name="warn" /></span><h2 className="h3">{title}</h2></div>
      <p className="small muted">{body ?? t("pages.error.tryLater")}{code && <> {t("pages.error.code")} <span className="mono">{code}</span></>}</p>
      {action}
    </div>
  );
}

export function AccessDenied({ area }: { area?: string }) {
  const t = useT();
  return (
    <div className="card state-box" role="alert">
      <div className="row" style={{ gap: 10 }}><Icon name="lock" /><h2 className="h3">{t("pages.states.reserved", { area: area ?? t("pages.states.thisArea") })}</h2></div>
      <p className="small muted">{t("pages.states.reservedBody")}</p>
      <LinkButton href="/inicio" variant="out">{t("pages.states.home")}</LinkButton>
    </div>
  );
}

export function Skeleton({ height = 16, width = "100%", radius }: { height?: number; width?: number | string; radius?: number }) {
  return <div className="sk" style={{ height, width, borderRadius: radius }} aria-hidden="true" />;
}

export function LoadingBlock({ label, lines = 3 }: { label?: string; lines?: number }) {
  const t = useT();
  return (
    <div className="card stack" aria-busy="true" aria-live="polite">
      <span className="small muted">{label ?? t("common.loading")}</span>
      <Skeleton height={22} width="45%" />
      {Array.from({ length: lines }, (_, i) => <Skeleton key={i} height={14} width={`${90 - i * 12}%`} />)}
    </div>
  );
}

export function LaterPhase({ title, body }: { title: string; body?: ReactNode }) {
  const t = useT();
  return <EmptyState title={title} icon="clock" body={body ?? t("pages.states.later")} />;
}
