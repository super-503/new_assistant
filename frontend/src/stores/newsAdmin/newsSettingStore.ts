import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getAiModelApi,
  getNotificationChannelApi,
  listAiFieldsApi,
  listRejectReasonsApi,
  type NewsAiModelSetting,
  type NewsAiFieldSetting,
  type NewsFeishuNotificationSetting,
  type NewsRejectReasonSetting
} from '@/api/newsSetting'

/**
 * 基础配置的缓存：AI 字段和拒绝理由很少变化，首次进入页面时拉取，之后复用；
 * 本页保存、排序、启停后调用 refresh 重新拉取。
 */
export const useNewsSettingStore = defineStore('newsSetting', () => {
  /** 全部 AI 字段，按展示顺序；未加载时为 null */
  const aiFields = ref<NewsAiFieldSetting[] | null>(null)
  const aiModel = ref<NewsAiModelSetting | null>(null)
  const notificationChannel = ref<NewsFeishuNotificationSetting | null>(null)
  /** 全部拒绝理由，按排序；未加载时为 null */
  const rejectReasons = ref<NewsRejectReasonSetting[] | null>(null)

  const refreshAiFields = async () => {
    aiFields.value = await listAiFieldsApi()
  }

  const refreshRejectReasons = async () => {
    rejectReasons.value = await listRejectReasonsApi()
  }

  const ensureAiModel = async () => {
    aiModel.value ??= await getAiModelApi()
    return aiModel.value
  }

  const ensureNotificationChannel = async () => {
    notificationChannel.value ??= await getNotificationChannelApi()
    return notificationChannel.value
  }

  /** 未加载过才请求 */
  const ensureAiFields = () => (aiFields.value ? Promise.resolve() : refreshAiFields())

  const ensureRejectReasons = () => (rejectReasons.value ? Promise.resolve() : refreshRejectReasons())

  return {
    aiFields,
    aiModel,
    notificationChannel,
    rejectReasons,
    refreshAiFields,
    refreshRejectReasons,
    ensureAiFields,
    ensureAiModel,
    ensureNotificationChannel,
    ensureRejectReasons
  }
})
