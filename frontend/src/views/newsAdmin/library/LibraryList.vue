<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { debounce } from 'lodash-es'
import NewsFilterChip from '../components/NewsFilterChip.vue'
import NewsItemCard from '../components/NewsItemCard.vue'
import NewsTimeFilterChip, { type NewsDatePeriod } from '../components/NewsTimeFilterChip.vue'
import { formatDay } from '../components/newsFormat'
import LibraryVideoCard from './LibraryVideoCard.vue'
import { useLibraryContext } from './useLibrary'

/**
 * 资讯库列表（居中最宽 960px）：标题与条数同一行，筛选为搜索、时间、各标签字段（多选）；
 * 文章为行式列表，视频为最小 220px 的卡片网格。详情打开时列表只隐藏不卸载，保留滚动位置。
 */
const library = useLibraryContext()

/** 搜索输入停顿后再查询 */
const SEARCH_DELAY_MS = 300
/** 距底部不足该距离时加载下一页 */
const LOAD_MORE_THRESHOLD = 240
/** 入场延迟最多 8 档，每档 40ms */
const ENTER_STAGGER_LIMIT = 8
const ENTER_STAGGER_MS = 40

const scrollRef = ref<HTMLElement>()

const isArticle = computed(() => library.contentType.value === 'ARTICLE')
const nodeTitle = computed(() => {
  if (library.node.value.kind === 'feed') return library.feedNode.value?.feedName ?? ''
  if (library.node.value.kind === 'channel') return library.channelNode.value?.channelName ?? ''
  return isArticle.value ? '全部文章' : '全部视频'
})
const nodeSubtitle = computed(() => {
  const unit = isArticle.value ? '篇' : '条'
  const channelPrefix = library.node.value.kind === 'feed' ? `${library.channelNode.value?.channelName ?? ''} · ` : ''
  return `${channelPrefix}${library.total.value} ${unit}`
})
const isSettled = computed(() => library.hasLoaded.value && !library.isLoading.value && library.tree.value !== null)
const isLibraryEmpty = computed(() => isSettled.value && library.nodeCount.value === 0)
const isFilteredEmpty = computed(
  () => isSettled.value && library.nodeCount.value > 0 && library.items.value.length === 0
)

const getEnterDelay = (index: number) => `${Math.min(index, ENTER_STAGGER_LIMIT) * ENTER_STAGGER_MS}ms`

const onSearch = debounce(() => library.onFiltersChanged(), SEARCH_DELAY_MS)

const onKeywordInput = (event: Event) => {
  library.filters.keyword = (event.target as HTMLInputElement).value
  onSearch()
}

const onPeriodChange = (value: NewsDatePeriod | null) => {
  library.filters.publishPeriod = value
  library.onFiltersChanged()
}

const onTagChange = (fieldKey: string, value: string[]) => {
  library.filters.tags = { ...library.filters.tags, [fieldKey]: value }
  library.onFiltersChanged()
}

const isNearBottom = () => {
  const scroller = scrollRef.value!
  return scroller.scrollTop + scroller.clientHeight >= scroller.scrollHeight - LOAD_MORE_THRESHOLD
}

/** 滚动到接近底部、或内容不足一屏时继续加载下一页 */
const loadMoreIfNeeded = async () => {
  while (library.hasMore.value && !library.isLoading.value && isNearBottom()) {
    const loadedCount = library.items.value.length
    await library.loadItems(false)
    await nextTick()
    if (library.items.value.length === loadedCount) return
  }
}

/** 条目数变化后补足一屏 */
watch(
  () => library.items.value.length,
  () => nextTick(loadMoreIfNeeded)
)

onBeforeUnmount(() => onSearch.cancel())
</script>

<template>
  <section class="library-list" :class="{ 'is-covered': library.isDetailOpen.value }" aria-label="资讯列表">
    <header class="list-head">
      <div class="head-title-row">
        <h1 class="head-title">{{ nodeTitle }}</h1>
        <span class="head-count">{{ nodeSubtitle }}</span>
      </div>
      <div class="head-filters">
        <div class="search-box">
          <SvgIcon class="search-icon" icon-class="newsAdmin-search" size="18" />
          <input
            type="search"
            class="search-input"
            aria-label="搜索资讯"
            placeholder="搜索标题"
            :value="library.filters.keyword"
            @input="onKeywordInput"
          />
        </div>
        <NewsTimeFilterChip :model-value="library.filters.publishPeriod" @update:model-value="onPeriodChange" />
        <NewsFilterChip
          v-for="tagField in library.tagFields.value"
          :key="tagField.fieldKey"
          :label="tagField.name"
          multiple
          :options="tagField.options.map((option) => ({ value: option.value, label: option.value }))"
          :model-value="library.filters.tags[tagField.fieldKey] ?? []"
          @update:model-value="onTagChange(tagField.fieldKey, $event)"
        />
      </div>
    </header>

    <div ref="scrollRef" class="list-scroll" @scroll.passive="loadMoreIfNeeded">
      <div v-if="isArticle" class="list-inner">
        <div
          v-for="(item, index) in library.items.value"
          :key="item.id"
          class="article-row"
          :class="{ 'is-selected': library.selectedId.value === item.id }"
          :style="{ animationDelay: getEnterDelay(index) }"
        >
          <NewsItemCard
            :item="item"
            :meta="`${item.feedName ?? item.channelName} · ${formatDay(item.publishTime)}`"
            @open="library.openItem(item.id)"
          />
        </div>
      </div>
      <div v-else class="list-inner video-grid">
        <LibraryVideoCard
          v-for="(item, index) in library.items.value"
          :key="item.id"
          class="video-cell"
          :style="{ animationDelay: getEnterDelay(index) }"
          :item="item"
          :is-selected="library.selectedId.value === item.id"
          @open="library.openItem(item.id)"
        />
      </div>

      <div v-if="isLibraryEmpty" class="list-empty">
        <span class="empty-text">暂无已通过的资讯</span>
        <RouterLink class="empty-text-button" :to="{ name: 'NewsAdminReview' }">前往审核</RouterLink>
      </div>
      <div v-else-if="isFilteredEmpty" class="list-empty">
        <span class="empty-text">没有符合条件的资讯</span>
        <button type="button" class="empty-outline-button" @click="library.clearFilters">清除筛选</button>
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
@use '../components/newsMixins' as *;

.library-list {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  &.is-covered {
    visibility: hidden;
  }
}

/* 列表内容区最宽 960px，加左右各 24px 内边距 */
.list-head {
  flex: none;
  width: 100%;
  max-width: 1008px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-lg);
  padding: 20px var(--zg-space-2xl) var(--zg-space-lg);
}

.head-title-row {
  display: flex;
  align-items: baseline;
  gap: var(--zg-space-lg);
  min-width: 0;
}

.head-title {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  font-size: var(--zg-text-2xl);
  line-height: 28px;
  font-weight: var(--zg-weight-semibold);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.head-count {
  flex: none;
  color: var(--second-color);
  font-size: var(--zg-text-md);
  line-height: 20px;
  font-variant-numeric: tabular-nums;
}

.head-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--zg-space-md);
}

.search-box {
  position: relative;
  flex: 0 1 240px;
  min-width: 160px;
}

.search-icon {
  position: absolute;
  top: 9px;
  left: var(--zg-space-lg);
  color: var(--second-color);
}

.search-input {
  width: 100%;
  height: 36px;
  padding: 0 var(--zg-space-lg) 0 38px;
  border: 0;
  border-radius: var(--zg-radius-pill);
  background: var(--surface-container-high);
  color: var(--main-color);
  font-family: inherit;
  font-size: var(--zg-text-base);

  &::placeholder {
    color: var(--control-placeholder);
  }
  &:focus {
    outline: none;
    box-shadow: inset 0 0 0 2px var(--control-focus-border);
  }
}

.list-scroll {
  flex: 1;
  min-height: 0;
  padding: 0 var(--zg-space-2xl) var(--zg-space-2xl);
  overflow-y: auto;
}

.list-inner {
  max-width: 960px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
}

.article-row {
  padding: var(--zg-space-md) var(--zg-space-xs);
  border-bottom: 1px solid var(--surface-border);
  box-shadow: inset 3px 0 0 transparent;
  animation: news-enter 300ms var(--zg-ease-m3-emphasized-decelerate) both;
  transition: background-color var(--zg-duration-base) var(--zg-ease-m3-standard);

  &:hover {
    background-image: linear-gradient(var(--row-hover-overlay), var(--row-hover-overlay));
  }
  &.is-selected {
    background-color: var(--third-bg-color);
    box-shadow: inset 3px 0 0 var(--brand-indigo);
  }
}

.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--zg-space-xl);
  padding: var(--zg-space-xs) 0;
}

.video-cell {
  animation: news-enter 300ms var(--zg-ease-m3-emphasized-decelerate) both;
}

.list-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--zg-space-lg);
  padding: 96px var(--zg-space-xl);
  animation: news-enter 300ms var(--zg-ease-m3-emphasized-decelerate) both;
}

.empty-text {
  font-size: var(--zg-text-base);
}

.empty-text-button {
  @include text-button;
  font-size: var(--zg-text-md);
}

.empty-outline-button {
  @include text-button;
  padding: 0 var(--zg-space-xl);
  border: 1px solid var(--surface-outline);
  font-size: var(--zg-text-md);
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
