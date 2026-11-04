import { headers } from "next/headers";
import type { ReactNode } from "react";
import { AppShell } from "@/components/layout/AppShell";
import { ThemeSync } from "@/components/layout/ThemeSync";
import { AccessDenied } from "@/components/ui/States";
import { hasAny, requireMe, summary } from "@/lib/session";
import { toAttr } from "@/lib/theme";
import { getI18n } from "@/lib/i18n/server";

/** Área de gestão: a interface verifica o perfil para mostrar o ecrã certo; o backend verifica sempre. */
export default async function GestaoLayout({ children }: { children: ReactNode }) {
  const path = (await headers()).get("x-pathname") ?? "/autoria";
  const [me, s, { t }] = await Promise.all([requireMe(path), summary(path), getI18n()]);
  const allowed = hasAny(me, "AUTHOR", "REVIEWER", "JURY", "CONTEST_ORGANIZER", "ADMIN");
  return (
    <AppShell me={me} theme={toAttr(me.theme)} streak={s.streak} points={s.points} liveContest={s.liveContest}>
      <ThemeSync theme={me.theme} />
      {allowed ? children : <AccessDenied area={t("pages.states.managementArea")} />}
    </AppShell>
  );
}
