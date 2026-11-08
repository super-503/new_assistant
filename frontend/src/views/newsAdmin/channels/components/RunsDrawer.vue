<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { listChannelRunsApi, type NewsChannel, type NewsChannelRun } from '@/api/newsChannel'
import { formatItemTime } from '../../components/newsFormat'

/**
 * 运行记录侧边栏：只读，按渠道列出每次运行的时间、触发方式、结果、新增条数，点开看详情。
 * 从订阅源打开时，展开并定位到与该订阅源有关的最近一次运行（定时运行或该订阅源的手动运行）。
 */
const props = defineProps<{
  channel: NewsChannel
  /** 从订阅源打开时为该订阅源 id；从渠道头部打开为 null */
  focusFeedId: number | null
}>()

const visible = defineModel<boolean>({ required: true })

const runs = ref<NewsChannelRun[] | null>(null)
const expandedIds = ref<number[]>([])
const listRef = ref<HTMLElement>()

const toggle = (id: number) => {
  expandedIds.value = expandedIds.value.includes(id)
    ? expandedIds.value.filter((item) => item !== id)
    : [...expandedIds.value, id]
}

const onOpen = async () => {
  runs.value = null
  expandedIds.value = []
  const result = await listChannelRunsApi(props.channel.code)
  runs.value = result
  if (props.focusFeedId === null) return
  const latest = result.find((run) => run.triggerType === 'SCHEDULE' || run.feedId === props.focusFeedId)
  if (!latest) return
  expandedIds.value = [latest.id]
  await nextTick()
  listRef.value?.querySelector(`[data-run-id="${latest.id}"]`)?.scrollIntoView({ block: 'nearest' })
}
</script>

<template>
  <el-drawer
    v-model="visible"
    class="news-admin-drawer runs-drawer"
    :title="`${props.channel.name}运行记录`"
    size="440px"
    append-to-body
    @open="onOpen"
  >
    <p v-if="runs && runs.length === 0" class="runs-empty">暂无运行记录</p>
    <ul v-else-if="runs" ref="listRef" class="run-list">
      <li v-for="run in runs" :key="run.id" class="run-item" :data-run-id="run.id">
        <button type="button" class="run-head" :aria-expanded="expandedIds.includes(run.id)" @click="toggle(run.id)">
          <SvgIcon
            class="run-chevron"
            :class="{ 'is-open': expandedIds.includes(run.id) }"
            icon-class="newsAdmin-chevron-right"
            size="18"
          />
          <span class="run-time">{{ formatItemTime(run.startTime) }}</span>
          <span class="run-trigger">{{ run.triggerTypeDesc }}</span>
          <span class="run-result">
            <span
              class="result-dot"
              :class="run.status === 'SUCCEEDED' ? 'is-ok' : 'is-error'"
              aria-hidden="true"
            ></span>
            {{ run.statusDesc }}
          </span>
          <span class="run-count">新增 {{ run.newCount }}</span>
        </button>
        <div v-if="expandedIds.includes(run.id)" class="run-detail">
          <p v-if="run.feedName" class="detail-feed">订阅源：{{ run.feedName }}</p>
          <p class="detail-text">{{ run.detail }}</p>
        </div>
      </li>
    </ul>
  </el-drawer>
</template>

<style scoped lang="scss">
@use '../../components/newsMixins' as *;

.runs-empty {
  margin: 0;
  padding: 48px var(--zg-space-xl);
  color: var(--second-color);
  text-align: center;
}

.run-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.run-item {
  border-bottom: 1px solid var(--surface-border);
}

.run-head {
  @include reset-button;
  @include state-layer;
  @include focus-ring(-2px);
  display: grid;
  grid-template-columns: 20px minmax(0, 1fr) auto auto auto;
  align-items: center;
  gap: var(--zg-space-lg);
  width: 100%;
  height: 44px;
  padding: 0 var(--zg-space-md);
  border-radius: var(--zg-radius-md);
  color: var(--main-color);
  font-size: var(--zg-text-base);

  :deep(.svg-icon) {
    cursor: inherit;
  }
}

.run-chevron {
  color: var(--second-color);
  transition: transform var(--zg-duration-base) var(--zg-ease-m3-standard);

  &.is-open {
    transform: rotate(90deg);
  }
}

.run-time,
.run-count {
  font-variant-numeric: tabular-nums;
}

.run-trigger,
.run-count {
  color: var(--second-color);
}

.run-count {
  min-width: 56px;
}

.run-result {
  display: inline-flex;
  align-items: center;
  gap: var(--zg-space-sm);
}

.result-dot {
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

.run-detail {
  padding: 0 var(--zg-space-md) 14px 40px;
  color: var(--second-color);
  font-size: var(--zg-text-md);
  line-height: 20px;
  animation: run-enter 300ms var(--zg-ease-m3-emphasized-decelerate) both;

  p {
    margin: 0;
    white-space: pre-line;
    overflow-wrap: anywhere;
  }
}

.detail-feed {
  margin-bottom: var(--zg-space-xs);
}

@keyframes run-enter {
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
