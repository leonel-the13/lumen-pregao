import { expect, test } from "@playwright/test";
import { kz, login, num } from "./helpers";

test.describe.configure({ mode: "serial" });

test("sala: lançamento com correcção por linha no servidor e pontos", async ({ page }) => {
  test.setTimeout(120_000); // fluxo longo: login, trilha, sala, duas submissões corrigidas no servidor
  await login(page, "joao.cassoma@exemplo.ao");
  await page.goto("/trilhas/contabilidade-pgc");
  await page.getByRole("link", { name: /Compra de mercadorias com IVA/ }).click();
  await page.getByRole("link", { name: /Registar o lançamento/ }).click();
  await expect(page.getByRole("heading", { name: /Tarefa 3 · Registar o lançamento/ })).toBeVisible();
  const taskState = page.getByText(/Tentativa \d de \d|Concluída ·|Tentativas esgotadas/).first();
  await expect(taskState).toBeVisible();
  // Só faz sentido a partir da 1.ª tentativa (dados de desenvolvimento acabados de semear).
  test.skip(!(await taskState.textContent())?.startsWith("Tentativa 1 de"), "tarefa já tentada nesta base de dados");

  const params = page.locator(".param");
  await expect(params.first()).toBeVisible();
  const texts = await params.allTextContents();
  const valor = num(texts.find((t) => t.includes("Kz"))!);
  const taxa = num(texts.find((t) => t.includes("%"))!);
  const iva = Math.round(valor * taxa) / 100;

  const search = page.getByLabel("Adicionar conta do PGC");
  for (const q of ["26", "34.5.x", "45"]) {
    await search.fill(q);
    await page.getByRole("option").first().click();
  }
  await page.getByLabel("Débito da linha 1").first().fill(kz(valor));
  await page.getByLabel("Débito da linha 2").first().fill(kz(iva));
  await page.getByLabel("Crédito da linha 3").first().fill(kz(valor + iva));
  await expect(page.getByText("Equilibrado").first()).toBeVisible();
  await page.getByRole("button", { name: "Submeter lançamento" }).click();
  await expect(page.getByText("Quase: 1 conta incorrecta")).toBeVisible({ timeout: 15_000 });
  await expect(page.getByText("Conta errada").first()).toBeVisible();

  await page.getByRole("button", { name: "Remover linha 3" }).click();
  await search.fill("fornecedores");
  await page.getByRole("option", { name: /^32 Fornecedores/ }).click();
  await page.getByLabel("Crédito da linha 3").first().fill(kz(valor + iva));
  await page.getByRole("button", { name: "Submeter lançamento" }).click();
  await expect(page.getByText(/Lançamento correcto/)).toBeVisible({ timeout: 15_000 });
  await expect(page.getByText(/\+35 pts/).first()).toBeVisible();
});

test("tarefa numa sala bloqueada não abre", async ({ page }) => {
  await login(page, "ana.lopes@exemplo.ao");
  await page.goto("/trilhas/contabilidade-pgc");
  await expect(page.getByText(/Disponível ao concluir/).first()).toBeVisible();
});
