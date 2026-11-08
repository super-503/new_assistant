<script setup lang="ts">
/** 环形进度：重新加工、单字段生成、保存等进行中的按钮内使用，颜色跟随文字 */
const props = withDefaults(defineProps<{ size?: number }>(), { size: 16 })
</script>

<template>
  <svg
    class="news-spinner"
    :width="props.size"
    :height="props.size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="3"
    stroke-linecap="round"
    aria-hidden="true"
  >
    <path d="M12 3a9 9 0 1 0 9 9" />
  </svg>
</template>

<style scoped lang="scss">
.news-spinner {
  flex: none;
  animation: news-spin 900ms linear infinite;
}

@keyframes news-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
