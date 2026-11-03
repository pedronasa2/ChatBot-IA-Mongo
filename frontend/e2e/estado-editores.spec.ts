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
async function selecionar(page: Page, id: string) {
    const empresas = await (await page.request.get("/api/empresas")).json();
    const nome = empresas.find((e: {id: string}) => e.id === id).nome;
    await page.getByRole("combobox", {name: "Empresa", exact: true}).click();
    await page.getByRole("option", {name: nome, exact: true}).click();
    await expect(page.getByLabel("Nome comercial")).toHaveValue(nome);
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
        {aba: "Bot", campo: "Nome de apresentação", botao: "Salvar rascunho", path: /\/api\/empresas\/[^/]+\/rascunho$/}
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

for (const origem of ["dados", "permissoes"])
    test(`revisão fica associada ao formulário sujo de ${origem}`, async ({page, browser}) => {
        await entrar(page);
        const empresa = (await (await page.request.get("/api/empresas")).json())[0];
        const contexto = await browser.newContext({baseURL: process.env["E2E_BASE_URL"]});
        const admin = await contexto.newPage();
        try {
            await entrar(admin, "developer@example.test");
            await admin.getByRole("button", {name: "Negócios", exact: true}).click();
            await selecionar(admin, empresa.id);
            const atual = await (await admin.request.get(`/api/empresas/${empresa.id}`)).json();
            const flag = admin.getByLabel("Habilitar banco de candidatos", {exact: true});
            if (origem === "dados") await admin.getByLabel("Nome comercial").fill("Edição local ainda não salva");
            else await flag.setChecked(!atual.moduloCandidatosHabilitado);
            const base = await (await page.request.get(`/api/empresas/${empresa.id}`)).json();
            await put(page, `/api/empresas/${empresa.id}`, {...base, descricao: "Descrição da outra sessão " + origem});
            // Focus runs the existing permission refresh immediately, without waiting 15 seconds.
            const consulta = admin.waitForResponse(
                (r) => r.request().method() === "GET" && r.url().endsWith(`/api/empresas/${empresa.id}`)
            );
            await admin.evaluate(() => window.dispatchEvent(new Event("focus")));
            await consulta;
            if (origem === "dados") {
                await flag.setChecked(!atual.moduloCandidatosHabilitado);
                await admin.getByRole("button", {name: "Salvar permissões dos módulos", exact: true}).click();
                await expect(
                    admin.getByRole("status").filter({hasText: "Permissões dos módulos salvas"})
                ).toBeVisible();
                await admin.getByRole("button", {name: "Salvar informações", exact: true}).click();
            } else {
                await admin.getByLabel("Nome comercial").fill("Nome confirmado em revisão nova");
                await admin.getByRole("button", {name: "Salvar informações", exact: true}).click();
                await expect(admin.getByRole("status").filter({hasText: "Alterações salvas"})).toBeVisible();
                await admin.getByRole("button", {name: "Salvar permissões dos módulos", exact: true}).click();
            }
            await expect(admin.getByRole("alert")).toContainText("outra sessão");
            const depois = await (await page.request.get(`/api/empresas/${empresa.id}`)).json();
            expect(depois.descricao).toBe("Descrição da outra sessão " + origem);
        } finally {
            await contexto.close();
        }
    });

test("edição A atrasada não substitui B e histórico do navegador respeita descarte", async ({page}) => {
    await entrar(page, "developer@example.test");
    const e = (await (await page.request.get("/api/empresas")).json())[0];
    const atual = await (await page.request.get(`/api/empresas/${e.id}`)).json();
    await put(page, `/api/superusuario/empresas/${e.id}/modulos`, {
        moduloCandidatosHabilitado: true,
        moduloAgendaHabilitado: true,
        revisao: atual.revisao
    });
    await page.reload();
    await page.getByRole("button", {name: "Banco de candidatos", exact: true}).click();
    const a = {
        id: "11111111-1111-1111-1111-111111111111",
        empresaId: e.id,
        nomeCompleto: "Consulta A",
        email: "",
        telefone: "",
        cargoPretendido: "",
        urlCurriculo: "",
        resumoProfissional: "",
        observacoes: "",
        situacao: "novo",
        arquivado: false,
        revisao: 0,
        recebidoEm: new Date().toISOString(),
        atualizadoEm: new Date().toISOString()
    };
    const b = {...a, id: "22222222-2222-2222-2222-222222222222", nomeCompleto: "Consulta B"};
    const bloqueio = trava(),
        chegou = trava();
    await page.route(`**/api/empresas/${e.id}/candidatos**`, async (route) => {
        const u = new URL(route.request().url());
        if (u.pathname.endsWith(a.id)) {
            chegou.liberar();
            await bloqueio.espera;
            return route.fulfill({json: a});
        }
        if (u.pathname.endsWith(b.id)) return route.fulfill({json: b});
        return route.fulfill({json: {registros: [a, b], total: 2, pagina: 0, tamanho: 20}});
    });
    await page.getByRole("button", {name: "Buscar", exact: true}).click();
    await page
        .getByRole("row")
        .filter({hasText: a.nomeCompleto})
        .getByRole("button", {name: "Editar", exact: true})
        .click();
    await chegou.espera;
    await page
        .getByRole("row")
        .filter({hasText: b.nomeCompleto})
        .getByRole("button", {name: "Editar", exact: true})
        .click();
    await expect(page.getByLabel("Nome completo", {exact: true})).toHaveValue(b.nomeCompleto);
    bloqueio.liberar();
    await expect(page.getByLabel("Nome completo", {exact: true})).toHaveValue(b.nomeCompleto);
    await page.getByLabel("Nome completo", {exact: true}).fill("Rascunho B");
    page.once("dialog", (d) => d.dismiss());
    await page.goBack();
    await expect(page).toHaveURL(new RegExp(`/empresas/${e.id}/candidatos$`));
    await expect(page.getByLabel("Nome completo", {exact: true})).toHaveValue("Rascunho B");
    page.once("dialog", (d) => d.accept());
    await page.goBack();
    await expect(page).toHaveURL(/\/$/);
});

test("prévia ignora resposta da seleção anterior", async ({page}) => {
    await entrar(page, "developer@example.test");
    const empresas = await (await page.request.get("/api/empresas")).json();
    expect(empresas.length).toBeGreaterThan(1);
    const prompt = await (await page.request.get("/api/superusuario/prompt-global")).json();
    await put(page, "/api/superusuario/prompt-global", {
        ...prompt,
        promptSistema: "Regra para caracterização da prévia"
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

test("erro atrasado de carga de empresa não limpa o contexto que veio depois", async ({page}) => {
    await entrar(page, "developer@example.test");
    const empresas = await (await page.request.get("/api/empresas")).json();
    expect(empresas.length).toBeGreaterThan(1);
    const [a, b] = empresas;
    for (const e of [a, b]) {
        const atual = await (await page.request.get(`/api/empresas/${e.id}`)).json();
        await put(page, `/api/superusuario/empresas/${e.id}/modulos`, {
            moduloCandidatosHabilitado: true,
            moduloAgendaHabilitado: true,
            revisao: atual.revisao
        });
    }
    await page.reload();
    await page.getByRole("button", {name: "Banco de candidatos", exact: true}).click();
    await page.getByRole("button", {name: "Agenda de atendimentos", exact: true}).click();
    const bloqueio = trava(),
        chegou = trava();
    await page.route(`**/api/empresas/${b.id}/rascunho`, async (route) => {
        chegou.liberar();
        await bloqueio.espera;
        await route.fulfill({status: 404, json: {mensagem: "Erro da seleção antiga"}});
    });
    await page.getByRole("combobox", {name: "Empresa", exact: true}).click();
    await page.getByRole("option", {name: b.nome, exact: true}).click();
    await chegou.espera;
    await expect(page).toHaveURL(new RegExp(`/empresas/${b.id}/agenda$`));
    await page.goBack();
    await expect(page).toHaveURL(new RegExp(`/empresas/${a.id}/agenda$`));
    await expect(page.getByRole("heading", {name: "Agenda de atendimentos", exact: true})).toBeVisible();
    const resposta = page.waitForResponse((r) => r.url().endsWith(`/api/empresas/${b.id}/rascunho`));
    bloqueio.liberar();
    await resposta;
    await expect(page.getByRole("combobox", {name: "Empresa", exact: true})).toContainText(a.nome);
    await expect(page.getByRole("alert")).toHaveCount(0);
    await expect(page.getByRole("heading", {name: "Agenda de atendimentos", exact: true})).toBeVisible();
});
