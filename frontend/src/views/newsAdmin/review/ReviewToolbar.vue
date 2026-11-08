<script setup lang="ts">
import { computed, onBeforeUnmount } from 'vue'
import { debounce } from 'lodash-es'
import NewsFilterChip, { type NewsFilterOption } from '../components/NewsFilterChip.vue'
import NewsTimeFilterChip, { type NewsDatePeriod } from '../components/NewsTimeFilterChip.vue'
import ReviewCheckbox from './ReviewCheckbox.vue'
import { useReviewQueueContext, type ReviewDensity } from './useReviewQueue'

/**
 * 审核工具栏：全选、搜索、筛选（类型 / 渠道 / 订阅源 / 时间 / 各标签字段 / 排序）、卡片 / 紧凑切换。
 * 勾选任意一条后原地切换为批量操作栏。
 */
const queue = useReviewQueueContext()

/** 搜索输入停顿后再查询 */
const SEARCH_DELAY_MS = 300

const TYPE_OPTIONS: NewsFilterOption[] = [
  { value: 'ARTICLE', label: '文章' },
  { value: 'VIDEO', label: '视频' }
]
const SORT_OPTIONS: NewsFilterOption[] = [
  { value: 'NEWEST', label: '最新' },
  { value: 'OLDEST', label: '最早' }
]
const DENSITY_OPTIONS: { value: ReviewDensity; label: string; icon: string }[] = [
  { value: 'card', label: '卡片', icon: 'newsAdmin-view-card' },
  { value: 'compact', label: '紧凑', icon: 'newsAdmin-view-compact' }
]

const hasSelection = computed(() => queue.checkedIds.value.length > 0)
/** 详情面板打开时收起订阅源筛选，给列表留出宽度 */
const isFeedChipVisible = computed(() => queue.selectedId.value === null)

const channelOptions = computed<NewsFilterOption[]>(() =>
  queue.filterOptions.value.channels.map((channel) => ({ value: channel.channelCode, label: channel.channelName }))
)

/** 订阅源随所选渠道、类型收窄 */
const feedOptions = computed<NewsFilterOption[]>(() =>
  queue.filterOptions.value.channels
    .filter((channel) => queue.filters.channelCode.length === 0 || channel.channelCode === queue.filters.channelCode[0])
    .flatMap((channel) => channel.feeds)
    .filter((feed) => queue.filters.contentType.length === 0 || feed.contentType === queue.filters.contentType[0])
    .map((feed) => ({ value: String(feed.feedId), label: feed.feedName }))
)

const onSearch = debounce(() => queue.onFiltersChanged(), SEARCH_DELAY_MS)

const onKeywordInput = (event: Event) => {
  queue.filters.keyword = (event.target as HTMLInputElement).value
  onSearch()
}

const onTypeChange = (value: string[]) => {
  queue.filters.contentType = value
  queue.filters.feedId = []
  queue.onFiltersChanged()
}

const onChannelChange = (value: string[]) => {
  queue.filters.channelCode = value
  queue.filters.feedId = []
  queue.onFiltersChanged()
}

const onFeedChange = (value: string[]) => {
  queue.filters.feedId = value
  queue.onFiltersChanged()
}

const onPeriodChange = (value: NewsDatePeriod | null) => {
  queue.filters.publishPeriod = value
  queue.onFiltersChanged()
}

const onTagChange = (fieldKey: string, value: string[]) => {
  queue.filters.tags = { ...queue.filters.tags, [fieldKey]: value }
  queue.onFiltersChanged()
}

const onSortChange = (value: string[]) => {
  queue.filters.sort = value
  queue.onFiltersChanged()
}

onBeforeUnmount(() => onSearch.cancel())
</script>

<template>
  <div class="review-toolbar" :class="{ 'has-selection': hasSelection }">
    <ReviewCheckbox :state="queue.checkState.value" label="全选" @toggle="queue.toggleAllChecked" />

    <div v-if="!hasSelection" class="toolbar-filters">
      <div class="search-box">
        <SvgIcon class="search-icon" icon-class="newsAdmin-search" size="18" />
        <input
          type="search"
          class="search-input"
          aria-label="搜索标题或来源"
          placeholder="搜索标题或来源"
          :value="queue.filters.keyword"
          @input="onKeywordInput"
        />
      </div>
      <NewsFilterChip
        label="类型"
        :options="TYPE_OPTIONS"
        :model-value="queue.filters.contentType"
        @update:model-value="onTypeChange"
      />
      <NewsFilterChip
        label="渠道"
        :options="channelOptions"
        :model-value="queue.filters.channelCode"
        @update:model-value="onChannelChange"
      />
      <NewsFilterChip
        v-if="isFeedChipVisible"
        label="订阅源"
        :options="feedOptions"
        :model-value="queue.filters.feedId"
        @update:model-value="onFeedChange"
      />
      <NewsTimeFilterChip :model-value="queue.filters.publishPeriod" @update:model-value="onPeriodChange" />
      <NewsFilterChip
        v-for="tagField in queue.filterOptions.value.tagFields"
        :key="tagField.fieldKey"
        :label="tagField.name"
        :options="tagField.options.map((option) => ({ value: option.value, label: option.value }))"
        :model-value="queue.filters.tags[tagField.fieldKey] ?? []"
        @update:model-value="onTagChange(tagField.fieldKey, $event)"
      />
      <NewsFilterChip
        label="排序"
        neutral
        :options="SORT_OPTIONS"
        :model-value="queue.filters.sort"
        @update:model-value="onSortChange"
      />
    </div>

    <div v-else class="toolbar-batch">
      <span class="batch-count">已选 {{ queue.checkedIds.value.length }} 条</span>
      <template v-if="queue.status.value === 'PENDING'">
        <button type="button" class="batch-button" @click="queue.approve([...queue.checkedIds.value])">
          <SvgIcon class="icon-approve" icon-class="newsAdmin-check" size="16" />
          批量通过
        </button>
        <button type="button" class="batch-button" @click="queue.openReject([...queue.checkedIds.value])">
          <SvgIcon class="icon-reject" icon-class="newsAdmin-close" size="16" />
          批量拒绝
        </button>
      </template>
      <button v-else type="button" class="batch-button" @click="queue.restore([...queue.checkedIds.value])">
        <SvgIcon icon-class="newsAdmin-restore" size="16" />
        批量恢复到待审
      </button>
      <button type="button" class="batch-clear" @click="queue.clearChecked">取消选择</button>
    </div>

    <div class="toolbar-end">
      <span class="density-group" role="group" aria-label="显示密度">
        <button
          v-for="option in DENSITY_OPTIONS"
          :key="option.value"
          type="button"
          class="density-button"
          :class="{ 'is-active': queue.density.value === option.value }"
          :aria-label="option.label"
          :title="option.label"
          :aria-pressed="queue.density.value === option.value"
          @click="queue.density.value = option.value"
        >
          <SvgIcon :icon-class="option.icon" size="20" />
        </button>
      </span>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '../components/newsMixins' as *;

.review-toolbar {
  display: flex;
  align-items: center;
  gap: var(--zg-space-md);
  min-height: 52px;
  margin: var(--zg-space-xs) 0;
  padding: var(--zg-space-md) var(--zg-space-xs);
  border-radius: var(--zg-radius-lg);
  transition: background 250ms var(--zg-ease-m3-standard);

  &.has-selection {
    background: var(--surface-container-high);
  }
}

.toolbar-filters,
.toolbar-batch {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--zg-space-md);
}

.toolbar-batch {
  animation: news-enter 300ms var(--zg-ease-m3-emphasized-decelerate) both;
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

.batch-count {
  margin-right: var(--zg-space-xs);
  font-size: var(--zg-text-base);
  font-weight: var(--zg-weight-medium);
  font-variant-numeric: tabular-nums;
}

.batch-button {
  @include tonal-pill(var(--surface-container-highest));
}

.icon-approve {
  color: var(--status-approved);
}

.icon-reject {
  color: var(--status-rejected);
}

.batch-clear {
  @include text-button;
  font-size: var(--zg-text-md);
}

.toolbar-end {
  flex: none;
  display: inline-flex;
  align-items: center;
  gap: var(--zg-space-2xs);
  margin-left: auto;
}

.density-group {
  display: inline-flex;
  gap: var(--zg-space-2xs);
}

.density-button {
  @include icon-button;
  transition: background var(--zg-duration-base) var(--zg-ease-m3-standard);

  &.is-active {
    background: var(--selected-bg);
    color: var(--selected-text);
  }
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
