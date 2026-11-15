import { get, post } from '@/axios/request'
import type { NewsContentType } from './newsItem'
import { newsRequestData } from './newsAdminRequest'

/** 渠道代理配置页接口：与后端 NewsChannelController 对应 */
const PREFIX = '/ai/api/web/v1/news'

/** 凭证方式：与后端 NewsCredentialMode 对应，决定渠道设置里凭证区的形态 */
export type NewsCredentialMode = 'NONE' | 'QR' | 'TOKEN'

/** 凭证状态：与后端 NewsCredentialStatus 对应，NONE 为无需凭证（不显示） */
export type NewsCredentialStatus = 'NONE' | 'VALID' | 'INVALID'

/** 订阅源展示状态：与后端 NewsFeedStatus 对应，也是状态筛选项 */
export type NewsFeedStatus = 'OK' | 'ERROR' | 'PENDING' | 'DISABLED'

/** 扫码状态：与后端 NewsQrLoginStatus 对应 */
export type NewsQrLoginStatus = 'WAITING' | 'SCANNED' | 'CONFIRMED' | 'EXPIRED'

/** 渠道运行结果：与后端 NewsRunStatus 对应 */
export type NewsRunStatus = 'SUCCEEDED' | 'FAILED'

/** 渠道运行的触发方式：与后端 NewsRunTriggerType 对应 */
export type NewsRunTriggerType = 'SCHEDULE' | 'MANUAL'

/** 渠道：与后端 ChannelVO 对应 */
export interface NewsChannel {
  /** 渠道标识，只作为接口参数回传，前端不按它分支 */
  code: string
  name: string
  /** 原文来源域名，来源卡片上显示为「原文来源 域名」 */
  domain: string
  credentialMode: NewsCredentialMode
  /** 是否支持按关键词搜索来源；不支持时直接展示唯一的来源 */
  searchable: boolean
  /** 搜索来源输入框的占位提示，由渠道提供；不支持搜索的渠道为 null */
  searchHint: string | null
  /** 新增订阅源时默认选中的内容类型 */
  defaultContentType: NewsContentType
  enabled: boolean
  credentialStatus: NewsCredentialStatus
  credentialStatusDesc: string
  /** 登录账号；未登录时为 null */
  credentialAccount: string | null
  /** 凭证最近一次保存时间 yyyy-MM-dd HH:mm:ss；未登录时为 null */
  credentialUpdateTime: string | null
  feedCount: number
  /** 凭证已失效或有订阅源拉取失败，名称旁显示红色状态点 */
  needsAttention: boolean
}

/** 订阅源：与后端 FeedVO 对应 */
export interface NewsFeed {
  id: number
  /** 插件内的来源标识，编辑时据此预览来源 */
  sourceKey: string
  /** 来源在渠道上的名称，「使用默认名称」填入的值 */
  sourceName: string
  displayName: string
  avatarUrl: string | null
  description: string | null
  contentType: NewsContentType
  contentTypeDesc: string
  enabled: boolean
  status: NewsFeedStatus
  statusDesc: string
  /** 拉取失败的原因；状态不是拉取失败时为 null */
  error: string | null
  /** 最近一次拉取时间 yyyy-MM-dd HH:mm:ss；未拉取过时为 null */
  lastPullTime: string | null
  /** 正在拉取时禁用立即拉取和重试 */
  pulling: boolean
}

/** 「需处理」里拉取失败的订阅源：与后端 TodoFeedVO 对应 */
export interface NewsTodoFeed {
  channelCode: string
  channelName: string
  feed: NewsFeed
}

/** 「需处理」汇总：与后端 ChannelTodoVO 对应 */
export interface NewsChannelTodo {
  /** 凭证失效的渠道数加拉取失败的订阅源数 */
  total: number
  credentialChannels: NewsChannel[]
  failedFeeds: NewsTodoFeed[]
}

/** 渠道的一次运行：与后端 ChannelRunVO 对应 */
export interface NewsChannelRun {
  id: number
  triggerType: NewsRunTriggerType
  triggerTypeDesc: string
  status: NewsRunStatus
  statusDesc: string
  newCount: number
  detail: string
  /** 手动触发时拉取的订阅源；定时运行为 null */
  feedId: number | null
  /** 手动触发时拉取的订阅源显示名称；定时运行或订阅源已删除时为 null */
  feedName: string | null
  startTime: string
  endTime: string | null
}

/** 「验证连接」结果：与后端 CredentialVerifyVO 对应 */
export interface NewsCredentialVerify {
  valid: boolean
  /** 凭证可用时的登录账号 */
  account: string | null
  /** 凭证不可用时的失败原因 */
  message: string | null
}

/** 发起扫码登录：与后端 QrLoginStartVO 对应 */
export interface NewsQrLoginStart {
  sessionId: string
  /** 二维码图片，data URL，直接作为 img 的 src */
  qrImage: string
  expireSeconds: number
}

/** 扫码轮询结果：与后端 QrLoginPollVO 对应 */
export interface NewsQrLoginPoll {
  status: NewsQrLoginStatus
  statusDesc: string
  /** CONFIRMED 时为登录账号 */
  account: string | null
}

/** 渠道返回的一项统计：与后端 StatItemVO 对应，数值由前端格式化 */
export interface NewsSourceStat {
  name: string
  value: number
}

/** 搜索来源的一条结果：与后端 SourceCandidateVO 对应 */
export interface NewsSourceCandidate {
  sourceKey: string
  name: string
  /** 来源在渠道上的可读标识，如微信号、UID、@用户名 */
  handle: string | null
  avatarUrl: string | null
  description: string | null
  stats: NewsSourceStat[]
}

/** 来源卡片「最近更新」的一条：与后端 SourceRecentItemVO 对应 */
export interface NewsSourceRecentItem {
  title: string
  thumbnailUrl: string | null
  publishTime: string
  /** 视频时长（秒），视频才有 */
  durationSeconds: number | null
  stats: NewsSourceStat[]
}

/** 来源卡片：与后端 SourcePreviewVO 对应 */
export interface NewsSourcePreview {
  source: NewsSourceCandidate
  contentType: NewsContentType
  contentTypeDesc: string
  recentItems: NewsSourceRecentItem[]
}

/** 订阅来源：与后端 FeedCreateRequest 对应，来源信息原样取自搜索结果 */
export interface NewsFeedCreateInput {
  sourceKey: string
  sourceName: string
  avatarUrl: string | null
  description: string | null
  displayName: string
  contentType: NewsContentType
  enabled: boolean
}

/** 编辑订阅源：与后端 FeedUpdateRequest 对应 */
export interface NewsFeedUpdateInput {
  displayName: string
  contentType: NewsContentType
  enabled: boolean
}

/** 订阅源列表筛选；不传的项不筛选 */
export interface NewsFeedQuery {
  status?: NewsFeedStatus
  keyword?: string
}

export const listChannelsApi = () => newsRequestData(get<NewsChannel[]>(`${PREFIX}/channels`, {}))

/** 启用或停用渠道 */
export const saveChannelSettingsApi = (channelCode: string, enabled: boolean) =>
  newsRequestData(post<null>(`${PREFIX}/channels/${channelCode}/settings`, { enabled }))

/** 验证连接：用渠道已保存的凭证查询登录账号。失败原因在结果里，接口异常同样就地展示 */
export const verifyCredentialApi = (channelCode: string) =>
  newsRequestData(
    post<NewsCredentialVerify>(`${PREFIX}/channels/${channelCode}/credential/verify`, {}),
    false
  )

/** 验证连接：用输入框里的 Token 查询登录账号，不保存。失败原因在结果里，接口异常同样就地展示 */
export const verifyTokenApi = (channelCode: string, token: string) =>
  newsRequestData(
    post<NewsCredentialVerify>(
      `${PREFIX}/channels/${channelCode}/credential/token/verify`,
      { token }
    ),
    false
  )

export const startQrLoginApi = (channelCode: string) =>
  newsRequestData(
    post<NewsQrLoginStart>(`${PREFIX}/channels/${channelCode}/credential/qr/start`, {}),
    false
  )

export const pollQrLoginApi = (channelCode: string, sessionId: string) =>
  newsRequestData(
    post<NewsQrLoginPoll>(`${PREFIX}/channels/${channelCode}/credential/qr/poll`, { sessionId }),
    false
  )

/** 保存 Token：校验通过才保存，返回保存后的渠道；校验失败的原因由调用方就地展示 */
export const saveTokenApi = (channelCode: string, token: string) =>
  newsRequestData(
    post<NewsChannel>(`${PREFIX}/channels/${channelCode}/credential/token`, { token }),
    false
  )

/** 渠道最近 50 次运行记录，按开始时间倒序 */
export const listChannelRunsApi = (channelCode: string) =>
  newsRequestData(get<NewsChannelRun[]>(`${PREFIX}/channels/${channelCode}/runs`, {}))

export const getChannelTodoApi = () => newsRequestData(get<NewsChannelTodo>(`${PREFIX}/todo`, {}))

export const listFeedsApi = (channelCode: string, query: NewsFeedQuery) =>
  newsRequestData(get<NewsFeed[]>(`${PREFIX}/channels/${channelCode}/feeds`, query))

/** 订阅来源；重复来源等失败原因由调用方就地展示 */
export const createFeedApi = (channelCode: string, input: NewsFeedCreateInput) =>
  newsRequestData(post<null>(`${PREFIX}/channels/${channelCode}/feeds`, input), false)

/** 搜索来源；不支持搜索的渠道不传关键词，直接返回唯一的来源。失败原因由调用方就地展示 */
export const searchSourcesApi = (channelCode: string, keyword?: string) =>
  newsRequestData(
    post<NewsSourceCandidate[]>(`${PREFIX}/channels/${channelCode}/sources/search`, { keyword }),
    false
  )

/** 预览来源；失败原因由调用方就地展示 */
export const previewSourceApi = (channelCode: string, sourceKey: string) =>
  newsRequestData(
    post<NewsSourcePreview>(`${PREFIX}/channels/${channelCode}/sources/preview`, { sourceKey }),
    false
  )

/** 编辑订阅源；失败原因由调用方就地展示 */
export const updateFeedApi = (feedId: number, input: NewsFeedUpdateInput) =>
  newsRequestData(post<null>(`${PREFIX}/feeds/${feedId}`, input), false)

export const setFeedEnabledApi = (feedId: number, enabled: boolean) =>
  newsRequestData(post<null>(`${PREFIX}/feeds/${feedId}/enabled`, { enabled }))

/** 删除订阅源，已入库的资讯保留 */
export const deleteFeedApi = (feedId: number) =>
  newsRequestData(post<null>(`${PREFIX}/feeds/${feedId}/delete`, {}))

/** 立即拉取：加入拉取队列后异步执行 */
export const pullFeedApi = (feedId: number) =>
  newsRequestData(post<null>(`${PREFIX}/feeds/${feedId}/pull`, {}))

/** 重试拉取失败的订阅源；渠道凭证失效时失败原因为「重试失败：渠道凭证失效」 */
export const retryFeedApi = (feedId: number) =>
  newsRequestData(post<null>(`${PREFIX}/feeds/${feedId}/retry`, {}))
