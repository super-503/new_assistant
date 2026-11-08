<script setup lang="ts">
import { computed } from 'vue'
import type { NewsFieldValue, NewsItemField } from '@/api/newsItem'
import NewsOptionChip from './NewsOptionChip.vue'
import NewsSpinner from './NewsSpinner.vue'
import NewsTagChip from './NewsTagChip.vue'
import {
  formatFieldValue,
  getOptionColorIndex,
  getRequiredMessage,
  getSelectedOptions,
  isChoiceField,
  toggleFieldOption
} from './newsFieldValue'

/**
 * AI 结构化结果里的一个字段，按输出类型渲染控件：
 * 单行文本、数字为输入框，多行文本为多行输入框，单选、多选为可点选的标签组；
 * 只读时只展示值；未生成时显示「未生成」，可「填写」或「生成」。
 */
const props = defineProps<{
  field: NewsItemField
  /** 当前值（含未保存的修改） */
  value: NewsFieldValue
  /** 显示未生成状态 */
  isMissing: boolean
  /** 有未保存的修改 */
  isDirty: boolean
  /** 只展示值：已拒绝的资讯、资讯库 */
  readonly: boolean
  /** 显示必填提示 */
  showError: boolean
  /** 正在单独生成 */
  isGenerating: boolean
}>()

const emit = defineEmits<{
  update: [value: NewsFieldValue]
  fill: []
  generate: []
}>()

const inputId = computed(() => `news-field-${props.field.fieldKey}`)
const isReadonly = computed(() => props.readonly || !props.field.editable)
const isChoice = computed(() => isChoiceField(props.field))
/** 标签和 * 只在可编辑时提示 */
const isRequiredMark = computed(() => props.field.required && !isReadonly.value)
/** 「· 已修改」：本次未保存的修改，或之前人工修改过 */
const isEdited = computed(() => props.isDirty || props.field.edited)
const selectedOptions = computed(() => getSelectedOptions(props.value))
const textValue = computed(() => formatFieldValue(props.value))

const onTextInput = (event: Event) => {
  emit('update', (event.target as HTMLInputElement | HTMLTextAreaElement).value)
}

const onNumberInput = (event: Event) => {
  const raw = (event.target as HTMLInputElement).value
  emit('update', raw === '' ? null : Number(raw))
}

const onToggleOption = (optionValue: string) => {
  emit('update', toggleFieldOption(props.field, props.value, optionValue))
}
</script>

<template>
  <div class="ai-field" :class="{ 'has-error': props.showError }">
    <label :id="`${inputId}-label`" class="field-label" :for="inputId">
      {{ props.field.name }}<span v-if="isRequiredMark" class="required-mark" aria-hidden="true"> *</span
      ><span v-if="isEdited && !props.isMissing" class="edited-mark"> · 已修改</span>
    </label>

    <!-- 未生成 -->
    <div v-if="props.isMissing" class="field-missing">
      <span class="missing-text">未生成</span>
      <button v-if="!isReadonly" type="button" class="fill-button" @click="emit('fill')">填写</button>
      <button
        v-if="!props.readonly"
        type="button"
        class="generate-button"
        :disabled="props.isGenerating"
        @click="emit('generate')"
      >
        <NewsSpinner v-if="props.isGenerating" />
        <SvgIcon v-else icon-class="newsAdmin-spark" size="16" />
        {{ props.isGenerating ? '生成中' : '生成' }}
      </button>
    </div>

    <!-- 只读 -->
    <template v-else-if="isReadonly">
      <div v-if="isChoice && props.field.showAsTag" class="field-tags">
        <NewsTagChip
          v-for="option in selectedOptions"
          :key="option"
          :label="option"
          :color-index="getOptionColorIndex(props.field, option) ?? undefined"
        />
      </div>
      <span v-else class="field-read" :class="{ 'is-title': props.field.kind === 'SYSTEM' }">{{ textValue }}</span>
    </template>

    <!-- 可点选的标签组 -->
    <div v-else-if="isChoice" class="field-options" role="group" :aria-labelledby="`${inputId}-label`">
      <NewsOptionChip
        v-for="option in props.field.options"
        :key="option.value"
        :label="option.value"
        :selected="selectedOptions.includes(option.value)"
        :color-index="getOptionColorIndex(props.field, option.value)"
        @toggle="onToggleOption(option.value)"
      />
    </div>

    <textarea
      v-else-if="props.field.outputType === 'TEXT' && props.field.multiline"
      :id="inputId"
      class="field-control is-textarea"
      rows="4"
      :value="textValue"
      :aria-required="props.field.required"
      @input="onTextInput"
    ></textarea>

    <input
      v-else
      :id="inputId"
      class="field-control"
      :type="props.field.outputType === 'NUMBER' ? 'number' : 'text'"
      :value="textValue"
      :aria-required="props.field.required"
      @input="props.field.outputType === 'NUMBER' ? onNumberInput($event) : onTextInput($event)"
    />

    <span v-if="props.showError" class="field-error" role="alert">{{ getRequiredMessage(props.field) }}</span>
  </div>
</template>

<style scoped lang="scss">
@use './newsMixins' as *;

.ai-field {
  display: grid;
  gap: var(--zg-space-sm);
}

.field-label {
  color: var(--second-color);
  font-size: var(--zg-text-md);
  line-height: 20px;
  font-weight: var(--zg-weight-medium);

  .has-error & {
    color: var(--status-rejected);
  }
}

.required-mark {
  color: var(--status-rejected);
}

.edited-mark {
  color: var(--brand-indigo);
}

.field-control {
  width: 100%;
  height: 40px;
  padding: 0 var(--zg-space-lg);
  border: 1px solid transparent;
  border-radius: var(--zg-radius-md);
  background: var(--control-bg);
  color: var(--control-text);
  font-family: inherit;
  font-size: var(--zg-text-base);

  &.is-textarea {
    height: auto;
    padding: var(--zg-space-md) var(--zg-space-lg);
    line-height: 22px;
    resize: vertical;
  }
  &::placeholder {
    color: var(--control-placeholder);
  }
  &:focus {
    outline: none;
    border-color: var(--control-focus-border);
  }
  .has-error & {
    border-color: var(--control-error-border);
  }
}

.field-options,
.field-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--zg-space-md);
}

.field-tags {
  gap: var(--zg-space-sm);
}

.field-read {
  color: var(--main-color);
  font-size: var(--zg-text-base);
  line-height: 22px;
  white-space: pre-wrap;

  &.is-title {
    font-size: var(--zg-text-lg);
    line-height: 24px;
    font-weight: var(--zg-weight-semibold);
  }
}

.field-missing {
  display: flex;
  align-items: center;
  gap: var(--zg-space-md);
  min-height: 40px;
  padding: var(--zg-space-xs) var(--zg-space-xs) var(--zg-space-xs) var(--zg-space-lg);
  border: 1px dashed var(--surface-border);
  border-radius: var(--zg-radius-md);

  .has-error & {
    border-color: var(--control-error-border);
  }
}

.missing-text {
  flex: 1;
  min-width: 0;
  color: var(--surface-outline);
  font-size: var(--zg-text-base);
  line-height: 20px;
}

.fill-button {
  @include text-button;
  font-size: var(--zg-text-md);
}

.generate-button {
  @include selected-button;
  gap: var(--zg-space-sm);
  padding: 0 14px 0 10px;
  font-size: var(--zg-text-md);

  &:disabled {
    background: var(--selected-bg);
    color: var(--selected-text);
    cursor: wait;
  }

  :deep(.svg-icon) {
    cursor: inherit;
  }
}

.field-error {
  color: var(--status-rejected);
  font-size: var(--zg-text-sm);
  line-height: 16px;
}
</style>
