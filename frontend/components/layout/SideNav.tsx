"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { Icon } from "@/components/ui/Icon";
import { Avatar } from "@/components/ui/Avatar";
import { useT } from "@/lib/i18n/client";
import { SIDEBAR_COLLAPSED, SIDEBAR_COOKIE } from "@/lib/sidebar";
import type { Me } from "@/lib/types";
import { Brand } from "./Brand";
import { navGroups } from "./nav";

/** Barra lateral: aberta (ícone e texto) ou fechada (só ícones, com o nome em tooltip). O estado fica em cookie. */
export function SideNav({ me, liveContest, initialCollapsed = false }: { me: Me; liveContest?: boolean; initialCollapsed?: boolean }) {
  const path = usePathname();
  const t = useT();
  const [collapsed, setCollapsed] = useState(initialCollapsed);
  const toggle = () => {
    const next = !collapsed;
    setCollapsed(next);
    document.querySelector(".shell")?.classList.toggle("side-closed", next);
    document.cookie = `${SIDEBAR_COOKIE}=${next ? SIDEBAR_COLLAPSED : "open"}; path=/; max-age=31536000; samesite=lax`;
  };
  return (
    <nav className="side app-side" aria-label={t("shell.nav.label")}>
      <div className="side-head">
        <Brand href="/inicio" />
        <button type="button" className="btn ghost icon-btn side-toggle" onClick={toggle} aria-expanded={!collapsed}
          aria-label={collapsed ? t("shell.nav.expand") : t("shell.nav.collapse")} title={collapsed ? t("shell.nav.expand") : t("shell.nav.collapse")}>
          <Icon name="sidebar" />
        </button>
      </div>
      {navGroups(me.roles).map((g) => (
        <div key={g.label} className="stack" style={{ gap: 4 }}>
          <div className="nav-sec label" aria-hidden="true">{t(g.label)}</div>
          {g.items.map((i) => {
            const on = path === i.href || path.startsWith(i.href + "/");
            const label = t(i.label);
            return (
              <Link key={i.href} href={i.href} className={["nav", on && "on"].filter(Boolean).join(" ")} aria-current={on ? "page" : undefined}
                title={collapsed ? label : undefined}>
                <Icon name={i.icon} />
                <span className="nav-text">{label}</span>
                {i.live && liveContest && <span className="badge acc nav-live" style={{ marginLeft: "auto", height: 20 }}>{t("shell.nav.live")}</span>}
              </Link>
            );
          })}
        </div>
      ))}
      <div className="spacer" />
      <Link href="/perfil" className="card flat side-me" style={{ padding: 12, display: "flex", gap: 10, alignItems: "center", textDecoration: "none", color: "inherit" }}
        title={collapsed ? me.shortName : undefined} aria-label={`${t("shell.nav.profile")} · ${me.shortName}`}>
        <Avatar initials={me.initials} />
        <div className="side-me-text" style={{ minWidth: 0 }}>
          <div className="h3" style={{ fontSize: 14 }}>{me.shortName}</div>
          <div className="small muted">{[me.course?.name, me.year && t("common.yearN", { n: me.year })].filter(Boolean).join(" · ")}</div>
        </div>
      </Link>
    </nav>
  );
}

export function MobileNav() {
  const path = usePathname();
  const t = useT();
  const items = [
    { href: "/inicio", label: t("shell.nav.home"), icon: "home" },
    { href: "/trilhas", label: t("shell.nav.tracks"), icon: "route" },
    { href: "/competicoes", label: t("shell.nav.mobile.compete"), icon: "trophy" },
    { href: "/rankings", label: t("shell.nav.mobile.ranking"), icon: "chart" },
    { href: "/perfil", label: t("shell.nav.mobile.profile"), icon: "user" },
  ];
  return (
    <nav className="mobile-nav" aria-label={t("shell.nav.quick")}>
      {items.map((i) => {
        const on = path === i.href || path.startsWith(i.href + "/") || (i.href === "/perfil" && (path.startsWith("/portefolio") || path.startsWith("/definicoes")));
        return (
          <Link key={i.href} href={i.href} className={on ? "on" : undefined} aria-current={on ? "page" : undefined}>
            <Icon name={i.icon} />{i.label}
          </Link>
        );
      })}
    </nav>
  );
}
