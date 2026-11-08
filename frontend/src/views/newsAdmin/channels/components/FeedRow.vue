<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  deleteFeedApi,
  pullFeedApi,
  retryFeedApi,
  setFeedEnabledApi,
  type NewsFeed,
  type NewsFeedStatus
} from '@/api/newsChannel'
import { formatItemTime } from '../../components/newsFormat'
import NewsSpinner from '../../components/NewsSpinner.vue'
import { confirmAction, notifySuccess } from '../../newsNotify'

/** 订阅源表格的一行：启用开关、立即拉取、重试、编辑、更多（运行记录 / 删除订阅源） */
const props = defineProps<{
  feed: NewsFeed
}>()

const emit = defineEmits<{
  edit: [feed: NewsFeed]
  /** 打开该订阅源所在渠道的运行记录 */
  openRuns: [feed: NewsFeed]
  /** 订阅源的启用或删除已生效，需要重新加载 */
  changed: []
  /** 拉取已提交队列，结束后还需要刷新渠道概览：拉取可能把渠道凭证置为失效 */
  pullQueued: []
}>()

/** 状态点的配色类名 */
const STATUS_DOT: Record<NewsFeedStatus, string> = {
  OK: 'is-ok',
  ERROR: 'is-error',
  PENDING: 'is-muted',
  DISABLED: 'is-muted'
}

/** 首字头像：按码点取，避免拆开代理对 */
const initial = computed(() => Array.from(props.feed.displayName)[0] ?? '')

const lastPullText = computed(() => {
  if (props.feed.pulling) return '拉取中'
  return props.feed.lastPullTime ? formatItemTime(props.feed.lastPullTime) : '—'
})

const isRequesting = ref(false)

/** 启用开关：接口成功后由列表重新加载出新状态 */
const beforeToggle = async () => {
  await setFeedEnabledApi(props.feed.id, !props.feed.enabled)
  emit('changed')
  return true
}

const pull = async () => {
  isRequesting.value = true
  try {
    await pullFeedApi(props.feed.id)
    notifySuccess('已加入拉取队列')
    emit('pullQueued')
  } finally {
    isRequesting.value = false
  }
}

const retry = async () => {
  isRequesting.value = true
  try {
    await retryFeedApi(props.feed.id)
    notifySuccess('已加入拉取队列')
    emit('pullQueued')
  } finally {
    isRequesting.value = false
  }
}

const remove = async () => {
  const confirmed = await confirmAction({
    title: '删除订阅源？',
    message: `「${props.feed.displayName}」将不再拉取，已入库的资讯保留。`,
    confirmText: '删除',
    danger: true
  })
  if (!confirmed) return
  await deleteFeedApi(props.feed.id)
  notifySuccess(`已删除「${props.feed.displayName}」`)
  emit('changed')
}

const onMenuCommand = (command: 'runs' | 'delete') => {
  if (command === 'runs') emit('openRuns', props.feed)
  else void remove()
}
</script>

<template>
  <div class="feed-row" role="row">
    <div class="cell-name" role="cell">
      <span class="feed-avatar" aria-hidden="true">
        <img
          v-if="props.feed.avatarUrl"
          :src="props.feed.avatarUrl"
          alt=""
          loading="lazy"
          referrerpolicy="no-referrer"
        />
        <template v-else>{{ initial }}</template>
      </span>
      <span class="name-text">
        <span class="feed-name">{{ props.feed.displayName }}</span>
        <span v-if="props.feed.description" class="feed-desc">{{ props.feed.description }}</span>
      </span>
    </div>
    <span class="cell-muted" role="cell">{{ props.feed.contentTypeDesc }}</span>
    <span class="cell-muted cell-number" role="cell">{{ lastPullText }}</span>
    <span class="cell-status" role="cell">
      <span class="status-line">
        <span class="status-dot" :class="STATUS_DOT[props.feed.status]" aria-hidden="true"></span>
        {{ props.feed.statusDesc }}
      </span>
      <span v-if="props.feed.status === 'ERROR'" class="status-error">
        <span class="error-text" :title="props.feed.error ?? undefined">{{ props.feed.error }}</span>
        <button type="button" class="retry-button" :disabled="props.feed.pulling || isRequesting" @click="retry">
          重试
        </button>
      </span>
    </span>
    <div class="cell-actions" role="cell">
      <el-switch
        :model-value="props.feed.enabled"
        :before-change="beforeToggle"
        :aria-label="`启用：${props.feed.displayName}`"
      />
      <el-button text type="primary" :disabled="props.feed.pulling || isRequesting" @click="pull">
        <NewsSpinner v-if="props.feed.pulling" />
        {{ props.feed.pulling ? '拉取中' : '立即拉取' }}
      </el-button>
      <el-button text type="primary" @click="emit('edit', props.feed)">编辑</el-button>
      <el-dropdown trigger="click" popper-class="news-admin-popper" :show-arrow="false" @command="onMenuCommand">
        <button type="button" class="more-button" :aria-label="`更多操作：${props.feed.displayName}`">
          <SvgIcon icon-class="newsAdmin-more-vert" size="20" />
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="runs">
              <SvgIcon icon-class="newsAdmin-history" size="20" />
              运行记录
            </el-dropdown-item>
            <el-dropdown-item command="delete">
              <SvgIcon icon-class="newsAdmin-delete" size="20" />
              删除订阅源
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '../../components/newsMixins' as *;

.feed-row {
  display: grid;
  grid-template-columns: var(--feed-columns);
  gap: var(--zg-space-lg);
  align-items: center;
  min-height: 56px;
  padding: var(--zg-space-md) 20px;
  border-top: 1px solid var(--surface-border);
}

.cell-name {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: var(--zg-space-lg);
}

.feed-avatar {
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  overflow: hidden;
  border-radius: 50%;
  background: var(--surface-container-highest);
  color: var(--second-color);
  font-size: var(--zg-text-base);
  font-weight: var(--zg-weight-medium);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.name-text {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.feed-name,
.feed-desc {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.feed-name {
  font-weight: var(--zg-weight-medium);
}

.feed-desc {
  color: var(--second-color);
  font-size: var(--zg-text-sm);
  line-height: 16px;
}

.cell-muted {
  color: var(--second-color);
}

.cell-number {
  font-variant-numeric: tabular-nums;
}

.cell-status {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-2xs);
}

.status-line {
  display: inline-flex;
  align-items: center;
  gap: var(--zg-space-md);
}

.status-dot {
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
  &.is-muted {
    background: var(--surface-outline);
  }
}

.status-error {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: var(--zg-space-xs);
  color: var(--status-rejected);
  font-size: var(--zg-text-sm);
}

.error-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.retry-button {
  @include text-button;
  height: 28px;
  padding: 0 var(--zg-space-md);
  font-size: var(--zg-text-sm);

  &:disabled {
    color: var(--text-disabled);
    cursor: not-allowed;
  }
}

.cell-actions {
  display: flex;
  align-items: center;
  gap: var(--zg-space-xs);

  .el-switch {
    margin-right: var(--zg-space-xs);
  }

  .el-button + .el-button {
    margin-left: 0;
  }
}

.more-button {
  @include icon-button;
}
</style>
