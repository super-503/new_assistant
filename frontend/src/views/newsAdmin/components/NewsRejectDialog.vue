<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import {
  batchRejectApi,
  getRejectReasonsApi,
  rejectItemApi,
  type NewsBatchFailure,
  type NewsRejectReasonOption
} from '@/api/newsItem'
import NewsSpinner from './NewsSpinner.vue'

/** 拒绝结果：成功的资讯与失败项 */
export interface NewsRejectOutcome {
  succeededIds: number[]
  failures: NewsBatchFailure[]
}

/**
 * 拒绝弹窗：理由单选、必选，未选时确认按钮禁用；可填补充说明。
 * 单条标题为「拒绝资讯」，多条为批量拒绝；确认后由本组件提交，结果通过 rejected 事件交给父组件。
 */
const props = defineProps<{
  /** 要拒绝的资讯 */
  ids: number[]
}>()

const emit = defineEmits<{ rejected: [outcome: NewsRejectOutcome] }>()

const visible = defineModel<boolean>('visible', { required: true })

/** 补充说明最长 500 字 */
const NOTE_MAX_LENGTH = 500

const reasons = ref<NewsRejectReasonOption[]>([])
const reasonId = ref<number | null>(null)
const note = ref('')
const isSubmitting = ref(false)

const isBatch = computed(() => props.ids.length > 1)
const title = computed(() => (isBatch.value ? '批量拒绝资讯' : '拒绝资讯'))
const description = computed(
  () => `${isBatch.value ? `已选择 ${props.ids.length} 条资讯，` : ''}拒绝后可在「已拒绝」中恢复。`
)
const confirmText = computed(() => (isBatch.value ? `确认拒绝 ${props.ids.length} 条` : '确认拒绝'))
const hasInput = computed(() => reasonId.value !== null || note.value.trim() !== '')

const onOpen = async () => {
  reasonId.value = null
  note.value = ''
  try {
    reasons.value = await getRejectReasonsApi()
  } catch (e) {
    console.error(e)
  }
}

/** 点遮罩、按 Esc 或右上角关闭：填过内容时先确认放弃 */
const onBeforeClose = (done: () => void) => {
  if (!hasInput.value) {
    done()
    return
  }
  ElMessageBox.confirm('已选择的理由和填写的说明将不会保存。', '放弃未保存的修改？', {
    customClass: 'dark-message-box news-admin-message-box',
    confirmButtonText: '放弃修改',
    cancelButtonText: '继续编辑'
  }).then(done, () => undefined)
}

const onConfirm = async () => {
  if (reasonId.value === null) return
  const noteText = note.value.trim() === '' ? null : note.value.trim()
  isSubmitting.value = true
  try {
    if (isBatch.value) {
      const result = await batchRejectApi(props.ids, reasonId.value, noteText)
      const failedIds = result.failures.map((failure) => failure.id)
      emit('rejected', { succeededIds: props.ids.filter((id) => !failedIds.includes(id)), failures: result.failures })
    } else {
      await rejectItemApi(props.ids[0], reasonId.value, noteText)
      emit('rejected', { succeededIds: [...props.ids], failures: [] })
    }
    visible.value = false
  } catch (e) {
    console.error(e)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    class="dark-dialog news-admin-dialog reject-dialog"
    :title="title"
    width="480px"
    append-to-body
    :before-close="onBeforeClose"
    @open="onOpen"
  >
    <p class="reject-desc">{{ description }}</p>
    <div class="reason-list" role="radiogroup" aria-label="拒绝理由">
      <label v-for="reason in reasons" :key="reason.id" class="reason-item">
        <input v-model="reasonId" type="radio" name="news-reject-reason" :value="reason.id" />
        {{ reason.name }}
      </label>
    </div>
    <div class="note-field">
      <label for="news-reject-note" class="note-label">补充说明（选填）</label>
      <textarea
        id="news-reject-note"
        v-model="note"
        class="note-input"
        rows="3"
        :maxlength="NOTE_MAX_LENGTH"
        placeholder="会记录在处理记录中"
      ></textarea>
    </div>
    <template #footer>
      <div class="reject-actions">
        <button type="button" class="dialog-text-button" @click="visible = false">取消</button>
        <button
          type="button"
          class="dialog-text-button"
          :disabled="reasonId === null || isSubmitting"
          @click="onConfirm"
        >
          <NewsSpinner v-if="isSubmitting" />
          {{ confirmText }}
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
@use './newsMixins' as *;

.reject-desc {
  margin: 0;
  color: var(--dialog-text);
  font-size: var(--zg-text-base);
  line-height: 20px;
}

.reason-list {
  display: flex;
  flex-direction: column;
  margin: var(--zg-space-lg) calc(var(--zg-space-lg) * -1) 0;
}

.reason-item {
  @include state-layer;
  display: flex;
  align-items: center;
  gap: var(--zg-space-lg);
  height: 40px;
  padding: 0 var(--zg-space-lg);
  border-radius: var(--zg-radius-pill);
  color: var(--main-color);
  font-size: var(--zg-text-base);
  cursor: pointer;

  input {
    width: 20px;
    height: 20px;
    margin: 0;
    accent-color: var(--brand-indigo);
    cursor: pointer;
  }
}

.note-field {
  display: grid;
  gap: var(--zg-space-sm);
  margin-top: var(--zg-space-xl);
}

.note-label {
  color: var(--second-color);
  font-size: var(--zg-text-md);
  line-height: 20px;
  font-weight: var(--zg-weight-medium);
}

.note-input {
  width: 100%;
  padding: var(--zg-space-md) var(--zg-space-lg);
  border: 1px solid transparent;
  border-radius: var(--zg-radius-md);
  background: var(--control-bg);
  color: var(--control-text);
  font-family: inherit;
  font-size: var(--zg-text-base);
  line-height: 22px;
  resize: vertical;

  &::placeholder {
    color: var(--control-placeholder);
  }
  &:focus {
    outline: none;
    border-color: var(--control-focus-border);
  }
}

.reject-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--zg-space-md);
}

.dialog-text-button {
  @include text-button;
  gap: var(--zg-space-sm);

  &:disabled {
    color: var(--text-disabled);
    cursor: not-allowed;
  }
}
</style>
