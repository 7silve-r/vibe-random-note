import { defineStore } from 'pinia';
import { ref } from 'vue';
export const useUi = defineStore('ui', () => {
  const notices = ref<{ id: number; text: string; error: boolean }[]>([]);
  const authOpen = ref(false);
  const dialog = ref<{
    text: string;
    input: boolean;
    resolve: (value: string | boolean) => void;
  } | null>(null);
  function confirm(text: string): Promise<boolean> {
    return new Promise((resolve) => {
      dialog.value = { text, input: false, resolve: (value) => resolve(value === true) };
    });
  }
  function ask(text: string): Promise<string> {
    return new Promise((resolve) => {
      dialog.value = {
        text,
        input: true,
        resolve: (value) => resolve(typeof value === 'string' ? value : ''),
      };
    });
  }
  function answer(value: string | boolean) {
    dialog.value?.resolve(value);
    dialog.value = null;
  }
  let id = 0;
  function notify(text: string, error = false) {
    const key = ++id;
    notices.value.push({ id: key, text, error });
  }
  function dismissNotice(id: number) {
    notices.value = notices.value.filter((n) => n.id !== id);
  }
  async function run<T>(fn: () => Promise<T>, message?: string): Promise<T | undefined> {
    try {
      const data = await fn();
      if (message) notify(message);
      return data;
    } catch (e) {
      notify(e instanceof Error ? e.message : '操作未完成', true);
    }
  }
  return { notices, authOpen, notify, dismissNotice, run, dialog, confirm, ask, answer };
});
