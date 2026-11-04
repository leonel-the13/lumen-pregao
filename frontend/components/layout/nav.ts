import type { MessageKey } from "@/lib/i18n/translate";
import type { Role } from "@/lib/types";

export interface NavItem { href: string; label: MessageKey; icon: string; roles?: Role[]; live?: boolean }
export interface NavGroup { label: MessageKey; items: NavItem[] }

/** 3 grupos fixos e «Gestão» só para perfis autorizados. A visibilidade nunca substitui a verificação no servidor. */
export function navGroups(roles: Role[]): NavGroup[] {
  const groups: NavGroup[] = [
    { label: "shell.nav.groups.learn", items: [
      { href: "/inicio", label: "shell.nav.home", icon: "home" },
      { href: "/trilhas", label: "shell.nav.tracks", icon: "route" },
      { href: "/salas", label: "shell.nav.rooms", icon: "door" },
      { href: "/simulacoes", label: "shell.nav.simulations", icon: "building" },
      { href: "/exames", label: "shell.nav.exams", icon: "exam" },
    ] },
    { label: "shell.nav.groups.compete", items: [
      { href: "/competicoes", label: "shell.nav.contests", icon: "trophy", live: true },
      { href: "/rankings", label: "shell.nav.rankings", icon: "chart" },
    ] },
    { label: "shell.nav.groups.me", items: [
      { href: "/portefolio", label: "shell.nav.portfolio", icon: "briefcase" },
      { href: "/perfil", label: "shell.nav.profile", icon: "user" },
      { href: "/definicoes", label: "shell.nav.settings", icon: "cog" },
    ] },
  ];
  const gestao: NavItem[] = ([
    { href: "/autoria", label: "shell.nav.authoring", icon: "pen", roles: ["AUTHOR", "REVIEWER", "ADMIN"] },
    { href: "/juri", label: "shell.nav.jury", icon: "scale", roles: ["JURY"] },
    { href: "/admin", label: "shell.nav.admin", icon: "sliders", roles: ["ADMIN", "CONTEST_ORGANIZER"] },
  ] as NavItem[]).filter((i) => i.roles!.some((r) => roles.includes(r)));
  if (gestao.length) groups.push({ label: "shell.nav.groups.manage", items: gestao });
  return groups;
}
