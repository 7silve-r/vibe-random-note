import { beforeEach, it, expect, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { usePlayer } from '../src/stores/player';
let audio: any;
class AudioMock extends EventTarget {
  src = '';
  paused = true;
  preload = '';
  volume = 1;
  currentTime = 0;
  duration = 100;
  constructor() {
    super();
    audio = this;
  }
  async play() {
    this.paused = false;
    this.dispatchEvent(new Event('play'));
  }
  pause() {
    this.paused = true;
    this.dispatchEvent(new Event('pause'));
  }
}
beforeEach(() => {
  vi.stubGlobal('Audio', AudioMock);
  vi.stubGlobal(
    'fetch',
    vi.fn(async () => ({ ok: true, json: async () => ({ code: 200, data: { lyric: '' } }) })),
  );
  setActivePinia(createPinia());
});
it('queue', async () => {
  const p = usePlayer(),
    songs = [
      { songId: 1, songName: 'A', audioUrl: 'http://a.test/a.wav' },
      { songId: 2, songName: 'B', audioUrl: 'http://a.test/b.wav' },
    ];
  await p.play(songs[0], songs);
  expect(p.playing).toBe(true);
  p.next();
  expect(p.current?.songId).toBe(2);
  p.next(-1);
  expect(p.current?.songId).toBe(1);
});
it('same', async () => {
  const p = usePlayer(),
    song = { songId: 1, songName: 'A', audioUrl: 'http://a.test/a.wav' };
  await p.play(song);
  audio.currentTime = 20;
  await p.play(song);
  expect(audio.currentTime).toBe(20);
});
it('seek', async () => {
  const p = usePlayer();
  audio.dispatchEvent(new Event('loadedmetadata'));
  p.seek(120);
  expect(audio.currentTime).toBe(100);
  p.setVolume(2);
  expect(audio.volume).toBe(1);
});
it('missing', async () => {
  const p = usePlayer();
  await p.play({ songId: 1, songName: 'A' });
  expect(p.current).toBeUndefined();
});
it('lyrics follow seeking and ignore an older song response', async () => {
  let first!: (value: any) => void;
  vi.stubGlobal(
    'fetch',
    vi.fn((url: string) =>
      url.includes('/1/lyrics')
        ? new Promise((resolve) => (first = resolve))
        : Promise.resolve({
            ok: true,
            json: async () => ({
              code: 200,
              data: { lyric: '[00:01.00]第二首第一句\n[00:05.00]第二首第二句' },
            }),
          }),
    ),
  );
  const player = usePlayer();
  await player.play({ songId: 1, songName: 'A', audioUrl: 'http://a.test/a.wav' });
  await player.play({ songId: 2, songName: 'B', audioUrl: 'http://a.test/b.wav' });
  await vi.waitFor(() => expect(player.lyrics.lines).toHaveLength(2));
  first({ ok: true, json: async () => ({ code: 200, data: { lyric: '[00:00.00]旧歌曲' } }) });
  await Promise.resolve();
  await Promise.resolve();
  await Promise.resolve();
  expect(player.lyrics.lines[0]?.text).toBe('第二首第一句');
  audio.dispatchEvent(new Event('loadedmetadata'));
  player.seek(6);
  expect(player.activeLyric).toBe(1);
  player.seek(2);
  expect(player.activeLyric).toBe(0);
  player.toggle();
  expect(player.time).toBe(2);
});
