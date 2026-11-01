import type { ReactNode } from "react";
import type { Me } from "@/lib/types";
import type { ThemeAttr } from "@/lib/theme";
import { Footer } from "./Footer";
import { GlobalWatchers } from "./GlobalWatchers";
import { MobileNav, SideNav } from "./SideNav";
import { Topbar } from "./Topbar";
import { CrumbsProvider } from "./CrumbsContext";

export function AppShell({ me, theme, streak, points, liveContest, children }:
  { me: Me; theme: ThemeAttr; streak?: number; points?: number; liveContest?: boolean; children: ReactNode }) {
  return (
    <CrumbsProvider>
    <div className="shell">
      <SideNav me={me} liveContest={liveContest} />
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
