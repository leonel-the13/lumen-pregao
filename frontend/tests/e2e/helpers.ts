import type { Page } from "@playwright/test";

export const PASSWORD = process.env.E2E_PASSWORD ?? "lumen-dev-2026!";

export async function login(page: Page, email: string, next = "/inicio") {
  await page.goto(`/entrar?next=${encodeURIComponent(next)}`);
  await page.waitForLoadState("networkidle"); // a página tem de estar interactiva antes do clique
  await page.getByLabel("Email institucional").fill(email);
  await page.getByLabel("Palavra-passe").fill(PASSWORD);
  await page.getByRole("button", { name: "Entrar" }).click();
  // a primeira compilação de uma rota no servidor de desenvolvimento pode demorar
  await page.waitForURL((u) => u.pathname === next.split("?")[0], { timeout: 45_000 });
}

/** «810 000,00 Kz» → 810000 · «14 %» → 14 */
export function num(text: string): number {
  return Number(text.replace(/[^\d,]/g, "").replace(",", "."));
}

export function kz(n: number): string {
  return n.toFixed(2).replace(".", ",");
}
