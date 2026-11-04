"use client";
import { useEffect, useRef } from "react";
import { Timer } from "@/components/ui/Timer";
import { formatDuration, formatTime } from "@/lib/format";
import { useServerNow, useServerOffset } from "@/lib/server-clock";
import { useLocale, useT } from "@/lib/i18n/client";

/** Contagem decrescente compacta com o relógio do servidor. */
export function ServerCountdown({ target, serverNow, label }: { target: string; serverNow: string; label?: string }) {
  const offset = useServerOffset(serverNow);
  return <Timer target={target} offsetMs={offset} label={label} />;
}

/** Contagem grande da sala de espera (dias · horas · min · seg). */
export function BigCountdown({ target, serverNow, onElapsed }: { target: string; serverNow: string; onElapsed?: () => void }) {
  const t = useT();
  const locale = useLocale();
  const offset = useServerOffset(serverNow);
  const now = useServerNow(offset);
  const left = now === null ? null : Math.max(0, new Date(target).getTime() - now);
  const fired = useRef(false);
  useEffect(() => {
    if (left === 0 && !fired.current) { fired.current = true; onElapsed?.(); }
  }, [left, onElapsed]);
  const parts = left === null ? ["--", "--", "--", "--"] : formatDuration(left, true).replace("d", "").split(/[\s:]+/);
  const [d, h, m, s] = parts.length === 4 ? parts : ["00", ...parts];
  const drift = Math.round(offset / 1000);
  return (
    <div className="stack">
      <div className={["countdown", left !== null && left < 60_000 && "warn"].filter(Boolean).join(" ")} role="timer" aria-label={t("compete.countdown.aria", { d, h, m })}>
        {[[d, t("compete.countdown.days")], [h, t("compete.countdown.hours")], [m, t("compete.countdown.min")], [s, t("compete.countdown.sec")]].map(([v, l]) => (
          <div key={l} className="u"><b>{v}</b><span className="label">{l}</span></div>
        ))}
      </div>
      <p className="small muted">
        {t("compete.countdown.official", { time: formatTime(target, locale) })}
        {now !== null && Math.abs(drift) >= 1 && t("compete.countdown.drift", { n: Math.abs(drift) })}
      </p>
    </div>
  );
}
