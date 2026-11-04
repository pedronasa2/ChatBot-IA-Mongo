import {test, expect, Page} from "@playwright/test";
import {readFileSync} from "node:fs";

async function entrar(page: Page, email = "browser@example.test") {
    await page.goto("/");
    await page.getByLabel("E-mail", {exact: true}).fill(email);
    await page.getByLabel("Senha", {exact: true}).fill(readFileSync(process.env["E2E_CREDENTIAL_FILE"]!, "utf8"));
    await page.getByRole("button", {name: "Entrar no painel", exact: true}).click();
    await expect(page.getByRole("button", {name: "Sair", exact: true})).toBeVisible();
}
async function put(page: Page, url: string, data: object) {
    const csrf = await (await page.request.get("/api/auth/csrf")).json();
    const response = await page.request.put(url, {headers: {[csrf.headerName]: csrf.token}, data});
    expect(response.status()).toBe(200);
    return response.json();
}
function trava() {
    let liberar!: () => void;
    const espera = new Promise<void>((r) => (liberar = r));
    return {espera, liberar: () => liberar()};
}

test("empresa, bot e prompt bloqueiam campos enquanto a resposta de gravação está pendente", async ({page}) => {
    await entrar(page);
    for (const editor of [
        {aba: "Meu negócio", campo: "Nome comercial", botao: "Salvar informações", path: /\/api\/empresas\/[^/]+$/},
        {aba: "Bot", campo: "Nome de apresentação", botao: "Salvar configurações", path: /\/api\/empresas\/[^/]+\/bot$/}
    ]) {
        await page.getByRole("button", {name: editor.aba, exact: true}).click();
        const campo = page.getByLabel(editor.campo, {exact: true});
        await campo.fill("Edição confirmada " + editor.aba);
        const bloqueio = trava(),
            chegou = trava();
        await page.route(editor.path, async (route) => {
            if (route.request().method() !== "PUT") return route.continue();
            const response = await route.fetch();
            chegou.liberar();
            await bloqueio.espera;
            await route.fulfill({response});
        });
        try {
            await page.getByRole("button", {name: editor.botao, exact: true}).click();
            await chegou.espera;
            await expect(campo).toBeDisabled();
        } finally {
            bloqueio.liberar();
        }
        await expect(campo).toBeEnabled();
        await expect(campo).toHaveValue("Edição confirmada " + editor.aba);
        await page.unroute(editor.path);
    }
    await page.getByRole("button", {name: "Sair", exact: true}).click();
    await expect(page.getByRole("heading", {name: "Entre no painel", exact: true})).toBeVisible();
    await entrar(page, "developer@example.test");
    await page.getByRole("button", {name: "Superusuário", exact: true}).click();
    const campo = page.getByLabel("System prompt supremo");
    await campo.fill("Regra de caracterização pendente");
    const bloqueio = trava(),
        chegou = trava();
    await page.route("**/api/superusuario/prompt-global", async (route) => {
        if (route.request().method() !== "PUT") return route.continue();
        const response = await route.fetch();
        chegou.liberar();
        await bloqueio.espera;
        await route.fulfill({response});
    });
    try {
        await page.getByRole("button", {name: "Salvar prompt global", exact: true}).click();
        await chegou.espera;
        await expect(campo).toBeDisabled();
    } finally {
        bloqueio.liberar();
    }
    await expect(campo).toBeEnabled();
    await expect(campo).toHaveValue("Regra de caracterização pendente");
});

test("prévia ignora resposta da seleção anterior", async ({page}) => {
    await entrar(page, "developer@example.test");
    const empresas = await (await page.request.get("/api/empresas")).json();
    expect(empresas.length).toBeGreaterThan(1);
    const prompt = await (await page.request.get("/api/superusuario/prompt-global")).json();
    await put(page, "/api/superusuario/prompt-global", {
        ...prompt,
        promptGlobal: "Regra para caracterização da prévia"
    });
    await page.getByRole("button", {name: "Superusuário", exact: true}).click();
    const bloqueio = trava(),
        chegou = trava();
    await page.route("**/api/superusuario/prompt-global/previa/*", async (route) => {
        chegou.liberar();
        await bloqueio.espera;
        await route.fulfill({json: {nomeEmpresa: "Anterior", promptComposto: "Texto da seleção anterior"}});
    });
    await page.getByRole("button", {name: "Ver prompt combinado", exact: true}).click();
    await chegou.espera;
    await page.getByRole("combobox", {name: "Empresa da prévia", exact: true}).click();
    const opcoes = page.getByRole("option");
    await opcoes.nth(1).click();
    bloqueio.liberar();
    await expect(page.locator(".prompt-previa pre")).toHaveCount(0);
});

test("edição local mantém a revisão quando outra sessão altera a empresa", async ({page}) => {
    await entrar(page);
    const e = (await (await page.request.get("/api/empresas")).json())[0];
    await page.getByRole("button", {name: "Meu negócio", exact: true}).click();
    await page.getByLabel("Nome comercial").fill("Rascunho local");
    const base = await (await page.request.get(`/api/empresas/${e.id}`)).json();
    await put(page, `/api/empresas/${e.id}`, {...base, descricao: "Descrição da outra sessão"});
    const consulta = page.waitForResponse(r => r.request().method() === "GET" && r.url().endsWith(`/api/empresas/${e.id}`));
    await page.evaluate(() => window.dispatchEvent(new Event("focus")));
    await consulta;
    await expect(page.getByLabel("Nome comercial")).toHaveValue("Rascunho local");
    await page.getByRole("button", {name: "Salvar informações", exact: true}).click();
    await expect(page.getByRole("alert")).toContainText("outra sessão");
    const depois = await (await page.request.get(`/api/empresas/${e.id}`)).json();
    expect(depois.descricao).toBe("Descrição da outra sessão");
});

for (const sujo of ["empresa", "bot"] as const) {
    test(`revisão compartilhada não sobrescreve formulário sujo de ${sujo}`, async ({page}) => {
        await entrar(page);
        if (sujo === "empresa") {
            await page.getByRole("button", {name: "Meu negócio", exact: true}).click();
            await page.getByLabel("Nome comercial").fill("Edição pendente da empresa");
            await page.getByRole("button", {name: "Bot", exact: true}).click();
            await page.getByLabel("Nome de apresentação").fill("Bot confirmado");
            await page.getByRole("button", {name: "Salvar configurações", exact: true}).click();
            await expect(page.getByRole("status").filter({hasText: "Alterações salvas."})).toBeVisible();
            await page.getByRole("button", {name: "Meu negócio", exact: true}).click();
            await expect(page.getByLabel("Nome comercial")).toHaveValue("Edição pendente da empresa");
            await page.getByRole("button", {name: "Salvar informações", exact: true}).click();
        } else {
            await page.getByRole("button", {name: "Bot", exact: true}).click();
            await page.getByLabel("Nome de apresentação").fill("Edição pendente do bot");
            await page.getByRole("button", {name: "Meu negócio", exact: true}).click();
            await page.getByLabel("Nome comercial").fill("Empresa confirmada");
            await page.getByRole("button", {name: "Salvar informações", exact: true}).click();
            await expect(page.getByRole("status").filter({hasText: "Alterações salvas."})).toBeVisible();
            await page.getByRole("button", {name: "Bot", exact: true}).click();
            await expect(page.getByLabel("Nome de apresentação")).toHaveValue("Edição pendente do bot");
            await page.getByRole("button", {name: "Salvar configurações", exact: true}).click();
        }
        await expect(page.getByRole("alert")).toContainText("outra sessão");
        const e = (await (await page.request.get("/api/empresas")).json())[0];
        const atual = await (await page.request.get(`/api/empresas/${e.id}`)).json();
        if (sujo === "empresa") expect(atual.bot.nome).toBe("Bot confirmado");
        else expect(atual.nome).toBe("Empresa confirmada");
    });
}
