<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue';
import {
  Play,
  Pause,
  SkipBack,
  SkipForward,
  Maximize2,
  Volume2,
  GripHorizontal,
  Headphones,
  X,
} from 'lucide-vue-next';
import { usePlayer, clock } from '../stores/player';
import SmartImage from './SmartImage.vue';
const player = usePlayer(),
  expanded = ref(false),
  queueOpen = ref(false),
  dock = ref<HTMLElement>(),
  x = ref(18),
  y = ref(100);
let moving = false,
  offsetX = 0,
  offsetY = 0;
function clamp() {
  x.value = Math.max(8, Math.min(x.value, innerWidth - (dock.value?.offsetWidth || 84) - 8));
  y.value = Math.max(8, Math.min(y.value, innerHeight - (dock.value?.offsetHeight || 84) - 8));
}
function down(e: PointerEvent) {
  moving = true;
  offsetX = e.clientX - x.value;
  offsetY = e.clientY - y.value;
  (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
}
function move(e: PointerEvent) {
  if (moving) {
    x.value = e.clientX - offsetX;
    y.value = e.clientY - offsetY;
    clamp();
  }
}
async function expand() {
  expanded.value = !expanded.value;
  queueOpen.value = false;
  await nextTick();
  clamp();
}
onMounted(() => {
  x.value = innerWidth - 110;
  y.value = innerHeight - 150;
  window.addEventListener('resize', clamp);
});
onBeforeUnmount(() => window.removeEventListener('resize', clamp));
</script>
<template>
  <aside
    v-if="player.current && !player.hidden"
    ref="dock"
    class="player-dock glass"
    :class="{ expanded }"
    :style="{ left: x + 'px', top: y + 'px' }"
    aria-label="全局音乐播放器"
  >
    <button
      class="drag-handle"
      aria-label="拖动播放器"
      @pointerdown="down"
      @pointermove="move"
      @pointerup="moving = false"
      @pointercancel="moving = false"
    >
      <GripHorizontal :size="16" /></button
    ><button
      class="player-cover"
      :aria-expanded="expanded"
      aria-label="展开或收起播放器"
      @click="expand"
    >
      <SmartImage
        :src="player.current.coverUrl"
        :alt="player.current.songName + '的专辑封面'"
      /></button
    ><template v-if="expanded"
      ><div class="row">
        <div class="truncate">
          <strong>{{ player.current.songName }}</strong
          ><small>{{ player.current.artistName }}</small>
        </div>
        <button aria-label="隐藏小窗" @click="player.hidden = true"><X :size="16" /></button>
      </div>
      <input
        aria-label="播放进度"
        type="range"
        min="0"
        :max="player.duration || 1"
        step=".1"
        :value="player.time"
        @input="player.seek(Number(($event.target as HTMLInputElement).value))"
      />
      <div class="row muted tiny">
        <span>{{ clock(player.time) }}</span
        ><span>{{ clock(player.duration) }}</span>
      </div>
      <div class="controls">
        <button aria-label="上一首" @click="player.next(-1)"><SkipBack :size="18" /></button
        ><button
          class="primary round"
          :aria-label="player.playing ? '暂停' : '播放'"
          @click="player.toggle"
        >
          <Pause v-if="player.playing" :size="19" /><Play v-else :size="19" /></button
        ><button aria-label="下一首" @click="player.next()"><SkipForward :size="18" /></button>
      </div>
      <div class="row">
        <button class="tiny" @click="player.setMode">
          {{ { order: '顺序循环', repeat: '单曲循环', shuffle: '随机播放' }[player.mode] }}</button
        ><RouterLink :to="'/music/song/' + player.current.songId" aria-label="进入完整音乐区"
          ><Maximize2 :size="16"
        /></RouterLink>
      </div>
      <label class="volume"
        ><Volume2 :size="15" /><input
          aria-label="音量"
          type="range"
          min="0"
          max="1"
          step=".01"
          :value="player.volume"
          @input="player.setVolume(Number(($event.target as HTMLInputElement).value))" /></label
      ><button class="tiny" @click="queueOpen = !queueOpen">
        播放队列 · {{ player.queue.length }}
      </button>
      <div v-if="queueOpen" class="queue">
        <button
          v-for="song in player.queue"
          :key="song.songId"
          :class="{ active: song.songId === player.current.songId }"
          @click="player.play(song, player.queue)"
        >
          {{ song.songName }}
        </button>
      </div></template
    >
  </aside>
  <button
    v-else-if="player.current"
    class="player-restore glass"
    aria-label="恢复音乐小窗"
    @click="player.hidden = false"
  >
    <Headphones :size="22" />
  </button>
</template>
