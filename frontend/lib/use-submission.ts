"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import { get, newIdempotencyKey, post } from "./api-client";
import { ApiError } from "./errors";
import { cookieLocale } from "./i18n/config";
import { MESSAGES } from "./i18n";
import { translator } from "./i18n/translate";
import type { SubmissionAccepted, SubmissionView } from "./types";

/**
 * Submete com Idempotency-Key estável enquanto o pedido não tem resposta (duplo clique não duplica)
 * e acompanha o estado até GRADED/REJECTED. Em FAILED continua a acompanhar: o servidor volta a corrigir.
 */
export function useSubmission(onDone?: (s: SubmissionView) => void) {
  const [current, setCurrent] = useState<SubmissionView | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [sending, setSending] = useState(false);
  const keyRef = useRef<string | null>(null);
  const pollRef = useRef<ReturnType<typeof setTimeout>>(undefined);
  const done = useRef(onDone);
  done.current = onDone;

  const poll = useCallback((id: string, delay = 600) => {
    clearTimeout(pollRef.current);
    pollRef.current = setTimeout(async () => {
      try {
        const s = await get<SubmissionView>(`/submissions/${id}`);
        setCurrent(s);
        if (s.status === "GRADED" || s.status === "REJECTED") done.current?.(s);
        else poll(id, s.status === "FAILED" ? 4000 : Math.min(delay * 1.4, 3000));
      } catch {
        poll(id, 3000);
      }
    }, delay);
  }, []);

  useEffect(() => () => clearTimeout(pollRef.current), []);

  const submit = useCallback(async (url: string, payload: unknown) => {
    if (sending) return;
    setSending(true);
    setError(null);
    keyRef.current ??= newIdempotencyKey();
    try {
      const r = await post<SubmissionAccepted>(url, payload, { idempotencyKey: keyRef.current });
      keyRef.current = null;
      setCurrent({ id: r.submissionId, status: r.status as SubmissionView["status"], receivedAt: r.receivedAt });
      poll(r.submissionId, 400);
    } catch (e) {
      // Erros de rede mantêm a chave: reenviar devolve a submissão original se já tiver chegado.
      if (!(e instanceof ApiError && e.status === 0)) keyRef.current = null;
      setError(e instanceof ApiError ? e : new ApiError(0, { detail: translator(MESSAGES[cookieLocale()])("common.unexpected") }));
    } finally {
      setSending(false);
    }
  }, [sending, poll]);

  const track = useCallback((s: SubmissionView) => {
    setCurrent(s);
    if (s.status === "QUEUED" || s.status === "PROCESSING" || s.status === "FAILED") poll(s.id, 800);
  }, [poll]);

  return { current, error, sending, submit, track, reset: () => { setCurrent(null); setError(null); } };
}
