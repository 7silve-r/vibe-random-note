export interface LyricLine {
  time: number;
  text: string;
}
export function parseLyrics(raw = '') {
  const lines: LyricLine[] = [],
    plain: string[] = [];
  const offset = Number(/\[offset:([+-]?\d+)\]/i.exec(raw)?.[1] || 0) / 1000;
  for (const original of raw.replace(/^\uFEFF/, '').split(/\r?\n/)) {
    const line = original.trim();
    if (!line || /^\[[a-z]+:[^\]]*\]$/i.test(line)) continue;
    const pattern = /\[(\d{1,3}):([0-5]\d)(?:[.:](\d{1,3}))?\]/g;
    const stamps = [...line.matchAll(pattern)];
    const text = line.replace(pattern, '').trim();
    plain.push(text);
    for (const stamp of stamps)
      lines.push({
        time: Math.max(
          0,
          Number(stamp[1]) * 60 + Number(stamp[2]) + Number(`0.${stamp[3] || '0'}`) - offset,
        ),
        text,
      });
  }
  lines.sort((a, b) => a.time - b.time);
  const grouped: LyricLine[] = [];
  for (const line of lines) {
    const previous = grouped.at(-1);
    if (previous?.time === line.time) {
      if (line.text && !previous.text.split(' / ').includes(line.text))
        previous.text = [previous.text, line.text].filter(Boolean).join(' / ');
    } else grouped.push({ ...line });
  }
  return { lines: grouped, plain, timed: grouped.length > 0 };
}
export function lyricIndex(lines: LyricLine[], time: number) {
  let low = 0,
    high = lines.length;
  while (low < high) {
    const middle = (low + high) >>> 1;
    if (lines[middle]!.time <= time) low = middle + 1;
    else high = middle;
  }
  return low - 1;
}
