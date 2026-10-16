<script setup lang="ts">
import { computed, ref, watch, onMounted, onBeforeUnmount } from 'vue';
import { Sun, Cloud, CloudRain, Feather, Check, Trash2 } from 'lucide-vue-next';
import { useAuth } from '../stores/auth';
import { useUi } from '../stores/ui';
import { localDay, type Poem } from '../data/poems';
import { api } from '../api';
const auth = useAuth(),
  ui = useUi();
const now = ref(new Date());
const day = computed(() => localDay(now.value));
const dateLabel = computed(() =>
  new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long',
  }).format(now.value),
);
const poem = ref<Poem | null>(null),
  poemError = ref(''),
  poemLoading = ref(false);
async function loadPoem() {
  poemLoading.value = true;
  poemError.value = '';
  try {
    poem.value = await api<Poem>('/public/poetry/daily');
  } catch {
    poemError.value = '今日诗签暂未送达';
  } finally {
    poemLoading.value = false;
  }
}
watch(day, loadPoem, { immediate: true });
const moods = [
  { name: '晴朗', icon: Sun, color: '#f6e2bf' },
  { name: '平静', icon: Cloud, color: '#d5e3f6' },
  { name: '放空', icon: Feather, color: '#e7dcf5' },
  { name: '微雨', icon: CloudRain, color: '#d8e9e7' },
];
const mood = ref('平静'),
  note = ref(''),
  saved = ref(false);
const key = computed(() => `vibe-moment:${auth.user?.id ?? 'guest'}:${day.value}`);
const tint = computed(() => moods.find((m) => m.name === mood.value)?.color || '#d5e3f6');
watch(
  key,
  () => {
    mood.value = '平静';
    note.value = '';
    saved.value = false;
    try {
      const raw = localStorage.getItem(key.value);
      if (!raw) return;
      const data = JSON.parse(raw);
      if (typeof data?.text === 'string') note.value = data.text.slice(0, 140);
      if (moods.some((m) => m.name === data?.mood)) mood.value = data.mood;
      saved.value = true;
    } catch {
      ui.notify('无法读取本机留笺，你仍可在此书写。', true);
    }
  },
  { immediate: true },
);
function save() {
  try {
    localStorage.setItem(
      key.value,
      JSON.stringify({ mood: mood.value, text: note.value.slice(0, 140) }),
    );
    saved.value = true;
  } catch {
    ui.notify('无法保存到当前浏览器，请检查存储空间或隐私设置。', true);
  }
}
function clear() {
  try {
    localStorage.removeItem(key.value);
    note.value = '';
    mood.value = '平静';
    saved.value = false;
  } catch {
    ui.notify('无法清除本机留笺。', true);
  }
}
let clock: ReturnType<typeof setInterval>;
onMounted(() => {
  clock = setInterval(() => {
    now.value = new Date();
  }, 60000);
});
onBeforeUnmount(() => clearInterval(clock));
</script>
<template>
  <section class="home-studio" :style="{ '--mood-tint': tint }">
    <div class="studio-heading">
      <span class="eyebrow">VIBE RANDOM NOTES / TODAY</span>
      <h1>今日<span>的一隅</span></h1>
      <time :datetime="day">{{ dateLabel }}</time>
    </div>
    <section class="poetry-feature" aria-label="今日诗签">
      <div class="row">
        <span class="eyebrow">今日诗签</span><span class="poem-number">一日 · 一签</span>
      </div>
      <p v-if="poemLoading" class="muted" role="status">正在展开今日诗签…</p>
      <div v-else-if="poemError">
        <p class="muted">{{ poemError }}</p>
        <button @click="loadPoem">再试一次</button>
      </div>
      <template v-else-if="poem">
        <blockquote :key="poem.id" aria-live="polite">
          <p v-for="(line, index) in (poem.excerpt || '').split('\n')" :key="index">{{ line }}</p>
        </blockquote>
        <div class="row wrap">
          <RouterLink :to="'/poetry/' + poem.id" class="poem-credit"
            >{{ poem.author }} ·《{{ poem.title }}》</RouterLink
          >
          <RouterLink :to="'/poetry/' + poem.id" class="poem-next">读完整首 →</RouterLink>
        </div>
      </template>
    </section>
    <span class="studio-mark" aria-hidden="true">{{ day.slice(5).replace('-', ' / ') }}</span>
  </section>
  <section class="moment" :style="{ '--mood-tint': tint }">
    <div class="moment-heading">
      <span class="eyebrow">A MOMENT / {{ day.slice(5).replace('-', '.') }}</span>
      <h2>心情留笺</h2>
      <p>只留在当前浏览器，不会发布为日记。</p>
      <div class="mood-orb" aria-hidden="true">
        <component :is="moods.find((m) => m.name === mood)?.icon" :size="36" :stroke-width="1" />
      </div>
    </div>
    <form class="moment-form" @submit.prevent="save">
      <div class="mood-options" role="group" aria-label="此刻的心情">
        <button
          v-for="m in moods"
          :key="m.name"
          type="button"
          :aria-pressed="mood === m.name"
          :class="{ selected: mood === m.name }"
          @click="
            mood = m.name;
            saved = false;
          "
        >
          <component :is="m.icon" :size="16" />{{ m.name }}
        </button>
      </div>
      <textarea
        v-model="note"
        maxlength="140"
        rows="3"
        aria-label="今日留笺"
        placeholder="记下此刻的一句话…"
        @input="saved = false"
      ></textarea>
      <div class="row moment-actions">
        <span class="tiny muted" role="status">{{
          saved ? '已保存在本机' : `${note.length} / 140`
        }}</span>
        <div class="row">
          <button type="button" aria-label="清除今日留笺" @click="clear">
            <Trash2 :size="14" /></button
          ><button class="primary" type="submit"><Check :size="14" />留在今天</button>
        </div>
      </div>
    </form>
  </section>
</template>
