<script setup lang="ts">
import { useReviewQueueContext } from './useReviewQueue'

/** 批量操作的失败项：逐条列出标题和原因；失败的条目仍在队列里并保持勾选 */
const queue = useReviewQueueContext()
</script>

<template>
  <div v-if="queue.batchFailures.value.length > 0" class="batch-failures" role="alert">
    <SvgIcon class="failures-icon" icon-class="newsAdmin-warning" size="20" />
    <div class="failures-body">
      <span class="failures-title">{{ queue.batchFailures.value.length }} 条处理失败</span>
      <ul class="failures-list">
        <li v-for="failure in queue.batchFailures.value" :key="failure.id">
          <span class="failure-title">{{ failure.title ?? `资讯 ${failure.id}` }}</span>：{{ failure.reason }}
        </li>
      </ul>
    </div>
    <button type="button" class="failures-close" aria-label="关闭" @click="queue.batchFailures.value = []">
      <SvgIcon icon-class="newsAdmin-close" size="18" />
    </button>
  </div>
</template>

<style scoped lang="scss">
@use '../components/newsMixins' as *;

.batch-failures {
  display: flex;
  align-items: flex-start;
  gap: var(--zg-space-lg);
  margin-bottom: var(--zg-space-md);
  padding: var(--zg-space-lg) var(--zg-space-md) var(--zg-space-lg) var(--zg-space-xl);
  border-radius: var(--zg-radius-lg);
  background: var(--status-rejected-container);
  color: var(--on-status-rejected-container);
  font-size: var(--zg-text-md);
  line-height: 20px;
  animation: news-enter 300ms var(--zg-ease-m3-emphasized-decelerate) both;
}

.failures-icon {
  flex: none;
  margin-top: var(--zg-space-2xs);
}

.failures-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-xs);
}

.failures-title {
  font-weight: var(--zg-weight-medium);
}

.failures-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.failure-title {
  font-weight: var(--zg-weight-medium);
}

.failures-close {
  @include icon-button;
  color: var(--on-status-rejected-container);
}

@keyframes news-enter {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}
</style>
