<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useId, watch } from 'vue';
import { ChevronDown, X, Check } from 'lucide-vue-next';
type Value = string | number;
const props = withDefaults(
  defineProps<{
    modelValue?: Value | Value[];
    options: { value: Value; label: string }[];
    multiple?: boolean;
    searchable?: boolean;
    required?: boolean;
    disabled?: boolean;
    placeholder?: string;
    label: string;
  }>(),
  { placeholder: '请选择' },
);
const emit = defineEmits<{ 'update:modelValue': [value: any]; change: [value: any] }>();
const root = ref<HTMLElement>(),
  menu = ref<HTMLElement>(),
  trigger = ref<HTMLButtonElement>();
const open = ref(false),
  search = ref(''),
  active = ref(0),
  position = ref({});
const id = useId();
const values = computed<Value[]>(() =>
  Array.isArray(props.modelValue)
    ? props.modelValue
    : props.modelValue == null
      ? []
      : [props.modelValue],
);
const selected = computed(() =>
  values.value.map((value) => props.options.find((item) => item.value === value)).filter(Boolean),
);
const filtered = computed(() =>
  props.options.filter((item) => item.label.toLowerCase().includes(search.value.toLowerCase())),
);
function place() {
  if (!open.value || !trigger.value) return;
  const rect = trigger.value.getBoundingClientRect();
  const width = Math.min(Math.max(rect.width, 210), innerWidth - 24);
  const below = innerHeight - rect.bottom - 12;
  const above = rect.top - 12;
  const upwards = below < 240 && above > below;
  position.value = {
    position: 'fixed',
    width: `${width}px`,
    left: `${Math.max(12, Math.min(rect.left, innerWidth - width - 12))}px`,
    top: upwards ? undefined : `${rect.bottom + 6}px`,
    bottom: upwards ? `${innerHeight - rect.top + 6}px` : undefined,
    maxHeight: `${Math.max(80, Math.min(330, (upwards ? above : below) - 6))}px`,
  };
}
async function show() {
  if (props.disabled) return;
  open.value = !open.value;
  search.value = '';
  active.value = Math.max(
    0,
    props.options.findIndex((item) => values.value.includes(item.value)),
  );
  await nextTick();
  place();
  if (open.value && props.searchable) menu.value?.querySelector('input')?.focus();
}
function choose(value: Value) {
  const result = props.multiple
    ? values.value.includes(value)
      ? values.value.filter((item) => item !== value)
      : [...values.value, value]
    : value;
  emit('update:modelValue', result);
  emit('change', result);
  if (!props.multiple) {
    open.value = false;
    trigger.value?.focus();
  }
}
async function keyboard(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    if (open.value) event.stopPropagation();
    open.value = false;
    trigger.value?.focus();
    return;
  }
  if (event.key === 'Tab') {
    if (open.value && menu.value?.contains(document.activeElement)) trigger.value?.focus();
    open.value = false;
    return;
  }
  if (!['ArrowDown', 'ArrowUp', 'Home', 'End', 'Enter'].includes(event.key)) return;
  if (!open.value) {
    if (['ArrowDown', 'ArrowUp'].includes(event.key)) {
      event.preventDefault();
      await show();
    }
    return;
  }
  event.preventDefault();
  if (event.key === 'Enter') {
    const option = filtered.value[active.value];
    if (option) choose(option.value);
    return;
  }
  const last = filtered.value.length - 1;
  active.value =
    event.key === 'Home'
      ? 0
      : event.key === 'End'
        ? last
        : Math.max(0, Math.min(last, active.value + (event.key === 'ArrowDown' ? 1 : -1)));
  await nextTick();
  menu.value?.querySelector(`[data-index="${active.value}"]`)?.scrollIntoView({ block: 'nearest' });
}
function outside(event: Event) {
  if (!root.value?.contains(event.target as Node) && !menu.value?.contains(event.target as Node))
    open.value = false;
}
watch(search, () => (active.value = 0));
onMounted(() => {
  document.addEventListener('pointerdown', outside);
  window.addEventListener('resize', place);
  window.addEventListener('scroll', place, true);
});
onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', outside);
  window.removeEventListener('resize', place);
  window.removeEventListener('scroll', place, true);
});
</script>
<template>
  <div ref="root" class="app-select" @keydown="keyboard">
    <button
      ref="trigger"
      type="button"
      class="select-trigger"
      role="combobox"
      :aria-label="label"
      :aria-expanded="open"
      :aria-controls="id"
      aria-haspopup="listbox"
      :aria-activedescendant="open && filtered.length ? `${id}-${active}` : undefined"
      :disabled="disabled"
      @click="show"
    >
      <span>{{
        multiple
          ? selected.length
            ? `已选 ${selected.length} 项`
            : placeholder
          : selected[0]?.label || placeholder
      }}</span
      ><ChevronDown :size="16" />
    </button>
    <input
      class="select-validity"
      :value="values.join(',')"
      :required="required"
      :disabled="disabled"
      tabindex="-1"
      aria-hidden="true"
      @invalid="trigger?.focus()"
    />
    <div v-if="multiple && selected.length" class="select-tags">
      <button
        v-for="item in selected"
        :key="item!.value"
        type="button"
        :aria-label="`移除${item!.label}`"
        @click="choose(item!.value)"
      >
        {{ item!.label }}<X :size="12" />
      </button>
    </div>
    <Teleport to="body">
      <div v-if="open" ref="menu" class="select-menu" :style="position" @keydown="keyboard">
        <input
          v-if="searchable"
          v-model="search"
          :aria-label="`搜索${label}`"
          placeholder="输入关键词筛选…"
          autocomplete="off"
        />
        <div
          :id="id"
          role="listbox"
          :aria-label="label"
          :aria-multiselectable="multiple || undefined"
        >
          <button
            v-for="(item, index) in filtered"
            :id="`${id}-${index}`"
            :key="item.value"
            type="button"
            role="option"
            :aria-selected="values.includes(item.value)"
            :data-index="index"
            :class="{ focused: index === active }"
            @pointermove="active = index"
            @click="choose(item.value)"
          >
            <input
              v-if="multiple"
              type="checkbox"
              :checked="values.includes(item.value)"
              tabindex="-1"
              aria-hidden="true"
            />
            <span>{{ item.label }}</span
            ><Check v-if="!multiple && values.includes(item.value)" :size="15" />
          </button>
          <p v-if="!filtered.length" class="tiny muted">没有匹配的选项</p>
        </div>
      </div>
    </Teleport>
  </div>
</template>
