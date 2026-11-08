<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import type { NewsReviewStatus } from '@/api/newsItem'
import NewsRejectDialog from '../components/NewsRejectDialog.vue'
import NewsSnackbar from '../components/NewsSnackbar.vue'
import NewsTabs, { type NewsTab } from '../components/NewsTabs.vue'
import ReviewBatchFailures from './ReviewBatchFailures.vue'
import ReviewDetailPanel from './ReviewDetailPanel.vue'
import ReviewQueueItem from './ReviewQueueItem.vue'
import ReviewToolbar from './ReviewToolbar.vue'
import { provideReviewQueue } from './useReviewQueue'

/**
 * 资讯审核：分类标签、工具栏、审核队列（居中最宽 960px），右侧详情面板。
 * 键盘焦点在队列内时：J / K 上下切换并打开详情，A 通过，R 拒绝，Esc 关闭详情。
 */
const queue = provideReviewQueue()

/** 距底部不足该距离时加载下一页 */
const LOAD_MORE_THRESHOLD = 240

const STATUSES: NewsReviewStatus[] = ['PENDING', 'REJECTED']

const tabs = computed<NewsTab[]>(() => [
  { key: 'PENDING', label: '待审', count: queue.pendingCount.value },
  { key: 'REJECTED', label: '已拒绝', count: queue.rejectedCount.value }
])

const listRef = ref<HTMLElement>()

const isEmpty = computed(() => queue.hasLoaded.value && !queue.isLoading.value && queue.items.value.length === 0)

const onTabChange = (key: string) => {
  const next = STATUSES.find((status) => status === key)!
  if (next !== queue.status.value) void queue.setStatus(next)
}

const isNearBottom = () => {
  const list = listRef.value!
  return list.scrollTop + list.clientHeight >= list.scrollHeight - LOAD_MORE_THRESHOLD
}

/** 滚动到接近底部、或内容不足一屏时继续加载下一页 */
const loadMoreIfNeeded = async () => {
  while (queue.hasMore.value && !queue.isLoading.value && isNearBottom()) {
    const loadedCount = queue.items.value.length
    await queue.loadItems(false)
    await nextTick()
    if (queue.items.value.length === loadedCount) return
  }
}

const scrollToItem = (id: number) => {
  void nextTick(() => listRef.value!.querySelector(`[data-item-id="${id}"]`)?.scrollIntoView({ block: 'nearest' }))
}

/** 队列内的快捷键；输入框内和带修饰键时不响应 */
const onListKeydown = (event: KeyboardEvent) => {
  const target = event.target as HTMLElement
  if (['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName)) return
  if (event.metaKey || event.ctrlKey || event.altKey) return
  const key = event.key.toLowerCase()
  const current = queue.items.value.find((item) => item.id === queue.selectedId.value) ?? null
  if (key === 'j' || key === 'arrowdown' || key === 'k' || key === 'arrowup') {
    event.preventDefault()
    const nextId = queue.moveSelection(key === 'j' || key === 'arrowdown' ? 1 : -1)
    if (nextId !== null) scrollToItem(nextId)
    return
  }
  if (key === 'a' && current?.status === 'PENDING') {
    event.preventDefault()
    // 卡片离开队列后焦点回到队列，便于连续操作
    void queue.approve([current.id]).then(() => listRef.value?.focus())
    return
  }
  if (key === 'r' && current?.status === 'PENDING') {
    event.preventDefault()
    queue.openReject([current.id])
    return
  }
  if (key === 'escape' && current) void queue.closeItem()
}

/** 条目数变化后（重新加载、离开队列）补足一屏 */
watch(
  () => queue.items.value.length,
  () => nextTick(loadMoreIfNeeded)
)

onMounted(() => {
  void queue.loadFilterOptions()
  void queue.loadItems(true)
})
</script>

<template>
  <div class="review-page">
    <main class="review-main">
      <header class="review-head">
        <div class="review-inner">
          <h1 class="sr-only">资讯审核</h1>
          <NewsTabs
            :model-value="queue.status.value"
            :tabs="tabs"
            label="审核分类"
            @update:model-value="onTabChange"
          />
          <ReviewToolbar />
        </div>
      </header>

      <div
        ref="listRef"
        class="review-list"
        tabindex="0"
        aria-label="资讯队列，J、K 切换条目，A 通过，R 拒绝"
        @keydown="onListKeydown"
        @scroll.passive="loadMoreIfNeeded"
      >
        <div class="review-inner">
          <ReviewBatchFailures />
          <div v-if="queue.isLoading.value" class="list-progress" role="progressbar" aria-label="加载中"></div>
          <ReviewQueueItem v-for="(item, index) in queue.items.value" :key="item.id" :item="item" :index="index" />

          <div v-if="isEmpty && queue.hasActiveFilters.value" class="list-empty">
            <span class="empty-text">没有符合条件的资讯</span>
            <button type="button" class="empty-outline-button" @click="queue.clearFilters">清除筛选</button>
          </div>
          <div v-else-if="isEmpty && queue.status.value === 'PENDING'" class="list-empty">
            <span class="empty-text">当前没有待审资讯</span>
            <RouterLink class="empty-text-button" :to="{ name: 'NewsAdminChannels' }">管理订阅源</RouterLink>
          </div>
        </div>
      </div>
    </main>

    <ReviewDetailPanel v-if="queue.selectedId.value !== null" />

    <NewsRejectDialog
      v-model:visible="queue.rejectDialogVisible.value"
      :ids="queue.rejectIds.value"
      @rejected="queue.onRejected"
    />
    <NewsSnackbar
      v-if="queue.snackbar.value"
      :key="queue.snackbar.value.key"
      :message="queue.snackbar.value.message"
      :action-text="queue.snackbar.value.undo ? '撤销' : null"
      @action="queue.undo"
      @close="queue.closeSnackbar"
    />
  </div>
</template>

<style scoped lang="scss">
@use '../components/newsMixins' as *;

.review-page {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  overflow: hidden;
}

.review-main {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.review-head {
  flex: none;
  padding: var(--zg-space-xs) var(--zg-space-2xl) 0;
}

/* 内容区居中，最宽 960px */
.review-inner {
  max-width: 960px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
}

.review-list {
  flex: 1;
  min-height: 0;
  padding: 0 var(--zg-space-2xl) var(--zg-space-2xl);
  overflow-y: auto;
  outline: none;

  &:focus-visible {
    box-shadow: inset 0 0 0 2px var(--focus-ring);
  }
}

.list-progress {
  position: relative;
  height: 4px;
  margin-bottom: var(--zg-space-md);
  overflow: hidden;
  border-radius: var(--zg-radius-xs);
  background: var(--surface-container-highest);

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: inherit;
    background: var(--brand-indigo);
    transform-origin: left;
    animation: news-linear 1.3s var(--zg-ease-m3-standard) infinite;
  }
}

.list-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--zg-space-lg);
  padding: 96px var(--zg-space-xl);
  color: var(--second-color);
  animation: news-enter 300ms var(--zg-ease-m3-emphasized-decelerate) both;
}

.empty-text {
  color: var(--main-color);
  font-size: 15px;
  line-height: 22px;
}

.empty-text-button {
  @include text-button;
}

.empty-outline-button {
  @include text-button;
  padding: 0 var(--zg-space-xl);
  border: 1px solid var(--surface-outline);
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

@keyframes news-linear {
  0% {
    transform: translateX(-100%) scaleX(0.3);
  }
  60% {
    transform: translateX(40%) scaleX(0.5);
  }
  100% {
    transform: translateX(100%) scaleX(0.3);
  }
}
</style>
