"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Icon } from "@/components/ui/Icon";
import { Avatar } from "@/components/ui/Avatar";
import type { Me } from "@/lib/types";
import { Brand } from "./Brand";
import { navGroups } from "./nav";

export function SideNav({ me, liveContest }: { me: Me; liveContest?: boolean }) {
  const path = usePathname();
  return (
    <nav className="side app-side" aria-label="Navegação principal">
      <Brand href="/inicio" />
      {navGroups(me.roles).map((g) => (
        <div key={g.label} className="stack" style={{ gap: 4 }}>
          <div className="nav-sec label" aria-hidden="true">{g.label}</div>
          {g.items.map((i) => {
            const on = path === i.href || path.startsWith(i.href + "/");
            return (
              <Link key={i.href} href={i.href} className={["nav", on && "on"].filter(Boolean).join(" ")} aria-current={on ? "page" : undefined}>
                <Icon name={i.icon} />
                {i.label}
                {i.live && liveContest && <span className="badge acc" style={{ marginLeft: "auto", height: 20 }}>Ao vivo</span>}
              </Link>
            );
          })}
        </div>
      ))}
      <div className="spacer" />
      <div className="card flat" style={{ padding: 12, display: "flex", gap: 10, alignItems: "center" }}>
        <Avatar initials={me.initials} />
        <div style={{ minWidth: 0 }}>
          <div className="h3" style={{ fontSize: 14 }}>{me.shortName}</div>
          <div className="small muted">{[me.course?.name, me.year && `${me.year}.º ano`].filter(Boolean).join(" · ")}</div>
        </div>
      </div>
    </nav>
  );
}

export function MobileNav() {
  const path = usePathname();
  const items = [
    { href: "/inicio", label: "Início", icon: "home" },
    { href: "/trilhas", label: "Trilhas", icon: "route" },
    { href: "/competicoes", label: "Competir", icon: "trophy" },
    { href: "/rankings", label: "Ranking", icon: "chart" },
    { href: "/portefolio", label: "Perfil", icon: "user" },
  ];
  return (
    <nav className="mobile-nav" aria-label="Navegação rápida">
      {items.map((i) => {
        const on = path === i.href || path.startsWith(i.href + "/");
        return (
          <Link key={i.href} href={i.href} className={on ? "on" : undefined} aria-current={on ? "page" : undefined}>
            <Icon name={i.icon} />{i.label}
          </Link>
        );
      })}
    </nav>
  );
}
