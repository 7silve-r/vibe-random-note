<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { GripHorizontal, Pause, Play, SkipBack, SkipForward, X, Music2 } from 'lucide-vue-next';
import { usePlayer } from '../stores/player';
const player = usePlayer(),
  panel = ref<HTMLElement>(),
  x = ref(0),
  y = ref(0);
let moving = false,
  offsetX = 0,
  offsetY = 0;
const currentLine = computed(() =>
  player.lyricLoading
    ? '正在载入歌词…'
    : player.lyricError
      ? '歌词暂时无法加载'
      : player.lyrics.timed
        ? player.lyrics.lines[player.activeLyric]?.text || '♪'
        : player.lyrics.plain[0] || player.current?.songName || '',
);
const nextLine = computed(() =>
  player.lyrics.timed
    ? player.lyrics.lines[player.activeLyric + 1]?.text || ''
    : player.lyrics.plain.length
      ? '纯文本歌词 · 可前往音乐页阅读全文'
      : '暂无歌词，让旋律继续',
);
function clamp() {
  x.value = Math.max(8, Math.min(x.value, innerWidth - (panel.value?.offsetWidth || 300) - 8));
  y.value = Math.max(8, Math.min(y.value, innerHeight - (panel.value?.offsetHeight || 150) - 8));
}
function down(event: PointerEvent) {
  moving = true;
  offsetX = event.clientX - x.value;
  offsetY = event.clientY - y.value;
  (event.currentTarget as HTMLElement).setPointerCapture(event.pointerId);
}
function move(event: PointerEvent) {
  if (moving) {
    x.value = event.clientX - offsetX;
    y.value = event.clientY - offsetY;
    clamp();
  }
}
watch(
  () => [player.current?.songId, player.hidden, currentLine.value],
  async () => {
    await nextTick();
    clamp();
  },
);
onMounted(() => {
  x.value = Math.max(8, (innerWidth - 600) / 2);
  y.value = innerHeight - 190;
  clamp();
  window.addEventListener('resize', clamp);
});
onBeforeUnmount(() => window.removeEventListener('resize', clamp));
</script>
<template>
  <aside
    v-if="player.current && !player.hidden"
    ref="panel"
    class="lyric-overlay"
    :style="{ left: `${x}px`, top: `${y}px` }"
    aria-label="悬浮歌词"
  >
    <div class="lyric-overlay-toolbar">
      <button
        class="drag-handle"
        aria-label="拖动歌词"
        @pointerdown="down"
        @pointermove="move"
        @pointerup="moving = false"
        @pointercancel="moving = false"
      >
        <GripHorizontal :size="17" />
      </button>
      <RouterLink :to="`/music/song/${player.current.songId}`" class="truncate"
        >{{ player.current.songName }} · {{ player.current.artistName }}</RouterLink
      >
      <button aria-label="上一首" @click="player.next(-1)"><SkipBack :size="16" /></button>
      <button :aria-label="player.playing ? '暂停' : '播放'" @click="player.toggle">
        <Pause v-if="player.playing" :size="16" /><Play v-else :size="16" />
      </button>
      <button aria-label="下一首" @click="player.next()"><SkipForward :size="16" /></button>
      <button aria-label="隐藏歌词" @click="player.hidden = true"><X :size="16" /></button>
    </div>
    <p class="lyric-current">{{ currentLine }}</p>
    <p v-if="!player.lyricLoading && !player.lyricError" class="lyric-next">{{ nextLine }}</p>
    <button v-if="player.lyricError" class="tiny" @click="player.loadLyrics()">重新加载歌词</button>
  </aside>
  <button
    v-else-if="player.current"
    class="lyric-restore glass"
    aria-label="显示悬浮歌词"
    @click="player.hidden = false"
  >
    <Music2 :size="20" />
  </button>
</template>
