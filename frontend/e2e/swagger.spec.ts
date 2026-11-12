import {test, expect} from "@playwright/test";
import {readFileSync} from "node:fs";

test("Swagger autentica, consulta a API e encerra a sessão com CSRF", async ({page}) => {
    const erros: string[] = [];
    page.on("pageerror", erro => erros.push(erro.message));
    await page.goto("/swagger-ui/index.html");
    await expect(page.locator(".swagger-ui .info .title")).toContainText("AtendeFlow API");
    expect((await page.request.get("/api/auth/me")).status()).toBe(401);

    const login = page.locator(".opblock").filter({has: page.locator('[data-path="/api/auth/login"]')});
    await login.locator(".opblock-summary").click();
    await login.getByRole("button", {name: "Try it out", exact: true}).click();
    await login.getByPlaceholder("email", {exact: true}).fill("browser@example.test");
    await login.getByPlaceholder("senha", {exact: true}).fill(readFileSync(process.env["E2E_CREDENTIAL_FILE"]!, "utf8"));
    const autenticacao = page.waitForResponse(resposta => resposta.url().endsWith("/api/auth/login") && resposta.request().method() === "POST");
    await login.getByRole("button", {name: "Execute", exact: true}).click();
    expect((await autenticacao).status()).toBe(204);

    const empresas = page.locator(".opblock.opblock-get").filter({has: page.locator('[data-path="/api/empresas"]')});
    await empresas.locator(".opblock-summary").click();
    await empresas.getByRole("button", {name: "Try it out", exact: true}).click();
    const consulta = page.waitForResponse(resposta => resposta.url().endsWith("/api/empresas") && resposta.request().method() === "GET");
    await empresas.getByRole("button", {name: "Execute", exact: true}).click();
    const resultado = await consulta;
    expect(resultado.status()).toBe(200);
    expect((await resultado.json()).length).toBe(1);

    const logout = page.locator(".opblock").filter({has: page.locator('[data-path="/api/auth/logout"]')});
    await logout.locator(".opblock-summary").click();
    await logout.getByRole("button", {name: "Try it out", exact: true}).click();
    const saida = page.waitForResponse(resposta => resposta.url().endsWith("/api/auth/logout") && resposta.request().method() === "POST");
    await logout.getByRole("button", {name: "Execute", exact: true}).click();
    expect((await saida).status()).toBe(204);
    expect((await page.request.get("/api/auth/me")).status()).toBe(401);
    expect(erros).toEqual([]);
});
