<script setup lang="ts">
import AppSelect from '../components/AppSelect.vue';
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeft, Shuffle, Search } from 'lucide-vue-next';
import { api, query } from '../api';
import { useAuth } from '../stores/auth';
import type { Poem, PoetryCatalog } from '../data/poems';
import PoemReader from '../components/PoemReader.vue';
import StatePanel from '../components/StatePanel.vue';
import Pager from '../components/Pager.vue';
const route = useRoute(),
  router = useRouter(),
  auth = useAuth();
const tab = computed(() =>
  ['library', 'favorites'].includes(String(route.query.tab))
    ? String(route.query.tab)
    : 'encounter',
);
const detail = computed(() => !!route.params.id);
const poem = ref<Poem | null>(null),
  catalog = ref<PoetryCatalog | null>(null),
  rows = ref<Poem[]>([]),
  total = ref(0),
  loading = ref(false),
  error = ref('');
const keyword = ref(''),
  dynasty = ref(''),
  kind = ref(''),
  dataset = ref(''),
  history = ref<number[]>([]);
const page = computed(() => Math.max(1, Number(route.query.page) || 1));
let request = 0;
async function load(preserve = false) {
  const ticket = ++request;
  error.value = '';
  loading.value = true;
  keyword.value = String(route.query.keyword || '');
  dynasty.value = String(route.query.dynasty || '');
  kind.value = String(route.query.kind || '');
  dataset.value = String(route.query.dataset || '');
  try {
    if (!catalog.value) {
      const result = await api<PoetryCatalog>('/public/poetry/catalog');
      if (ticket !== request) return;
      catalog.value = result;
    }
    if (detail.value || tab.value === 'encounter') {
      const id = detail.value ? route.params.id : preserve ? poem.value?.id : undefined;
      const result = await api<Poem>(id ? `/public/poetry/${id}` : '/public/poetry/random');
      if (ticket === request) {
        poem.value = result;
        if (!preserve) history.value = [];
      }
    } else if (tab.value !== 'favorites' || auth.user) {
      const path = tab.value === 'favorites' ? '/my/poetry/favorites' : '/public/poetry';
      const result = await api<{ list: Poem[]; total: number }>(
        path +
          '?' +
          query({
            pageNum: page.value,
            pageSize: 12,
            keyword: keyword.value,
            dynasty: dynasty.value,
            kind: kind.value,
            dataset: dataset.value,
            authorId: route.query.authorId,
          }),
      );
      if (ticket === request) {
        rows.value = result.list;
        total.value = result.total;
      }
    } else {
      rows.value = [];
      total.value = 0;
    }
  } catch (e) {
    if (ticket === request) error.value = (e as Error).message;
  } finally {
    if (ticket === request) loading.value = false;
  }
}
async function turn(back = false) {
  if (loading.value) return;
  const previous = poem.value?.id;
  const id = back ? history.value.at(-1) : undefined;
  if (back && !id) return;
  const ticket = ++request;
  loading.value = true;
  error.value = '';
  try {
    const result = await api<Poem>(
      back ? `/public/poetry/${id}` : '/public/poetry/random?' + query({ exclude: previous }),
    );
    if (ticket !== request) return;
    if (back) history.value.pop();
    else if (previous) {
      history.value.push(previous);
      if (history.value.length > 30) history.value.shift();
    }
    poem.value = result;
  } catch (e) {
    if (ticket === request) error.value = (e as Error).message;
  } finally {
    if (ticket === request) loading.value = false;
  }
}
function search() {
  router.push({
    path: '/poetry',
    query: {
      tab: tab.value,
      keyword: keyword.value.trim(),
      dynasty: dynasty.value,
      kind: kind.value,
      dataset: dataset.value,
      authorId: route.query.authorId,
    },
  });
}
function changePage(value: number) {
  router.push({ query: { ...route.query, page: value } });
}
watch(
  () => [route.fullPath, auth.user?.id],
  (value, previous) => load(!!previous && value[0] === previous[0]),
  { immediate: true },
);
</script>
<template>
  <header class="section-head">
    <div>
      <span class="eyebrow">POETRY / 诗词</span>
      <h1>字里，遇见</h1>
      <p>翻过一页，让一句诗停在此刻。</p>
    </div>
    <RouterLink v-if="detail" to="/poetry?tab=library" class="poetry-back"
      ><ArrowLeft :size="16" />返回诗库</RouterLink
    >
  </header>
  <nav class="tabs" aria-label="诗词导航">
    <RouterLink to="/poetry" :class="{ active: !detail && tab === 'encounter' }">遇见</RouterLink>
    <RouterLink to="/poetry?tab=library" :class="{ active: !detail && tab === 'library' }"
      >诗库</RouterLink
    >
    <RouterLink to="/poetry?tab=favorites" :class="{ active: !detail && tab === 'favorites' }"
      >我的收藏</RouterLink
    >
  </nav>
  <form
    v-if="!detail && (tab === 'library' || (tab === 'favorites' && auth.user))"
    class="poetry-filters"
    @submit.prevent="search"
  >
    <input v-model="keyword" aria-label="搜索诗词" maxlength="100" placeholder="搜索标题或作者…" />
    <AppSelect
      v-model="dataset"
      label="文集"
      :options="[
        { value: '', label: '全部文集' },
        ...(catalog?.collections || []).map((item) => ({ value: item.code, label: item.name })),
      ]"
    />
    <AppSelect
      v-model="dynasty"
      label="朝代"
      :options="[
        { value: '', label: '全部时期' },
        ...(catalog?.dynasties || []).map((item) => ({ value: item, label: item })),
      ]"
    />
    <AppSelect
      v-model="kind"
      label="体裁"
      :options="[
        { value: '', label: '全部体裁' },
        ...(catalog?.kinds || []).map((item) => ({ value: item, label: item })),
      ]"
    />
    <button type="submit"><Search :size="17" />查找</button>
    <RouterLink v-if="route.query.authorId" to="/poetry?tab=library">清除作者筛选</RouterLink>
  </form>
  <StatePanel v-if="loading" mood="loading" text="正在翻开诗页…" />
  <StatePanel v-else-if="error" mood="error" :text="error"
    ><button @click="load()">重新加载</button></StatePanel
  >
  <template v-else-if="detail || tab === 'encounter'">
    <PoemReader
      v-if="poem"
      :key="poem.id"
      :poem="poem"
      :preview="!detail"
      @collected="poem.collected = $event"
    />
    <div v-if="!detail" class="poetry-turn">
      <button :disabled="!history.length" @click="turn(true)">
        <ArrowLeft :size="16" />上一首
      </button>
      <button class="primary" @click="turn()"><Shuffle :size="16" />换一首</button>
    </div>
    <div v-if="!detail" class="poetry-collections">
      <RouterLink
        v-for="item in catalog?.collections"
        :key="item.code"
        :to="{ path: '/poetry', query: { tab: 'library', dataset: item.code } }"
      >
        <span class="eyebrow">文集 / {{ item.total }} 篇</span>
        <h3>{{ item.name }}</h3>
        <span>翻开这一卷 →</span>
      </RouterLink>
    </div>
  </template>
  <StatePanel v-else-if="tab === 'favorites' && !auth.user" text="登录后，收好喜欢的诗词。"
    ><button class="primary" @click="auth.require()">登录后查看收藏</button></StatePanel
  >
  <template v-else>
    <p class="tiny muted">
      {{
        route.query.authorId
          ? '这位作者的作品'
          : tab === 'favorites'
            ? '留给自己的诗句'
            : '慢慢找，慢慢读'
      }}
      · {{ total }} 篇
    </p>
    <div v-if="rows.length" class="poetry-list">
      <RouterLink
        v-for="item in rows"
        :key="item.id"
        :to="'/poetry/' + item.id"
        class="poetry-item"
      >
        <span class="eyebrow">{{ item.dynasty }} · {{ item.kind }}</span>
        <h2>{{ item.title }}</h2>
        <span class="muted">{{ item.author }}</span>
        <p>{{ item.paragraphs.join('') }}</p>
        <span aria-hidden="true" class="poetry-item-arrow">↗</span>
      </RouterLink>
    </div>
    <StatePanel
      v-else
      :text="
        tab === 'favorites'
          ? '还没有收藏，去遇见一首喜欢的诗吧。'
          : '没有找到作品，试试其他标题或作者。'
      "
    />
    <Pager v-if="total > 12" :page="page" :total="total" @change="changePage" />
  </template>
  <p class="poetry-source">
    诗词数据来自
    <a
      href="https://github.com/chinese-poetry/chinese-poetry"
      target="_blank"
      rel="noopener noreferrer"
      >chinese-poetry</a
    >
    · MIT
  </p>
</template>
