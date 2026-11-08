<script setup lang="ts">
import { computed, ref } from 'vue'
import { updateLibraryTagsApi, type NewsFieldValue, type NewsItemDetail, type NewsItemField } from '@/api/newsItem'
import NewsOptionChip from '../components/NewsOptionChip.vue'
import NewsSpinner from '../components/NewsSpinner.vue'
import NewsTagChip from '../components/NewsTagChip.vue'
import {
  getOptionColorIndex,
  getSelectedOptions,
  isEmptyFieldValue,
  normalizeFieldValue,
  toggleFieldOption
} from '../components/newsFieldValue'

/**
 * 资讯库详情里的一个标签字段：平时显示标签和修改按钮，修改时原地展开可点选的选项，保存后刷新详情。
 */
const props = defineProps<{
  itemId: number
  field: NewsItemField
}>()

const emit = defineEmits<{ saved: [detail: NewsItemDetail] }>()

const isEditing = ref(false)
const draftValue = ref<NewsFieldValue>(null)
const isSaving = ref(false)

const selectedOptions = computed(() => getSelectedOptions(props.field.value))
const draftOptions = computed(() => getSelectedOptions(draftValue.value))
const isDraftInvalid = computed(() => props.field.required && isEmptyFieldValue(draftValue.value))
const requiredMessage = computed(() =>
  props.field.outputType === 'MULTI' ? `至少选择一个${props.field.name}` : `请选择${props.field.name}`
)

const startEdit = () => {
  draftValue.value = props.field.value
  isEditing.value = true
}

const onToggle = (optionValue: string) => {
  draftValue.value = toggleFieldOption(props.field, draftValue.value, optionValue)
}

const onSave = async () => {
  if (isDraftInvalid.value) return
  isSaving.value = true
  try {
    const next = await updateLibraryTagsApi(props.itemId, [
      { fieldKey: props.field.fieldKey, value: normalizeFieldValue(props.field, draftValue.value) }
    ])
    isEditing.value = false
    emit('saved', next)
  } catch (e) {
    console.error(e)
  } finally {
    isSaving.value = false
  }
}
</script>

<template>
  <div v-if="!isEditing" class="tag-field">
    <NewsTagChip
      v-for="option in selectedOptions"
      :key="option"
      :label="option"
      :color-index="getOptionColorIndex(props.field, option) ?? undefined"
    />
    <span v-if="selectedOptions.length === 0" class="tag-empty">{{ props.field.name }}未设置</span>
    <button
      v-if="props.field.editable"
      type="button"
      class="edit-button"
      :aria-label="`修改${props.field.name}`"
      :title="`修改${props.field.name}`"
      @click="startEdit"
    >
      <SvgIcon icon-class="newsAdmin-edit" size="16" />
    </button>
  </div>

  <div v-else class="tag-field is-editing" role="group" :aria-label="props.field.name">
    <NewsOptionChip
      v-for="option in props.field.options"
      :key="option.value"
      :label="option.value"
      :selected="draftOptions.includes(option.value)"
      :color-index="getOptionColorIndex(props.field, option.value)"
      :disabled="isSaving"
      @toggle="onToggle(option.value)"
    />
    <span class="edit-actions">
      <button type="button" class="cancel-button" @click="isEditing = false">取消</button>
      <button type="button" class="save-button" :disabled="isDraftInvalid || isSaving" @click="onSave">
        <NewsSpinner v-if="isSaving" />
        保存
      </button>
    </span>
    <span v-if="isDraftInvalid" class="edit-error" role="alert">{{ requiredMessage }}</span>
  </div>
</template>

<style scoped lang="scss">
@use '../components/newsMixins' as *;

.tag-field {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--zg-space-md);

  &.is-editing {
    animation: news-enter 300ms var(--zg-ease-m3-emphasized-decelerate) both;
  }
}

.tag-empty {
  color: var(--surface-outline);
  font-size: var(--zg-text-md);
}

.edit-button {
  @include icon-button(28px);
}

.edit-actions {
  display: inline-flex;
  gap: var(--zg-space-xs);
  margin-left: auto;
}

.cancel-button {
  @include text-button;
  font-size: var(--zg-text-md);
}

.save-button {
  @include reset-button;
  @include state-layer;
  @include focus-ring;
  display: inline-flex;
  align-items: center;
  gap: var(--zg-space-sm);
  height: 32px;
  padding: 0 var(--zg-space-xl);
  border-radius: var(--zg-radius-pill);
  background: var(--brand-indigo);
  color: var(--on-brand);
  font-size: var(--zg-text-md);
  font-weight: var(--zg-weight-medium);
  transition: transform var(--zg-duration-fast) var(--zg-ease-m3-standard);

  &:active {
    transform: scale(0.97);
  }
  &:disabled {
    background: var(--surface-container-highest);
    color: var(--text-disabled);
    cursor: not-allowed;
    transform: none;
  }
}

.edit-error {
  flex-basis: 100%;
  color: var(--status-rejected);
  font-size: var(--zg-text-sm);
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
