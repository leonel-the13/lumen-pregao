import "server-only";
import { serverFetchOrLogin } from "./api-server";
import type { Me } from "./types";

export interface Summary { points: number; streak: number; liveContest: boolean }

export async function requireMe(next: string): Promise<Me> {
  return serverFetchOrLogin<Me>("/me", next);
}

export async function summary(next: string): Promise<Summary> {
  return serverFetchOrLogin<Summary>("/me/summary", next);
}

export function hasAny(me: Me, ...roles: Me["roles"]) {
  return roles.some((r) => me.roles.includes(r));
}
