"use client";
import { ApiError } from "./errors";
import { clientT } from "./i18n/client";

const BASE = process.env.NEXT_PUBLIC_API_BASE ?? "/api/v1";

function csrfToken(): string | undefined {
  return document.cookie.split("; ").find((c) => c.startsWith("XSRF-TOKEN="))?.split("=")[1];
}

async function ensureCsrf(): Promise<string | undefined> {
  let token = csrfToken();
  if (!token) {
    await fetch(`${BASE}/auth/csrf`, { credentials: "same-origin" });
    token = csrfToken();
  }
  return token;
}

export interface RequestOptions {
  idempotencyKey?: string;
  signal?: AbortSignal;
}

/** Pedido do browser: cookie de sessão + X-CSRF-Token nas escritas. 401 leva à página de entrada. */
export async function api<T>(method: string, path: string, body?: unknown, opts: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: "application/json" };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (method !== "GET") {
    const token = await ensureCsrf();
    if (token) headers["X-CSRF-Token"] = decodeURIComponent(token);
  }
  if (opts.idempotencyKey) headers["Idempotency-Key"] = opts.idempotencyKey;
  let res: Response;
  try {
    res = await fetch(`${BASE}${path}`, {
      method, headers, credentials: "same-origin", signal: opts.signal,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    const t = clientT();
    throw new ApiError(0, { code: "NETWORK", title: t("auth.login.offline"), detail: t("common.network") });
  }
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  let parsed: unknown;
  try {
    parsed = text ? JSON.parse(text) : undefined;
  } catch {
    parsed = undefined;
  }
  if (!res.ok) {
    const err = new ApiError(res.status, (parsed as object) ?? { status: res.status });
    if (res.status === 401 && typeof window !== "undefined" && !path.startsWith("/auth/")) {
      window.dispatchEvent(new CustomEvent("lp:session-expired"));
    }
    throw err;
  }
  return parsed as T;
}

export const get = <T,>(path: string) => api<T>("GET", path);
export const post = <T,>(path: string, body?: unknown, opts?: RequestOptions) => api<T>("POST", path, body ?? {}, opts);
export const put = <T,>(path: string, body?: unknown) => api<T>("PUT", path, body);
export const patch = <T,>(path: string, body?: unknown) => api<T>("PATCH", path, body);
export const del = <T,>(path: string, body?: unknown) => api<T>("DELETE", path, body);

export function newIdempotencyKey(): string {
  return crypto.randomUUID();
}
