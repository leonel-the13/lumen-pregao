"use client";
import { useEffect, useState } from "react";

/**
 * Desvio entre o relógio do servidor e o do cliente (serverNow − clientNow).
 * Os cronómetros apresentam o tempo do servidor; nunca decidem prazos.
 */
export function useServerOffset(serverNowIso: string): number {
  const [offset] = useState(() => new Date(serverNowIso).getTime() - Date.now());
  return offset;
}

/**
 * Instante actual do servidor, actualizado a cada segundo. Devolve null até montar no browser,
 * para que o HTML do servidor e a primeira renderização no cliente coincidam (sem erros de hidratação).
 */
export function useServerNow(offsetMs: number, intervalMs = 1000): number | null {
  const [now, setNow] = useState<number | null>(null);
  useEffect(() => {
    setNow(Date.now() + offsetMs);
    const id = setInterval(() => setNow(Date.now() + offsetMs), intervalMs);
    return () => clearInterval(id);
  }, [offsetMs, intervalMs]);
  return now;
}
