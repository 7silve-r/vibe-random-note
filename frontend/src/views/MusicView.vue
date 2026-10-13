<script setup lang="ts">
import StatePanel from '../components/StatePanel.vue';
import { ref, watch, computed } from 'vue';
import { useRoute } from 'vue-router';
import { api } from '../api';
import { useAuth } from '../stores/auth';
import { useUi } from '../stores/ui';
import { usePlayer } from '../stores/player';
import SongList from '../components/SongList.vue';
import SmartImage from '../components/SmartImage.vue';
import Pager from '../components/Pager.vue';
import Comments from '../components/Comments.vue';
const route = useRoute(),
  auth = useAuth(),
  ui = useUi(),
  player = usePlayer();
const searchBy = ref('songName'),
  recommendedLists = ref<any[]>([]);
const tab = ref('songs'),
  search = ref(''),
  page = ref(1),
  total = ref(0),
  rows = ref<any[]>([]),
  detail = ref<any>(null),
  loading = ref(false),
  error = ref(''),
  banners = ref<any[]>([]),
  style = ref(''),
  styles = ref<any[]>([]);
const kind = computed(() => String(route.params.kind || '')),
  id = computed(() => Number(route.params.id));
let generation = 0;
const tabs = [
  ['songs', '所有音乐'],
  ['playlists', '歌单'],
  ['artists', '歌手'],
  ['recommended', '为你推荐'],
  ['favorites', '收藏歌曲'],
  ['favoriteLists', '收藏歌单'],
];
async function load() {
  const ticket = ++generation;
  loading.value = true;
  error.value = '';
  try {
    if (id.value) {
      const names: Record<string, string> = {
        song: 'Song',
        playlist: 'Playlist',
        artist: 'Artist',
      };
      if (!names[kind.value]) throw Error('这个页面不存在');
      const data = await api(
        `/music/public/${kind.value}/get${names[kind.value]}Detail/${id.value}`,
      );
      if (ticket === generation) detail.value = data;
    } else {
      detail.value = null;
      if (['favorites', 'favoriteLists'].includes(tab.value) && !auth.require()) {
        tab.value = 'songs';
      }
      const params = {
        pageNum: page.value,
        pageSize: 12,
        songName: searchBy.value === 'songName' ? search.value : undefined,
        artistName:
          tab.value === 'artists' || searchBy.value === 'artistName' ? search.value : undefined,
        album: searchBy.value === 'album' ? search.value : undefined,
        title: search.value,
        style: style.value || undefined,
      };
      let data: any;
      if (tab.value === 'songs')
        data = await api('/music/public/song/listSongs', 'POST', {
          ...params,
          artistName: params.artistName,
        });
      else if (tab.value === 'playlists')
        data = await api('/music/public/playlist/listPlaylists', 'POST', params);
      else if (tab.value === 'artists')
        data = await api('/music/public/artist/listArtists', 'POST', params);
      else if (tab.value === 'recommended')
        data = { list: await api('/music/public/song/getRecommendedSongs'), total: 0 };
      else
        data = await api(
          '/music/favorite/' +
            (tab.value === 'favorites' ? 'getFavoriteSongs' : 'getFavoritePlaylists'),
          'POST',
          { ...params },
        );
      if (ticket === generation) {
        rows.value = data.list || [];
        total.value = data.total || rows.value.length;
      }
    }
  } catch (e) {
    if (ticket === generation) error.value = (e as Error).message;
  } finally {
    if (ticket === generation) loading.value = false;
  }
}
async function favorite() {
  if (!auth.require()) return;
  await ui.run(async () => {
    const type = kind.value === 'song' ? 'Song' : 'Playlist';
    await api(
      `/music/favorite/${detail.value.collected ? 'cancelCollect' : 'collect'}${type}?${kind.value}Id=${id.value}`,
      detail.value.collected ? 'DELETE' : 'POST',
    );
    await load();
  }, '收藏已更新');
}
watch(
  () => route.fullPath,
  () => {
    page.value = 1;
    load();
  },
  { immediate: true },
);
watch(
  () => auth.user?.id,
  () => load(),
);
ui.run(async () => {
  banners.value = await api('/music/public/banner/getBannerList');
  styles.value = await api('/music/public/styles');
  recommendedLists.value = await api('/music/public/playlist/getRecommendedPlaylists');
});
</script>
<template>
  <header class="section-head">
    <div>
      <span class="eyebrow">MUSIC / 音楽</span>
      <h1>听见 · 音乐</h1>
      <p>谁家玉笛暗飞声，散入春风满洛城。<small>李白 ·《春夜洛城闻笛》</small></p>
    </div>
    <RouterLink v-if="id" to="/music"><button>返回音乐</button></RouterLink>
  </header>
  <StatePanel v-if="error" mood="error">
    <p role="alert">{{ error }}</p>
    <button @click="load">重新加载</button>
  </StatePanel>
  <StatePanel v-else-if="loading" mood="loading" text="正在加载音乐…" />
  <template v-else-if="detail"
    ><section class="detail-hero panel">
      <SmartImage class="cover" :src="detail.coverUrl || detail.avatar" />
      <div class="detail-copy">
        <span class="eyebrow">{{
          kind === 'song' ? 'SONG' : kind === 'artist' ? 'ARTIST' : 'PLAYLIST'
        }}</span>
        <h1>{{ detail.songName || detail.title || detail.artistName }}</h1>
        <p>{{ detail.artistName }} {{ detail.album ? ' / ' + detail.album : '' }}</p>
        <p>{{ detail.introduction }}</p>
        <div class="actions">
          <button
            class="primary"
            @click="
              kind === 'song'
                ? player.play(detail)
                : detail.songs?.length
                  ? player.play(detail.songs[0], detail.songs)
                  : ui.notify('这里还没有歌曲')
            "
          >
            播放{{ kind === 'song' ? '' : '全部' }}</button
          ><button v-if="kind !== 'artist'" @click="favorite">
            {{ detail.collected ? '取消收藏' : '收藏' }}
          </button>
        </div>
      </div>
    </section>
    <section v-if="kind === 'song'" class="panel">
      <h2>歌词</h2>
      <div class="lyric">{{ detail.lyric || '这首歌还没有歌词。让旋律替它说话。' }}</div>
    </section>
    <section v-else class="panel">
      <SongList :songs="detail.songs || []" @refresh="load" />
    </section>
    <Comments
      v-if="kind === 'song' || kind === 'playlist'"
      :kind="kind"
      :id="id"
      :items="detail.comments"
      @refresh="load" /></template
  ><template v-else
    ><div v-if="banners.length" class="banner-strip">
      <SmartImage v-for="b in banners" :key="b.bannerId" :src="b.bannerUrl" alt="音乐推荐海报" />
    </div>
    <div class="tabs">
      <button
        v-for="[value, label] in tabs"
        :key="value"
        :class="{ active: tab === value }"
        @click="
          tab = value;
          page = 1;
          load();
        "
      >
        {{ label }}
      </button>
    </div>
    <form
      class="toolbar"
      @submit.prevent="
        page = 1;
        load();
      "
    >
      <select v-if="['songs', 'favorites'].includes(tab)" v-model="searchBy" aria-label="搜索条件">
        <option value="songName">歌曲名</option>
        <option value="artistName">歌手名</option>
        <option value="album">专辑名</option></select
      ><input
        v-model="search"
        :placeholder="
          tab === 'artists'
            ? '搜索歌手'
            : tab === 'playlists' || tab === 'favoriteLists'
              ? '搜索歌单'
              : '搜索歌曲'
        "
        aria-label="搜索音乐"
      /><select v-if="tab === 'playlists'" v-model="style" aria-label="音乐风格">
        <option value="">全部风格</option>
        <option v-for="s in styles" :key="s.styleId">{{ s.name }}</option></select
      ><button>搜索</button>
    </form>
    <section v-if="['songs', 'recommended', 'favorites'].includes(tab)" class="panel">
      <SongList :songs="rows" @refresh="load" />
    </section>
    <div v-else-if="rows.length" class="grid">
      <RouterLink
        v-for="item in rows"
        :key="item.artistId || item.playlistId"
        :to="
          '/music/' +
          (tab === 'artists' ? 'artist/' : 'playlist/') +
          (item.artistId || item.playlistId)
        "
        class="content-item"
        ><SmartImage class="cover" :src="item.avatar || item.coverUrl" />
        <h3>{{ item.artistName || item.title }}</h3>
        <span class="muted tiny"
          >{{ tab === 'artists' ? '遇见歌手' : '打开这份歌单' }} ↗</span
        ></RouterLink
      >
    </div>
    <StatePanel v-else text="暂时没有找到内容，换个关键词试试吧。" />
    <section v-if="tab === 'recommended' && recommendedLists.length" class="panel">
      <h2>灵感歌单</h2>
      <div class="grid">
        <RouterLink
          v-for="item in recommendedLists"
          :key="item.playlistId"
          :to="'/music/playlist/' + item.playlistId"
          class="content-item"
          ><SmartImage class="cover" :src="item.coverUrl" />
          <h3>{{ item.title }}</h3></RouterLink
        >
      </div>
    </section>
    <Pager
      v-if="tab !== 'recommended'"
      :page="page"
      :total="total"
      @change="
        page = $event;
        load();
      "
  /></template>
</template>
