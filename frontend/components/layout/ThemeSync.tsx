"use client";
import { useEffect } from "react";
import { THEME_COOKIE, toAttr } from "@/lib/theme";

/** Aplica a preferência guardada no perfil (prevalece sobre o cookie do dispositivo). */
export function ThemeSync({ theme }: { theme: string }) {
  useEffect(() => {
    const attr = toAttr(theme);
    if (document.documentElement.dataset.theme !== attr) {
      document.documentElement.dataset.theme = attr;
      document.cookie = `${THEME_COOKIE}=${attr}; path=/; max-age=31536000; samesite=lax`;
    }
  }, [theme]);
  return null;
}
