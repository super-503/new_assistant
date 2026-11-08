<script setup lang="ts">
import { computed } from 'vue'

/** 20px 来源头像：有图片地址显示图片，否则显示名称首字，中性色 */
const props = defineProps<{
  /** 来源名称，用于取首字 */
  name: string
  /** 头像图片地址；空字符串或不传时显示首字 */
  src?: string
}>()

/** 名称首字，按码点取，避免把代理对拆开 */
const initial = computed(() => Array.from(props.name)[0] ?? '')
</script>

<template>
  <span class="source-avatar" aria-hidden="true">
    <img v-if="props.src" :src="props.src" alt="" loading="lazy" />
    <template v-else>{{ initial }}</template>
  </span>
</template>

<style scoped lang="scss">
.source-avatar {
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  overflow: hidden;
  border-radius: 50%;
  background: var(--surface-container-highest);
  color: var(--second-color);
  font-size: var(--zg-text-xs);
  line-height: 1;
  font-weight: var(--zg-weight-semibold);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}
</style>
