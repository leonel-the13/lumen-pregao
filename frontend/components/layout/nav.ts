import type { Role } from "@/lib/types";

export interface NavItem { href: string; label: string; icon: string; roles?: Role[]; live?: boolean }
export interface NavGroup { label: string; items: NavItem[] }

/** 3 grupos fixos e «Gestão» só para perfis autorizados. A visibilidade nunca substitui a verificação no servidor. */
export function navGroups(roles: Role[]): NavGroup[] {
  const groups: NavGroup[] = [
    { label: "Aprender", items: [
      { href: "/inicio", label: "Início", icon: "home" },
      { href: "/trilhas", label: "Trilhas", icon: "route" },
      { href: "/salas", label: "Salas", icon: "door" },
      { href: "/simulacoes", label: "Simulações", icon: "building" },
      { href: "/exames", label: "Exames", icon: "exam" },
    ] },
    { label: "Competir", items: [
      { href: "/competicoes", label: "Competições", icon: "trophy", live: true },
      { href: "/rankings", label: "Rankings", icon: "chart" },
    ] },
    { label: "Eu", items: [
      { href: "/portefolio", label: "Portefólio", icon: "briefcase" },
      { href: "/definicoes", label: "Perfil e definições", icon: "user" },
    ] },
  ];
  const gestao: NavItem[] = [
    { href: "/autoria", label: "Autoria", icon: "pen", roles: ["AUTHOR", "REVIEWER", "ADMIN"] },
    { href: "/juri", label: "Júri", icon: "scale", roles: ["JURY"] },
    { href: "/admin", label: "Administração", icon: "sliders", roles: ["ADMIN", "CONTEST_ORGANIZER"] },
  ].filter((i) => i.roles!.some((r) => roles.includes(r as Role))) as NavItem[];
  if (gestao.length) groups.push({ label: "Gestão", items: gestao });
  return groups;
}
