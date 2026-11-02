import { expect, test } from "@playwright/test";
import { login } from "./helpers";

test("landing: secções novas, termos e créditos discretos", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: /Feito no DCSA/ })).toBeVisible();
  await expect(page.getByText("Perguntas frequentes")).toBeVisible();
  await page.getByText("Quem pode participar?").click();
  await expect(page.getByText(/qualquer curso, com o email institucional de estudante/)).toBeVisible();
  await expect(page.getByText("Desenvolvido por Victor Kangombe")).toBeAttached();
  await page.getByRole("contentinfo").getByRole("link", { name: "Termos" }).click();
  await page.waitForURL(/\/termos$/, { timeout: 45_000 });
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Termos e condições de utilização");
});

test("registo: email isptec sem número de estudante é recusado no browser", async ({ page }) => {
  await page.goto("/registo");
  await page.waitForLoadState("networkidle");
  await page.getByLabel("Nome completo").fill("Teste E2E");
  await page.getByLabel("Email institucional").fill("joao.silva@isptec.co.ao");
  await page.getByRole("button", { name: /Criar conta/ }).click();
  await expect(page.getByText(/começa pelo teu número de 8 dígitos/)).toBeVisible();
});

test("feedback chega à administração e o sino mostra a notificação", async ({ page }) => {
  await login(page, "marta.neto@exemplo.ao");
  await page.getByRole("button", { name: "Dar feedback" }).click();
  const dialog = page.getByRole("dialog", { name: "Dar feedback" });
  await dialog.getByRole("radio", { name: "5 estrelas" }).click();
  await dialog.getByLabel("Mensagem").fill("Teste E2E: gostei muito das salas de IVA.");
  await dialog.getByRole("button", { name: "Enviar" }).click();
  await expect(page.getByText("Obrigado pelo feedback!")).toBeVisible();

  // O sino abre o painel (lista ou estado vazio) e fecha com Esc.
  await page.getByRole("button", { name: /^Notificações/ }).click();
  await expect(page.getByRole("region", { name: "Notificações" })).toBeVisible();
  await page.keyboard.press("Escape");
  await expect(page.getByRole("region", { name: "Notificações" })).toBeHidden();
});

test("sala: o estudante avalia depois de iniciar uma tarefa", async ({ page }) => {
  await login(page, "joao.cassoma@exemplo.ao", "/trilhas/fiscalidade-iva");
  await page.getByRole("link", { name: /Natureza/ }).first().click();
  await page.waitForURL(/\/salas\//, { timeout: 45_000 });
  // Abrir a tarefa inicia a tentativa; ao recarregar, a sala já permite avaliar.
  await expect(page.getByText(/Tentativa \d de \d|Concluída ·|Tentativas esgotadas/).first()).toBeVisible({ timeout: 20_000 });
  await page.reload();
  const card = page.locator(".card", { has: page.getByText("Avaliação da sala") });
  await expect(card).toBeVisible({ timeout: 20_000 });
  const change = card.getByRole("button", { name: "Alterar" });
  if (await change.isVisible()) await change.click(); // já avaliada numa execução anterior
  await card.getByRole("radio", { name: "4 estrelas" }).click();
  await card.getByRole("button", { name: "Guardar avaliação" }).click();
  await expect(page.getByText("Obrigado pela avaliação!")).toBeVisible();
});
