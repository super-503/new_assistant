<script setup lang="ts">
import type { NewsFieldValue, NewsItemField } from '@/api/newsItem'
import NewsAiFieldInput from './NewsAiFieldInput.vue'
import NewsSpinner from './NewsSpinner.vue'
import { isEmptyFieldValue, isSameFieldValue } from './newsFieldValue'

/**
 * AI 结构化结果卡片：主色淡底加细描边，左上角 ✦ 标题，右上角「重新加工」。
 * 按 AI 字段配置逐个渲染字段；未保存的修改由父组件以 draft 传入（只含动过的字段）。
 */
const props = defineProps<{
  fields: NewsItemField[]
  /** 未保存的修改：字段标识 → 新值 */
  draft: Record<string, NewsFieldValue>
  /** 只展示值 */
  readonly: boolean
  /** 显示必填提示 */
  showErrors: boolean
  /** 正在单独生成的字段标识 */
  generatingKey: string | null
  /** 显示「重新加工」 */
  canReprocess: boolean
  isReprocessing: boolean
}>()

const emit = defineEmits<{
  update: [fieldKey: string, value: NewsFieldValue]
  fill: [fieldKey: string]
  generate: [fieldKey: string]
  reprocess: []
}>()

const hasDraft = (field: NewsItemField) => Object.prototype.hasOwnProperty.call(props.draft, field.fieldKey)
const getValue = (field: NewsItemField) => (hasDraft(field) ? props.draft[field.fieldKey] : field.value)
const isMissing = (field: NewsItemField) => field.missing && !hasDraft(field)
const isDirty = (field: NewsItemField) => hasDraft(field) && !isSameFieldValue(props.draft[field.fieldKey], field.value)
const hasError = (field: NewsItemField) =>
  props.showErrors && !props.readonly && field.required && isEmptyFieldValue(getValue(field))
</script>

<template>
  <section class="ai-card" aria-labelledby="news-ai-card-title">
    <header class="card-head">
      <SvgIcon class="head-spark" icon-class="newsAdmin-spark" size="16" />
      <span id="news-ai-card-title" class="head-title">AI 结构化结果</span>
      <button
        v-if="props.canReprocess"
        type="button"
        class="reprocess-button"
        aria-label="重新加工"
        title="重新加工"
        :disabled="props.isReprocessing"
        @click="emit('reprocess')"
      >
        <NewsSpinner v-if="props.isReprocessing" :size="18" />
        <SvgIcon v-else icon-class="newsAdmin-refresh" size="18" />
      </button>
    </header>
    <NewsAiFieldInput
      v-for="field in props.fields"
      :key="field.fieldKey"
      :field="field"
      :value="getValue(field)"
      :is-missing="isMissing(field)"
      :is-dirty="isDirty(field)"
      :readonly="props.readonly"
      :show-error="hasError(field)"
      :is-generating="props.generatingKey === field.fieldKey"
      @update="emit('update', field.fieldKey, $event)"
      @fill="emit('fill', field.fieldKey)"
      @generate="emit('generate', field.fieldKey)"
    />
  </section>
</template>

<style scoped lang="scss">
@use './newsMixins' as *;

.ai-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--zg-space-xl);
  padding: var(--zg-space-xl);
  border: 1px solid var(--brand-tint-border);
  border-radius: var(--zg-radius-lg);
  background: linear-gradient(var(--brand-tint), var(--brand-tint)), var(--surface-container);
  animation: news-enter 300ms var(--zg-ease-m3-emphasized-decelerate) both;
}

.card-head {
  display: flex;
  align-items: center;
  gap: var(--zg-space-md);
  height: 32px;
  margin: calc(var(--zg-space-sm) * -1) calc(var(--zg-space-md) * -1) calc(var(--zg-space-xs) * -1) 0;
}

.head-spark {
  flex: none;
  color: var(--brand-indigo);
}

.head-title {
  flex: 1;
  min-width: 0;
  color: var(--brand-indigo);
  font-size: var(--zg-text-md);
  line-height: 20px;
  font-weight: var(--zg-weight-medium);
}

.reprocess-button {
  @include icon-button;

  &:disabled {
    cursor: wait;
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
