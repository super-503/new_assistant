<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { retryFeedApi, type NewsChannelTodo, type NewsTodoFeed } from '@/api/newsChannel'
import NewsSourceAvatar from '../../components/NewsSourceAvatar.vue'
import { notifySuccess } from '../../newsNotify'

/** 「需处理」视图：凭证失效的渠道、拉取失败的订阅源 */
const props = defineProps<{
  todo: NewsChannelTodo
}>()

const emit = defineEmits<{
  /** 跳到某个渠道 */
  select: [channelCode: string]
  /** 打开渠道设置更新凭证 */
  renew: [channelCode: string]
  edit: [item: NewsTodoFeed]
  add: []
  /** 重试已加入队列，需处理汇总需要刷新 */
  changed: []
}>()

/** 订阅源在拉取时，按这个间隔刷新汇总直到拉取结束 */
const PULLING_REFRESH_MS = 3000

/** 正在提交重试的订阅源 */
const retryingIds = ref<number[]>([])

/**
 * 拉取期间汇总会变（凭证可能被置为失效、订阅源可能仍然失败），拉取结束后要再取一次最终状态。
 */
let refreshTimer: ReturnType<typeof setTimeout> | undefined

const refreshLater = () => {
  clearTimeout(refreshTimer)
  refreshTimer = setTimeout(() => emit('changed'), PULLING_REFRESH_MS)
}

const retry = async (item: NewsTodoFeed) => {
  retryingIds.value = [...retryingIds.value, item.feed.id]
  try {
    await retryFeedApi(item.feed.id)
    notifySuccess('已加入拉取队列')
    emit('changed')
    refreshLater()
  } finally {
    retryingIds.value = retryingIds.value.filter((id) => id !== item.feed.id)
  }
}

watch(
  () => props.todo,
  () => {
    clearTimeout(refreshTimer)
    if (props.todo.failedFeeds.some((item) => item.feed.pulling)) refreshLater()
  }
)

onBeforeUnmount(() => clearTimeout(refreshTimer))
</script>

<template>
  <div class="todo-view">
    <header class="view-header">
      <h1 class="page-title">需处理</h1>
      <el-button type="primary" @click="emit('add')">
        <SvgIcon icon-class="newsAdmin-add" size="18" />
        添加订阅源
      </el-button>
    </header>

    <section v-if="props.todo.credentialChannels.length > 0" class="todo-section" aria-labelledby="todo-credential">
      <h2 id="todo-credential" class="section-title">渠道凭证</h2>
      <div class="todo-list">
        <div v-for="channel in props.todo.credentialChannels" :key="channel.code" class="todo-row">
          <NewsSourceAvatar :name="channel.name" />
          <button type="button" class="channel-link" @click="emit('select', channel.code)">
            {{ channel.name }}
          </button>
          <span class="credential-state">
            <span class="state-dot" aria-hidden="true"></span>
            凭证{{ channel.credentialStatusDesc }}
          </span>
          <el-button @click="emit('renew', channel.code)">更新凭证</el-button>
        </div>
      </div>
    </section>

    <section v-if="props.todo.failedFeeds.length > 0" class="todo-section" aria-labelledby="todo-feeds">
      <h2 id="todo-feeds" class="section-title">拉取失败</h2>
      <div class="todo-list">
        <div v-for="item in props.todo.failedFeeds" :key="item.feed.id" class="todo-row">
          <span class="feed-name">{{ item.feed.displayName }}</span>
          <button type="button" class="channel-link is-muted" @click="emit('select', item.channelCode)">
            {{ item.channelName }}
          </button>
          <span class="feed-error" :title="item.feed.error ?? undefined">{{ item.feed.error }}</span>
          <el-button
            text
            type="primary"
            :disabled="item.feed.pulling || retryingIds.includes(item.feed.id)"
            @click="retry(item)"
          >
            {{ item.feed.pulling || retryingIds.includes(item.feed.id) ? '重试中' : '重试' }}
          </el-button>
          <el-button text type="primary" @click="emit('edit', item)">编辑</el-button>
        </div>
      </div>
    </section>

    <div v-if="props.todo.total === 0" class="empty-state">暂无需处理事项</div>
  </div>
</template>

<style scoped lang="scss">
@use '../../components/newsMixins' as *;

.todo-view {
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-xl);
}

.view-header {
  display: flex;
  align-items: center;
  gap: var(--zg-space-md);
  min-height: 48px;
}

.page-title {
  flex: 1;
  min-width: 0;
  margin: 0;
  font-size: var(--zg-text-2xl);
  line-height: 28px;
  font-weight: var(--zg-weight-semibold);
}

.todo-section {
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-md);
  animation: todo-enter 300ms var(--zg-ease-m3-emphasized-decelerate) both;
}

.section-title {
  margin: 0;
  padding: 0 var(--zg-space-xs);
  color: var(--second-color);
  font-size: var(--zg-text-md);
  line-height: 20px;
  font-weight: var(--zg-weight-medium);
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
  overflow: hidden;
  border-radius: var(--zg-radius-lg);
  background: var(--surface-border);
}

.todo-row {
  display: flex;
  align-items: center;
  gap: var(--zg-space-lg);
  height: 52px;
  padding: 0 var(--zg-space-lg) 0 var(--zg-space-xl);
  background: var(--third-bg-color);

  .el-button + .el-button {
    margin-left: 0;
  }
}

.channel-link {
  @include text-button;
  margin-left: calc(-1 * var(--zg-space-md));
  padding: 0 var(--zg-space-md);
  color: var(--main-color);

  &.is-muted {
    height: 28px;
    margin-left: calc(-1 * var(--zg-space-xs));
    color: var(--second-color);
    font-size: var(--zg-text-md);
    font-weight: var(--zg-weight-regular);
  }
}

.credential-state {
  flex: 1;
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: var(--zg-space-sm);
  color: var(--second-color);
  font-size: var(--zg-text-md);
}

.state-dot {
  flex: none;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--status-rejected);
}

.feed-name {
  flex: 0 1 auto;
  min-width: 0;
  overflow: hidden;
  font-weight: var(--zg-weight-medium);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.feed-error {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: var(--status-rejected);
  font-size: var(--zg-text-md);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-state {
  display: flex;
  justify-content: center;
  padding: 64px var(--zg-space-xl);
  color: var(--second-color);
}

@keyframes todo-enter {
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
