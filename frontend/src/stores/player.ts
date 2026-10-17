import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { api, media } from '../api';
import { parseLyrics, lyricIndex } from '../utils/lyrics';
import { useUi } from './ui';
export interface Song {
  songId: number;
  songName: string;
  artistName?: string;
  audioUrl?: string;
  coverUrl?: string;
  duration?: string;
}
export const usePlayer = defineStore('player', () => {
  const audio = new Audio();
  audio.preload = 'metadata';
  const queue = ref<Song[]>([]),
    index = ref(-1),
    playing = ref(false),
    time = ref(0),
    duration = ref(0),
    volume = ref(0.65),
    mode = ref<'order' | 'repeat' | 'shuffle'>('order'),
    hidden = ref(false);
  const current = computed(() => queue.value[index.value]);
  const lyric = ref(''),
    lyricLoading = ref(false),
    lyricError = ref('');
  const lyrics = computed(() => parseLyrics(lyric.value));
  const activeLyric = computed(() => lyricIndex(lyrics.value.lines, time.value));
  let lyricRequest = 0;
  async function loadLyrics() {
    const id = current.value?.songId;
    if (!id) return;
    const ticket = ++lyricRequest;
    lyricLoading.value = true;
    lyricError.value = '';
    try {
      const result = await api<{ lyric?: string }>(`/music/public/song/${id}/lyrics`);
      if (ticket === lyricRequest && current.value?.songId === id)
        lyric.value = result?.lyric || '';
    } catch (error) {
      if (ticket === lyricRequest) lyricError.value = (error as Error).message;
    } finally {
      if (ticket === lyricRequest) lyricLoading.value = false;
    }
  }
  let request = 0;
  audio.volume = volume.value;
  audio.addEventListener('play', () => (playing.value = true));
  audio.addEventListener('pause', () => (playing.value = false));
  audio.addEventListener('timeupdate', () => (time.value = audio.currentTime));
  audio.addEventListener(
    'loadedmetadata',
    () => (duration.value = Number.isFinite(audio.duration) ? audio.duration : 0),
  );
  audio.addEventListener('ended', () => {
    if (mode.value === 'repeat') {
      audio.currentTime = 0;
      resume();
    } else next();
  });
  audio.addEventListener('error', () => {
    playing.value = false;
    useUi().notify('这首音乐暂时无法播放，请检查音频是否已上传', true);
  });
  async function resume() {
    try {
      await audio.play();
    } catch (e) {
      if ((e as Error).name !== 'AbortError') useUi().notify('播放未能开始，请再次点击播放', true);
    }
  }
  async function play(song: Song, list?: Song[]) {
    if (!media(song.audioUrl)) {
      useUi().notify('这首歌曲还没有可播放的音频', true);
      return;
    }
    hidden.value = false;
    if (current.value?.songId === song.songId && audio.src === media(song.audioUrl)) {
      if (lyricError.value) void loadLyrics();
      await resume();
      return;
    }
    request++;
    const ticket = request;
    audio.pause();
    queue.value = list?.length ? [...list] : [song];
    index.value = queue.value.findIndex((s) => s.songId === song.songId);
    if (index.value < 0) {
      queue.value.push(song);
      index.value = queue.value.length - 1;
    }
    time.value = 0;
    lyric.value = '';
    void loadLyrics();
    duration.value = 0;
    audio.src = media(song.audioUrl);
    if (ticket === request) await resume();
  }
  function toggle() {
    if (!current.value) return;
    audio.paused ? resume() : audio.pause();
  }
  function next(step = 1) {
    if (!queue.value.length) return;
    let pos = (index.value + step + queue.value.length) % queue.value.length;
    if (mode.value === 'shuffle' && queue.value.length > 1)
      pos =
        (index.value + 1 + Math.floor(Math.random() * (queue.value.length - 1))) %
        queue.value.length;
    play(queue.value[pos], queue.value);
  }
  function seek(value: number) {
    if (Number.isFinite(value) && duration.value > 0) {
      audio.currentTime = Math.min(duration.value, Math.max(0, value));
      time.value = audio.currentTime;
    }
  }
  function setVolume(value: number) {
    volume.value = Math.max(0, Math.min(1, value));
    audio.volume = volume.value;
  }
  function setMode() {
    mode.value = mode.value === 'order' ? 'repeat' : mode.value === 'repeat' ? 'shuffle' : 'order';
  }
  return {
    queue,
    lyrics,
    activeLyric,
    lyricLoading,
    lyricError,
    loadLyrics,
    index,
    playing,
    time,
    duration,
    volume,
    mode,
    hidden,
    current,
    play,
    toggle,
    next,
    seek,
    setVolume,
    setMode,
  };
});
export function clock(seconds: number) {
  return `${Math.floor((seconds || 0) / 60)}:${String(Math.floor((seconds || 0) % 60)).padStart(2, '0')}`;
}
