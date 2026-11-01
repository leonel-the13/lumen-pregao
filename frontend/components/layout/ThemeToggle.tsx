"use client";
import { useState } from "react";
import { Icon } from "@/components/ui/Icon";
import { patch } from "@/lib/api-client";
import { THEME_COOKIE, type ThemeAttr } from "@/lib/theme";

/** Alterna escuro/claro: aplica já, guarda em cookie (antes do login) e no perfil (PATCH /me). */
export function ThemeToggle({ initial, persist = true }: { initial: ThemeAttr; persist?: boolean }) {
  const [theme, setTheme] = useState<ThemeAttr>(initial);
  const next: ThemeAttr = theme === "dark" ? "light" : "dark";
  return (
    <button type="button" className="btn ghost icon-btn" aria-label={next === "light" ? "Mudar para modo claro" : "Mudar para modo escuro"}
      onClick={async () => {
        setTheme(next);
        document.documentElement.dataset.theme = next;
        document.cookie = `${THEME_COOKIE}=${next}; path=/; max-age=31536000; samesite=lax`;
        if (persist) {
          try { await patch("/me", { theme: next === "light" ? "LIGHT" : "DARK" }); } catch { /* fica só no cookie */ }
        }
      }}>
      <Icon name={theme === "dark" ? "sun" : "moon"} />
    </button>
  );
}
