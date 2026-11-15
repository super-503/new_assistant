import { deleteRequest, get, post, putRequest } from '@/axios/request'
import type { NewsAiFieldKind, NewsAiFieldOutputType, NewsContentType } from './newsItem'
import { newsRequestData } from './newsAdminRequest'

/** 基础配置页接口：与后端 NewsSettingController 对应 */
const PREFIX = '/ai/api/web/v1/news/setting'

/** 模型配置及服务端密钥状态。 */
export interface NewsAiModelSetting {
  configured: boolean
  modelUrl: string
  model: string
  apiKeyConfigured: boolean
}

export interface NewsAiModelInput {
  modelUrl: string
  model: string
  /** null 沿用同一地址的密钥，空串清除密钥。 */
  apiKey: string | null
}

export const getAiModelApi = () => newsRequestData(get<NewsAiModelSetting>(`${PREFIX}/ai-model`))

export const saveAiModelApi = (input: NewsAiModelInput) =>
  newsRequestData(putRequest<NewsAiModelSetting>(`${PREFIX}/ai-model`, input))

export const verifyAiModelApi = (input: NewsAiModelInput) =>
  newsRequestData(post<null>(`${PREFIX}/ai-model/verify`, input))

export interface NewsFeishuAppInput {
  appId: string
  /** null 沿用同一个应用已保存的密钥。 */
  appSecret: string | null
}

export interface NewsFeishuProfile {
  name: string | null
  avatarUrl: string | null
}

export interface NewsFeishuAuthorizedUser extends NewsFeishuProfile {
  openId: string
}

export interface NewsFeishuApp extends NewsFeishuProfile {
  appId: string
}

export interface NewsFeishuAppDraft extends NewsFeishuApp, NewsFeishuAppInput {}

export interface NewsFeishuRecipient extends NewsFeishuAuthorizedUser {
  appId: string
}

export interface NewsFeishuCardSettings {
  title: string
  color: 'blue' | 'green' | 'orange' | 'purple'
  showAuthor: boolean
  template?: NewsFeishuCardTemplate | null
}

export interface NewsFeishuCardTemplate {
  id: string
  version: string
  variablesJson: string
  variableTypes?: Record<string, string> | null
}

export type NewsDigestDeliveryChannel = 'FEISHU'

export interface NewsDigestContentSettings {
  deliveryChannels: NewsDigestDeliveryChannel[]
  channelCodes: string[]
  contentTypes: NewsContentType[]
  reviewDays: number | null
  fields: string[]
  useAiTitle: boolean
  compact: boolean
}

export interface NewsFeishuChannel {
  revision: number
  apps: NewsFeishuApp[]
  recipients: NewsFeishuRecipient[]
  card: NewsFeishuCardSettings
  content?: NewsDigestContentSettings | null
  enabled: boolean
}

export interface NewsFeishuChannelInput extends NewsFeishuChannel {
  apps: NewsFeishuAppDraft[]
  content: NewsDigestContentSettings
}

interface NewsFeishuAuthorizationStart {
  state: string
  authorizationUrl: string
  expiresIn: number
}

const FEISHU_CHANNEL = `${PREFIX}/notification-channel/feishu`

export const getFeishuChannelApi = () => newsRequestData(get<NewsFeishuChannel>(FEISHU_CHANNEL))

export const saveFeishuChannelApi = (input: NewsFeishuChannelInput) =>
  newsRequestData(putRequest<NewsFeishuChannel>(FEISHU_CHANNEL, input))

export const listFeishuAuthorizedUsersApi = (appId: string) =>
  newsRequestData(get<NewsFeishuAuthorizedUser[]>(`${FEISHU_CHANNEL}/authorized-users`, { appId }))

export const testFeishuCardApi = (
  input: NewsFeishuAppInput & { openId: string; card: NewsFeishuCardSettings; content: NewsDigestContentSettings }
) => newsRequestData(post<null>(`${FEISHU_CHANNEL}/test-card`, input))

export const getFeishuBotProfileApi = (input: NewsFeishuAppInput, toast = true) =>
  newsRequestData(post<NewsFeishuProfile>(`${PREFIX}/notification-channel/bot-profile`, input), toast)

export const startFeishuAuthorizationApi = (input: NewsFeishuAppInput) =>
  newsRequestData(post<NewsFeishuAuthorizationStart>(`${PREFIX}/notification-channel/authorization`, input), false)

export const getFeishuAuthorizationResultApi = (state: string) =>
  newsRequestData(get<NewsFeishuAuthorizedUser | null>(`${PREFIX}/notification-channel/authorization/result`, { state }), false)

/** AI 字段的来源内容：与后端 NewsAiFieldSource 对应 */
export type NewsAiFieldSource = 'BODY' | 'TITLE' | 'TITLE_BODY'

/** AI 字段配置：与后端 NewsAiFieldVO 对应 */
export interface NewsAiFieldSetting {
  id: number
  fieldKey: string
  name: string
  /** SYSTEM 显示「系统」标签，BUILTIN 显示「内置」标签 */
  kind: NewsAiFieldKind
  kindDesc: string
  source: NewsAiFieldSource
  sourceDesc: string
  outputType: NewsAiFieldOutputType
  /** 输出类型的展示名：单行文本 / 多行文本 / 数字 / 单选 / 多选 */
  outputTypeLabel: string
  multiline: boolean
  /** 单选、多选的选项 */
  options: string[]
  showAsTag: boolean
  prompt: string
  required: boolean
  editable: boolean
  enabled: boolean
  /** 是否只对文章生成 */
  articleOnly: boolean
  sortNo: number
  /** 只有普通字段可以删除 */
  deletable: boolean
  /** 系统字段、内置字段锁定字段标识 */
  fieldKeyLocked: boolean
  /** 系统字段、内置字段锁定输出类型 */
  outputTypeLocked: boolean
  /** 内置字段只有提示词可以修改，列表里的启用开关也不可操作 */
  promptOnly: boolean
}

/** 新增或编辑 AI 字段：与后端 NewsAiFieldSaveRequest 对应 */
export interface NewsAiFieldInput {
  name: string
  fieldKey: string
  source: NewsAiFieldSource
  outputType: NewsAiFieldOutputType
  /** 只有文本可以开启 */
  multiline: boolean
  /** 单选、多选的选项；其他输出类型传空数组 */
  options: string[]
  /** 只有单选、多选可以开启 */
  showAsTag: boolean
  prompt: string
  required: boolean
  editable: boolean
  enabled: boolean
}

/** 拒绝理由配置：与后端 NewsRejectReasonVO 对应 */
export interface NewsRejectReasonSetting {
  id: number
  name: string
  enabled: boolean
  sortNo: number
}

/** 新增或编辑拒绝理由：与后端 NewsRejectReasonSaveRequest 对应 */
export interface NewsRejectReasonInput {
  name: string
  enabled: boolean
}

/** 全部 AI 字段，按展示顺序 */
export const listAiFieldsApi = () =>
  newsRequestData(get<NewsAiFieldSetting[]>(`${PREFIX}/ai-fields`, {}))

/** 新增普通 AI 字段，返回新字段的主键；失败原因由调用方就地展示 */
export const createAiFieldApi = (input: NewsAiFieldInput) =>
  newsRequestData(post<number>(`${PREFIX}/ai-fields`, input), false)

/** 编辑 AI 字段，锁定项须与当前值一致；失败原因由调用方就地展示 */
export const updateAiFieldApi = (id: number, input: NewsAiFieldInput) =>
  newsRequestData(putRequest<null>(`${PREFIX}/ai-fields/${id}`, input), false)

/** 删除普通 AI 字段 */
export const deleteAiFieldApi = (id: number) =>
  newsRequestData<null>(deleteRequest(`${PREFIX}/ai-fields/${id}`, {}))

/** 启用或停用 AI 字段，内置字段不可操作 */
export const setAiFieldEnabledApi = (id: number, enabled: boolean) =>
  newsRequestData(putRequest<null>(`${PREFIX}/ai-fields/${id}/enabled`, { enabled }))

/** 按传入的完整顺序重排全部 AI 字段 */
export const sortAiFieldsApi = (ids: number[]) =>
  newsRequestData(putRequest<null>(`${PREFIX}/ai-fields/sort`, { ids }))

/** 全部拒绝理由，按排序 */
export const listRejectReasonsApi = () =>
  newsRequestData(get<NewsRejectReasonSetting[]>(`${PREFIX}/reject-reasons`, {}))

/** 新增拒绝理由，返回新理由的主键；失败原因由调用方就地展示 */
export const createRejectReasonApi = (input: NewsRejectReasonInput) =>
  newsRequestData(post<number>(`${PREFIX}/reject-reasons`, input), false)

/** 编辑拒绝理由；失败原因由调用方就地展示 */
export const updateRejectReasonApi = (id: number, input: NewsRejectReasonInput) =>
  newsRequestData(putRequest<null>(`${PREFIX}/reject-reasons/${id}`, input), false)

export const setRejectReasonEnabledApi = (id: number, enabled: boolean) =>
  newsRequestData(putRequest<null>(`${PREFIX}/reject-reasons/${id}/enabled`, { enabled }))
