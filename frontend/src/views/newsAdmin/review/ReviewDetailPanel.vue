<script setup lang="ts">
import { computed } from 'vue'
import NewsAiFieldCard from '../components/NewsAiFieldCard.vue'
import NewsLogTimeline from '../components/NewsLogTimeline.vue'
import NewsPlayback from '../components/NewsPlayback.vue'
import NewsReader from '../components/NewsReader.vue'
import NewsSpinner from '../components/NewsSpinner.vue'
import { formatItemTime } from '../components/newsFormat'
import { useReviewQueueContext } from './useReviewQueue'

/**
 * 审核详情面板：头部来源信息、结构化失败提示、AI 结构化结果卡片、原文、处理记录。
 * 底部操作栏只在有未保存的修改（取消 / 保存修改）或已拒绝（恢复到待审）时出现。
 */
const queue = useReviewQueueContext()
const reviewDetail = queue.detail

/** 正文不可获取时的提示 */
const BODY_EMPTY_TEXT = '正文暂不可获取。可以在新标签页打开原文查看，标题和摘要来自订阅源。'

const detail = computed(() => reviewDetail.detail.value)
/** 详情加载前用队列里的卡片填头部 */
const card = computed(() => queue.items.value.find((item) => item.id === queue.selectedId.value) ?? null)
const source = computed(() => detail.value ?? card.value)
const meta = computed(() => {
  if (!source.value) return ''
  return [source.value.channelName, source.value.feedName, formatItemTime(source.value.publishTime)]
    .filter((part) => part !== null)
    .join(' · ')
})
const isRejected = computed(() => detail.value?.status === 'REJECTED')
const isVideo = computed(() => detail.value?.contentType === 'VIDEO')
const isFailed = computed(() => !isRejected.value && (detail.value?.missingFields.length ?? 0) > 0)
const failedTitle = computed(
  () => `${(detail.value?.missingFields ?? []).map((field) => field.name).join('、')}未生成`
)
</script>

<template>
  <aside class="detail-panel" aria-label="资讯详情">
    <header class="panel-head">
      <h2 class="panel-title">资讯详情</h2>
      <a
        v-if="source"
        class="head-icon"
        :href="source.url"
        target="_blank"
        rel="noopener noreferrer"
        aria-label="在新标签页打开原文"
        title="打开原文"
      >
        <SvgIcon icon-class="newsAdmin-open-in-new" size="22" />
      </a>
      <button type="button" class="head-icon" aria-label="关闭详情" title="关闭详情" @click="queue.closeItem">
        <SvgIcon icon-class="newsAdmin-close" size="20" />
      </button>
    </header>
    <p class="panel-meta">{{ meta }}</p>
    <div v-if="reviewDetail.isLoading.value" class="panel-progress" role="progressbar" aria-label="加载中"></div>

    <div v-if="detail" class="panel-body">
      <h3 v-if="detail.fields.length === 0 && detail.title" class="source-title">{{ detail.title }}</h3>
      <div v-if="isFailed" class="failed-alert" role="alert">
        <span class="failed-title">{{ failedTitle }}</span>
        <span class="failed-hint">原文可正常查看。可以重新加工，或逐项生成、填写。</span>
        <div>
          <button
            type="button"
            class="failed-button"
            :disabled="reviewDetail.isReprocessing.value"
            @click="reviewDetail.reprocess"
          >
            <NewsSpinner v-if="reviewDetail.isReprocessing.value" />
            {{ reviewDetail.isReprocessing.value ? '加工中' : '重新加工' }}
          </button>
        </div>
      </div>

      <NewsAiFieldCard
        v-if="detail.fields.length > 0"
        :fields="detail.fields"
        :draft="reviewDetail.draft.value"
        :readonly="isRejected"
        :show-errors="reviewDetail.showErrors.value"
        :generating-key="reviewDetail.generatingKey.value"
        :can-reprocess="!isRejected"
        :is-reprocessing="reviewDetail.isReprocessing.value"
        @update="reviewDetail.updateDraft"
        @fill="reviewDetail.fillField"
        @generate="reviewDetail.generateField"
        @reprocess="reviewDetail.reprocess"
      />

      <section class="body-section" aria-labelledby="review-body-heading">
        <span id="review-body-heading" class="body-heading">{{ isVideo ? '视频简介' : '原文' }}</span>
        <template v-if="isVideo">
          <NewsPlayback :playback="detail.playback" :cover="detail.coverUrl" :link="detail.url" :title="detail.title" />
          <NewsReader v-if="detail.bodyHtml" :html="detail.bodyHtml" />
        </template>
        <NewsReader v-else :html="detail.bodyHtml ?? ''" :empty-text="BODY_EMPTY_TEXT" />
      </section>

      <NewsLogTimeline :key="detail.id" :logs="detail.logs" :default-open="isRejected" />
    </div>

    <footer v-if="detail && !isRejected && reviewDetail.isDirty.value" class="panel-foot">
      <span class="foot-hint">有未保存的修改</span>
      <button type="button" class="foot-text-button" @click="reviewDetail.resetDraft">取消修改</button>
      <button
        type="button"
        class="foot-save-button"
        :disabled="reviewDetail.isSaving.value"
        @click="reviewDetail.saveDraft(true)"
      >
        <NewsSpinner v-if="reviewDetail.isSaving.value" />
        保存修改
      </button>
    </footer>
    <footer v-if="detail && isRejected" class="panel-foot">
      <button type="button" class="foot-restore-button" @click="queue.restore([detail.id])">
        <SvgIcon icon-class="newsAdmin-restore" size="18" />
        恢复到待审
      </button>
    </footer>
  </aside>
</template>

<style scoped lang="scss">
@use '../components/newsMixins' as *;

.detail-panel {
  flex: 0 0 588px;
  min-width: 0;
  display: flex;
  flex-direction: column;
  margin: var(--zg-space-lg) var(--zg-space-lg) var(--zg-space-lg) 0;
  overflow: hidden;
  border-radius: var(--zg-radius-xl);
  background: var(--third-bg-color);
  animation: news-sheet 350ms var(--zg-ease-m3-emphasized-decelerate);
}

.panel-head {
  display: flex;
  align-items: center;
  gap: var(--zg-space-md);
  min-height: 56px;
  padding: var(--zg-space-md) var(--zg-space-lg) var(--zg-space-md) var(--zg-space-2xl);
}

.panel-title {
  flex: 1;
  margin: 0;
  font-size: var(--zg-text-xl);
  line-height: 26px;
  font-weight: var(--zg-weight-medium);
}

.head-icon {
  @include icon-button;
}

.panel-meta {
  margin: 0;
  padding: 0 var(--zg-space-2xl);
  color: var(--second-color);
  font-size: var(--zg-text-sm);
  line-height: 16px;
  letter-spacing: 0.4px;
}

.panel-progress {
  position: relative;
  height: 4px;
  margin: var(--zg-space-lg) var(--zg-space-2xl) 0;
  overflow: hidden;
  border-radius: var(--zg-radius-xs);
  background: var(--surface-container-highest);

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: inherit;
    background: var(--brand-indigo);
    transform-origin: left;
    animation: news-linear 1.3s var(--zg-ease-m3-standard) infinite;
  }
}

.panel-body {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-content: start;
  gap: var(--zg-space-2xl);
  padding: var(--zg-space-lg) var(--zg-space-2xl) var(--zg-space-2xl);
  overflow-y: auto;
}

.failed-alert {
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-lg);
  padding: var(--zg-space-xl);
  border-radius: var(--zg-radius-lg);
  background: var(--status-rejected-container);
  color: var(--on-status-rejected-container);
  animation: news-enter 300ms var(--zg-ease-m3-emphasized-decelerate) both;
}

.source-title {
  margin: 0;
  overflow-wrap: anywhere;
  font-size: var(--zg-text-xl);
  font-weight: var(--zg-weight-medium);
}

.failed-title {
  font-size: 15px;
  line-height: 22px;
  font-weight: var(--zg-weight-medium);
}

.failed-hint {
  font-size: var(--zg-text-md);
}

/* 失败提示里的按钮：与提示底色反相 */
.failed-button {
  @include selected-button;
  background: var(--on-status-rejected-container);
  color: var(--status-rejected-container);

  &:disabled {
    background: var(--on-status-rejected-container);
    color: var(--status-rejected-container);
    cursor: wait;
  }
}

.body-section {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--zg-space-lg);
}

.body-heading {
  color: var(--second-color);
  font-size: var(--zg-text-sm);
  line-height: 16px;
  font-weight: var(--zg-weight-medium);
}

.panel-foot {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--zg-space-md);
  padding: var(--zg-space-xl) var(--zg-space-2xl);
  border-top: 1px solid var(--surface-border);
  animation: news-enter 300ms var(--zg-ease-m3-emphasized-decelerate) both;
}

.foot-hint {
  align-self: center;
  margin-right: auto;
  color: var(--second-color);
  font-size: var(--zg-text-sm);
}

.foot-text-button {
  @include text-button;
}

.foot-save-button {
  @include selected-button;
}

.foot-restore-button {
  @include selected-button;
  flex: 1;
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
