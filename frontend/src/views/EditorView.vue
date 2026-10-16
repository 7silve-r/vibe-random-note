<script setup lang="ts">
import StatePanel from '../components/StatePanel.vue';
import { ref, watch, onBeforeUnmount, computed } from 'vue';
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router';
import { api } from '../api';
import { useAuth } from '../stores/auth';
import { useUi } from '../stores/ui';
import { markdown } from '../utils/markdown';
import { poemQuote, type Poem } from '../data/poems';
const route = useRoute(),
  router = useRouter(),
  auth = useAuth(),
  ui = useUi(),
  title = ref(''),
  content = ref(''),
  cateId = ref(''),
  state = ref('私有'),
  categories = ref<any[]>([]),
  file = ref<File>(),
  busy = ref(false),
  ready = ref(false),
  dirty = ref(false),
  saved = ref(''),
  error = ref(''),
  draft = ref<any>(null);
const quotation = ref<Poem | null>(null);
const key = computed(() => `vibe-draft:${auth.user?.id}:${route.params.id || 'new'}`);
function data() {
  return { title: title.value, content: content.value, cateId: cateId.value, state: state.value };
}
function persist() {
  if (!ready.value || !auth.user) return;
  try {
    localStorage.setItem(key.value, JSON.stringify(data()));
    saved.value = '草稿已保存在此浏览器';
  } catch {
    saved.value = '本机草稿保存失败，请及时保存日记';
  }
}
watch([title, content, cateId, state], () => {
  if (ready.value) {
    dirty.value = true;
    persist();
  }
});
async function init() {
  ready.value = false;
  error.value = '';
  title.value = '';
  content.value = '';
  cateId.value = '';
  state.value = '私有';
  draft.value = null;
  file.value = undefined;
  quotation.value = null;
  if (!auth.require()) return;
  try {
    categories.value = await api('/my/categories');
    if (route.params.id) {
      const d = await api('/my/article/info?id=' + route.params.id);
      title.value = d.title;
      content.value = d.content;
      cateId.value = String(d.cateId);
      state.value = d.state;
    }
    const raw = localStorage.getItem(key.value);
    draft.value = raw ? JSON.parse(raw) : null;
    if (!route.params.id && route.query.poem) {
      quotation.value = await api<Poem>(
        '/public/poetry/' + encodeURIComponent(String(route.query.poem)),
      );
    }
    await Promise.resolve();
    ready.value = true;
    dirty.value = false;
    if (quotation.value && !draft.value) insertQuote();
  } catch (e) {
    error.value = (e as Error).message;
  }
}
function insertQuote() {
  if (!quotation.value) return;
  if (draft.value) restore();
  const quote = poemQuote(quotation.value);
  if (!content.value.includes(quote))
    content.value = content.value ? content.value + '\n\n' + quote : quote;
  if (!title.value) title.value = `读《${quotation.value.title}》有感`.slice(0, 200);
  quotation.value = null;
  dirty.value = true;
  persist();
}
function discard() {
  localStorage.removeItem(key.value);
  draft.value = null;
}
function restore() {
  const d = draft.value;
  title.value = d.title || '';
  content.value = d.content || '';
  cateId.value = String(d.cateId || '');
  state.value = d.state === '公开' ? '公开' : '私有';
  draft.value = null;
}
async function category() {
  const name = await ui.ask('给新分类起个名字');
  if (!name?.trim()) return;
  await ui.run(async () => {
    await api('/my/categories', 'POST', { cateName: name.trim(), cateAlias: name.trim() });
    categories.value = await api('/my/categories');
    cateId.value = String(categories.value.at(-1)?.id || '');
  }, '分类已创建');
}
async function save() {
  if (!auth.require() || busy.value) return;
  busy.value = true;
  await ui.run(async () => {
    if (!cateId.value) throw Error('请先选择或创建一个分类');
    const form = new FormData();
    Object.entries(data()).forEach(([k, v]) => form.append(k, v));
    if (file.value) form.append('file', file.value);
    await api(
      '/my/article' + (route.params.id ? '/' + route.params.id : ''),
      route.params.id ? 'PUT' : 'POST',
      form,
    );
    localStorage.removeItem(key.value);
    dirty.value = false;
    router.push('/diary?tab=mine');
  }, '日记已保存');
  busy.value = false;
}
function leave(e: BeforeUnloadEvent) {
  if (dirty.value) {
    e.preventDefault();
    e.returnValue = '';
  }
}
window.addEventListener('beforeunload', leave);
onBeforeUnmount(() => window.removeEventListener('beforeunload', leave));
onBeforeRouteLeave(
  async () =>
    !dirty.value ||
    (await ui.confirm('日记还未保存到账号，本机文字草稿已保留。确定离开？封面需要重新选择。')),
);
watch(() => [auth.user?.id, route.params.id, route.query.poem], init, { immediate: true });
</script>
<template>
  <header class="section-head">
    <div>
      <span class="eyebrow">WRITE / 书写</span>
      <h1>{{ route.params.id ? '续写这一页' : '写下此刻' }}</h1>
      <p>{{ saved || '支持 Markdown · 标题、列表、引用、代码和表格' }}</p>
    </div>
    <RouterLink to="/diary"><button>返回日记</button></RouterLink>
  </header>
  <StatePanel v-if="!auth.user">
    <button class="primary" @click="auth.require()">登录后开始记录</button>
  </StatePanel>
  <StatePanel v-else-if="error" mood="error">
    <p>{{ error }}</p>
    <button @click="init">重新加载</button>
  </StatePanel>
  <form v-else-if="ready" class="panel form" @submit.prevent="save">
    <div v-if="quotation" class="row wrap">
      <span>已带来《{{ quotation.title }}》，现有草稿会保留。</span
      ><button type="button" @click="insertQuote">在草稿中引用诗词</button>
    </div>
    <div v-if="draft" class="row wrap">
      <span>发现这篇日记的本机草稿。</span><button type="button" @click="restore">恢复草稿</button
      ><button type="button" @click="discard">丢弃草稿</button>
    </div>
    <label
      >标题<input v-model="title" required maxlength="200" placeholder="给今天起个名字…"
    /></label>
    <div class="row wrap">
      <select v-model="cateId" required aria-label="选择分类" style="flex: 1">
        <option value="">选择分类</option>
        <option v-for="c in categories" :key="c.id" :value="String(c.id)">
          {{ c.cateName }}
        </option></select
      ><button type="button" @click="category">＋ 分类</button
      ><select v-model="state" aria-label="可见性" style="flex: 1">
        <option>私有</option>
        <option>公开</option>
      </select>
    </div>
    <p class="muted tiny">
      {{
        state === '私有'
          ? '只有你自己可以阅读，包括管理员也无法查看。'
          : '所有人都能阅读，登录后的朋友可以点赞、收藏和评论。'
      }}
    </p>
    <div class="editor-grid">
      <label
        >书写<textarea
          v-model="content"
          required
          maxlength="200000"
          placeholder="## 今天的小小幸福&#10;&#10;在这里，慢慢写。"
        ></textarea>
      </label>
      <section>
        <span class="muted tiny">预览</span>
        <div class="markdown" v-html="markdown(content)"></div>
      </section>
    </div>
    <label
      >封面图片（可选，最大 5 MB）<input
        type="file"
        accept="image/jpeg,image/png"
        @change="file = ($event.target as HTMLInputElement).files?.[0]" /></label
    ><button class="primary" :disabled="busy">{{ busy ? '正在保存…' : '保存日记' }}</button>
  </form>
</template>
