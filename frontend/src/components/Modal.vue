<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue';
defineProps<{ title: string }>();
const emit = defineEmits(['close']);
const box = ref<HTMLElement>();
let previous: HTMLElement | null = null;
function key(e: KeyboardEvent) {
  if (e.key === 'Escape') emit('close');
  if (e.key === 'Tab') {
    const items = box.value?.querySelectorAll<HTMLElement>(
      'button:not(:disabled),input:not([tabindex="-1"]),textarea,select,a[href]',
    );
    if (!items?.length) return;
    const first = items[0],
      last = items[items.length - 1];
    if (e.shiftKey && document.activeElement === first) {
      e.preventDefault();
      last.focus();
    } else if (!e.shiftKey && document.activeElement === last) {
      e.preventDefault();
      first.focus();
    }
  }
}
onMounted(() => {
  previous = document.activeElement as HTMLElement;
  box.value?.focus();
  document.addEventListener('keydown', key);
});
onBeforeUnmount(() => {
  document.removeEventListener('keydown', key);
  previous?.focus();
});
</script>
<template>
  <Teleport to="body"
    ><div class="veil" @click.self="emit('close')">
      <section
        ref="box"
        class="modal glass"
        role="dialog"
        aria-modal="true"
        :aria-label="title"
        tabindex="-1"
      >
        <header class="row">
          <h2>{{ title }}</h2>
          <button aria-label="关闭" @click="emit('close')">×</button>
        </header>
        <slot />
      </section></div
  ></Teleport>
</template>
