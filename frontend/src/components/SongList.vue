<script setup lang="ts">
import StatePanel from './StatePanel.vue';
import { Play, Heart } from 'lucide-vue-next';
import { usePlayer } from '../stores/player';
import { useAuth } from '../stores/auth';
import { useUi } from '../stores/ui';
import { api } from '../api';
import SmartImage from './SmartImage.vue';
defineProps<{ songs: any[] }>();
const emit = defineEmits(['refresh']);
const player = usePlayer(),
  auth = useAuth(),
  ui = useUi();
async function collect(song: any) {
  if (!auth.require()) return;
  await ui.run(
    async () => {
      await api(
        `/music/favorite/${song.collected ? 'cancelCollectSong' : 'collectSong'}?songId=${song.songId}`,
        song.collected ? 'DELETE' : 'POST',
      );
      emit('refresh');
    },
    song.collected ? '已取消收藏' : '已收藏',
  );
}
</script>
<template>
  <StatePanel v-if="!songs.length" text="暂无歌曲。" />
  <div v-for="(song, i) in songs" :key="song.songId" class="song-row">
    <span class="index">{{ String(i + 1).padStart(2, '0') }}</span
    ><SmartImage class="cover" :src="song.coverUrl" /><RouterLink
      class="song-text truncate"
      :to="'/music/song/' + song.songId"
      ><strong>{{ song.songName }}</strong
      ><small>{{ song.artistName || '未知歌手' }} · {{ song.album || '单曲' }}</small></RouterLink
    ><button :aria-label="'播放 ' + song.songName" @click="player.play(song, songs)">
      <Play :size="17" /></button
    ><button :aria-label="song.collected ? '取消收藏歌曲' : '收藏歌曲'" @click="collect(song)">
      <Heart :size="17" :fill="song.collected ? 'currentColor' : 'none'" />
    </button>
  </div>
</template>
