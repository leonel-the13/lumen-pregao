export const THEME_COOKIE = "lp_theme";
export type ThemeAttr = "dark" | "light";

export function toAttr(theme: string | undefined | null): ThemeAttr {
  return theme === "LIGHT" || theme === "light" ? "light" : "dark";
}
