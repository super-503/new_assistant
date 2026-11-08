<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { NewsApiError } from '@/api/newsAdminRequest'
import {
  createFeedApi,
  previewSourceApi,
  updateFeedApi,
  type NewsChannel,
  type NewsFeed,
  type NewsSourceCandidate,
  type NewsSourcePreview
} from '@/api/newsChannel'
import type { NewsContentType } from '@/api/newsItem'
import { confirmDiscard, notifySuccess } from '../../newsNotify'
import FeedSourceCard from './FeedSourceCard.vue'
import FeedSourcePicker from './FeedSourcePicker.vue'

/**
 * 添加 / 编辑订阅源弹窗：选渠道 → 搜索 → 选中来源 → 填写显示名称、内容类型、启用订阅。
 * 编辑时回填名称、类型、启用状态，渠道和来源不可修改。
 */
const props = defineProps<{
  channels: NewsChannel[]
}>()

const emit = defineEmits<{
  /** 已添加或已保存 */
  saved: []
}>()

const CONTENT_TYPES: { value: NewsContentType; label: string }[] = [
  { value: 'ARTICLE', label: '文章' },
  { value: 'VIDEO', label: '视频' }
]

const visible = ref(false)
/** 编辑中的订阅源；添加时为 null */
const editingFeed = ref<NewsFeed | null>(null)
const channelCode = ref('')
const keyword = ref('')
/** 选中的来源；编辑时为订阅源自身的来源 */
const selected = ref<NewsSourceCandidate | null>(null)
const preview = ref<NewsSourcePreview | null>(null)
const isPreviewing = ref(false)
const previewError = ref('')
/** 保存失败的原因，如「该来源已订阅」 */
const submitError = ref('')
const isSubmitting = ref(false)

const form = reactive({ displayName: '', contentType: 'ARTICLE' as NewsContentType, enabled: true })
/** 打开时的表单值，用于判断是否有未保存的修改 */
let initialForm = { ...form }

const formRef = ref<FormInstance>()
const rules: FormRules = {
  displayName: [
    { required: true, whitespace: true, message: '请填写显示名称', trigger: 'blur' },
    { max: 128, message: '显示名称不能超过 128 个字', trigger: 'blur' }
  ]
}

const isEdit = computed(() => editingFeed.value !== null)
const channel = computed(() => props.channels.find((item) => item.code === channelCode.value))
const canSubmit = computed(() => preview.value !== null && !isPreviewing.value)

const isDirty = computed(() => {
  const formChanged =
    form.displayName !== initialForm.displayName ||
    form.contentType !== initialForm.contentType ||
    form.enabled !== initialForm.enabled
  if (isEdit.value) return formChanged
  return formChanged || keyword.value.trim() !== '' || selected.value !== null
})

const resetSource = () => {
  previewSeq++
  isPreviewing.value = false
  selected.value = null
  preview.value = null
  previewError.value = ''
  submitError.value = ''
}

/** 只采用最后一次预览请求的结果，换选中项后旧请求作废 */
let previewSeq = 0

const loadPreview = async () => {
  if (!selected.value) return
  const seq = ++previewSeq
  isPreviewing.value = true
  previewError.value = ''
  preview.value = null
  try {
    const result = await previewSourceApi(channelCode.value, selected.value.sourceKey)
    if (seq !== previewSeq) return
    preview.value = result
    if (!isEdit.value) form.contentType = result.contentType
  } catch (error) {
    if (!(error instanceof NewsApiError)) throw error
    if (seq === previewSeq) previewError.value = error.message
  } finally {
    if (seq === previewSeq) isPreviewing.value = false
  }
}

const onPick = (candidate: NewsSourceCandidate) => {
  if (selected.value?.sourceKey === candidate.sourceKey) return
  selected.value = candidate
  submitError.value = ''
  void loadPreview()
}

const onChannelChange = () => {
  keyword.value = ''
  resetSource()
  if (channel.value) form.contentType = channel.value.defaultContentType
}

const fillDefaultName = () => {
  if (selected.value) form.displayName = selected.value.name
}

/** 添加：默认选中给定渠道 */
const openAdd = (code: string) => {
  editingFeed.value = null
  channelCode.value = code
  keyword.value = ''
  resetSource()
  const target = props.channels.find((item) => item.code === code)
  Object.assign(form, { displayName: '', contentType: target?.defaultContentType ?? 'ARTICLE', enabled: true })
  initialForm = { ...form }
  formRef.value?.clearValidate()
  visible.value = true
}

/** 编辑：回填订阅源，并按其来源加载来源卡片 */
const openEdit = (code: string, feed: NewsFeed) => {
  editingFeed.value = feed
  channelCode.value = code
  keyword.value = ''
  resetSource()
  selected.value = {
    sourceKey: feed.sourceKey,
    name: feed.sourceName,
    handle: null,
    avatarUrl: feed.avatarUrl,
    description: feed.description,
    stats: []
  }
  Object.assign(form, { displayName: feed.displayName, contentType: feed.contentType, enabled: feed.enabled })
  initialForm = { ...form }
  formRef.value?.clearValidate()
  visible.value = true
  void loadPreview()
}

const onBeforeClose = async (done: () => void) => {
  if (isDirty.value && !(await confirmDiscard())) return
  done()
}

const close = () => onBeforeClose(() => (visible.value = false))

const submit = async () => {
  if (!formRef.value || !selected.value) return
  const valid = await formRef.value.validate().then(
    () => true,
    () => false
  )
  if (!valid) return
  const displayName = form.displayName.trim()
  submitError.value = ''
  isSubmitting.value = true
  try {
    if (editingFeed.value) {
      await updateFeedApi(editingFeed.value.id, { displayName, contentType: form.contentType, enabled: form.enabled })
    } else {
      await createFeedApi(channelCode.value, {
        sourceKey: selected.value.sourceKey,
        sourceName: selected.value.name,
        avatarUrl: selected.value.avatarUrl,
        description: selected.value.description,
        displayName,
        contentType: form.contentType,
        enabled: form.enabled
      })
    }
  } catch (error) {
    if (!(error instanceof NewsApiError)) throw error
    submitError.value = error.message
    return
  } finally {
    isSubmitting.value = false
  }
  notifySuccess(isEdit.value ? `已保存「${displayName}」` : `已添加「${displayName}」`)
  visible.value = false
  emit('saved')
}

defineExpose({ openAdd, openEdit })
</script>

<template>
  <el-dialog
    v-model="visible"
    class="dark-dialog news-admin-dialog feed-dialog"
    :title="isEdit ? '编辑订阅源' : '添加订阅源'"
    width="600px"
    append-to-body
    destroy-on-close
    :show-close="false"
    :before-close="onBeforeClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      require-asterisk-position="right"
      @submit.prevent
    >
      <el-form-item label="渠道" :class="{ 'is-locked': isEdit }">
        <el-select
          v-model="channelCode"
          :disabled="isEdit"
          popper-class="news-admin-popper"
          aria-label="渠道"
          @change="onChannelChange"
        >
          <el-option v-for="item in props.channels" :key="item.code" :label="item.name" :value="item.code" />
        </el-select>
      </el-form-item>

      <div v-if="channel" class="source-area">
        <FeedSourcePicker
          v-if="!isEdit"
          :key="channel.code"
          v-model:keyword="keyword"
          :channel="channel"
          :selected-key="selected?.sourceKey ?? null"
          @pick="onPick"
          @search="resetSource"
        />
        <FeedSourceCard
          v-if="selected"
          :preview="preview"
          :loading="isPreviewing"
          :error="previewError"
          :domain="channel.domain"
          @retry="loadPreview"
        />
      </div>

      <el-form-item label="显示名称" prop="displayName">
        <div class="name-row">
          <el-input v-model="form.displayName" :placeholder="selected?.name ?? ''" maxlength="128" />
          <el-button v-if="selected" text type="primary" class="default-name" @click="fillDefaultName">
            使用默认名称
          </el-button>
        </div>
      </el-form-item>

      <div class="inline-field">
        <span id="feed-content-type" class="inline-label">内容类型</span>
        <div class="segmented" role="radiogroup" aria-labelledby="feed-content-type">
          <button
            v-for="type in CONTENT_TYPES"
            :key="type.value"
            type="button"
            role="radio"
            class="segment"
            :class="{ 'is-checked': form.contentType === type.value }"
            :aria-checked="form.contentType === type.value"
            @click="form.contentType = type.value"
          >
            <SvgIcon
              v-if="form.contentType === type.value"
              class="segment-check"
              icon-class="newsAdmin-check"
              size="18"
            />
            {{ type.label }}
          </button>
        </div>
      </div>

      <div class="inline-field">
        <span id="feed-enabled" class="inline-label">启用订阅</span>
        <el-switch v-model="form.enabled" aria-labelledby="feed-enabled" />
      </div>

      <p v-if="submitError" class="submit-error" role="alert">{{ submitError }}</p>
    </el-form>

    <template #footer>
      <el-button text type="primary" @click="close">取消</el-button>
      <el-button text type="primary" :disabled="!canSubmit" :loading="isSubmitting" @click="submit">
        {{ isEdit ? '保存' : '添加' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
@use '../../components/newsMixins' as *;

.source-area {
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-lg);
  margin-bottom: var(--zg-space-xl);
}

.is-locked {
  opacity: 0.6;
}

.name-row {
  display: flex;
  align-items: center;
  gap: var(--zg-space-md);
  width: 100%;

  .el-input {
    flex: 1;
    min-width: 0;
  }
}

.default-name {
  flex: none;
}

.inline-field {
  display: flex;
  align-items: center;
  gap: var(--zg-space-xl);
  min-height: 40px;
}

.inline-label {
  flex: 1;
  min-width: 0;
}

.segmented {
  display: inline-flex;
  overflow: hidden;
  border: 1px solid var(--surface-outline);
  border-radius: var(--zg-radius-pill);
}

.segment {
  @include reset-button;
  @include state-layer;
  @include focus-ring(-2px);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--zg-space-sm);
  min-width: 80px;
  height: 32px;
  padding: 0 14px;
  font-size: var(--zg-text-base);
  font-weight: var(--zg-weight-medium);
  transition: background var(--zg-duration-base) var(--zg-ease-m3-standard);

  & + & {
    border-left: 1px solid var(--surface-outline);
  }

  &.is-checked {
    background: var(--selected-bg);
    color: var(--selected-text);
  }

  :deep(.svg-icon) {
    cursor: inherit;
  }
}

.segment-check {
  animation: segment-pop var(--zg-duration-base) var(--zg-ease-m3-standard);
}

.submit-error {
  margin: var(--zg-space-md) 0 0;
  color: var(--status-rejected);
  font-size: var(--zg-text-sm);
  line-height: 16px;
}

@keyframes segment-pop {
  from {
    opacity: 0;
    transform: scale(0.4);
  }
  to {
    opacity: 1;
    transform: none;
  }
}
</style>
