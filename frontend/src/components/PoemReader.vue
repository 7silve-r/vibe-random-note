<script setup lang="ts">
import { computed, ref } from 'vue';
import { Bookmark, Copy, PenLine } from 'lucide-vue-next';
import { useRouter } from 'vue-router';
import { api } from '../api';
import { useAuth } from '../stores/auth';
import { useUi } from '../stores/ui';
import type { Poem } from '../data/poems';
const props = defineProps<{ poem: Poem; preview?: boolean }>();
const emit = defineEmits<{ collected: [value: boolean] }>();
const auth = useAuth(),
  ui = useUi(),
  router = useRouter(),
  busy = ref(false);
const lines = computed(() =>
  props.preview ? props.poem.paragraphs.slice(0, 8) : props.poem.paragraphs,
);
const source = computed(() => {
  const revision = /^[a-f0-9]{40}$/i.test(props.poem.sourceVersion)
    ? props.poem.sourceVersion
    : 'master';
  return (
    `https://github.com/chinese-poetry/chinese-poetry/blob/${revision}/` +
    props.poem.sourceRef.split('/').map(encodeURIComponent).join('/')
  );
});
async function collect() {
  if (!auth.require() || busy.value) return;
  busy.value = true;
  const poem = props.poem;
  await ui.run(async () => {
    await api(`/my/poetry/${poem.id}/favorite`, poem.collected ? 'DELETE' : 'PUT');
    if (props.poem.id === poem.id) emit('collected', !poem.collected);
  });
  busy.value = false;
}
async function copy() {
  await ui.run(async () => {
    if (!navigator.clipboard) throw Error('当前浏览器无法复制，请选择诗词文字复制');
    await navigator.clipboard.writeText(
      `${props.poem.title}\n${props.poem.dynasty} · ${props.poem.author}\n\n${props.poem.paragraphs.join('\n')}`,
    );
  }, '诗词已复制');
}
function write() {
  if (auth.require()) router.push({ path: '/diary/write', query: { poem: props.poem.id } });
}
</script>
<template>
  <article class="poem-reader">
    <span class="eyebrow"
      >{{ poem.dynasty }} / {{ poem.kind
      }}<template v-if="poem.rhythmic"> · {{ poem.rhythmic }}</template></span
    >
    <h2>{{ poem.title }}</h2>
    <RouterLink
      class="poem-author"
      :to="{ path: '/poetry', query: { tab: 'library', authorId: poem.authorId } }"
      >{{ poem.author }}</RouterLink
    >
    <div class="poem-lines">
      <p v-for="(line, index) in lines" :key="index">{{ line }}</p>
    </div>
    <RouterLink v-if="preview" class="poem-full-link" :to="'/poetry/' + poem.id"
      >{{ poem.paragraphs.length > 8 ? '继续读完这首诗词' : '查看全文与出处' }} →</RouterLink
    >
    <div class="poem-actions">
      <button :disabled="busy" :aria-pressed="poem.collected" @click="collect">
        <Bookmark :size="17" :fill="poem.collected ? 'currentColor' : 'none'" />{{
          poem.collected ? '已收藏' : '收藏'
        }}
      </button>
      <button @click="copy"><Copy :size="17" />复制诗词</button>
      <button @click="write"><PenLine :size="17" />写点感想</button>
    </div>
    <template v-if="!preview">
      <div v-if="poem.authorDescription" class="poet-description">
        <h3>关于 {{ poem.author }}</h3>
        <p>{{ poem.authorDescription }}</p>
      </div>
      <p class="poetry-source">
        <RouterLink :to="{ path: '/poetry', query: { tab: 'library', dataset: poem.dataset } }"
          >阅读同一文集</RouterLink
        >
        · 正文以简体展示 ·
        <a :href="source" target="_blank" rel="noopener noreferrer">查看原始出处</a>
      </p>
    </template>
  </article>
</template>
