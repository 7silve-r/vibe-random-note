<script setup lang="ts">
import { computed, onBeforeUnmount, watch } from 'vue';
import { useUi } from '../stores/ui';
import CharacterIcon from './CharacterIcon.vue';
const ui = useUi();
const visible = computed(() => ui.notices.slice(0, 3));
const timers = new Map<number, ReturnType<typeof setTimeout>>();
watch(
  visible,
  (notices) => {
    for (const [id, timer] of timers) {
      if (!notices.some((n) => n.id === id)) {
        clearTimeout(timer);
        timers.delete(id);
      }
    }
    for (const notice of notices) {
      if (!timers.has(notice.id)) {
        timers.set(
          notice.id,
          setTimeout(() => ui.dismissNotice(notice.id), 10000),
        );
      }
    }
  },
  { immediate: true },
);
onBeforeUnmount(() => {
  for (const timer of timers.values()) clearTimeout(timer);
});
</script>
<template>
  <div class="notice-stream" aria-live="polite" aria-relevant="additions">
    <div
      v-for="notice in visible"
      :key="notice.id"
      class="notice-message glass"
      :class="{ error: notice.error }"
      role="status"
    >
      <CharacterIcon :mood="notice.error ? 'error' : 'success'" />
      <span>{{ notice.text }}</span>
      <button aria-label="关闭提示" @click="ui.dismissNotice(notice.id)">×</button>
    </div>
  </div>
</template>
