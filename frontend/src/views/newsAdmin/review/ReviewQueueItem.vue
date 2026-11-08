<script setup lang="ts">
import { computed } from 'vue'
import type { NewsItemCard as NewsItemCardData } from '@/api/newsItem'
import NewsItemCard from '../components/NewsItemCard.vue'
import NewsSourceAvatar from '../components/NewsSourceAvatar.vue'
import NewsTagChip from '../components/NewsTagChip.vue'
import { formatItemTime } from '../components/newsFormat'
import ReviewCheckbox from './ReviewCheckbox.vue'
import { useReviewQueueContext } from './useReviewQueue'

/**
 * 审核队列的一条：卡片显示（来源、标题、摘要、缩略图与底部操作）或紧凑显示（一行）。
 * 待审条目可通过 / 拒绝，已拒绝条目可恢复到待审；正在看详情的条目左侧带主色竖条。
 */
const props = defineProps<{
  item: NewsItemCardData
  /** 在列表中的序号，用于依次入场 */
  index: number
}>()

const queue = useReviewQueueContext()

/** 入场延迟最多 8 档，每档 40ms */
const ENTER_STAGGER_LIMIT = 8
const ENTER_STAGGER_MS = 40

const isChecked = computed(() => queue.checkedIds.value.includes(props.item.id))
const isSelected = computed(() => queue.selectedId.value === props.item.id)
const isLeaving = computed(() => queue.leavingIds.value.includes(props.item.id))
const isPending = computed(() => props.item.status === 'PENDING')
const isRejected = computed(() => props.item.status === 'REJECTED')
const sourceName = computed(() => props.item.feedName ?? props.item.channelName)
const itemTime = computed(() => formatItemTime(props.item.publishTime))
const meta = computed(() =>
  [props.item.channelName, props.item.feedName, itemTime.value].filter((part) => part !== null).join(' · ')
)
const enterDelay = computed(() => `${Math.min(props.index, ENTER_STAGGER_LIMIT) * ENTER_STAGGER_MS}ms`)
</script>

<template>
  <article
    class="queue-item"
    :class="{
      'is-compact': queue.density.value === 'compact',
      'is-checked': isChecked,
      'is-selected': isSelected,
      'is-leaving': isLeaving
    }"
    :style="isLeaving ? undefined : { animationDelay: enterDelay }"
    :data-item-id="props.item.id"
  >
    <ReviewCheckbox
      class="item-check"
      :state="isChecked ? 'all' : 'none'"
      :label="`选择：${props.item.title}`"
      @toggle="queue.toggleChecked(props.item.id)"
    />

    <!-- 卡片显示 -->
    <div v-if="queue.density.value === 'card'" class="item-body">
      <NewsItemCard :item="props.item" :meta="meta" hide-tags @open="queue.openItem(props.item.id)" />
      <div v-if="isRejected" class="reject-info">
        <span class="reject-reason">拒绝理由：{{ props.item.rejectReasonName }}</span>
        <span v-if="props.item.reviewTime" class="reject-by">
          {{ props.item.reviewer }} · {{ formatItemTime(props.item.reviewTime) }}
        </span>
      </div>
      <footer class="item-footer">
        <div v-if="props.item.tags.length > 0" class="footer-tags">
          <div v-for="tag in props.item.tags" :key="tag.fieldKey" class="tag-group">
            <span class="tag-label">{{ tag.kind === 'BUILTIN' ? '岗位' : tag.name }}</span>
            <NewsTagChip
              v-for="option in tag.values"
              :key="option.value"
              class="footer-tag"
              :label="option.value"
              :color-index="option.colorIndex ?? undefined"
            />
          </div>
        </div>
        <div class="item-actions">
          <template v-if="isPending">
            <button type="button" class="action-pill" title="通过（A）" @click="queue.approve([props.item.id])">
              <SvgIcon class="icon-approve" icon-class="newsAdmin-check" size="16" />
              通过
            </button>
            <button type="button" class="action-pill" title="拒绝（R）" @click="queue.openReject([props.item.id])">
              <SvgIcon class="icon-reject" icon-class="newsAdmin-close" size="16" />
              拒绝
            </button>
          </template>
          <button v-if="isRejected" type="button" class="action-pill" @click="queue.restore([props.item.id])">
            <SvgIcon icon-class="newsAdmin-restore" size="16" />
            恢复到待审
          </button>
          <a
            class="action-icon-pill"
            :href="props.item.url"
            target="_blank"
            rel="noopener noreferrer"
            :aria-label="`打开原文：${props.item.title}`"
            title="打开原文"
          >
            <SvgIcon icon-class="newsAdmin-open-in-new" size="16" />
          </a>
        </div>
      </footer>
    </div>

    <!-- 紧凑显示 -->
    <template v-else>
      <button type="button" class="compact-main" @click="queue.openItem(props.item.id)">
        <NewsSourceAvatar :name="sourceName" :src="props.item.feedAvatarUrl ?? undefined" />
        <span class="compact-title">{{ props.item.title }}</span>
        <span class="compact-tags">
          <template v-for="tag in props.item.tags" :key="tag.fieldKey">
            <NewsTagChip
              v-for="option in tag.values"
              :key="`${tag.fieldKey}-${option.value}`"
              :label="option.value"
              :color-index="option.colorIndex ?? undefined"
            />
          </template>
        </span>
        <span class="compact-time">{{ itemTime }}</span>
      </button>
      <template v-if="isPending">
        <button
          type="button"
          class="compact-icon"
          :aria-label="`通过：${props.item.title}`"
          title="通过（A）"
          @click="queue.approve([props.item.id])"
        >
          <SvgIcon class="icon-approve" icon-class="newsAdmin-check" size="18" />
        </button>
        <button
          type="button"
          class="compact-icon"
          :aria-label="`拒绝：${props.item.title}`"
          title="拒绝（R）"
          @click="queue.openReject([props.item.id])"
        >
          <SvgIcon class="icon-reject" icon-class="newsAdmin-close" size="18" />
        </button>
      </template>
      <button
        v-if="isRejected"
        type="button"
        class="compact-icon"
        :aria-label="`恢复到待审：${props.item.title}`"
        title="恢复到待审"
        @click="queue.restore([props.item.id])"
      >
        <SvgIcon icon-class="newsAdmin-restore" size="18" />
      </button>
    </template>
  </article>
</template>

<style scoped lang="scss">
@use '../components/newsMixins' as *;

.queue-item {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: var(--zg-space-xs);
  padding: var(--zg-space-md) var(--zg-space-md) var(--zg-space-sm) var(--zg-space-xs);
  border-bottom: 1px solid var(--surface-border);
  box-shadow: inset 3px 0 0 transparent;
  animation: news-enter 300ms var(--zg-ease-m3-emphasized-decelerate) both;
  transition:
    background-color var(--zg-duration-base) var(--zg-ease-m3-standard),
    box-shadow var(--zg-duration-base) var(--zg-ease-m3-standard);

  &:hover {
    background-image: linear-gradient(var(--row-hover-overlay), var(--row-hover-overlay));
  }
  &.is-checked {
    background-color: var(--brand-tint);
  }
  &.is-selected {
    background-color: var(--third-bg-color);
    box-shadow: inset 3px 0 0 var(--brand-indigo);
  }
  &.is-leaving {
    pointer-events: none;
    animation: news-leave 220ms var(--zg-ease-m3-emphasized-accelerate) forwards;
  }
  &.is-compact {
    align-items: center;
    padding: var(--zg-space-xs) var(--zg-space-md) var(--zg-space-xs) var(--zg-space-xs);
  }
}

.item-check {
  margin-top: var(--zg-space-2xs);

  .is-compact & {
    margin-top: 0;
  }
}

.item-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-xs);
}

.reject-info {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--zg-space-sm) var(--zg-space-lg);
  padding: 0 var(--zg-space-md);
}

.reject-reason {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 var(--zg-space-md);
  border-radius: var(--zg-radius-sm);
  background: var(--status-rejected-container);
  color: var(--on-status-rejected-container);
  font-size: var(--zg-text-sm);
  font-weight: var(--zg-weight-medium);
}

.reject-by {
  color: var(--second-color);
  font-size: var(--zg-text-sm);
}

.item-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--zg-space-md) var(--zg-space-xl);
  padding: var(--zg-space-xs) var(--zg-space-md) var(--zg-space-sm);
}

.footer-tags {
  display: flex;
  flex: 1 1 200px;
  flex-wrap: wrap;
  align-items: center;
  min-width: 0;
  gap: var(--zg-space-sm) 0;
}

.tag-group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  min-width: 0;
  gap: var(--zg-space-sm);

  & + & {
    margin-inline-start: var(--zg-space-lg);
    padding-inline-start: var(--zg-space-lg);
    border-inline-start: 1px solid var(--surface-border);
  }
}

.tag-label {
  color: var(--second-color);
  font-size: var(--zg-text-sm);
  font-weight: var(--zg-weight-medium);
}

.tag-group .footer-tag {
  max-width: 100%;
  height: auto;
  min-height: 24px;
  white-space: normal;
  overflow-wrap: anywhere;
}

.item-actions {
  display: flex;
  flex: none;
  gap: var(--zg-space-md);
  margin-inline-start: auto;
}

.action-pill {
  @include tonal-pill;
}

.action-icon-pill {
  @include tonal-pill;
  justify-content: center;
  width: 32px;
  padding: 0;
  color: var(--second-color);
}

.icon-approve {
  color: var(--status-approved);
}

.icon-reject {
  color: var(--status-rejected);
}

.compact-main {
  @include reset-button;
  @include focus-ring(-2px);
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: var(--zg-space-md);
  height: 40px;
  padding: 0 var(--zg-space-md);
  border-radius: var(--zg-radius-md);
}

.compact-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: var(--main-color);
  font-size: var(--zg-text-base);
  font-weight: var(--zg-weight-medium);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.compact-tags {
  flex: none;
  display: flex;
  gap: var(--zg-space-sm);
}

.compact-time {
  flex: none;
  width: 96px;
  color: var(--second-color);
  font-size: var(--zg-text-sm);
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.compact-icon {
  @include icon-button(28px);
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

@keyframes news-leave {
  to {
    opacity: 0;
    transform: translateX(32px);
  }
}
</style>
