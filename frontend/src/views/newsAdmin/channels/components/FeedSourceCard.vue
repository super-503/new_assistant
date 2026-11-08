<script setup lang="ts">
import type { NewsSourcePreview } from '@/api/newsChannel'
import { formatDuration, formatMinute } from '../../components/newsFormat'
import NewsTagChip from '../../components/NewsTagChip.vue'
import { formatStats } from '../channelFormat'
import SourceSkeleton from './SourceSkeleton.vue'

/** 选中来源后的来源卡片：来源信息、内容类型、原文来源域名，以及「最近更新」列表 */
const props = defineProps<{
  /** 来源卡片；加载中或加载失败时为 null */
  preview: NewsSourcePreview | null
  loading: boolean
  /** 加载失败的原因 */
  error: string
  /** 渠道的原文来源域名 */
  domain: string
}>()

const emit = defineEmits<{
  retry: []
}>()

/** 最近更新一行的说明：发布时间精确到分钟，后接统计项 */
const recentMeta = (item: NewsSourcePreview['recentItems'][number]) =>
  [formatMinute(item.publishTime), formatStats(item.stats)].filter((part) => part !== '').join(' · ')
</script>

<template>
  <SourceSkeleton v-if="props.loading" :rows="2" />

  <span v-else-if="props.error" class="card-error" role="alert">
    {{ props.error }}
    <button type="button" class="inline-retry" @click="emit('retry')">重试</button>
  </span>

  <div v-else-if="props.preview" class="source-card">
    <div class="card-head">
      <span class="card-avatar" aria-hidden="true">
        <img
          v-if="props.preview.source.avatarUrl"
          :src="props.preview.source.avatarUrl"
          alt=""
          referrerpolicy="no-referrer"
        />
        <template v-else>{{ Array.from(props.preview.source.name)[0] }}</template>
      </span>
      <div class="card-title">
        <span class="card-name">{{ props.preview.source.name }}</span>
        <span v-if="props.preview.source.description" class="card-desc">{{ props.preview.source.description }}</span>
      </div>
      <div class="card-meta">
        <NewsTagChip :label="props.preview.contentTypeDesc" />
        <span class="card-domain">原文来源 {{ props.domain }}</span>
      </div>
    </div>

    <section class="recent" aria-labelledby="feed-recent-title">
      <h3 id="feed-recent-title" class="recent-title">最近更新</h3>
      <p v-if="props.preview.recentItems.length === 0" class="recent-empty">暂无内容</p>
      <ul v-else class="recent-list">
        <li v-for="(item, index) in props.preview.recentItems" :key="index" class="recent-item">
          <span class="recent-thumb">
            <img v-if="item.thumbnailUrl" :src="item.thumbnailUrl" alt="" loading="lazy" referrerpolicy="no-referrer" />
            <span v-if="item.durationSeconds !== null" class="duration-badge">
              {{ formatDuration(item.durationSeconds) }}
            </span>
          </span>
          <span class="recent-text">
            <span class="recent-name" :title="item.title">{{ item.title }}</span>
            <span class="recent-meta">{{ recentMeta(item) }}</span>
          </span>
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped lang="scss">
@use '../../components/newsMixins' as *;

.card-error {
  display: flex;
  align-items: center;
  gap: var(--zg-space-xs);
  color: var(--status-rejected);
  font-size: var(--zg-text-sm);
  line-height: 16px;
}

.inline-retry {
  @include text-button;
  height: 28px;
  padding: 0 var(--zg-space-md);
  font-size: var(--zg-text-sm);
}

.source-card {
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-lg);
  padding: var(--zg-space-xl);
  border-radius: var(--zg-radius-lg);
  background: var(--surface-container-highest);
  animation: card-enter 300ms var(--zg-ease-m3-emphasized-decelerate) both;
}

.card-head {
  display: flex;
  align-items: center;
  gap: var(--zg-space-xl);
}

.card-avatar {
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  overflow: hidden;
  border-radius: 50%;
  background: var(--third-bg-color);
  color: var(--second-color);
  font-size: var(--zg-text-xl);
  font-weight: var(--zg-weight-medium);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.card-title {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.card-name {
  font-size: 15px;
  line-height: 22px;
  font-weight: var(--zg-weight-medium);
  letter-spacing: 0.15px;
}

.card-desc {
  display: -webkit-box;
  overflow: hidden;
  color: var(--second-color);
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-meta {
  flex: none;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--zg-space-xs);
}

.card-domain {
  color: var(--second-color);
  font-size: var(--zg-text-sm);
}

.recent {
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-md);
}

.recent-title {
  margin: 0;
  color: var(--second-color);
  font-size: var(--zg-text-sm);
  line-height: 16px;
  font-weight: var(--zg-weight-medium);
  letter-spacing: 0.5px;
}

.recent-empty {
  margin: 0;
  color: var(--second-color);
}

.recent-list {
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-md);
  margin: 0;
  padding: 0;
  list-style: none;
}

.recent-item {
  display: flex;
  align-items: center;
  gap: var(--zg-space-xl);
}

.recent-thumb {
  position: relative;
  flex: none;
  width: 112px;
  height: 64px;
  overflow: hidden;
  border-radius: var(--zg-radius-md);
  background: var(--third-bg-color);

  img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.duration-badge {
  position: absolute;
  right: var(--zg-space-xs);
  bottom: var(--zg-space-xs);
  height: 18px;
  padding: 0 var(--zg-space-sm);
  border-radius: 9px;
  background: var(--media-badge-bg);
  color: var(--media-badge-text);
  font-size: var(--zg-text-xs);
  line-height: 18px;
  font-variant-numeric: tabular-nums;
}

.recent-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-2xs);
}

.recent-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-meta {
  color: var(--second-color);
  font-size: var(--zg-text-sm);
  font-variant-numeric: tabular-nums;
}

@keyframes card-enter {
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
