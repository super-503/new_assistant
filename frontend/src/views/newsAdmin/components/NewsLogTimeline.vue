<script setup lang="ts">
import { computed, ref } from 'vue'
import type { NewsItemAction, NewsItemLog } from '@/api/newsItem'
import { formatLogTime } from './newsFormat'

/**
 * 处理记录：默认收起成一行显示最近一条，展开后为完整时间线（含拒绝理由等附加信息）。
 * 切换资讯时由父组件以 key 重建，回到 defaultOpen 状态。
 */
const props = defineProps<{
  /** 最近的在前 */
  logs: NewsItemLog[]
  defaultOpen: boolean
}>()

/** 各动作的图标；失败、拒绝用错误色调 */
const ACTION_ICON: Record<NewsItemAction, string> = {
  COLLECTED: 'newsAdmin-rss',
  PROCESSED: 'newsAdmin-spark',
  PROCESS_FAILED: 'newsAdmin-warning',
  FIELD_EDITED: 'newsAdmin-edit',
  APPROVED: 'newsAdmin-check',
  REJECTED: 'newsAdmin-close',
  RESTORED: 'newsAdmin-restore'
}
const DANGER_ACTIONS: NewsItemAction[] = ['PROCESS_FAILED', 'REJECTED']

const isOpen = ref(props.defaultOpen)

const latestText = computed(() => {
  const latest = props.logs[0]
  return latest ? `${latest.actionName} · ${latest.operator} · ${formatLogTime(latest.time)}` : ''
})
</script>

<template>
  <section class="log-timeline" aria-label="处理记录">
    <button type="button" class="log-toggle" :aria-expanded="isOpen" @click="isOpen = !isOpen">
      <SvgIcon icon-class="newsAdmin-restore" size="16" />
      <span class="toggle-title">处理记录</span>
      <span class="toggle-latest">{{ latestText }}</span>
      <SvgIcon class="toggle-chevron" :class="{ 'is-open': isOpen }" icon-class="newsAdmin-expand-more" size="18" />
    </button>
    <ol v-if="isOpen" class="log-list">
      <li v-for="(log, index) in props.logs" :key="log.id" class="log-item">
        <span class="log-rail" aria-hidden="true">
          <span class="log-icon" :class="{ 'is-danger': DANGER_ACTIONS.includes(log.action) }">
            <SvgIcon :icon-class="ACTION_ICON[log.action]" size="14" />
          </span>
          <span v-if="index < props.logs.length - 1" class="log-line"></span>
        </span>
        <div class="log-body">
          <span class="log-head">
            <span class="log-title">{{ log.actionName }}</span>
            <span class="log-time">{{ formatLogTime(log.time) }} · {{ log.operator }}</span>
          </span>
          <span v-for="line in log.lines" :key="line.label" class="log-extra">
            {{ line.label }}：<span class="extra-value" :class="{ 'is-danger': line.danger }">{{ line.value }}</span>
          </span>
        </div>
      </li>
    </ol>
  </section>
</template>

<style scoped lang="scss">
@use './newsMixins' as *;

.log-timeline {
  border-top: 1px solid var(--surface-border);
}

.log-toggle {
  @include reset-button;
  @include state-layer;
  @include focus-ring(-2px);
  display: flex;
  align-items: center;
  gap: var(--zg-space-md);
  width: 100%;
  height: 44px;
  padding: 0 var(--zg-space-xs);
  border-radius: var(--zg-radius-md);
  color: var(--second-color);
  font-size: var(--zg-text-md);

  :deep(.svg-icon) {
    flex: none;
    cursor: inherit;
  }
}

.toggle-title {
  flex: none;
  font-weight: var(--zg-weight-medium);
}

.toggle-latest {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toggle-chevron {
  transition: transform 250ms var(--zg-ease-m3-standard);

  &.is-open {
    transform: rotate(180deg);
  }
}

.log-list {
  display: flex;
  flex-direction: column;
  margin: 0;
  padding: var(--zg-space-xs) var(--zg-space-xs) 0;
  list-style: none;
  animation: news-enter 300ms var(--zg-ease-m3-emphasized-decelerate) both;
}

.log-item {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr);
  gap: var(--zg-space-lg);
}

.log-rail {
  position: relative;
  display: flex;
  justify-content: center;
}

.log-icon {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--surface-container-highest);
  color: var(--second-color);

  &.is-danger {
    background: var(--status-rejected-container);
    color: var(--on-status-rejected-container);
  }
}

.log-line {
  position: absolute;
  top: 28px;
  bottom: 2px;
  width: 1px;
  background: var(--surface-border);
}

.log-body {
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-2xs);
  padding: var(--zg-space-2xs) 0 var(--zg-space-lg);
}

.log-head {
  display: flex;
  align-items: baseline;
  gap: var(--zg-space-lg);
  font-size: var(--zg-text-md);
  line-height: 20px;
}

.log-title {
  flex: 1;
  min-width: 0;
  font-weight: var(--zg-weight-medium);
}

.log-time {
  flex: none;
  color: var(--second-color);
  font-variant-numeric: tabular-nums;
}

.log-extra {
  color: var(--second-color);
  font-size: var(--zg-text-md);
  line-height: 20px;
}

.extra-value {
  color: var(--main-color);

  &.is-danger {
    color: var(--status-rejected);
    font-weight: var(--zg-weight-medium);
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
