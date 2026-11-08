<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { listFeedsApi, type NewsChannel, type NewsFeed, type NewsFeedStatus } from '@/api/newsChannel'
import NewsFilterChip, { type NewsFilterOption } from '../../components/NewsFilterChip.vue'
import FeedRow from './FeedRow.vue'

/** 右侧当前渠道视图：头部、凭证失效警示条、工具栏、订阅源表格与空状态 */
const props = defineProps<{
  channel: NewsChannel
}>()

const emit = defineEmits<{
  openSettings: []
  /** 打开运行记录；从订阅源打开时带上该订阅源 */
  openRuns: [feed: NewsFeed | null]
  add: []
  edit: [feed: NewsFeed]
  /** 订阅源有变化，渠道列表的数量与状态点需要刷新 */
  changed: []
}>()

const STATUS_OPTIONS: NewsFilterOption[] = [
  { value: 'OK', label: '正常' },
  { value: 'ERROR', label: '拉取失败' },
  { value: 'PENDING', label: '待首次拉取' },
  { value: 'DISABLED', label: '已停用' }
]

/** 有订阅源在拉取时，按这个间隔刷新列表直到拉取结束 */
const PULLING_REFRESH_MS = 3000
/** 搜索输入停顿后再请求 */
const SEARCH_DEBOUNCE_MS = 300

const keyword = ref('')
const statusFilter = ref<string[]>([])
const feeds = ref<NewsFeed[] | null>(null)

const hasFilter = computed(() => keyword.value.trim() !== '' || statusFilter.value.length > 0)
const hasCredential = computed(() => props.channel.credentialStatus !== 'NONE')
const credentialInvalid = computed(() => props.channel.credentialStatus === 'INVALID')

let refreshTimer: ReturnType<typeof setTimeout> | undefined
let searchTimer: ReturnType<typeof setTimeout> | undefined
/** 只采用最后一次请求的结果，切换渠道或筛选时旧请求作废 */
let requestSeq = 0
/** 已提交拉取，等拉取结束后再刷新渠道概览，因为拉取可能把渠道凭证置为失效 */
let overviewRefreshPending = false

const loadFeeds = async () => {
  clearTimeout(refreshTimer)
  const seq = ++requestSeq
  const keywordText = keyword.value.trim()
  const result = await listFeedsApi(props.channel.code, {
    status: statusFilter.value[0] as NewsFeedStatus | undefined,
    keyword: keywordText === '' ? undefined : keywordText
  })
  if (seq !== requestSeq) return
  feeds.value = result
  if (result.some((feed) => feed.pulling)) {
    refreshTimer = setTimeout(loadFeeds, PULLING_REFRESH_MS)
    return
  }
  if (overviewRefreshPending) {
    overviewRefreshPending = false
    emit('changed')
  }
}

const onFeedChanged = () => {
  void loadFeeds()
  emit('changed')
}

/** 拉取提交后先刷新列表显示「拉取中」，拉取结束时由 loadFeeds 再刷新渠道概览 */
const onPullQueued = () => {
  overviewRefreshPending = true
  void loadFeeds()
}

const clearFilters = () => {
  keyword.value = ''
  statusFilter.value = []
}

watch(
  () => props.channel.code,
  () => {
    keyword.value = ''
    statusFilter.value = []
    feeds.value = null
    void loadFeeds()
  },
  { immediate: true }
)

watch(statusFilter, () => void loadFeeds())

watch(keyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(loadFeeds, SEARCH_DEBOUNCE_MS)
})

onBeforeUnmount(() => {
  clearTimeout(refreshTimer)
  clearTimeout(searchTimer)
})

defineExpose({ reload: loadFeeds })
</script>

<template>
  <div class="channel-view">
    <header class="view-header">
      <div class="header-title">
        <h1 class="page-title">{{ props.channel.name }}</h1>
        <div class="header-meta">
          <span v-if="hasCredential" class="credential-state">
            <span class="state-dot" :class="credentialInvalid ? 'is-error' : 'is-ok'" aria-hidden="true"></span>
            凭证{{ props.channel.credentialStatusDesc }}
          </span>
          <span v-if="!props.channel.enabled">已停用</span>
        </div>
      </div>
      <el-button text type="primary" @click="emit('openRuns', null)">
        <SvgIcon icon-class="newsAdmin-history" size="18" />
        运行记录
      </el-button>
      <el-button text type="primary" @click="emit('openSettings')">
        <SvgIcon icon-class="newsAdmin-gear" size="18" />
        渠道设置
      </el-button>
    </header>

    <div v-if="credentialInvalid" class="credential-banner" role="alert">
      <SvgIcon icon-class="newsAdmin-warning" size="20" />
      <span class="banner-text">凭证已失效</span>
      <el-button @click="emit('openSettings')">更新凭证</el-button>
    </div>

    <div class="toolbar">
      <el-input
        v-model="keyword"
        class="news-search-input toolbar-search"
        placeholder="搜索订阅源"
        aria-label="搜索订阅源"
      >
        <template #prefix>
          <SvgIcon icon-class="newsAdmin-search" size="18" />
        </template>
      </el-input>
      <NewsFilterChip v-model="statusFilter" label="状态" :options="STATUS_OPTIONS" />
      <el-button type="primary" class="add-button" @click="emit('add')">
        <SvgIcon icon-class="newsAdmin-add" size="18" />
        添加订阅源
      </el-button>
    </div>

    <div v-if="feeds && feeds.length > 0" class="feed-table" role="table" aria-label="订阅源">
      <div class="table-head" role="row">
        <span role="columnheader">订阅源</span>
        <span role="columnheader">类型</span>
        <span role="columnheader">最近拉取</span>
        <span role="columnheader">状态</span>
        <span role="columnheader">操作</span>
      </div>
      <FeedRow
        v-for="feed in feeds"
        :key="feed.id"
        :feed="feed"
        @edit="emit('edit', $event)"
        @open-runs="emit('openRuns', $event)"
        @changed="onFeedChanged"
        @pull-queued="onPullQueued"
      />
    </div>

    <div v-else-if="feeds && hasFilter" class="empty-state">
      <span>没有符合条件的订阅源</span>
      <el-button plain @click="clearFilters">清除筛选</el-button>
    </div>

    <div v-else-if="feeds" class="empty-state">暂无订阅源</div>
  </div>
</template>

<style scoped lang="scss">
.channel-view {
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-xl);
}

.view-header {
  display: flex;
  align-items: center;
  gap: var(--zg-space-md);
  min-height: 48px;

  .el-button + .el-button {
    margin-left: 0;
  }
}

.header-title {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--zg-space-xs) var(--zg-space-lg);
}

.page-title {
  margin: 0;
  font-size: var(--zg-text-2xl);
  line-height: 28px;
  font-weight: var(--zg-weight-semibold);
}

.header-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--zg-space-xs) var(--zg-space-lg);
  color: var(--second-color);
  font-size: var(--zg-text-md);
}

.credential-state {
  display: inline-flex;
  align-items: center;
  gap: var(--zg-space-sm);
}

.state-dot {
  flex: none;
  width: 8px;
  height: 8px;
  border-radius: 50%;

  &.is-ok {
    background: var(--status-approved);
  }
  &.is-error {
    background: var(--status-rejected);
  }
}

.credential-banner {
  display: flex;
  align-items: center;
  gap: var(--zg-space-lg);
  padding: var(--zg-space-lg) var(--zg-space-xl);
  border-radius: var(--zg-radius-lg);
  background: var(--status-rejected-container);
  color: var(--on-status-rejected-container);
  animation: channel-enter 300ms var(--zg-ease-m3-emphasized-decelerate) both;
}

.banner-text {
  flex: 1;
  min-width: 0;
  font-weight: var(--zg-weight-medium);
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--zg-space-md);
}

.toolbar-search {
  flex: 0 1 240px;
  min-width: 160px;
}

.add-button {
  margin-left: auto;
}

.feed-table {
  --feed-columns: minmax(240px, 2.4fr) repeat(3, minmax(120px, 1fr)) minmax(300px, 2fr);

  min-width: 880px;
  border-radius: var(--zg-radius-lg);
  background: var(--third-bg-color);
  animation: channel-enter 300ms var(--zg-ease-m3-emphasized-decelerate) both;
}

.table-head {
  display: grid;
  grid-template-columns: var(--feed-columns);
  gap: var(--zg-space-lg);
  align-items: center;
  height: 40px;
  padding: 0 20px;
  color: var(--second-color);
  font-size: var(--zg-text-sm);
  font-weight: var(--zg-weight-medium);
  letter-spacing: 0.5px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--zg-space-lg);
  padding: 64px var(--zg-space-xl);
  color: var(--second-color);
  animation: channel-enter 300ms var(--zg-ease-m3-emphasized-decelerate) both;
}

@keyframes channel-enter {
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
