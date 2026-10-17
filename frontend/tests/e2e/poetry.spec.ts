import { select } from './select';
import { test, expect, Page } from '@playwright/test';
const first = {
  id: 1,
  title: '相思',
  author: '王维',
  authorId: 1,
  dynasty: '唐',
  kind: '诗',
  dataset: 'tang',
  paragraphs: ['红豆生南国，春来发几枝。', '愿君多采撷，此物最相思。'],
  excerpt: '愿君多采撷，此物最相思。',
  authorDescription: '唐代诗人。',
  sourceRef: '全唐诗/唐诗三百首.json',
  sourceVersion: 'local',
  collected: false,
};
const second = {
  ...first,
  id: 2,
  title: '山居秋暝',
  paragraphs: ['空山新雨后，天气晚来秋。', '明月松间照，清泉石上流。'],
};
const shijing = {
  ...first,
  id: 3,
  title: '国风 · 周南 · 关雎',
  author: '佚名',
  authorId: 3,
  dynasty: '先秦',
  dataset: 'shijing',
  paragraphs: ['关关雎鸠，在河之洲。'],
  sourceRef: '诗经/shijing.json',
};
const catalog = {
  collections: [
    { code: 'tang', name: '唐诗', total: 2 },
    { code: 'shijing', name: '诗经', total: 1 },
  ],
  dynasties: ['唐', '先秦'],
  kinds: ['诗'],
};
async function setup(page: Page, login = false) {
  await page.addInitScript((login) => {
    sessionStorage.setItem('vibe-entered', 'yes');
    if (login) sessionStorage.setItem('vibe-token', 'test');
  }, login);
  const saved = new Set<number>();
  await page.route('**/backend/**', async (route) => {
    const url = new URL(route.request().url()),
      path = url.pathname.replace('/backend', '');
    let data: any = null;
    if (path === '/my/profile')
      data = { id: 1, username: 'reader', nickname: '读者', role: 'USER' };
    else if (path === '/my/categories') data = [{ id: 1, cateName: '日常' }];
    else if (/^\/my\/poetry\/\d+\/favorite$/.test(path)) {
      const id = Number(path.split('/')[3]);
      if (route.request().method() === 'PUT') saved.add(id);
      else saved.delete(id);
    } else if (path === '/public/poetry' || path === '/my/poetry/favorites') {
      let list = [first, second, shijing];
      if (path.startsWith('/my/')) list = list.filter((p) => saved.has(p.id));
      if (url.searchParams.get('dataset'))
        list = list.filter((p) => p.dataset === url.searchParams.get('dataset'));
      if (url.searchParams.get('dynasty'))
        list = list.filter((p) => p.dynasty === url.searchParams.get('dynasty'));
      if (url.searchParams.get('keyword'))
        list = list.filter((p) => p.title.includes(url.searchParams.get('keyword')!));
      data = { list, total: list.length };
    } else if (path === '/public/poetry/catalog') data = catalog;
    else if (path.startsWith('/public/poetry/')) {
      const value = path.endsWith('/3')
        ? shijing
        : path.endsWith('/2') ||
            (path.endsWith('/random') && url.searchParams.get('exclude') === '1')
          ? second
          : first;
      data = { ...value, collected: saved.has(value.id) };
    }
    await route.fulfill({ json: { code: 200, data } });
  });
}

test('encounter history and anonymous collection', async ({ page }) => {
  await setup(page);
  await page.goto('/poetry');
  await expect(page.getByRole('heading', { name: '相思', exact: true })).toBeVisible();
  await page.getByRole('button', { name: '换一首', exact: true }).click();
  await expect(page.getByRole('heading', { name: '山居秋暝' })).toBeVisible();
  await page.getByRole('button', { name: '上一首', exact: true }).click();
  await expect(page.getByRole('heading', { name: '相思', exact: true })).toBeVisible();
  await page.getByRole('button', { name: '收藏', exact: true }).click();
  await expect(page.getByRole('dialog')).toBeVisible();
});

test('search and favorites', async ({ page }) => {
  await setup(page, true);
  await page.goto('/poetry?tab=library');
  await page.getByLabel('搜索诗词').fill('相思');
  await page.getByRole('button', { name: '查找', exact: true }).click();
  await expect(page.locator('.poetry-item')).toHaveCount(1);
  await page.locator('.poetry-item').click();
  await page.getByRole('button', { name: '收藏', exact: true }).click();
  await expect(page.getByRole('button', { name: '已收藏', exact: true })).toHaveAttribute(
    'aria-pressed',
    'true',
  );
  await page.getByRole('link', { name: '我的收藏', exact: true }).click();
  await expect(page.locator('.poetry-item')).toHaveCount(1);
  await page.locator('.poetry-item').click();
  await page.getByRole('button', { name: '已收藏', exact: true }).click();
  await page.getByRole('link', { name: '我的收藏', exact: true }).click();
  await expect(page.getByText('还没有收藏，去遇见一首喜欢的诗吧。')).toBeVisible();
});

test('imported collections drive navigation, filters, favorites and quotes', async ({ page }) => {
  await setup(page, true);
  await page.goto('/poetry');
  await page.locator('.poetry-collections').getByRole('link', { name: /诗经/ }).click();
  await expect(page).toHaveURL(/dataset=shijing/);
  await expect(page.getByRole('combobox', { name: '文集', exact: true })).toContainText('诗经');
  await expect(page.locator('.poetry-item')).toHaveCount(1);
  await page.getByRole('combobox', { name: '朝代', exact: true }).click();
  await expect(page.getByRole('option')).toHaveText(['全部时期', '唐', '先秦']);
  await page.keyboard.press('Escape');
  await select(page, '朝代', '先秦');
  await page.getByRole('button', { name: '查找', exact: true }).click();
  await page.reload();
  await expect(page.getByRole('combobox', { name: '文集', exact: true })).toContainText('诗经');
  await expect(page.getByRole('combobox', { name: '朝代', exact: true })).toContainText('先秦');
  for (const width of [320, 768, 1280]) {
    await page.setViewportSize({ width, height: 844 });
    await page.screenshot({
      path: `test-results/poetry-filters-${width}.png`,
      animations: 'disabled',
    });
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(
      true,
    );
  }
  await page.locator('.poetry-item').click();
  await expect(page.locator('.poem-lines')).toContainText('关关雎鸠');
  await page.getByRole('button', { name: '收藏', exact: true }).click();
  await expect(page.getByRole('button', { name: '已收藏', exact: true })).toBeVisible();
  await page.getByRole('link', { name: '我的收藏', exact: true }).click();
  await select(page, '文集', '诗经');
  await page.getByRole('button', { name: '查找', exact: true }).click();
  await expect(page).toHaveURL(/tab=favorites/);
  await page.locator('.poetry-item').click();
  await page.getByRole('button', { name: '写点感想' }).click();
  await expect(page.getByLabel('书写', { exact: true })).toHaveValue(/关关雎鸠/);
  await expect(page.getByRole('combobox', { name: '可见性', exact: true })).toContainText('私有');
});

test('catalog failure can be retried', async ({ page }) => {
  await setup(page);
  let failed = true;
  await page.route('**/backend/public/poetry/catalog', (route) =>
    route.fulfill({
      status: failed ? 503 : 200,
      json: failed ? { code: 503, message: '文集暂不可用' } : { code: 200, data: catalog },
    }),
  );
  await page.goto('/poetry?tab=library');
  await expect(page.getByRole('alert')).toHaveText('文集暂不可用');
  failed = false;
  await page.getByRole('button', { name: '重新加载', exact: true }).click();
  await page.getByRole('combobox', { name: '文集', exact: true }).click();
  await expect(page.getByRole('option')).toHaveCount(3);
  await page.keyboard.press('Escape');
  await expect(page.locator('.poetry-item')).toHaveCount(3);
});

test('quote starts a private draft without publishing', async ({ page }) => {
  await setup(page, true);
  const writes: string[] = [];
  page.on('request', (request) => {
    if (request.method() === 'POST' && request.url().includes('/my/article'))
      writes.push(request.url());
  });
  await page.goto('/poetry/1');
  await page.getByRole('button', { name: '写点感想' }).click();
  await expect(page.getByLabel('标题', { exact: true })).toHaveValue('读《相思》有感');
  await expect(page.getByLabel('书写', { exact: true })).toHaveValue(/> 红豆生南国/);
  await expect(page.getByRole('combobox', { name: '可见性', exact: true })).toContainText('私有');
  expect(writes).toEqual([]);
  expect(
    await page.evaluate(() => JSON.parse(localStorage.getItem('vibe-draft:1:new')!).content),
  ).toContain('王维');
});

test('quote preserves existing draft', async ({ page }) => {
  await setup(page, true);
  await page.addInitScript(() =>
    localStorage.setItem(
      'vibe-draft:1:new',
      JSON.stringify({ title: '原有草稿', content: '不要丢失的文字', cateId: '1', state: '私有' }),
    ),
  );
  await page.goto('/diary/write?poem=1');
  await expect(page.getByRole('button', { name: '在草稿中引用诗词' })).toBeVisible();
  expect(
    await page.evaluate(() => JSON.parse(localStorage.getItem('vibe-draft:1:new')!).content),
  ).toBe('不要丢失的文字');
  await page.getByRole('button', { name: '在草稿中引用诗词' }).click();
  await expect(page.getByLabel('标题', { exact: true })).toHaveValue('原有草稿');
  await expect(page.getByLabel('书写', { exact: true })).toHaveValue(
    /不要丢失的文字[\s\S]*红豆生南国/,
  );
});

test('poetry responsive reading and request recovery', async ({ page }) => {
  await setup(page);
  await page.goto('/poetry/1');
  await expect(page.locator('.poem-lines')).toContainText('愿君多采撷');
  for (const width of [320, 390, 768, 1280]) {
    await page.setViewportSize({ width, height: 844 });
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(
      true,
    );
  }
  await page.screenshot({ path: 'test-results/poetry-desktop.png', fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({ path: 'test-results/poetry-mobile.png', fullPage: true });
  let failed = true;
  await page.route('**/backend/public/poetry/random', (route) =>
    route.fulfill({
      status: failed ? 503 : 200,
      json: failed ? { code: 503, message: '诗库暂不可用' } : { code: 200, data: first },
    }),
  );
  await page.getByRole('link', { name: '遇见', exact: true }).click();
  await expect(page.getByRole('alert')).toHaveText('诗库暂不可用');
  failed = false;
  await page.getByRole('button', { name: '重新加载', exact: true }).click();
  await expect(page.locator('.poem-lines')).toContainText('红豆生南国');
});
