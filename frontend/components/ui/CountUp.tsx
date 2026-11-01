"use client";
import { useEffect, useState } from "react";
import { formatPoints } from "@/lib/format";

/** Contagem até ao valor (320 ms, design A5). Sem animação com prefers-reduced-motion. */
export function CountUp({ value, duration = 600 }: { value: number; duration?: number }) {
  const [n, setN] = useState(value);
  useEffect(() => {
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches || value === 0) { setN(value); return; }
    let raf = 0;
    const start = performance.now();
    const tick = (t: number) => {
      const k = Math.min(1, (t - start) / duration);
      setN(Math.round(value * (1 - Math.pow(1 - k, 3))));
      if (k < 1) raf = requestAnimationFrame(tick);
    };
    setN(0);
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [value, duration]);
  return <span className="mono">{formatPoints(n)}</span>;
}
