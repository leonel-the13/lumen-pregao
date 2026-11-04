"use client";
import { useEffect, useRef } from "react";
import { formatDuration } from "@/lib/format";
import { useServerNow } from "@/lib/server-clock";
import { useT } from "@/lib/i18n/client";

/**
 * Cronómetro com o relógio do servidor (offset = serverNow − clientNow). Só apresenta: nunca decide prazos.
 * Os dígitos mudam sem animação; o último minuto ganha anel (warn), os últimos 10 s ficam críticos.
 */
export function Timer({ target, offsetMs, label, withDays = true, onElapsed }:
  { target: string; offsetMs: number; label?: string; withDays?: boolean; onElapsed?: () => void }) {
  const t = useT();
  const now = useServerNow(offsetMs);
  const left = now === null ? null : new Date(target).getTime() - now;
  const fired = useRef(false);
  useEffect(() => {
    if (left !== null && left <= 0 && !fired.current) {
      fired.current = true;
      onElapsed?.();
    }
  }, [left, onElapsed]);
  const cls = left === null ? "" : left <= 10_000 ? "crit" : left <= 60_000 ? "warn" : "";
  return (
    <span className={["timer", cls].filter(Boolean).join(" ")} role="timer" aria-live="off">
      <span className="t">{left === null ? "--:--:--" : formatDuration(left, withDays)}</span>
      <span className="src">{label ?? t("compete.countdown.server")}</span>
    </span>
  );
}
