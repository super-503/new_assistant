<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import type { NewsContentType } from '@/api/newsItem'
import NewsSourceAvatar from '../components/NewsSourceAvatar.vue'
import { useLibraryContext } from './useLibrary'

/**
 * 资讯库左侧分类栏（220px）：类型切换「文章 / 视频」，「渠道」小标题与「管理订阅源」，
 * 「全部」一行，渠道为可折叠分组（点文字查看整个渠道），其下订阅源各一行。
 */
const library = useLibraryContext()

const TYPE_TABS: { value: NewsContentType; label: string; icon: string }[] = [
  { value: 'ARTICLE', label: '文章', icon: 'newsAdmin-article' },
  { value: 'VIDEO', label: '视频', icon: 'newsAdmin-video' }
]

/** 收起的渠道；默认全部展开 */
const collapsedChannels = ref<string[]>([])

const channels = computed(() => library.typeNode.value?.channels ?? [])
const allLabel = computed(() => (library.contentType.value === 'ARTICLE' ? '全部文章' : '全部视频'))

const getTypeCount = (type: NewsContentType) =>
  library.tree.value?.types.find((item) => item.contentType === type)?.count ?? 0

const isExpanded = (channelCode: string) => !collapsedChannels.value.includes(channelCode)

const toggleChannel = (channelCode: string) => {
  collapsedChannels.value = isExpanded(channelCode)
    ? [...collapsedChannels.value, channelCode]
    : collapsedChannels.value.filter((code) => code !== channelCode)
}

/** 点渠道名：查看整个渠道并展开 */
const selectChannel = (channelCode: string) => {
  collapsedChannels.value = collapsedChannels.value.filter((code) => code !== channelCode)
  library.selectNode({ kind: 'channel', channelCode })
}

const isChannelSelected = (channelCode: string) =>
  library.node.value.kind === 'channel' && library.node.value.channelCode === channelCode

const isFeedSelected = (feedId: number) => library.node.value.kind === 'feed' && library.node.value.feedId === feedId
</script>

<template>
  <nav class="side-tree" aria-label="渠道">
    <div class="type-tabs" role="tablist" aria-label="资讯类型">
      <button
        v-for="tab in TYPE_TABS"
        :key="tab.value"
        type="button"
        role="tab"
        class="type-tab"
        :class="{ 'is-active': library.contentType.value === tab.value }"
        :aria-selected="library.contentType.value === tab.value"
        @click="library.setContentType(tab.value)"
      >
        <SvgIcon :icon-class="tab.icon" size="18" />
        {{ tab.label }}
        <span class="type-count">{{ getTypeCount(tab.value) }}</span>
      </button>
    </div>

    <div class="tree-heading">
      <span class="heading-text">渠道</span>
      <RouterLink class="manage-link" :to="{ name: 'NewsAdminChannels' }" aria-label="管理订阅源" title="管理订阅源">
        <SvgIcon icon-class="newsAdmin-gear" size="18" />
      </RouterLink>
    </div>

    <button
      type="button"
      class="tree-row"
      :class="{ 'is-active': library.node.value.kind === 'all' }"
      @click="library.selectNode({ kind: 'all' })"
    >
      <SvgIcon icon-class="newsAdmin-list-all" size="20" />
      <span class="row-name">{{ allLabel }}</span>
      <span class="row-count">{{ library.typeNode.value?.count ?? 0 }}</span>
    </button>

    <div v-for="channel in channels" :key="channel.channelCode" class="tree-group">
      <div class="group-head">
        <button
          type="button"
          class="group-toggle"
          :aria-label="`${isExpanded(channel.channelCode) ? '收起' : '展开'}${channel.channelName}`"
          :aria-expanded="isExpanded(channel.channelCode)"
          @click="toggleChannel(channel.channelCode)"
        >
          <SvgIcon
            class="toggle-chevron"
            :class="{ 'is-open': isExpanded(channel.channelCode) }"
            icon-class="newsAdmin-chevron-right"
            size="18"
          />
        </button>
        <button
          type="button"
          class="group-name"
          :class="{ 'is-active': isChannelSelected(channel.channelCode) }"
          @click="selectChannel(channel.channelCode)"
        >
          <span class="row-name">{{ channel.channelName }}</span>
          <span class="row-count">{{ channel.count }}</span>
        </button>
      </div>
      <div v-if="isExpanded(channel.channelCode)" class="group-feeds">
        <button
          v-for="feed in channel.feeds"
          :key="feed.feedId"
          type="button"
          class="tree-row is-feed"
          :class="{ 'is-active': isFeedSelected(feed.feedId) }"
          @click="library.selectNode({ kind: 'feed', channelCode: channel.channelCode, feedId: feed.feedId })"
        >
          <NewsSourceAvatar :name="feed.feedName" :src="feed.avatarUrl ?? undefined" />
          <span class="row-name">{{ feed.feedName }}</span>
          <span class="row-count">{{ feed.count }}</span>
        </button>
      </div>
    </div>
  </nav>
</template>

<style scoped lang="scss">
@use '../components/newsMixins' as *;

.side-tree {
  flex: 0 0 220px;
  display: flex;
  flex-direction: column;
  padding: var(--zg-space-lg) var(--zg-space-md) var(--zg-space-xl);
  border-right: 1px solid var(--surface-border);
  overflow-y: auto;
}

.type-tabs {
  flex: none;
  display: flex;
  gap: var(--zg-space-xs);
  padding: var(--zg-space-xs);
  border-radius: var(--zg-radius-pill);
  background: var(--surface-container);
}

.type-tab {
  @include reset-button;
  @include state-layer;
  @include focus-ring(1px);
  flex: 1;
  min-width: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--zg-space-sm);
  height: 40px;
  padding: 0 var(--zg-space-md);
  border-radius: var(--zg-radius-pill);
  color: var(--second-color);
  font-size: var(--zg-text-base);
  font-weight: var(--zg-weight-medium);
  transition:
    background var(--zg-duration-base) var(--zg-ease-m3-standard),
    color var(--zg-duration-base) var(--zg-ease-m3-standard);

  &.is-active {
    background: var(--selected-bg);
    color: var(--selected-text);
  }

  :deep(.svg-icon) {
    flex: none;
    cursor: inherit;
  }
}

.type-count,
.row-count {
  flex: none;
  font-size: var(--zg-text-sm);
  font-variant-numeric: tabular-nums;
}

.tree-heading {
  flex: none;
  display: flex;
  align-items: center;
  height: 32px;
  margin: var(--zg-space-lg) 0 var(--zg-space-xs);
  padding-left: var(--zg-space-lg);
}

.heading-text {
  flex: 1;
  color: var(--second-color);
  font-size: var(--zg-text-md);
  font-weight: var(--zg-weight-medium);
}

.manage-link {
  @include icon-button;
}

.tree-row {
  @include reset-button;
  @include state-layer;
  @include focus-ring(-2px);
  flex: none;
  display: flex;
  align-items: center;
  gap: var(--zg-space-md);
  width: 100%;
  height: 36px;
  padding: 0 var(--zg-space-lg);
  border-radius: var(--zg-radius-pill);
  color: var(--second-color);
  font-size: var(--zg-text-base);
  font-weight: var(--zg-weight-medium);
  transition: background var(--zg-duration-base) var(--zg-ease-m3-standard);

  &.is-feed {
    padding-left: 40px;
  }
  &.is-active {
    background: var(--selected-bg);
    color: var(--selected-text);
    font-weight: var(--zg-weight-semibold);
  }

  :deep(.svg-icon) {
    flex: none;
    cursor: inherit;
  }
}

.row-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-group {
  flex: none;
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-2xs);
  margin-top: var(--zg-space-md);
}

.group-head {
  display: flex;
  align-items: center;
}

.group-toggle {
  @include icon-button(24px);
  margin-left: var(--zg-space-md);
}

.toggle-chevron {
  transition: transform var(--zg-duration-base) var(--zg-ease-m3-standard);

  &.is-open {
    transform: rotate(90deg);
  }
}

.group-name {
  @include reset-button;
  @include state-layer;
  @include focus-ring(-2px);
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: var(--zg-space-md);
  height: 32px;
  padding: 0 var(--zg-space-lg) 0 var(--zg-space-sm);
  border-radius: var(--zg-radius-xl);
  color: var(--second-color);
  font-size: var(--zg-text-sm);
  line-height: 16px;
  font-weight: var(--zg-weight-medium);
  transition: color var(--zg-duration-base) var(--zg-ease-m3-standard);

  &.is-active {
    color: var(--brand-indigo);
  }
}

.group-feeds {
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-2xs);
  animation: news-enter 300ms var(--zg-ease-m3-emphasized-decelerate) both;
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
