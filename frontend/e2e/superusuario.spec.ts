import { test, expect } from '@playwright/test';
import { readFileSync } from 'node:fs';

test('membro da empresa não vê nem acessa a administração', async ({ page }) => {
  await page.goto('/');
  await page.getByLabel('E-mail', { exact: true }).fill('browser@example.test');
  await page.getByLabel('Senha', { exact: true }).fill(readFileSync(process.env['E2E_CREDENTIAL_FILE']!, 'utf8'));
  await page.getByRole('button', { name: 'Entrar no painel' }).click();
  await expect(page.getByRole('heading', { name: 'Atendimento com a sua identidade' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Superusuário', exact: true })).toHaveCount(0);
  const resposta = await page.request.get('/api/superusuario/prompt-global');
  expect(resposta.status()).toBe(403);
});

test('desenvolvedor sem vínculo salva o prompt, vê a prévia e conta eventos únicos', async ({ page }, informacoes) => {
  const erros: string[] = [];
  page.on('pageerror', erro => erros.push(erro.message));
  await page.goto('/');
  await page.getByLabel('E-mail', { exact: true }).fill('developer@example.test');
  await page.getByLabel('Senha', { exact: true }).fill(readFileSync(process.env['E2E_CREDENTIAL_FILE']!, 'utf8'));
  await page.getByRole('button', { name: 'Entrar no painel' }).click();
  await expect(page.getByRole('button', { name: 'Meu negócio', exact: true })).toHaveCount(0);
  await page.getByRole('button', { name: 'Negócios', exact: true }).click();
  await expect(page.getByRole('combobox', { name: 'Empresa', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Banco de candidatos', exact: true })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Agenda de atendimentos', exact: true })).toHaveCount(0);
  await expect(page.getByLabel('Habilitar banco de candidatos', { exact: true })).toHaveCount(0);
  await expect(page.getByLabel('Habilitar agenda de atendimentos', { exact: true })).toHaveCount(0);
  await expect(page.getByLabel('Nome comercial', { exact: true })).toBeVisible();
  await expect(page.getByRole('combobox', { name: 'Empresa', exact: true }))
    .toContainText(await page.getByLabel('Nome comercial', { exact: true }).inputValue());
  const descricao = `Dados do cliente atualizados pelo superusuário ${informacoes.project.name}`;
  await page.getByLabel('Descrição do negócio', { exact: true }).fill(descricao);
  await page.getByRole('button', { name: 'Salvar informações', exact: true }).click();
  await expect(page.getByRole('status').filter({ hasText: 'Alterações salvas' })).toBeVisible();
  await page.reload();
  await page.getByRole('button', { name: 'Negócios', exact: true }).click();
  await expect(page.getByLabel('Descrição do negócio', { exact: true })).toHaveValue(descricao);
  await page.getByRole('button', { name: 'Superusuário', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Painel do desenvolvedor' })).toBeVisible();
  const regras = `Regra global ${informacoes.project.name}: respeite os limites de atendimento.`;
  await page.getByLabel('System prompt supremo').fill(regras);
  page.once('dialog', dialogo => dialogo.dismiss());
  await page.getByRole('button', { name: 'Negócios', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Painel do desenvolvedor' })).toBeVisible();
  await page.route('**/api/superusuario/prompt-global', async rota => {
    if (rota.request().method() !== 'PUT') return rota.continue();
    return rota.fulfill({ status: 403, contentType: 'application/json', body: '{}' });
  });
  await page.getByRole('button', { name: 'Salvar prompt global', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('suas alterações foram mantidas');
  await expect(page.getByLabel('System prompt supremo')).toHaveValue(regras);
  await page.unroute('**/api/superusuario/prompt-global');
  await page.getByRole('button', { name: 'Salvar prompt global', exact: true }).click();
  await expect(page.getByRole('status').filter({ hasText: 'Prompt global salvo.' })).toBeVisible();
  await page.reload();
  await page.getByRole('button', { name: 'Superusuário', exact: true }).click();
  await expect(page.getByLabel('System prompt supremo')).toHaveValue(regras);
  await page.getByRole('button', { name: 'Ver prompt combinado', exact: true }).click();
  await expect(page.locator('.prompt-previa pre')).toContainText(regras);
  await expect(page.locator('.prompt-previa pre')).toContainText('CONFIGURAÇÃO COMPLEMENTAR DA EMPRESA');
  await page.screenshot({ path: `test-results/${informacoes.project.name}-superusuario-prompt.png`, fullPage: true });
  await page.getByRole('button', { name: 'Métricas', exact: true }).click();
  const antes = await (await page.request.get('/api/superusuario/metricas')).json();
  const evento = {
    empresaId: antes.porEmpresa[0].empresaId,
    enviadoEm: new Date(Date.now() - 2000).toISOString(),
  };
  const opcoes = { headers: { 'X-Token-Metricas': process.env['E2E_METRICS_TOKEN']!, 'Idempotency-Key': informacoes.project.name === 'desktop' ? '111111111111111111111111' : '222222222222222222222222' }, data: evento };
  const primeiro = await page.request.post('/api/integracoes/mensagens', opcoes);
  expect(primeiro.status()).toBe(201);
  const repetido = await page.request.post('/api/integracoes/mensagens', opcoes);
  expect(repetido.status()).toBe(200);
  expect((await repetido.json()).registrado).toBe(false);
  await page.getByRole('button', { name: 'Atualizar métricas', exact: true }).click();
  await expect(page.locator('.metrica-card').filter({ hasText: 'Total de mensagens' }).locator('strong'))
    .toHaveText((antes.totalMensagens + 1).toLocaleString('pt-BR'));
  await expect(page.getByRole('heading', { name: 'Mensagens por empresa' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Histórico do prompt global' })).toHaveCount(0);
  await expect(page.getByText('Erros nas últimas 24h', {exact:true})).toHaveCount(0);
  await page.getByRole('combobox', {name:'Agrupar por', exact:true}).click();
  await page.getByRole('option', {name:'Hora', exact:true}).click();
  const grafico = page.waitForResponse(r => r.url().includes('/api/superusuario/metricas?') && r.url().includes('intervalo=hour'));
  await page.getByRole('button', {name:'Aplicar filtros', exact:true}).click();
  expect((await (await grafico).json()).volume.length).toBeGreaterThanOrEqual(24);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
  await page.screenshot({ path: `test-results/${informacoes.project.name}-superusuario-metricas.png`, fullPage: true });
  await page.getByRole('button', { name: 'Sair', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Entre no painel' })).toBeVisible();
  expect(erros).toEqual([]);
});
