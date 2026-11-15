import { get, post, putRequest } from '@/axios/request'
import { newsRequestData } from './newsAdminRequest'
import type { NewsDigestContentSettings, NewsDigestDeliveryChannel } from './newsSetting'

const PATH = '/ai/api/web/v1/news/setting/push-plans'

export type NewsPushPlanCard = 'BUILTIN' | 'SAVED'

export interface NewsPushPlanDefinition {
  name: string
  sendTime: string
  channel: NewsDigestDeliveryChannel
  appId: string
  recipientOpenIds: string[]
  card: NewsPushPlanCard
  content: NewsDigestContentSettings
}

export interface NewsPushPlanInput {
  revision?: number
  enabled: boolean
  definition: NewsPushPlanDefinition
}

export interface NewsPushPlan extends NewsPushPlanInput {
  id: number
  revision: number
}

interface NewsPushPlanList {
  timeZone: string
  plans: NewsPushPlan[]
}

export const listPushPlansApi = () => newsRequestData(get<NewsPushPlanList>(PATH))

export const savePushPlanApi = (id: number | undefined, input: NewsPushPlanInput) =>
  newsRequestData(id === undefined ? post<NewsPushPlan>(PATH, input) : putRequest<NewsPushPlan>(`${PATH}/${id}`, input))

export const previewPushPlanApi = (definition: NewsPushPlanDefinition, openId: string) =>
  newsRequestData(post<null>(`${PATH}/preview`, { definition, openId }), false)

export interface NewsDigestPreviewItem {
  title: string
  url: string | null
  fields: { key: string; label: string; display: string }[]
}

export const previewDigestContentApi = (content: NewsDigestContentSettings) =>
  newsRequestData(post<NewsDigestPreviewItem[]>(`${PATH}/content-preview`, content), false)
