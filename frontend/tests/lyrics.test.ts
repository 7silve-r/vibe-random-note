import { describe, expect, it } from 'vitest';
import { lyricIndex, parseLyrics } from '../src/utils/lyrics';
import { normalizeDate } from '../src/utils/date';

describe('lyrics', () => {
  it('sorts repeated timestamps, merges translations and handles offsets', () => {
    const { lines, timed } = parseLyrics(
      '[ar:歌手]\n[offset:500]\n[00:10.50][00:02.50]一句\n[00:02.50]translation\n[00:04.00]\n',
    );
    expect(timed).toBe(true);
    expect(lines).toEqual([
      { time: 2, text: '一句 / translation' },
      { time: 3.5, text: '' },
      { time: 10, text: '一句' },
    ]);
    expect(lyricIndex(lines, 0)).toBe(-1);
    expect(lyricIndex(lines, 2)).toBe(0);
    expect(lyricIndex(lines, 9)).toBe(1);
    expect(lyricIndex(lines, 99)).toBe(2);
    expect(lyricIndex(lines, 1)).toBe(-1);
  });
  it('keeps plain lyrics and empty files usable', () => {
    expect(parseLyrics('第一行\n第二行')).toEqual({
      lines: [],
      plain: ['第一行', '第二行'],
      timed: false,
    });
    expect(parseLyrics().timed).toBe(false);
    expect(
      parseLyrics('[00:01.1]一\n[00:02.12]二\n[00:03.123]三').lines.map((line) => line.time),
    ).toEqual([1.1, 2.12, 3.123]);
  });
});
it('normalizes typed dates and rejects impossible dates', () => {
  expect(normalizeDate('2024/2/29')).toBe('2024-02-29');
  expect(normalizeDate('2026.1.2')).toBe('2026-01-02');
  for (const value of ['2026-02-29', '2026-04-31', '2026-13-01', 'abc', '2026-00-01'])
    expect(normalizeDate(value)).toBeNull();
});
