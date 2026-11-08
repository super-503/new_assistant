<script setup lang="ts">
import { computed } from 'vue'
import type { NewsItemCard } from '@/api/newsItem'
import NewsSourceAvatar from './NewsSourceAvatar.vue'
import NewsTagChip from './NewsTagChip.vue'
import { formatDuration } from './newsFormat'

/**
 * 资讯卡片主体（审核卡片、资讯库文章行共用），整块可点击打开详情：
 * 来源、标题和摘要在左，缩略图在右。标签可由调用方在卡片外展示。
 */
const props = defineProps<{
  item: NewsItemCard
  /** 来源信息，如「渠道 · 订阅源 · 时间」 */
  meta: string
  hideTags?: boolean
}>()

const emit = defineEmits<{ open: [] }>()

const sourceName = computed(() => props.item.feedName ?? props.item.channelName)
const isFailed = computed(() => props.item.missingFields.length > 0)
</script>

<template>
  <button type="button" class="item-card" @click="emit('open')">
    <span class="card-main">
      <span class="card-meta">
        <NewsSourceAvatar :name="sourceName" :src="props.item.feedAvatarUrl ?? undefined" />
        <span class="meta-text">{{ props.meta }}</span>
        <span v-if="isFailed || (!props.hideTags && props.item.tags.length > 0)" class="meta-tags">
          <template v-if="!props.hideTags">
            <template v-for="tag in props.item.tags" :key="tag.fieldKey">
              <NewsTagChip
                v-for="option in tag.values"
                :key="option.value"
                :label="option.value"
                :color-index="option.colorIndex ?? undefined"
              />
            </template>
          </template>
          <span v-if="isFailed" class="failed-badge">
            <SvgIcon icon-class="newsAdmin-warning" size="14" />
            结构化失败
          </span>
        </span>
      </span>
      <span class="card-title">{{ props.item.title }}</span>
      <span v-if="props.item.summary" class="card-summary">{{ props.item.summary }}</span>
    </span>
    <span v-if="props.item.coverUrl" class="card-thumb">
      <img :src="props.item.coverUrl" alt="" loading="lazy" referrerpolicy="no-referrer" />
      <span v-if="props.item.durationSeconds !== null" class="thumb-duration">
        {{ formatDuration(props.item.durationSeconds) }}
      </span>
    </span>
  </button>
</template>

<style scoped lang="scss">
@use './newsMixins' as *;

.item-card {
  @include reset-button;
  @include focus-ring(-2px);
  display: flex;
  align-items: flex-start;
  gap: 20px;
  width: 100%;
  padding: var(--zg-space-sm) var(--zg-space-md);
  border-radius: var(--zg-radius-md);

  &:hover .card-thumb img {
    transform: scale(1.03);
  }
}

.card-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-sm);
}

.card-meta {
  display: flex;
  align-items: center;
  gap: var(--zg-space-md);
  min-width: 0;
  color: var(--second-color);
  font-size: var(--zg-text-sm);
  line-height: 16px;
}

/* 来源信息过长时截断，标签完整显示 */
.meta-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-tags {
  flex: none;
  display: flex;
  gap: var(--zg-space-sm);
  margin-left: var(--zg-space-xs);
}

.failed-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--zg-space-xs);
  height: 24px;
  padding: 0 var(--zg-space-md) 0 var(--zg-space-sm);
  border-radius: var(--zg-radius-sm);
  background: var(--status-rejected-container);
  color: var(--on-status-rejected-container);
  font-weight: var(--zg-weight-medium);
}

.card-title {
  color: var(--main-color);
  font-size: 15px;
  line-height: 22px;
  font-weight: var(--zg-weight-semibold);
}

.card-summary {
  display: -webkit-box;
  overflow: hidden;
  color: var(--second-color);
  font-size: var(--zg-text-base);
  line-height: 21px;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-thumb {
  position: relative;
  flex: none;
  width: 120px;
  height: 80px;
  margin-top: var(--zg-space-xs);
  overflow: hidden;
  border-radius: var(--zg-radius-md);
  background: var(--surface-container-highest);

  img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 400ms var(--zg-ease-m3-standard);
  }
}

.thumb-duration {
  position: absolute;
  right: var(--zg-space-xs);
  bottom: var(--zg-space-xs);
  height: 18px;
  padding: 0 var(--zg-space-sm);
  border-radius: var(--zg-radius-pill);
  background: var(--media-overlay-scrim);
  color: var(--media-overlay-text);
  font-size: var(--zg-text-xs);
  line-height: 18px;
  font-weight: var(--zg-weight-medium);
  font-variant-numeric: tabular-nums;
}
</style>
