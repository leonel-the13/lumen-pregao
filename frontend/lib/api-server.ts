import "server-only";
import { cookies, headers } from "next/headers";
import { redirect } from "next/navigation";
import { clientIp } from "./client-ip";
import { ApiError } from "./errors";

const BACKEND = process.env.BACKEND_INTERNAL_URL ?? "http://localhost:8080";

/** Pedido do Server Component ao backend, reencaminhando o cookie de sessão do utilizador. */
export async function serverFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const jar = await cookies();
  const cookie = jar.getAll().map((c) => `${c.name}=${c.value}`).join("; ");
  const incoming = await headers();
  const ip = clientIp(incoming);
  const proxy: Record<string, string> = ip && process.env.PROXY_SECRET ? { "X-Client-IP": ip, "X-Proxy-Secret": process.env.PROXY_SECRET } : {};
  const res = await fetch(`${BACKEND}/api/v1${path}`, {
    ...init,
    headers: { Accept: "application/json", ...proxy, ...(cookie ? { Cookie: cookie } : {}), ...(init?.headers ?? {}) },
    cache: "no-store",
  });
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  const body = text ? JSON.parse(text) : undefined;
  if (!res.ok) throw new ApiError(res.status, body ?? { status: res.status });
  return body as T;
}

/** Como serverFetch, mas sem sessão leva à página de entrada. */
export async function serverFetchOrLogin<T>(path: string, next: string): Promise<T> {
  try {
    return await serverFetch<T>(path);
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) redirect(`/entrar?expirada=1&next=${encodeURIComponent(next)}`);
    throw e;
  }
}

export async function serverFetchOptional<T>(path: string): Promise<T | null> {
  try {
    return await serverFetch<T>(path);
  } catch (e) {
    if (e instanceof ApiError && (e.status === 401 || e.status === 404)) return null;
    throw e;
  }
}
