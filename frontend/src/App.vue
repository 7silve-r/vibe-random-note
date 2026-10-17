<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue';
import {
  Headphones,
  BookOpen,
  Feather,
  House,
  UserRound,
  ShieldCheck,
  Menu,
  X,
} from 'lucide-vue-next';
import { useRoute } from 'vue-router';
import { useAuth } from './stores/auth';
import { useUi } from './stores/ui';
import AuthDialog from './components/AuthDialog.vue';
import SmartImage from './components/SmartImage.vue';
import ConfirmDialog from './components/ConfirmDialog.vue';
import LyricOverlay from './components/LyricOverlay.vue';
import CharacterIcon from './components/CharacterIcon.vue';
import NoticeStream from './components/NoticeStream.vue';
import StatePanel from './components/StatePanel.vue';
import GiftPoem from './components/GiftPoem.vue';
const auth = useAuth(),
  ui = useUi(),
  route = useRoute();
const welcome = ref(!sessionStorage.getItem('vibe-entered'));
const menuOpen = ref(false);
function enter() {
  welcome.value = false;
  sessionStorage.setItem('vibe-entered', 'yes');
}
function unauthorized() {
  auth.clear();
  ui.authOpen = true;
}
function escape(e: KeyboardEvent) {
  if (
    welcome.value &&
    !ui.authOpen &&
    !ui.dialog &&
    !e.isComposing &&
    (e.key === 'Enter' || e.key === ' ')
  ) {
    e.preventDefault();
    enter();
    return;
  }
  if (e.key === 'Escape') menuOpen.value = false;
}
onMounted(() => {
  auth.init();
  window.addEventListener('vibe:unauthorized', unauthorized);
  window.addEventListener('keydown', escape);
});
onBeforeUnmount(() => {
  window.removeEventListener('vibe:unauthorized', unauthorized);
  window.removeEventListener('keydown', escape);
});
</script>
<template>
  <div
    class="ambient"
    :class="{ 'is-welcome': welcome, 'is-home': !welcome && route.path === '/' }"
    aria-hidden="true"
  >
    <img class="sky-scene" src="/sky-braids.png" alt="" fetchpriority="high" />
  </div>
  <div
    v-if="welcome"
    class="welcome"
    role="button"
    tabindex="0"
    aria-label="点击任意处开始"
    @click="enter"
  >
    <div class="welcome-copy">
      <h1 class="glass-wordmark" aria-label="Vibe Random Notes">
        <span class="word-vibe" aria-hidden="true">Vibe</span>
        <span class="word-random" aria-hidden="true">Random</span>
        <span class="word-notes" aria-hidden="true">Notes</span>
      </h1>
    </div>
    <GiftPoem />
    <span class="start-hint"
      ><span class="start-label">点击任意处开始</span
      ><span class="enter-hint">Enter to begin</span></span
    >
  </div>
  <template v-else>
    <a href="#main-content" class="skip-link">跳到正文</a>
    <button
      class="menu-toggle"
      :aria-expanded="menuOpen"
      aria-controls="side-navigation"
      aria-label="切换侧边导航"
      @click="menuOpen = !menuOpen"
    >
      <X v-if="menuOpen" :size="19" /><Menu v-else :size="19" />
    </button>
    <button
      v-if="menuOpen"
      class="nav-scrim"
      aria-label="关闭侧边导航"
      @click="menuOpen = false"
    ></button>
    <aside id="side-navigation" class="sidebar" :class="{ open: menuOpen }">
      <RouterLink to="/" class="brand" aria-label="Vibe Random Notes 主页" @click="menuOpen = false"
        ><CharacterIcon /><span>Vibe<small>RANDOM NOTES</small></span></RouterLink
      >
      <span class="nav-caption">随 心 记</span>
      <nav class="main-nav" aria-label="主导航">
        <RouterLink to="/" @click="menuOpen = false"
          ><House :size="18" /><span>主页</span><small>HOME</small></RouterLink
        >
        <RouterLink
          to="/music"
          aria-label="音乐"
          :class="{ active: route.path.startsWith('/music') }"
          @click="menuOpen = false"
          ><Headphones :size="18" /><span>音乐</span><small>MUSIC</small></RouterLink
        >
        <RouterLink
          to="/diary"
          aria-label="日记"
          :class="{ active: route.path.startsWith('/diary') }"
          @click="menuOpen = false"
          ><BookOpen :size="18" /><span>日记</span><small>DIARY</small></RouterLink
        >
        <RouterLink
          to="/poetry"
          aria-label="诗词"
          :class="{ active: route.path.startsWith('/poetry') }"
          @click="menuOpen = false"
          ><Feather :size="18" /><span>诗词</span><small>POETRY</small></RouterLink
        >
      </nav>
      <div class="sidebar-bottom">
        <span class="tiny muted">VIBE RANDOM NOTES</span
        ><button
          class="sidebar-flower"
          aria-label="查看欢迎页"
          title="查看欢迎页"
          @click="
            welcome = true;
            menuOpen = false;
          "
        >
          ✧
        </button>
      </div>
    </aside>
    <header class="account-bar">
      <RouterLink v-if="auth.user" to="/account" class="account-pill"
        ><SmartImage v-if="auth.user.userPic" :src="auth.user.userPic" /><CharacterIcon
          v-else
        /><span class="truncate">{{ auth.user.nickname || auth.user.username }}</span
        ><span v-if="auth.admin" class="badge"><ShieldCheck :size="13" />管理员</span></RouterLink
      >
      <button v-else class="account-pill" @click="ui.authOpen = true">
        <UserRound :size="17" />登录 / 注册
      </button>
    </header>
    <main id="main-content" class="workspace" tabindex="-1">
      <RouterView v-if="auth.ready" />
      <StatePanel v-else mood="loading" text="正在加载…" />
    </main>
    <LyricOverlay />
    <footer class="site-footer">
      <span>VIBE RANDOM NOTES</span><span>音乐 / 日记 / 诗词</span><span aria-hidden="true">✧</span>
    </footer>
  </template>
  <AuthDialog v-if="ui.authOpen" /><ConfirmDialog v-if="ui.dialog" />
  <NoticeStream />
</template>
