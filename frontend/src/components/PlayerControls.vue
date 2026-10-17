<script setup lang="ts">
import { ref } from 'vue';
import { Play, Pause, SkipBack, SkipForward, Volume2 } from 'lucide-vue-next';
import { usePlayer, clock } from '../stores/player';
const player = usePlayer(),
  queueOpen = ref(false);
</script>
<template>
  <section v-if="player.current" class="music-controls panel" aria-label="音乐播放控制">
    <div class="row wrap">
      <RouterLink :to="`/music/song/${player.current.songId}`"
        >{{ player.current.songName }} · {{ player.current.artistName }}</RouterLink
      ><button class="tiny" @click="player.hidden = !player.hidden">
        {{ player.hidden ? '显示悬浮歌词' : '隐藏悬浮歌词' }}
      </button>
    </div>
    <input
      aria-label="播放进度"
      type="range"
      min="0"
      :max="player.duration || 1"
      step="0.1"
      :value="player.time"
      @input="player.seek(Number(($event.target as HTMLInputElement).value))"
    />
    <div class="row tiny muted">
      <span>{{ clock(player.time) }}</span
      ><span>{{ clock(player.duration) }}</span>
    </div>
    <div class="controls">
      <button aria-label="上一首" @click="player.next(-1)"><SkipBack :size="20" /></button>
      <button
        class="primary round"
        :aria-label="player.playing ? '暂停' : '播放'"
        @click="player.toggle"
      >
        <Pause v-if="player.playing" :size="22" /><Play v-else :size="22" />
      </button>
      <button aria-label="下一首" @click="player.next()"><SkipForward :size="20" /></button>
      <button class="tiny" @click="player.setMode">
        {{ { order: '顺序循环', repeat: '单曲循环', shuffle: '随机播放' }[player.mode] }}
      </button>
    </div>
    <label class="volume"
      ><Volume2 :size="16" /><input
        aria-label="音量"
        type="range"
        min="0"
        max="1"
        step="0.01"
        :value="player.volume"
        @input="player.setVolume(Number(($event.target as HTMLInputElement).value))"
    /></label>
    <button class="tiny" @click="queueOpen = !queueOpen">
      播放队列 · {{ player.queue.length }}
    </button>
    <div v-if="queueOpen" class="queue">
      <button
        v-for="song in player.queue"
        :key="song.songId"
        :class="{ active: song.songId === player.current.songId }"
        @click="player.play(song, player.queue)"
      >
        {{ song.songName }} · {{ song.artistName }}
      </button>
    </div>
  </section>
</template>
