<script setup lang="ts">
/** 搜索来源、加载来源卡片时的骨架屏：头像加两行文字，下方若干行占位 */
const props = defineProps<{
  /** 头像下方的占位行数 */
  rows: number
}>()
</script>

<template>
  <div class="source-skeleton" aria-hidden="true">
    <div class="skeleton-head">
      <span class="shimmer skeleton-avatar"></span>
      <span class="skeleton-lines">
        <span class="shimmer skeleton-line is-short"></span>
        <span class="shimmer skeleton-line"></span>
      </span>
    </div>
    <span v-for="row in props.rows" :key="row" class="shimmer skeleton-block"></span>
  </div>
</template>

<style scoped lang="scss">
.source-skeleton {
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-lg);
  padding: var(--zg-space-xl);
  border-radius: var(--zg-radius-lg);
  background: var(--surface-container-highest);
}

.skeleton-head {
  display: flex;
  gap: var(--zg-space-xl);
}

.skeleton-lines {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-md);
}

.shimmer {
  display: block;
  background: linear-gradient(
    90deg,
    var(--surface-container-high) 0,
    var(--surface-container) 160px,
    var(--surface-container-high) 320px
  );
  background-size: 960px 100%;
  animation: skeleton-shimmer 1.3s linear infinite;
}

.skeleton-avatar {
  flex: none;
  width: 48px;
  height: 48px;
  border-radius: 50%;
}

.skeleton-line {
  width: 70%;
  height: 12px;
  border-radius: var(--zg-radius-xs);

  &.is-short {
    width: 40%;
    height: 16px;
  }
}

.skeleton-block {
  height: 40px;
  border-radius: var(--zg-radius-md);
}

@keyframes skeleton-shimmer {
  from {
    background-position: -480px 0;
  }
  to {
    background-position: 480px 0;
  }
}
</style>
