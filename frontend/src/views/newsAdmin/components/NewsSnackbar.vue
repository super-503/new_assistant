<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'

/**
 * 底部提示：反相表面，可带一个操作（如「撤销」），6 秒后自动关闭。
 * 每条提示由父组件以 key 重建，重新计时。
 */
const props = defineProps<{
  message: string
  /** 操作按钮文字；null 表示没有操作 */
  actionText: string | null
}>()

const emit = defineEmits<{ action: []; close: [] }>()

/** 自动关闭的时长 */
const AUTO_CLOSE_MS = 6000

let closeTimer: ReturnType<typeof setTimeout> | undefined

onMounted(() => {
  closeTimer = setTimeout(() => emit('close'), AUTO_CLOSE_MS)
})

onBeforeUnmount(() => clearTimeout(closeTimer))
</script>

<template>
  <div class="news-snackbar" role="status">
    <span class="snackbar-text">{{ props.message }}</span>
    <button v-if="props.actionText" type="button" class="snackbar-action" @click="emit('action')">
      {{ props.actionText }}
    </button>
    <button type="button" class="snackbar-close" aria-label="关闭提示" @click="emit('close')">
      <SvgIcon icon-class="newsAdmin-close" size="20" />
    </button>
  </div>
</template>

<style scoped lang="scss">
@use './newsMixins' as *;

/* 内容区居中：横向偏移导航栏宽度的一半 */
.news-snackbar {
  position: fixed;
  bottom: var(--zg-space-2xl);
  left: calc(50% + 40px);
  z-index: var(--zg-z-message);
  display: flex;
  align-items: center;
  gap: var(--zg-space-xs);
  min-height: 48px;
  max-width: calc(100% - 32px);
  padding: var(--zg-space-xs) var(--zg-space-md) var(--zg-space-xs) var(--zg-space-xl);
  border-radius: var(--zg-radius-xs);
  background: var(--snackbar-bg);
  color: var(--snackbar-text);
  box-shadow: var(--zg-shadow-m3-3);
  font-size: var(--zg-text-base);
  transform: translateX(-50%);
  animation: news-snack 300ms var(--zg-ease-m3-emphasized-decelerate);
}

.snackbar-text {
  min-width: 0;
  padding-right: var(--zg-space-md);
}

.snackbar-action {
  @include text-button;
  color: var(--snackbar-action);
}

.snackbar-close {
  @include icon-button;
  color: var(--snackbar-text);
}

@keyframes news-snack {
  from {
    opacity: 0;
    transform: translate(-50%, 12px);
  }
  to {
    opacity: 1;
    transform: translate(-50%, 0);
  }
}
</style>
