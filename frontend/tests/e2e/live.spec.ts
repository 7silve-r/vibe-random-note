import { select } from './select';
import { test, expect } from '@playwright/test';
import { randomBytes } from 'node:crypto';
test.use({ trace: 'off', screenshot: 'off', video: 'off' });
test('live diary', async ({ page, request }) => {
  test.skip(process.env.VIBE_LIVE !== 'true', '需要显式启用本机真实服务测试');
  const username = 'test_' + Date.now(),
    password = randomBytes(18).toString('hex');
  const result = await request.post('/backend/api/reg', {
    data: { username, password, rePassword: password },
  });
  expect(result.ok()).toBe(true);
  const login = await request.post('/backend/api/login', { data: { username, password } });
  const token = (await login.json()).data;
  const headers = { Authorization: 'Bearer ' + token };
  try {
    await request.post('/backend/my/categories', {
      headers,
      data: { cateName: '浏览器测试', cateAlias: 'browser' },
    });
    await page.addInitScript((t) => {
      sessionStorage.setItem('vibe-entered', 'yes');
      sessionStorage.setItem('vibe-token', t);
    }, token);
    await page.goto('/diary/write');
    await page.getByLabel('标题', { exact: true }).fill('浏览器联调 ' + username);
    await page.getByLabel('书写', { exact: true }).fill('# 联调\n\n真实服务记录');
    await select(page, '选择分类', '浏览器测试');
    await page.getByRole('button', { name: '保存日记', exact: true }).click();
    await expect(page).toHaveURL(/diary\?tab=mine/);
    await page.getByRole('link', { name: new RegExp('浏览器联调 ' + username) }).click();
    await expect(page.getByRole('heading', { name: '浏览器联调 ' + username })).toBeVisible();
    await expect(page).toHaveURL(/\/diary\/\d+\?own=1/);
    const id = new URL(page.url()).pathname.split('/').pop();
    const privateRead = await request.get('/backend/public/articles/' + id);
    expect(privateRead.ok()).toBe(false);
    await page.getByRole('button', { name: '编辑日记', exact: true }).click();
    await select(page, '可见性', '公开');
    await page.getByRole('button', { name: '保存日记', exact: true }).click();
    await expect(page).toHaveURL(/diary\?tab=mine/);
    const publicRead = await request.get('/backend/public/articles/' + id);
    expect(publicRead.ok(), await publicRead.text()).toBe(true);
    await page.goto('/diary/' + id);
    await page.getByRole('button', { name: /^点赞/ }).click();
    await expect(page.getByRole('button', { name: /取消点赞/ })).toBeVisible();
    await page.getByLabel('评论内容').fill('真实接口评论');
    await page.getByRole('button', { name: '发布评论' }).click();
    await expect(page.getByText('真实接口评论', { exact: true })).toBeVisible();
    await page.goto('/music');
    await expect(page.getByRole('heading', { name: '听见 · 音乐' })).toBeVisible();
    await expect(page.getByRole('alert')).toHaveCount(0);
  } finally {
    const response = await request.delete('/backend/my/account', { headers });
    expect(response.ok()).toBe(true);
  }
});
