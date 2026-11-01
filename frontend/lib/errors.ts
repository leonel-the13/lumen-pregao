import type { Problem } from "./types";

/** Erro de API em application/problem+json, com `code` estável. */
export class ApiError extends Error {
  constructor(public status: number, public problem: Problem) {
    super(problem.detail ?? problem.title ?? `Erro ${status}`);
  }
  get code() {
    return this.problem.code ?? `HTTP_${this.status}`;
  }
  get fields() {
    return this.problem.fields ?? {};
  }
}

export function isApiError(e: unknown): e is ApiError {
  return e instanceof ApiError;
}
