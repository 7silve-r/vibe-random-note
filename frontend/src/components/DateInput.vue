<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { normalizeDate } from '../utils/date';
const props = defineProps<{ modelValue?: string; label: string; required?: boolean }>();
const emit = defineEmits<{ 'update:modelValue': [value: string] }>();
const input = ref<HTMLInputElement>();
const error = computed(() =>
  props.modelValue && !normalizeDate(props.modelValue) ? '请输入有效日期，例如 2026-10-17' : '',
);
watch([error, input], () => input.value?.setCustomValidity(error.value), { flush: 'post' });
function normalize() {
  if (props.modelValue && !error.value) emit('update:modelValue', normalizeDate(props.modelValue)!);
}
</script>
<template>
  <input
    ref="input"
    type="text"
    inputmode="numeric"
    :value="modelValue || ''"
    :aria-label="label"
    :required="required"
    placeholder="YYYY-MM-DD"
    maxlength="10"
    :title="error || '直接输入日期，例如 2026-10-17'"
    @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    @blur="normalize"
  />
</template>
