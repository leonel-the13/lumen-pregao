"use client";
import { useEffect, useRef, useState } from "react";

export type ContestEvent =
  | { type: "clock"; data: { serverNow: string; state?: string } }
  | { type: "state"; data: { state: string; serverNow: string } }
  | { type: "scoreboard"; data: { serverNow: string } }
  | { type: "submission"; data: { submissionId: string; problemId: string; status: string; correct: boolean } };

/** Liga-se ao SSE do contest (ADR-0001). O EventSource reconecta-se sozinho. */
export function useContestEvents(contestId: string, onEvent: (e: ContestEvent) => void) {
  const handler = useRef(onEvent);
  handler.current = onEvent;
  const [connected, setConnected] = useState(false);
  const [lastEventAt, setLastEventAt] = useState<number | null>(null);

  useEffect(() => {
    const es = new EventSource(`/api/v1/contests/${contestId}/events`, { withCredentials: true });
    const listen = (type: ContestEvent["type"]) => (ev: MessageEvent) => {
      setLastEventAt(Date.now());
      try {
        handler.current({ type, data: JSON.parse(ev.data) } as ContestEvent);
      } catch {
        /* evento mal formado: ignora */
      }
    };
    es.onopen = () => setConnected(true);
    es.onerror = () => setConnected(false);
    (["clock", "state", "scoreboard", "submission"] as const).forEach((t) => es.addEventListener(t, listen(t)));
    return () => es.close();
  }, [contestId]);

  return { connected, lastEventAt };
}
