<script setup lang="ts">
import { computed } from 'vue'

/**
 * 资讯标签：
 * 传 colorIndex 为彩色变体，按序号循环使用 5 组标签配色（「显示为标签」的字段，序号取选项顺序）；
 * 不传为中性描边变体（岗位分类）。
 */
const props = defineProps<{
  label: string
  /** 选项序号，从 0 开始 */
  colorIndex?: number
}>()

/** 标签配色共 5 组 */
const TAG_COLOR_COUNT = 5

/** 彩色变体对应的配色类名；中性变体为 is-neutral */
const variantClass = computed(() =>
  props.colorIndex === undefined ? 'is-neutral' : `is-color-${(props.colorIndex % TAG_COLOR_COUNT) + 1}`
)
</script>

<template>
  <span class="news-tag" :class="variantClass">
    <span>{{ props.label }}</span>
    <slot />
  </span>
</template>

<style scoped lang="scss">
.news-tag {
  flex: none;
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 var(--zg-space-md);
  border: 1px solid transparent;
  border-radius: var(--zg-radius-sm);
  font-size: var(--zg-text-sm);
  font-weight: var(--zg-weight-medium);
  white-space: nowrap;
}

.is-neutral {
  border-color: var(--surface-border);
  color: var(--second-color);
}

.is-color-1 {
  background: var(--tag-cycle-1-bg);
  color: var(--tag-cycle-1-text);
}
.is-color-2 {
  background: var(--tag-cycle-2-bg);
  color: var(--tag-cycle-2-text);
}
.is-color-3 {
  background: var(--tag-cycle-3-bg);
  color: var(--tag-cycle-3-text);
}
.is-color-4 {
  background: var(--tag-cycle-4-bg);
  color: var(--tag-cycle-4-text);
}
.is-color-5 {
  background: var(--tag-cycle-5-bg);
  color: var(--tag-cycle-5-text);
}
</style>
