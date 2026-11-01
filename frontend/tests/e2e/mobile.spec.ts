import { expect, test } from "@playwright/test";
import { login } from "./helpers";

test("telemóvel: barra inferior e painel inicial", async ({ page }) => {
  await login(page, "kiala.afonso@exemplo.ao");
  const nav = page.getByRole("navigation", { name: "Navegação rápida" });
  await expect(nav).toBeVisible();
  await expect(nav.getByRole("link", { name: "Trilhas" })).toBeVisible();
  await expect(page.getByRole("heading", { level: 1 })).toContainText("Kiala");
  await nav.getByRole("link", { name: "Ranking" }).click();
  await expect(page).toHaveURL(/rankings/);
});
