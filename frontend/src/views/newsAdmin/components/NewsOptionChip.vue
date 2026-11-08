<script setup lang="ts">
import { computed } from 'vue'

/**
 * 单选、多选字段的可点选项：选中时弹出对勾。
 * 传 colorIndex 时选中态用标签循环配色（「显示为标签」的字段），否则用选中态底色。
 */
const props = defineProps<{
  label: string
  selected: boolean
  /** 标签配色序号；null 表示中性 */
  colorIndex: number | null
  disabled?: boolean
}>()

const emit = defineEmits<{ toggle: [] }>()

/** 标签配色共 5 组 */
const TAG_COLOR_COUNT = 5

const selectedClass = computed(() => {
  if (!props.selected) return ''
  return props.colorIndex === null ? 'is-selected' : `is-selected is-color-${(props.colorIndex % TAG_COLOR_COUNT) + 1}`
})
</script>

<template>
  <button
    type="button"
    class="option-chip"
    :class="selectedClass"
    :aria-pressed="props.selected"
    :disabled="props.disabled"
    @click="emit('toggle')"
  >
    <SvgIcon v-if="props.selected" class="option-check" icon-class="newsAdmin-check" size="16" />
    {{ props.label }}
  </button>
</template>

<style scoped lang="scss">
@use './newsMixins' as *;

.option-chip {
  @include reset-button;
  @include state-layer;
  @include focus-ring(1px);
  display: inline-flex;
  align-items: center;
  gap: var(--zg-space-xs);
  height: 28px;
  padding: 0 var(--zg-space-lg);
  border: 1px solid var(--surface-outline);
  border-radius: var(--zg-radius-md);
  color: var(--second-color);
  font-size: var(--zg-text-md);
  font-weight: var(--zg-weight-medium);
  white-space: nowrap;
  transition:
    background var(--zg-duration-base) var(--zg-ease-m3-standard),
    padding var(--zg-duration-base) var(--zg-ease-m3-standard);

  &.is-selected {
    padding-left: var(--zg-space-md);
    border-color: transparent;
    background: var(--selected-bg);
    color: var(--selected-text);
  }
  &.is-color-1 {
    background: var(--tag-cycle-1-bg);
    color: var(--tag-cycle-1-text);
  }
  &.is-color-2 {
    background: var(--tag-cycle-2-bg);
    color: var(--tag-cycle-2-text);
  }
  &.is-color-3 {
    background: var(--tag-cycle-3-bg);
    color: var(--tag-cycle-3-text);
  }
  &.is-color-4 {
    background: var(--tag-cycle-4-bg);
    color: var(--tag-cycle-4-text);
  }
  &.is-color-5 {
    background: var(--tag-cycle-5-bg);
    color: var(--tag-cycle-5-text);
  }
  &:disabled {
    color: var(--text-disabled);
    cursor: not-allowed;
  }

  :deep(.svg-icon) {
    cursor: inherit;
  }
}

.option-check {
  animation: news-pop var(--zg-duration-base) var(--zg-ease-m3-standard);
}

@keyframes news-pop {
  from {
    opacity: 0;
    transform: scale(0.4);
  }
  to {
    opacity: 1;
    transform: none;
  }
}
</style>
