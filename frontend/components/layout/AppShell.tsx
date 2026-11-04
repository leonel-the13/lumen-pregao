import { cookies } from "next/headers";
import type { ReactNode } from "react";
import type { Me } from "@/lib/types";
import type { ThemeAttr } from "@/lib/theme";
import { SIDEBAR_COLLAPSED, SIDEBAR_COOKIE } from "@/lib/sidebar";
import { Footer } from "./Footer";
import { GlobalWatchers } from "./GlobalWatchers";
import { MobileNav, SideNav } from "./SideNav";
import { Topbar } from "./Topbar";
import { CrumbsProvider } from "./CrumbsContext";

export async function AppShell({ me, theme, streak, points, liveContest, children }:
  { me: Me; theme: ThemeAttr; streak?: number; points?: number; liveContest?: boolean; children: ReactNode }) {
  const collapsed = (await cookies()).get(SIDEBAR_COOKIE)?.value === SIDEBAR_COLLAPSED;
  return (
    <CrumbsProvider>
    <div className={collapsed ? "shell side-closed" : "shell"}>
      <SideNav me={me} liveContest={liveContest} initialCollapsed={collapsed} />
      <div className="main">
        <GlobalWatchers />
        <Topbar streak={streak} points={points} theme={theme} />
        <main id="conteudo" className="content" tabIndex={-1}>{children}</main>
        <Footer />
      </div>
      <MobileNav />
    </div>
    </CrumbsProvider>
  );
}
