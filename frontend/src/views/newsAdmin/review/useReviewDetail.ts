import { computed, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { omit } from 'lodash-es'
import {
  generateFieldApi,
  getReviewDetailApi,
  reprocessItemApi,
  saveReviewFieldsApi,
  type NewsFieldValue,
  type NewsItemDetail,
  type NewsItemField
} from '@/api/newsItem'
import { isEmptyFieldValue, isSameFieldValue, normalizeFieldValue } from '../components/newsFieldValue'

interface ReviewDetailOptions {
  /** 详情被保存、重新加工或单字段生成刷新后调用，用于同步队列里的卡片 */
  onRefreshed: (detail: NewsItemDetail) => void
  /** 显示底部提示 */
  onNotify: (message: string) => void
}

/**
 * 审核详情面板的数据：当前资讯详情、未保存的字段修改（draft 只含动过的字段）、必填校验、
 * 保存 / 取消修改、重新加工、单字段生成，以及放弃修改前的确认。
 */
export function useReviewDetail(options: ReviewDetailOptions) {
  const detail = ref<NewsItemDetail | null>(null)
  const isLoading = ref(false)
  const draft = ref<Record<string, NewsFieldValue>>({})
  /** 保存或通过时校验过必填，开始显示必填提示 */
  const showErrors = ref(false)
  const isSaving = ref(false)
  const isReprocessing = ref(false)
  /** 正在单独生成的字段标识 */
  const generatingKey = ref<string | null>(null)
  /** 只采用最后一次打开的详情，避免快速切换时旧响应覆盖新内容 */
  let openSeq = 0

  const hasDraft = (field: NewsItemField) => Object.prototype.hasOwnProperty.call(draft.value, field.fieldKey)
  const getCurrentValue = (field: NewsItemField) => (hasDraft(field) ? draft.value[field.fieldKey] : field.value)

  /** 有未保存修改的字段 */
  const dirtyFields = computed(() =>
    (detail.value?.fields ?? []).filter(
      (field) => hasDraft(field) && !isSameFieldValue(draft.value[field.fieldKey], field.value)
    )
  )
  const isDirty = computed(() => dirtyFields.value.length > 0)
  /** 必填但当前为空的字段 */
  const emptyRequiredFields = computed(() =>
    (detail.value?.fields ?? []).filter((field) => field.required && isEmptyFieldValue(getCurrentValue(field)))
  )

  const resetDraft = () => {
    draft.value = {}
    showErrors.value = false
  }

  const applyRefreshed = (next: NewsItemDetail) => {
    detail.value = next
    options.onRefreshed(next)
  }

  const openDetail = async (id: number) => {
    const seq = ++openSeq
    resetDraft()
    detail.value = null
    isLoading.value = true
    try {
      const next = await getReviewDetailApi(id)
      if (seq === openSeq) detail.value = next
    } catch (e) {
      console.error(e)
    } finally {
      if (seq === openSeq) isLoading.value = false
    }
  }

  const closeDetail = () => {
    openSeq++
    detail.value = null
    isLoading.value = false
    resetDraft()
  }

  const updateDraft = (fieldKey: string, value: NewsFieldValue) => {
    draft.value = { ...draft.value, [fieldKey]: value }
  }

  /** 「填写」未生成的字段：从空值开始编辑 */
  const fillField = (fieldKey: string) => {
    const field = detail.value!.fields.find((item) => item.fieldKey === fieldKey)!
    updateDraft(fieldKey, field.outputType === 'MULTI' ? [] : null)
  }

  /** 校验必填：不满足时显示提示并返回 false */
  const validateRequired = () => {
    if (emptyRequiredFields.value.length === 0) return true
    showErrors.value = true
    return false
  }

  /**
   * 保存未保存的修改；没有修改时直接成功。
   * @returns 保存成功（或无需保存）返回 true
   */
  const saveDraft = async (notify: boolean) => {
    if (!isDirty.value) return true
    if (!validateRequired()) return false
    const current = detail.value!
    isSaving.value = true
    try {
      const fields = dirtyFields.value.map((field) => ({
        fieldKey: field.fieldKey,
        value: normalizeFieldValue(field, draft.value[field.fieldKey])
      }))
      applyRefreshed(await saveReviewFieldsApi(current.id, fields))
      resetDraft()
      if (notify) options.onNotify('已保存修改，资讯仍在待审')
      return true
    } catch (e) {
      console.error(e)
      return false
    } finally {
      isSaving.value = false
    }
  }

  /**
   * 有未保存的修改时确认是否放弃。
   * @returns 可以继续（没有修改或选择放弃）返回 true
   */
  const confirmDiscard = async () => {
    if (!isDirty.value) return true
    const names = dirtyFields.value.map((field) => field.name).join('、')
    const isDiscarded = await ElMessageBox.confirm(
      `${names}的修改还没有保存，放弃后恢复为修改前的内容。`,
      '放弃未保存的修改？',
      {
        customClass: 'dark-message-box news-admin-message-box is-danger',
        confirmButtonText: '放弃修改',
        cancelButtonText: '继续编辑'
      }
    ).then(
      () => true,
      () => false
    )
    if (isDiscarded) resetDraft()
    return isDiscarded
  }

  const reprocess = async () => {
    if (!(await confirmDiscard())) return
    const current = detail.value!
    isReprocessing.value = true
    try {
      applyRefreshed(await reprocessItemApi(current.id))
      resetDraft()
      options.onNotify('重新加工完成')
    } catch (e) {
      console.error(e)
    } finally {
      isReprocessing.value = false
    }
  }

  /** 单独生成一个字段；其他字段未保存的修改保留 */
  const generateField = async (fieldKey: string) => {
    const current = detail.value!
    generatingKey.value = fieldKey
    try {
      applyRefreshed(await generateFieldApi(current.id, fieldKey))
      draft.value = omit(draft.value, fieldKey)
    } catch (e) {
      console.error(e)
    } finally {
      generatingKey.value = null
    }
  }

  return {
    detail,
    isLoading,
    draft,
    showErrors,
    isSaving,
    isReprocessing,
    generatingKey,
    dirtyFields,
    isDirty,
    openDetail,
    closeDetail,
    updateDraft,
    fillField,
    resetDraft,
    validateRequired,
    saveDraft,
    confirmDiscard,
    reprocess,
    generateField
  }
}

export type ReviewDetailState = ReturnType<typeof useReviewDetail>
