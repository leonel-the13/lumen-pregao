"use client";
import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import { Icon } from "@/components/ui/Icon";
import { get, post } from "@/lib/api-client";
import { formatRelative } from "@/lib/format";
import { useLocale, useT } from "@/lib/i18n/client";

interface Notification { id: string; kind: string; title: string; body?: string; link?: string; read: boolean; at: string }

const KIND_ICON: Record<string, string> = {
  BADGE: "trophy", TEAM_INVITE: "user", TEAM: "user", CONTEST: "flag", EXAM: "exam", EDITORIAL: "pen",
  LEGAL_ALERT: "scale", FEEDBACK: "chat", SECURITY: "shield", WELCOME: "spark",
};

/** Sino da barra superior: contador de não lidas (actualizado a cada minuto) e painel com as mais recentes. */
export function NotificationBell() {
  const t = useT();
  const locale = useLocale();
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const [items, setItems] = useState<Notification[] | null>(null);
  const wrap = useRef<HTMLDivElement>(null);

  const refreshCount = useCallback(() => {
    get<{ unread: number }>("/me/notifications/unread-count").then((r) => setUnread(r.unread)).catch(() => {});
  }, []);

  useEffect(() => {
    refreshCount();
    const t = setInterval(() => { if (document.visibilityState === "visible") refreshCount(); }, 60_000);
    return () => clearInterval(t);
  }, [refreshCount]);

  useEffect(() => {
    if (!open) return;
    get<{ items: Notification[] }>("/me/notifications?limit=15").then((r) => setItems(r.items)).catch(() => setItems([]));
    const onDoc = (e: MouseEvent) => { if (!wrap.current?.contains(e.target as Node)) setOpen(false); };
    const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") setOpen(false); };
    document.addEventListener("mousedown", onDoc);
    document.addEventListener("keydown", onKey);
    return () => { document.removeEventListener("mousedown", onDoc); document.removeEventListener("keydown", onKey); };
  }, [open]);

  async function markRead(n: Notification) {
    if (n.read) return;
    setItems((xs) => xs?.map((x) => (x.id === n.id ? { ...x, read: true } : x)) ?? null);
    setUnread((u) => Math.max(0, u - 1));
    await post(`/me/notifications/${n.id}/read`).catch(() => {});
  }

  async function readAll() {
    setItems((xs) => xs?.map((x) => ({ ...x, read: true })) ?? null);
    setUnread(0);
    await post("/me/notifications/read-all").catch(() => {});
  }

  return (
    <div className="notif-wrap" ref={wrap}>
      <button type="button" className="btn ghost icon-btn notif-btn" aria-expanded={open} aria-haspopup="true"
        aria-label={unread > 0 ? t("shell.notifications.unreadLabel", { n: unread }) : t("shell.notifications.title")} onClick={() => setOpen((o) => !o)}>
        <Icon name="bell" />
        {unread > 0 && <span className="notif-count mono" aria-hidden="true">{unread > 9 ? "9+" : unread}</span>}
      </button>
      {open && (
        <div className="notif-panel card" role="region" aria-label={t("shell.notifications.title")}>
          <div className="row" style={{ justifyContent: "space-between", padding: "12px 14px", borderBottom: "1px solid var(--line)" }}>
            <b>{t("shell.notifications.title")}</b>
            {unread > 0 && <button type="button" className="btn ghost sm" onClick={readAll}>{t("shell.notifications.markAll")}</button>}
          </div>
          <ul className="notif-list">
            {items === null && <li className="muted small" style={{ padding: 14 }}>{t("common.loading")}</li>}
            {items?.length === 0 && <li className="muted small" style={{ padding: 14 }}>{t("shell.notifications.empty")}</li>}
            {items?.map((n) => {
              const inner = (
                <>
                  <span className="notif-ic"><Icon name={KIND_ICON[n.kind] ?? "info"} /></span>
                  <span className="stack" style={{ gap: 2, minWidth: 0 }}>
                    <b className="notif-title">{n.title}</b>
                    {n.body && <span className="small muted notif-body">{n.body}</span>}
                    <span className="small muted mono">{formatRelative(n.at, Date.now(), locale)}</span>
                  </span>
                  {!n.read && <span className="notif-dot" aria-label={t("shell.notifications.unread")} />}
                </>
              );
              return (
                <li key={n.id} className={n.read ? "" : "unread"}>
                  {n.link ? (
                    <Link href={n.link} className="notif-item" onClick={() => { markRead(n); setOpen(false); }}>{inner}</Link>
                  ) : (
                    <button type="button" className="notif-item" onClick={() => markRead(n)}>{inner}</button>
                  )}
                </li>
              );
            })}
          </ul>
        </div>
      )}
    </div>
  );
}
