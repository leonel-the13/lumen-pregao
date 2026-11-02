import { expect, test } from "@playwright/test";

test("landing mostra a proposta, o rodapé institucional e alterna o tema", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { level: 1 })).toContainText("Aprender é saber");
  await expect(page.getByRole("contentinfo").getByText("Iniciativa da Associação de Estudantes · Não é um canal oficial do ISPTEC")).toBeVisible();
  await expect(page.locator("html")).toHaveAttribute("data-theme", "dark");
  await page.getByRole("button", { name: "Mudar para modo claro" }).click();
  await expect(page.locator("html")).toHaveAttribute("data-theme", "light");
  await page.reload();
  await expect(page.locator("html")).toHaveAttribute("data-theme", "light");
  await page.getByRole("button", { name: "Mudar para modo escuro" }).click();
});

test("o primeiro Tab mostra o atalho para o conteúdo", async ({ page }) => {
  await page.goto("/");
  await page.keyboard.press("Tab");
  await expect(page.getByRole("link", { name: "Saltar para o conteúdo" })).toBeFocused();
});

test("login com dados errados mostra mensagem genérica", async ({ page }) => {
  await page.goto("/entrar");
  await page.getByLabel("Email institucional").fill("ninguem@exemplo.ao");
  await page.getByLabel("Palavra-passe", { exact: true }).fill("qualquer-coisa-123");
  await page.getByRole("button", { name: "Entrar" }).click();
  await expect(page.getByText("Verifica o email e a palavra-passe. Após 5 tentativas, espera 15 min.")).toBeVisible();
});

test("rotas autenticadas sem sessão levam à página de entrada", async ({ page }) => {
  await page.goto("/rankings");
  await expect(page).toHaveURL(/\/entrar\?next=%2Frankings/);
});

test("registo com domínio não permitido é recusado", async ({ page }) => {
  await page.goto("/registo");
  await page.waitForLoadState("networkidle");
  await page.getByLabel("Nome completo").fill("Teste E2E");
  await page.getByLabel("Email institucional").fill("teste@gmail.com");
  await page.getByLabel("Curso").selectOption({ label: "Economia" });
  await page.getByLabel("Ano").selectOption("2");
  await page.getByLabel("Palavra-passe", { exact: true }).fill("Mussulo#Balancete-77");
  await page.getByRole("checkbox").check();
  await page.getByRole("button", { name: "Criar conta" }).click();
  await expect(page.getByText("Só aceitamos o domínio de email institucional do ISPTEC.").last()).toBeVisible();
});

test("«Esqueci-me» responde sempre da mesma forma", async ({ page }) => {
  await page.goto("/entrar");
  await page.waitForLoadState("networkidle");
  await page.getByRole("link", { name: "Esqueci-me" }).click();
  await page.waitForURL(/esqueci/);
  await page.waitForLoadState("networkidle");
  await page.getByLabel("Email", { exact: true }).fill("ninguem@exemplo.ao");
  await page.getByRole("button", { name: "Enviar link" }).click();
  await expect(page.getByText("Se existir uma conta com este email")).toBeVisible();
});

test("link de redefinição inválido mostra o que fazer", async ({ page }) => {
  await page.goto("/redefinir?token=invalido");
  await page.waitForLoadState("networkidle");
  await page.getByLabel("Nova palavra-passe").fill("Mussulo#Balancete-77");
  await page.getByLabel("Confirmar palavra-passe").fill("Mussulo#Balancete-77");
  await page.getByRole("button", { name: "Guardar palavra-passe" }).click();
  await expect(page.getByText("Link inválido ou expirado")).toBeVisible();
});
