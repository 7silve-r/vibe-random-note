import { test, expect, Page } from '@playwright/test';
import { select } from './select';
const artists = [
  { artistId: 1, artistName: '甲歌手' },
  { artistId: 2, artistName: '乙歌手' },
];
const styles = [
  { styleId: 1, name: '流行' },
  { styleId: 2, name: '民谣' },
];
const song = {
  songId: 1,
  songName: '合唱',
  artistName: '甲歌手 / 乙歌手',
  artistIds: [1, 2],
  artists,
  styleIds: [1, 2],
  album: '专辑',
  releaseTime: '2026-10-17',
  audioUrl: 'http://127.0.0.1:5173/lyrics-test.wav',
  lyric: '[00:00.00]第一句\n[00:05.00]第二句\n[00:12.00]第三句',
};
async function setup(page: Page) {
  await page.addInitScript(() => {
    sessionStorage.setItem('vibe-entered', 'yes');
    sessionStorage.setItem('vibe-token', 'test');
  });
  await page.route('**/backend/**', (route) => {
    const path = new URL(route.request().url()).pathname;
    let data: any = { list: [], total: 0 };
    if (path.endsWith('/my/profile'))
      data = { id: 1, username: 'ADMIN', nickname: '管理员', role: 'ADMIN' };
    else if (path.endsWith('/listArtistNames')) data = artists;
    else if (path.endsWith('/styles')) data = styles;
    else if (path.endsWith('/listAdminSongs') || path.endsWith('/listSongs'))
      data = { list: [song], total: 1 };
    else if (path.includes('/getSongDetail/')) data = { ...song, comments: [] };
    else if (path.endsWith('/lyrics')) data = { lyric: song.lyric };
    return route.fulfill({ json: { code: 200, data } });
  });
}
test('song form supports multiple artists and styles, typed dates, and editing', async ({
  page,
}) => {
  await setup(page);
  await page.goto('/admin');
  await page.getByRole('button', { name: '新建歌曲' }).click();
  await page.getByLabel('歌曲名称', { exact: true }).fill('新合唱');
  await page.getByLabel('专辑', { exact: true }).fill('新专辑');
  await select(page, '歌手', '甲歌手');
  await select(page, '歌手', '乙歌手');
  await select(page, '风格', '流行');
  await select(page, '风格', '民谣');
  await expect(page.getByRole('button', { name: '移除甲歌手', exact: true })).toBeVisible();
  await page.getByLabel('发行日期').fill('2026-02-30');
  await page.getByRole('button', { name: '保存', exact: true }).click();
  await expect(page.getByRole('dialog')).toBeVisible();
  await page.getByLabel('发行日期').fill('2026/10/17');
  const saved = page.waitForRequest((r) => r.url().endsWith('/addSong'));
  await page.getByRole('button', { name: '保存', exact: true }).click();
  expect((await saved).postDataJSON()).toMatchObject({
    artistIds: [1, 2],
    styleIds: [1, 2],
    releaseTime: '2026-10-17',
  });
  await expect(page.getByRole('dialog')).toHaveCount(0);
  await page.getByRole('button', { name: '编辑', exact: true }).click();
  await expect(page.getByRole('button', { name: '移除乙歌手' })).toBeVisible();
  await page.getByRole('button', { name: '移除甲歌手' }).click();
  await page.getByRole('button', { name: '移除流行' }).click();
  await page.getByRole('button', { name: '移除民谣' }).click();
  const updated = page.waitForRequest((r) => r.url().endsWith('/updateSong'));
  await page.getByRole('button', { name: '保存', exact: true }).click();
  expect((await updated).postDataJSON()).toMatchObject({ artistIds: [2], styleIds: [] });
});
test('upload lyrics and keep dropdown menus within a small screen', async ({ page }) => {
  await setup(page);
  await page.goto('/admin');
  await page.getByRole('button', { name: '歌词', exact: true }).click();
  await page
    .locator('input[type=file]')
    .setInputFiles({ name: 'song.lrc', mimeType: 'text/plain', buffer: Buffer.from(song.lyric) });
  const request = page.waitForRequest((r) => r.url().endsWith('/songs/1/lyric'));
  await page.getByRole('button', { name: '上传', exact: true }).click();
  expect((await request).method()).toBe('PUT');
  await expect(page.getByRole('dialog')).toHaveCount(0);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.getByRole('button', { name: '新建歌曲' }).click();
  await page.getByRole('combobox', { name: '歌手', exact: true }).click();
  await page.getByLabel('搜索歌手').fill('乙');
  await expect(page.getByRole('option')).toHaveCount(1);
  await page.keyboard.press('ArrowDown');
  await page.keyboard.press('Enter');
  await page.keyboard.press('Escape');
  await expect(page.getByRole('dialog')).toBeVisible();
  await expect(page.getByRole('button', { name: '移除乙歌手' })).toBeVisible();
  await page.getByRole('combobox', { name: '风格', exact: true }).click();
  const box = await page.locator('.select-menu').boundingBox();
  expect(box!.x).toBeGreaterThanOrEqual(0);
  expect(box!.x + box!.width).toBeLessThanOrEqual(390);
  await page.screenshot({ path: 'test-results/music-select-mobile.png', animations: 'disabled' });
});
test('synchronized lyrics seek, pause, drag and survive navigation', async ({ page }) => {
  await setup(page);
  const rate = 8000,
    wav = Buffer.alloc(44 + rate * 30 * 2);
  wav.write('RIFF');
  wav.writeUInt32LE(wav.length - 8, 4);
  wav.write('WAVEfmt ', 8);
  wav.writeUInt32LE(16, 16);
  wav.writeUInt16LE(1, 20);
  wav.writeUInt16LE(1, 22);
  wav.writeUInt32LE(rate, 24);
  wav.writeUInt32LE(rate * 2, 28);
  wav.writeUInt16LE(2, 32);
  wav.writeUInt16LE(16, 34);
  wav.write('data', 36);
  wav.writeUInt32LE(wav.length - 44, 40);
  await page.route('**/lyrics-test.wav', (route) => {
    const range = /bytes=(\d+)-(\d*)/.exec(route.request().headers().range || '');
    const start = range ? Number(range[1]) : 0;
    const end = range?.[2] ? Math.min(Number(range[2]), wav.length - 1) : wav.length - 1;
    return route.fulfill({
      status: range ? 206 : 200,
      contentType: 'audio/wav',
      headers: {
        'Accept-Ranges': 'bytes',
        'Content-Length': String(end - start + 1),
        ...(range ? { 'Content-Range': `bytes ${start}-${end}/${wav.length}` } : {}),
      },
      body: wav.subarray(start, end + 1),
    });
  });
  await page.goto('/music/song/1');
  await page.getByRole('button', { name: '播放', exact: true }).click();
  const overlay = page.getByLabel('悬浮歌词', { exact: true });
  await expect(overlay.locator('.lyric-current')).toHaveText('第一句');
  await overlay.getByRole('button', { name: '暂停', exact: true }).click();
  await expect(page.getByLabel('播放进度')).toHaveAttribute('max', '30');
  await page.getByLabel('播放进度').fill('6');
  await expect(overlay.locator('.lyric-current')).toHaveText('第二句');
  await expect(page.locator('.lyric-reader .active')).toHaveText('第二句');
  await page.getByLabel('播放进度').fill('1');
  await expect(overlay.locator('.lyric-current')).toHaveText('第一句');
  const handle = await page.getByRole('button', { name: '拖动歌词' }).boundingBox();
  await page.mouse.move(handle!.x + 5, handle!.y + 5);
  await page.mouse.down();
  await page.mouse.move(100, 180);
  await page.mouse.up();
  await page.getByRole('link', { name: '日记', exact: true }).click();
  await expect(overlay.locator('.lyric-current')).toHaveText('第一句');
  await page.setViewportSize({ width: 320, height: 844 });
  await page.screenshot({ path: 'test-results/lyrics-overlay-mobile.png', animations: 'disabled' });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await overlay.getByRole('button', { name: '隐藏歌词' }).click();
  await page.getByRole('button', { name: '显示悬浮歌词', exact: true }).click();
  await expect(overlay).toBeVisible();
});
