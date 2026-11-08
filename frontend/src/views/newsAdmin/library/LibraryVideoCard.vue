<script setup lang="ts">
import type { NewsItemCard } from '@/api/newsItem'
import NewsTagChip from '../components/NewsTagChip.vue'
import { formatDay, formatDuration } from '../components/newsFormat'

/** 视频网格里的一张卡片：16:9 封面带时长角标，其下标题（两行）、「订阅源 · 日期」与标签 */
const props = defineProps<{
  item: NewsItemCard
  isSelected: boolean
}>()

const emit = defineEmits<{ open: [] }>()
</script>

<template>
  <button type="button" class="video-card" :class="{ 'is-selected': props.isSelected }" @click="emit('open')">
    <span class="video-thumb">
      <img v-if="props.item.coverUrl" :src="props.item.coverUrl" alt="" loading="lazy" referrerpolicy="no-referrer" />
      <span v-if="props.item.durationSeconds !== null" class="thumb-duration">
        {{ formatDuration(props.item.durationSeconds) }}
      </span>
    </span>
    <span class="video-info">
      <span class="video-title">{{ props.item.title }}</span>
      <span class="video-meta">{{ props.item.feedName ?? props.item.channelName }} · {{ formatDay(props.item.publishTime) }}</span>
      <span v-if="props.item.tags.length > 0" class="video-tags">
        <template v-for="tag in props.item.tags" :key="tag.fieldKey">
          <NewsTagChip
            v-for="option in tag.values"
            :key="`${tag.fieldKey}-${option.value}`"
            :label="option.value"
            :color-index="option.colorIndex ?? undefined"
          />
        </template>
      </span>
    </span>
  </button>
</template>

<style scoped lang="scss">
@use '../components/newsMixins' as *;

.video-card {
  @include reset-button;
  @include state-layer;
  @include focus-ring;
  display: flex;
  flex-direction: column;
  padding: var(--zg-space-md);
  border: 1px solid transparent;
  border-radius: var(--zg-radius-lg);
  background: var(--third-bg-color);
  transition:
    box-shadow 250ms var(--zg-ease-m3-standard),
    border-color 250ms var(--zg-ease-m3-standard),
    background 250ms var(--zg-ease-m3-standard);

  &:hover {
    box-shadow: var(--zg-shadow-m3-1);

    .video-thumb img {
      transform: scale(1.03);
    }
  }
  &.is-selected {
    border-color: var(--brand-indigo);
    background: var(--surface-container-high);
  }
}

.video-thumb {
  position: relative;
  display: block;
  aspect-ratio: 16 / 9;
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
  right: var(--zg-space-sm);
  bottom: var(--zg-space-sm);
  height: 20px;
  padding: 0 var(--zg-space-sm);
  border-radius: var(--zg-radius-pill);
  background: var(--media-overlay-scrim);
  color: var(--media-overlay-text);
  font-size: var(--zg-text-sm);
  line-height: 20px;
  font-weight: var(--zg-weight-medium);
  font-variant-numeric: tabular-nums;
}

.video-info {
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-sm);
  padding: var(--zg-space-md) var(--zg-space-xs) var(--zg-space-xs);
}

.video-title {
  display: -webkit-box;
  overflow: hidden;
  color: var(--main-color);
  font-size: var(--zg-text-base);
  line-height: 20px;
  font-weight: var(--zg-weight-medium);
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.video-meta {
  color: var(--second-color);
  font-size: var(--zg-text-sm);
  line-height: 16px;
}

.video-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--zg-space-sm);
}
</style>
