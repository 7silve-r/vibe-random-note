<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue';
import { usePlayer, type Song } from '../stores/player';
import { lyricIndex, parseLyrics } from '../utils/lyrics';
const props = defineProps<{ song: Song & { lyric?: string } }>();
const player = usePlayer(),
  container = ref<HTMLElement>();
const lyrics = computed(() => parseLyrics(props.song.lyric));
const active = computed(() =>
  player.current?.songId === props.song.songId ? lyricIndex(lyrics.value.lines, player.time) : -1,
);
watch(active, async () => {
  await nextTick();
  const line = container.value?.querySelector<HTMLElement>('.active');
  if (line && container.value)
    container.value.scrollTo({
      top:
        line.offsetTop -
        container.value.offsetTop -
        container.value.clientHeight / 2 +
        line.clientHeight / 2,
      behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth',
    });
});
function seek(time: number) {
  if (player.current?.songId === props.song.songId) player.seek(time);
}
</script>
<template>
  <div ref="container" class="lyric-reader" aria-label="歌曲歌词">
    <template v-if="lyrics.timed"
      ><button
        v-for="(line, index) in lyrics.lines"
        :key="index"
        :class="{ active: index === active }"
        :aria-current="index === active ? 'true' : undefined"
        @click="seek(line.time)"
      >
        {{ line.text || '♪' }}
      </button></template
    >
    <template v-else-if="lyrics.plain.length"
      ><p class="tiny muted">纯文本歌词</p>
      <p v-for="(line, index) in lyrics.plain" :key="index">{{ line }}</p></template
    >
    <p v-else class="muted">这首歌还没有歌词。让旋律替它说话。</p>
  </div>
</template>
