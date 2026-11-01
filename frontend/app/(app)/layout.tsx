import { headers } from "next/headers";
import type { ReactNode } from "react";
import { AppShell } from "@/components/layout/AppShell";
import { ThemeSync } from "@/components/layout/ThemeSync";
import { requireMe, summary } from "@/lib/session";
import { toAttr } from "@/lib/theme";

export default async function AppLayout({ children }: { children: ReactNode }) {
  const path = (await headers()).get("x-pathname") ?? "/inicio";
  const [me, s] = await Promise.all([requireMe(path), summary(path)]);
  return (
    <AppShell me={me} theme={toAttr(me.theme)} streak={s.streak} points={s.points} liveContest={s.liveContest}>
      <ThemeSync theme={me.theme} />
      {children}
    </AppShell>
  );
}
