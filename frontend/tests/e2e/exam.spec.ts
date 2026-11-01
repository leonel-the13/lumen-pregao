import { expect, test } from "@playwright/test";
import { login } from "./helpers";

/** CON-01: mudar o relógio do cliente não altera o prazo (o cronómetro usa serverNow − clientNow). */
test("o cronómetro do exame segue o relógio do servidor, não o do cliente", async ({ page }) => {
  await page.clock.install({ time: new Date(Date.now() + 3 * 3600 * 1000) });
  await login(page, "ana.lopes@exemplo.ao", "/inicio");
  await page.goto("/exames");
  const resume = page.getByRole("button", { name: "Continuar exame" }).first();
  if (await resume.isVisible().catch(() => false)) await resume.click();
  else {
    await page.getByRole("button", { name: "Começar exame" }).first().click();
    await page.getByRole("button", { name: "Começar", exact: true }).click();
  }
  await expect(page.getByText("Modo concentração")).toBeVisible();
  const timer = page.getByRole("timer");
  await expect(timer).not.toContainText("00:00:00");
  // O relógio do cliente está 3 h adiantado: se o cronómetro o usasse, mostraria 00:00:00.
  await expect(timer).toContainText(/00:[0-5]\d:\d\d/);
  await page.getByRole("button", { name: /Pergunta 2/ }).click();
  await expect(page.getByText("Pergunta 2 de")).toBeVisible();
  await page.getByRole("button", { name: "Terminar e submeter" }).click();
  await expect(page.getByRole("heading", { name: /Submeter com \d+ pergunt/ })).toBeVisible();
  await page.getByRole("button", { name: "Submeter", exact: true }).click();
  await expect(page.getByText("Respostas certas")).toBeVisible({ timeout: 20_000 });
});
