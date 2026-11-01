import { expect, type Page } from "@playwright/test";

export const PASSWORD = process.env.E2E_PASSWORD ?? "lumen-dev-2026!";

export async function login(page: Page, email: string, next = "/inicio") {
  await page.goto(`/entrar?next=${encodeURIComponent(next)}`);
  await page.getByLabel("Email institucional").fill(email);
  await page.getByLabel("Palavra-passe").fill(PASSWORD);
  await page.getByRole("button", { name: "Entrar" }).click();
  await expect(page).toHaveURL(new RegExp(next.replace(/[?]/g, "\\?")));
}

/** «810 000,00 Kz» → 810000 · «14 %» → 14 */
export function num(text: string): number {
  return Number(text.replace(/[^\d,]/g, "").replace(",", "."));
}

export function kz(n: number): string {
  return n.toFixed(2).replace(".", ",");
}
