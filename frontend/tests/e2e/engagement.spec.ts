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

test("o botão do olho mostra e esconde a palavra-passe", async ({ page }) => {
  await page.goto("/entrar");
  await page.waitForLoadState("networkidle");
  const field = page.getByLabel("Palavra-passe", { exact: true });
  await field.fill("segredo-de-teste");
  await expect(field).toHaveAttribute("type", "password");
  await page.getByRole("button", { name: "Mostrar a palavra-passe" }).click();
  await expect(field).toHaveAttribute("type", "text");
  await page.getByRole("button", { name: "Esconder a palavra-passe" }).click();
  await expect(field).toHaveAttribute("type", "password");
});

test("registo no computador, confirmação no telemóvel: o computador entra sozinho", async ({ page, browser, request }) => {
  test.setTimeout(90_000);
  const email = `e2e.${Date.now()}@exemplo.ao`;
  await page.goto("/registo");
  await page.waitForLoadState("networkidle");
  await page.getByLabel("Nome completo").fill("Teste Dois Dispositivos");
  await page.getByLabel("Email institucional").fill(email);
  await page.getByLabel("Curso").selectOption({ label: "Economia" });
  await page.getByLabel("Ano").selectOption("2");
  await page.getByLabel("Palavra-passe", { exact: true }).fill("Mussulo#Balancete-77");
  await page.getByRole("checkbox").check();
  await page.getByRole("button", { name: "Criar conta" }).click();
  await page.waitForURL(/\/verificar/, { timeout: 45_000 });
  await expect(page.getByText("À espera da confirmação…")).toBeVisible();
  // A contagem do «Reenviar» vem do servidor: depois do refresh não volta aos 45 s.
  await page.waitForTimeout(4_000);
  await page.reload();
  await expect(page.getByRole("button", { name: /Reenviar email \(00:(3\d|4[0-2])\)/ })).toBeVisible({ timeout: 10_000 });

  // O link chega ao Mailpit (servidor de email de desenvolvimento) e é aberto «no telemóvel».
  let link: string | undefined;
  for (let i = 0; i < 20 && !link; i++) {
    const list = await (await request.get(`http://localhost:8025/api/v1/search?query=to:${encodeURIComponent(email)}`)).json();
    const id = list.messages?.[0]?.ID;
    if (id) link = ((await (await request.get(`http://localhost:8025/api/v1/message/${id}`)).json()).Text as string).match(/https?:\/\/\S+\/verificar\?token=\S+/)?.[0];
    if (!link) await page.waitForTimeout(1_000);
  }
  expect(link).toBeTruthy();
  const phone = await browser.newContext();
  const phonePage = await phone.newPage();
  await phonePage.goto(link!.replace(/^https?:\/\/[^/]+/, process.env.E2E_BASE_URL ?? "http://localhost:3000"));
  await phonePage.waitForURL(/\/inicio/, { timeout: 45_000 });
  await phone.close();

  // O computador percebe sozinho e entra.
  await page.waitForURL(/\/inicio/, { timeout: 30_000 });
});
