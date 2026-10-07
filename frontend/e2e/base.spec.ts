import { test, expect } from "@playwright/test";
import { readFileSync } from "node:fs";
test("login, edição, gravação, recarga e logout", async ({
  page,
}, testInfo) => {
  const credentials = process.env["E2E_CREDENTIAL_FILE"];
  if (!credentials)
    throw new Error("Defina E2E_CREDENTIAL_FILE do servidor descartável.");
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto("/");
  await page.getByLabel("E-mail", { exact: true }).fill("browser@example.test");
  await page
    .getByLabel("Senha", { exact: true })
    .fill(readFileSync(credentials, "utf8"));
  await page.getByRole("button", { name: "Entrar no painel" }).click();
  await expect(
    page.getByRole("heading", { name: "Atendimento com a sua identidade" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Meu negócio", exact: true }).click();
  const name = "Oficina Aurora " + testInfo.project.name;
  await page.getByLabel("Nome comercial").fill(name);
  await page
    .getByLabel("Descrição do negócio")
    .fill("Atendimento local com cuidado.");
  await page.getByRole("button", { name: "Salvar informações" }).click();
  await expect(
    page
      .getByRole("status")
      .filter({ hasText: "Alterações salvas no rascunho." }),
  ).toBeVisible();
  await page.reload();
  await page.getByRole("button", { name: "Meu negócio", exact: true }).click();
  await expect(page.getByLabel("Nome comercial")).toHaveValue(name);
  await page.getByRole("button", { name: "Bot", exact: true }).click();
  await page
    .getByLabel("Nome de apresentação")
    .fill("Aurora " + testInfo.project.name);
  await page
    .getByLabel("Instruções adicionais")
    .fill("Responda de forma objetiva.");
  await page.getByRole("button", { name: "Salvar rascunho" }).click();
  await expect(
    page
      .getByRole("status")
      .filter({ hasText: "Alterações salvas no rascunho." }),
  ).toBeVisible();
  await page.reload();
  await page.getByRole("button", { name: "Bot", exact: true }).click();
  await expect(page.getByLabel("Nome de apresentação")).toHaveValue(
    "Aurora " + testInfo.project.name,
  );
  await page.screenshot({
    path: `test-results/${testInfo.project.name}-bot.png`,
    fullPage: true,
  });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
  await page.getByRole("button", { name: "Sair", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Entre no painel" }),
  ).toBeVisible();
  expect(errors).toEqual([]);
});
test("sessão removida retorna ao login com explicação", async ({
  page,
  context,
}) => {
  const file = process.env["E2E_CREDENTIAL_FILE"]!;
  await page.goto("/");
  await page.getByLabel("E-mail", { exact: true }).fill("browser@example.test");
  await page
    .getByLabel("Senha", { exact: true })
    .fill(readFileSync(file, "utf8"));
  await page.getByRole("button", { name: "Entrar no painel" }).click();
  await expect(
    page.getByRole("heading", { name: "Atendimento com a sua identidade" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Meu negócio", exact: true }).click();
  await page.getByLabel("Nome comercial").fill("Alteração antes de expirar");
  await context.clearCookies();
  await page.getByRole("button", { name: "Salvar informações" }).click();
  await expect(
    page.getByRole("heading", { name: "Entre no painel" }),
  ).toBeVisible();
  await expect(page.getByRole("alert")).toContainText("Sua sessão foi encerrada");
});
