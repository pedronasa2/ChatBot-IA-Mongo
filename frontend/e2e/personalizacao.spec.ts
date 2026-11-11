import {test, expect} from '@playwright/test';
import {readFileSync} from 'node:fs';

test('cores e logo da empresa persistem e podem ser removidos', async ({page}) => {
    await page.goto('/');
    await page.getByLabel('E-mail', {exact:true}).fill('browser@example.test');
    await page.getByLabel('Senha', {exact:true}).fill(readFileSync(process.env['E2E_CREDENTIAL_FILE']!, 'utf8'));
    await page.getByRole('button', {name:'Entrar no painel'}).click();
    await page.getByRole('button', {name:'Meu negócio', exact:true}).click();
    await page.getByLabel('Cor principal').fill('#54256b');
    await page.getByLabel('Cor secundária').fill('#815308');
    const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAYAAABytg0kAAAAEUlEQVR4nGNocFD4D8IMMAYAQLQHfcG590wAAAAASUVORK5CYII=', 'base64');
    await page.getByLabel('Foto da empresa', {exact:true}).setInputFiles({name:'logo.png', mimeType:'image/png', buffer:png});
    await expect(page.locator('.foto-preview')).toBeVisible();
    await page.getByRole('button', {name:'Salvar informações'}).click();
    await expect(page.getByRole('status').filter({hasText:'Alterações salvas.'})).toBeVisible();
    await expect(page.locator('.foto-marca')).toBeVisible();
    await page.reload();
    await page.getByRole('button', {name:'Meu negócio', exact:true}).click();
    await expect(page.getByLabel('Cor principal')).toHaveValue('#54256b');
    expect(await page.evaluate(()=>document.documentElement.style.getPropertyValue('--primary'))).toBe('#54256b');
    await page.getByRole('button', {name:'Restaurar cores padrão'}).click();
    await page.getByRole('button', {name:'Remover foto', exact:true}).click();
    await page.getByRole('button', {name:'Salvar informações'}).click();
    await expect(page.getByRole('status').filter({hasText:'Alterações salvas.'})).toBeVisible();
    await expect(page.locator('.foto-marca')).toHaveCount(0);
});
