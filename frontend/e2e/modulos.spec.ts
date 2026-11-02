import { test, expect, Page } from '@playwright/test';
import { readFileSync } from 'node:fs';

async function entrar(pagina: Page, email: string) {
  await pagina.goto('/');
  await pagina.getByLabel('E-mail', { exact: true }).fill(email);
  await pagina.getByLabel('Senha', { exact: true }).fill(readFileSync(process.env['E2E_CREDENTIAL_FILE']!, 'utf8'));
  await pagina.getByRole('button', { name: 'Entrar no painel' }).click();
  await expect(pagina.getByRole('button', { name: 'Sair', exact: true })).toBeVisible();
}
async function configurar(pagina: Page, candidatos: boolean, agenda: boolean) {
  await pagina.getByRole('button', { name: 'Negócios', exact: true }).click();
  const mudou = await pagina.getByLabel('Habilitar banco de candidatos', { exact: true }).isChecked() !== candidatos
    || await pagina.getByLabel('Habilitar agenda de atendimentos', { exact: true }).isChecked() !== agenda;
  await pagina.getByLabel('Habilitar banco de candidatos', { exact: true }).setChecked(candidatos);
  await pagina.getByLabel('Habilitar agenda de atendimentos', { exact: true }).setChecked(agenda);
  const salvar = pagina.getByRole('button', { name: 'Salvar permissões dos módulos', exact: true });
  if (mudou) {
    await expect(salvar).toBeEnabled();
    const resposta = pagina.waitForResponse(resposta => resposta.request().method() === 'PUT' && resposta.url().endsWith('/modulos'));
    await salvar.click();
    expect((await resposta).status()).toBe(200);
    await expect(pagina.getByRole('status').filter({ hasText: 'Permissões dos módulos salvas' })).toBeVisible();
  }
}

test('permissões por empresa, URLs protegidas, CRUD e preservação ao desabilitar', async ({ page, browser }, informacoes) => {
  test.setTimeout(60000);
  let empresaId = '';
  const contextoAdmin = await browser.newContext({ baseURL: process.env['E2E_BASE_URL'], viewport: { width: 1366, height: 900 } });
  const admin = await contextoAdmin.newPage();
  const erros: string[] = [];
  page.on('pageerror', erro => erros.push(erro.message));
  try {
    await entrar(page, 'browser@example.test');
    const empresa = (await (await page.request.get('/api/empresas')).json())[0];
    empresaId = empresa.id;
    await entrar(admin, 'developer@example.test');
    await admin.getByRole('button', { name: 'Negócios', exact: true }).click();
    await admin.getByRole('combobox', { name: 'Empresa', exact: true }).click();
    await admin.getByRole('option', { name: empresa.nome, exact: true }).click();
    await configurar(admin, false, false);
    await page.reload();
    await expect(page.getByRole('button', { name: 'Banco de candidatos', exact: true })).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Agenda de atendimentos', exact: true })).toHaveCount(0);
    await page.goto(`/empresas/${empresa.id}/candidatos`);
    await expect(page).toHaveURL(/\/$/);
    await expect(page.getByRole('heading', { name: 'Banco de candidatos', exact: true })).toHaveCount(0);
    await page.getByRole('button', { name: 'Meu negócio', exact: true }).click();
    await expect(page.getByLabel('Habilitar banco de candidatos')).toHaveCount(0);
    await expect(page.getByLabel('Habilitar agenda de atendimentos')).toHaveCount(0);
    await configurar(admin, true, false);
    await page.reload();
    await expect(page.getByRole('button', { name: 'Banco de candidatos', exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Agenda de atendimentos', exact: true })).toHaveCount(0);
    await page.getByRole('button', { name: 'Banco de candidatos', exact: true }).click();
    await expect(page).toHaveURL(new RegExp(`/empresas/${empresa.id}/candidatos$`));
    await page.getByRole('button', { name: 'Novo candidato', exact: true }).click();
    const nome = `Candidata ${informacoes.project.name}`;
    await page.getByLabel('Nome completo', { exact: true }).fill(nome);
    await page.getByLabel('E-mail do candidato', { exact: true }).fill('candidata@example.test');
    await page.getByLabel('Cargo pretendido', { exact: true }).fill('Analista');
    await page.getByLabel('Link do currículo', { exact: true }).fill('https://example.test/cv.pdf');
    await page.getByRole('button', { name: 'Salvar candidato', exact: true }).click();
    await expect(page.getByRole('status').filter({ hasText: 'Candidato salvo.' })).toBeVisible();
    const linha = page.getByRole('row').filter({ hasText: nome });
    await linha.getByRole('button', { name: 'Editar', exact: true }).click();
    await page.getByRole('combobox', { name: 'Situação da candidatura', exact: true }).click();
    await page.getByRole('option', { name: 'Aprovado', exact: true }).click();
    await page.getByRole('button', { name: 'Salvar candidato', exact: true }).click();
    await expect(linha).toContainText('Aprovado');
    await page.screenshot({ path: `test-results/${informacoes.project.name}-candidatos.png`, fullPage: true });
    await configurar(admin, true, true);
    await page.reload();
    await page.getByRole('button', { name: 'Agenda de atendimentos', exact: true }).click();
    await page.getByRole('button', { name: 'Novo agendamento', exact: true }).click();
    const pessoa = `Pessoa ${informacoes.project.name}`;
    await page.getByLabel('Nome da pessoa', { exact: true }).fill(pessoa);
    await page.getByLabel('Assunto do atendimento', { exact: true }).fill('Entrevista');
    await page.getByLabel('Início do atendimento', { exact: true }).fill('2026-10-20T10:00');
    await page.getByLabel('Fim do atendimento', { exact: true }).fill('2026-10-20T11:00');
    await page.getByRole('button', { name: 'Salvar agendamento', exact: true }).click();
    await expect(page.getByRole('row').filter({ hasText: pessoa })).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
    await page.screenshot({ path: `test-results/${informacoes.project.name}-agenda.png`, fullPage: true });
    await configurar(admin, false, true);
    await page.goto(`/empresas/${empresa.id}/candidatos`);
    await expect(page).toHaveURL(/\/$/);
    await expect(page.getByRole('button', { name: 'Banco de candidatos', exact: true })).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Agenda de atendimentos', exact: true })).toBeVisible();
    await configurar(admin, false, false);
    await page.goto(`/empresas/${empresa.id}/agenda`);
    await expect(page).toHaveURL(/\/$/);
    const integracao = await page.request.get(`/api/integracoes/empresas/${empresa.id}/agenda`, { headers: { 'X-Token-Modulos': process.env['E2E_MODULES_TOKEN']! } });
    expect(integracao.status()).toBe(403);
    await configurar(admin, true, true);
    await page.goto(`/empresas/${empresa.id}/candidatos`);
    await expect(page.getByRole('row').filter({ hasText: nome })).toContainText('Aprovado');
    await page.goto(`/empresas/${empresa.id}/agenda`);
    await expect(page.getByRole('row').filter({ hasText: pessoa })).toBeVisible();
    expect(erros).toEqual([]);
  } finally {
    if (empresaId) {
      const dados = await (await admin.request.get(`/api/empresas/${empresaId}`)).json();
      const csrf = await (await admin.request.get('/api/auth/csrf')).json();
      await admin.request.put(`/api/superusuario/empresas/${empresaId}/modulos`, {
        headers: { [csrf.headerName]: csrf.token },
        data: { moduloCandidatosHabilitado: false, moduloAgendaHabilitado: false, revisao: dados.revisao },
      });
    }
    await contextoAdmin.close();
  }
});
