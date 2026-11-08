<script setup lang="ts">
import { computed } from 'vue'
import type { NewsItemDetail, NewsItemField } from '@/api/newsItem'
import NewsAiFieldCard from '../components/NewsAiFieldCard.vue'
import NewsLogTimeline from '../components/NewsLogTimeline.vue'
import NewsPlayback from '../components/NewsPlayback.vue'
import NewsReader from '../components/NewsReader.vue'
import NewsSourceAvatar from '../components/NewsSourceAvatar.vue'
import { isEmptyFieldValue } from '../components/newsFieldValue'
import { formatMinute } from '../components/newsFormat'
import LibraryTagFieldEditor from './LibraryTagFieldEditor.vue'
import { useLibraryContext } from './useLibrary'

/**
 * 资讯库整页详情，覆盖分类栏右侧整块区域：
 * 顶栏为关闭、面包屑「类型 / 渠道 / 订阅源 / 标题」（类型和订阅源可点击）、打开原文；
 * 文章依次为标题、来源与「已通过」、标签字段、AI 结构化结果（只读，有值的非标签字段）、正文、处理记录；
 * 视频依次为播放区、标题、来源与「已通过」、标签字段、视频简介、处理记录。
 */
const emit = defineEmits<{ notify: [message: string] }>()

const library = useLibraryContext()

const detail = computed(() => library.detail.value)
const isArticle = computed(() => library.contentType.value === 'ARTICLE')
const typeLabel = computed(() => (isArticle.value ? '文章' : '视频'))
const linkLabel = computed(() => (isArticle.value ? '打开原文' : '在来源平台打开'))
const sourceName = computed(() => (detail.value ? (detail.value.feedName ?? detail.value.channelName) : ''))
const meta = computed(() => {
  if (!detail.value) return ''
  return [detail.value.channelName, detail.value.feedName, formatMinute(detail.value.publishTime)]
    .filter((part) => part !== null)
    .join(' · ')
})
const tagFields = computed(() => (detail.value?.fields ?? []).filter((field) => field.showAsTag))
/** AI 结构化结果卡片只展示有值的非标签字段，不含系统字段（标题） */
const aiFields = computed(() =>
  (detail.value?.fields ?? []).filter(
    (field) => !field.showAsTag && field.kind !== 'SYSTEM' && !isEmptyFieldValue(field.value)
  )
)

const onFeedCrumb = () => {
  const current = detail.value!
  library.selectNode({ kind: 'feed', channelCode: current.channelCode, feedId: current.feedId })
}

const onTagSaved = (field: NewsItemField, next: NewsItemDetail) => {
  library.onDetailUpdated(next)
  emit('notify', `${field.name}已更新`)
}
</script>

<template>
  <section class="library-detail" :aria-label="isArticle ? '文章详情' : '视频详情'">
    <div class="detail-bar">
      <button type="button" class="bar-icon" aria-label="关闭详情" title="关闭详情" @click="library.closeDetail">
        <SvgIcon icon-class="newsAdmin-close" size="20" />
      </button>
      <nav class="breadcrumb" aria-label="当前位置">
        <button type="button" class="crumb-button" @click="library.selectNode({ kind: 'all' })">{{ typeLabel }}</button>
        <template v-if="detail">
          <span class="crumb-sep" aria-hidden="true">/</span>
          <span class="crumb-text">{{ detail.channelName }}</span>
          <template v-if="detail.feedName">
            <span class="crumb-sep" aria-hidden="true">/</span>
            <button type="button" class="crumb-button is-feed" @click="onFeedCrumb">{{ detail.feedName }}</button>
          </template>
          <span class="crumb-sep" aria-hidden="true">/</span>
          <span class="crumb-current" aria-current="page">{{ detail.title }}</span>
        </template>
      </nav>
      <a
        v-if="detail"
        class="bar-icon"
        :href="detail.url"
        target="_blank"
        rel="noopener noreferrer"
        :aria-label="linkLabel"
        :title="linkLabel"
      >
        <SvgIcon icon-class="newsAdmin-open-in-new" size="18" />
      </a>
    </div>
    <div v-if="library.isDetailLoading.value" class="detail-progress" role="progressbar" aria-label="加载中"></div>

    <div v-if="detail" class="detail-body" :class="{ 'is-video': !isArticle }">
      <NewsPlayback
        v-if="!isArticle"
        :playback="detail.playback"
        :cover="detail.coverUrl"
        :link="detail.url"
        :title="detail.title"
      />
      <h2 class="detail-title">{{ detail.title }}</h2>
      <div class="detail-meta">
        <NewsSourceAvatar :name="sourceName" :src="detail.feedAvatarUrl ?? undefined" />
        <span class="meta-text">{{ meta }}</span>
        <span class="approved-badge">
          <SvgIcon icon-class="newsAdmin-check" size="14" />
          {{ detail.statusName }}
        </span>
      </div>

      <LibraryTagFieldEditor
        v-for="field in tagFields"
        :key="`${detail.id}-${field.fieldKey}`"
        :item-id="detail.id"
        :field="field"
        @saved="onTagSaved(field, $event)"
      />

      <template v-if="isArticle">
        <NewsAiFieldCard
          v-if="aiFields.length > 0"
          :fields="aiFields"
          :draft="{}"
          readonly
          :show-errors="false"
          :generating-key="null"
          :can-reprocess="false"
          :is-reprocessing="false"
        />
        <NewsReader :html="detail.bodyHtml ?? ''" />
      </template>
      <NewsReader v-else-if="detail.bodyHtml" :html="detail.bodyHtml" />

      <NewsLogTimeline :key="detail.id" class="detail-logs" :logs="detail.logs" :default-open="false" />
    </div>
  </section>
</template>

<style scoped lang="scss">
@use '../components/newsMixins' as *;

.library-detail {
  position: absolute;
  inset: 0;
  z-index: var(--zg-z-raised);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--second-bg-color);
  animation: news-sheet 350ms var(--zg-ease-m3-emphasized-decelerate);
}

.detail-bar {
  flex: none;
  display: flex;
  align-items: center;
  gap: var(--zg-space-md);
  height: 56px;
  padding: 0 var(--zg-space-lg);
  border-bottom: 1px solid var(--surface-border);
}

.bar-icon {
  @include icon-button;
}

.breadcrumb {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: var(--zg-space-2xs);
  color: var(--second-color);
  font-size: var(--zg-text-base);
  line-height: 20px;
  white-space: nowrap;
}

.crumb-button {
  @include reset-button;
  @include state-layer;
  @include focus-ring(0);
  flex: none;
  height: 28px;
  padding: 0 var(--zg-space-sm);
  border-radius: var(--zg-radius-sm);

  &.is-feed {
    max-width: 200px;
    text-overflow: ellipsis;
  }
}

.crumb-sep {
  flex: none;
  color: var(--surface-outline);
}

.crumb-text {
  flex: none;
  padding: 0 var(--zg-space-sm);
}

.crumb-current {
  flex: 0 1 auto;
  min-width: 0;
  padding: 0 var(--zg-space-sm);
  overflow: hidden;
  color: var(--main-color);
  text-overflow: ellipsis;
}

.detail-progress {
  position: relative;
  flex: none;
  height: 4px;
  overflow: hidden;
  background: var(--surface-container-highest);

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: var(--brand-indigo);
    transform-origin: left;
    animation: news-linear 1.3s var(--zg-ease-m3-standard) infinite;
  }
}

/* 文章正文区 760px 居中，视频 960px */
.detail-body {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 760px);
  align-content: start;
  justify-content: center;
  gap: var(--zg-space-xl);
  padding: var(--zg-space-2xl) var(--zg-space-2xl) 40px;
  overflow-y: auto;

  &.is-video {
    grid-template-columns: minmax(0, 960px);
  }
}

.detail-title {
  margin: 0;
  font-size: 22px;
  line-height: 30px;
  font-weight: var(--zg-weight-semibold);
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--zg-space-md);
}

.meta-text {
  color: var(--second-color);
  font-size: var(--zg-text-md);
}

.approved-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--zg-space-2xs);
  height: 24px;
  margin-left: auto;
  padding: 0 var(--zg-space-md) 0 var(--zg-space-sm);
  border-radius: var(--zg-radius-sm);
  background: var(--status-approved-container);
  color: var(--on-status-approved-container);
  font-size: var(--zg-text-sm);
  font-weight: var(--zg-weight-medium);
}

.detail-logs {
  margin-top: var(--zg-space-md);
}

@keyframes news-sheet {
  from {
    opacity: 0;
    transform: translateX(24px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

@keyframes news-linear {
  0% {
    transform: translateX(-100%) scaleX(0.3);
  }
  60% {
    transform: translateX(40%) scaleX(0.5);
  }
  100% {
    transform: translateX(100%) scaleX(0.3);
  }
}
</style>
