import { get, post } from '@/axios/request'
import { newsRequestData } from '@/api/newsAdminRequest'

/** 资讯审核、资讯库接口：与后端 NewsReviewController、NewsLibraryController 对应 */
const REVIEW_PREFIX = '/ai/api/web/v1/news/review'
const LIBRARY_PREFIX = '/ai/api/web/v1/news/library'

/** 资讯类型：与后端 NewsContentType 对应 */
export type NewsContentType = 'ARTICLE' | 'VIDEO'

/** 资讯状态：与后端 NewsItemStatus 对应 */
export type NewsItemStatus = 'PROCESSING' | 'PENDING' | 'APPROVED' | 'REJECTED'

/** AI 字段种类：与后端 NewsAiFieldKind 对应，BUILTIN 为岗位分类 */
export type NewsAiFieldKind = 'SYSTEM' | 'BUILTIN' | 'CUSTOM'

/** AI 字段输出类型：与后端 NewsAiFieldOutputType 对应 */
export type NewsAiFieldOutputType = 'TEXT' | 'NUMBER' | 'SINGLE' | 'MULTI'

/** 处理记录动作：与后端 NewsItemAction 对应 */
export type NewsItemAction =
  | 'COLLECTED'
  | 'PROCESSED'
  | 'PROCESS_FAILED'
  | 'FIELD_EDITED'
  | 'APPROVED'
  | 'REJECTED'
  | 'RESTORED'

/** 站内播放方式：与后端 NewsPlaybackKind 对应 */
export type NewsPlaybackKind = 'EMBED' | 'FILE'

/** 审核队列排序：与后端 NewsReviewSort 对应，按发布时间 */
export type NewsReviewSort = 'NEWEST' | 'OLDEST'

/** 审核分类 */
export type NewsReviewStatus = 'PENDING' | 'REJECTED'

/** AI 字段的值：文本、单选为字符串，数字为数值，多选为字符串数组，未生成或清空为 null */
export type NewsFieldValue = string | number | string[] | null

/** 单选、多选字段的一个取值及其标签配色：与后端 NewsFieldOptionVO 对应 */
export interface NewsFieldOption {
  value: string
  /** 标签配色序号；岗位分类、非标签字段、已不在选项里的值为 null，用中性样式 */
  colorIndex: number | null
}

/** 资讯卡片的一个标签字段及其取值：与后端 NewsItemTagVO 对应 */
export interface NewsItemTag {
  fieldKey: string
  name: string
  kind: NewsAiFieldKind
  values: NewsFieldOption[]
}

/** 字段的标识和名称：与后端 NewsFieldRefVO 对应 */
export interface NewsFieldRef {
  fieldKey: string
  name: string
}

/** 资讯列表项，审核队列和资讯库共用：与后端 NewsItemCardVO 对应 */
export interface NewsItemCard {
  id: number
  /** 展示标题：AI 字段「标题」的结果，没有时取原文标题 */
  title: string
  /** 文章为 AI 摘要，视频为视频简介；没有时为 null */
  summary: string | null
  contentType: NewsContentType
  channelCode: string
  channelName: string
  feedId: number
  /** 订阅源已删除时为 null */
  feedName: string | null
  /** 订阅源已删除或没有头像时为 null */
  feedAvatarUrl: string | null
  /** yyyy-MM-dd HH:mm:ss */
  publishTime: string
  coverUrl: string | null
  /** 视频时长（秒），文章为 null */
  durationSeconds: number | null
  /** 原文链接 */
  url: string
  status: NewsItemStatus
  /** 先是普通标签字段，再是岗位分类；没有值的字段不出现 */
  tags: NewsItemTag[]
  /** 未生成的字段；不为空时显示结构化失败提示 */
  missingFields: NewsFieldRef[]
  rejectReasonName: string | null
  rejectNote: string | null
  reviewer: string | null
  reviewTime: string | null
}

/** 站内播放方式：与后端 Playback 对应 */
export interface NewsPlayback {
  kind: NewsPlaybackKind
  /** EMBED 为内嵌播放器地址，FILE 为视频文件地址 */
  url: string
  /** FILE 的视频封面；EMBED 为 null */
  coverUrl: string | null
}

/** 渠道返回的一项统计：与后端 StatItem 对应 */
export interface NewsStatItem {
  name: string
  value: number
}

/** 详情里的一个 AI 字段：与后端 NewsItemFieldVO 对应 */
export interface NewsItemField {
  fieldKey: string
  name: string
  kind: NewsAiFieldKind
  outputType: NewsAiFieldOutputType
  /** 文本是否多行 */
  multiline: boolean
  /** 单选、多选的选项，按配置顺序；其他类型为空数组 */
  options: NewsFieldOption[]
  required: boolean
  /** 是否允许人工修改 */
  editable: boolean
  showAsTag: boolean
  /** 未生成时为 null */
  value: NewsFieldValue
  /** 是否人工修改过 */
  edited: boolean
  /** 是否未生成 */
  missing: boolean
}

/** 处理记录的一行附加信息：与后端 NewsLogLineVO 对应 */
export interface NewsLogLine {
  label: string
  value: string
  /** 是否以错误色强调 */
  danger: boolean
}

/** 资讯处理记录的一条：与后端 NewsItemLogVO 对应 */
export interface NewsItemLog {
  id: number
  time: string
  action: NewsItemAction
  actionName: string
  /** 操作人登录名；系统动作为「系统」 */
  operator: string
  lines: NewsLogLine[]
}

/** 资讯详情，审核详情面板和资讯库整页详情共用：与后端 NewsItemDetailVO 对应 */
export interface NewsItemDetail {
  id: number
  title: string
  originTitle: string
  summary: string | null
  contentType: NewsContentType
  /** 类型名：文章 / 视频 */
  contentTypeName: string
  channelCode: string
  channelName: string
  feedId: number
  feedName: string | null
  feedAvatarUrl: string | null
  author: string | null
  authorAvatarUrl: string | null
  publishTime: string
  coverUrl: string | null
  durationSeconds: number | null
  url: string
  status: NewsItemStatus
  statusName: string
  /** 清洗后的正文 HTML；为 null 表示正文暂不可获取 */
  bodyHtml: string | null
  playback: NewsPlayback | null
  stats: NewsStatItem[]
  tags: NewsItemTag[]
  missingFields: NewsFieldRef[]
  /** 对这条资讯适用的启用 AI 字段，按展示顺序 */
  fields: NewsItemField[]
  rejectReasonName: string | null
  rejectNote: string | null
  reviewer: string | null
  reviewTime: string | null
  /** 最近的在前 */
  logs: NewsItemLog[]
}

/** 分页参数 */
export interface NewsPageParam {
  pageNo: number
  pageSize: number
}

/** 分页结果的分页信息，total 为筛选后的条数 */
export interface NewsPagination {
  pageNo: number
  pageSize: number
  total: number
}

/** 一个标签字段的筛选：字段值命中任一选中值即保留 */
export interface NewsTagFilter {
  fieldKey: string
  values: string[]
}

/** 审核队列查询条件，null 表示不限：与后端 NewsReviewPageRequest 对应 */
export interface NewsReviewQuery {
  status: NewsReviewStatus
  contentType: NewsContentType | null
  channelCode: string | null
  feedId: number | null
  keyword: string | null
  /** 发布日期区间 [起始日, 结束日]，yyyy-MM-dd，两端都包含 */
  publishPeriod: [string, string] | null
  tagFilters: NewsTagFilter[]
  sort: NewsReviewSort
}

/** 审核队列的一页：与后端 NewsReviewPageVO 对应 */
export interface NewsReviewPage {
  /** 待审总数，不受筛选影响 */
  pendingCount: number
  /** 已拒绝总数，不受筛选影响 */
  rejectedCount: number
  list: NewsItemCard[]
  pagination: NewsPagination
}

/** 筛选里的一个订阅源：与后端 NewsFeedOptionVO 对应 */
export interface NewsFeedOption {
  feedId: number
  feedName: string
  avatarUrl: string | null
  contentType: NewsContentType
}

/** 筛选里的一个渠道及其订阅源：与后端 NewsChannelOptionVO 对应 */
export interface NewsChannelOption {
  channelCode: string
  channelName: string
  feeds: NewsFeedOption[]
}

/** 可用于筛选的标签字段：与后端 NewsTagFieldVO 对应 */
export interface NewsTagField {
  fieldKey: string
  /** 筛选 chip 未选中时的文字 */
  name: string
  kind: NewsAiFieldKind
  outputType: NewsAiFieldOutputType
  options: NewsFieldOption[]
}

/** 审核队列筛选栏的可选项：与后端 NewsReviewFilterOptionsVO 对应 */
export interface NewsReviewFilterOptions {
  channels: NewsChannelOption[]
  /** 先是普通标签字段，再是岗位分类 */
  tagFields: NewsTagField[]
}

/** 拒绝弹窗的一个拒绝理由：与后端 NewsRejectReasonOptionVO 对应 */
export interface NewsRejectReasonOption {
  id: number
  name: string
}

/** 批量操作中失败的一条：与后端 NewsBatchFailureVO 对应 */
export interface NewsBatchFailure {
  id: number
  /** 资讯不存在时为 null */
  title: string | null
  reason: string
}

/** 批量操作结果：与后端 NewsBatchResultVO 对应 */
export interface NewsBatchResult {
  successCount: number
  /** 按请求顺序 */
  failures: NewsBatchFailure[]
}

/** 一个 AI 字段的新值：与后端 NewsFieldValueRequest 对应 */
export interface NewsFieldValueInput {
  fieldKey: string
  value: NewsFieldValue
}

/** 资讯库分类树里的订阅源：与后端 NewsLibraryFeedNodeVO 对应 */
export interface NewsLibraryFeedNode {
  feedId: number
  feedName: string
  avatarUrl: string | null
  count: number
}

/** 资讯库分类树里的渠道分组：与后端 NewsLibraryChannelNodeVO 对应 */
export interface NewsLibraryChannelNode {
  channelCode: string
  channelName: string
  /** 含已删除订阅源的资讯 */
  count: number
  /** 现存的订阅源 */
  feeds: NewsLibraryFeedNode[]
}

/** 资讯库分类树里的一个类型：与后端 NewsLibraryTypeNodeVO 对应 */
export interface NewsLibraryTypeNode {
  contentType: NewsContentType
  name: string
  count: number
  channels: NewsLibraryChannelNode[]
}

/** 资讯库分类树：与后端 NewsLibraryTreeVO 对应 */
export interface NewsLibraryTree {
  /** 文章、视频两类，固定顺序 */
  types: NewsLibraryTypeNode[]
}

/** 资讯库筛选栏的可选项：与后端 NewsLibraryFilterOptionsVO 对应 */
export interface NewsLibraryFilterOptions {
  tagFields: NewsTagField[]
}

/** 资讯库查询条件，null 表示不限：与后端 NewsLibraryPageRequest 对应 */
export interface NewsLibraryQuery {
  contentType: NewsContentType
  channelCode: string | null
  feedId: number | null
  keyword: string | null
  /** 发布日期区间 [起始日, 结束日]，yyyy-MM-dd，两端都包含 */
  publishPeriod: [string, string] | null
  tagFilters: NewsTagFilter[]
}

/** 资讯库的一页 */
export interface NewsLibraryPage {
  list: NewsItemCard[]
  pagination: NewsPagination
}

// ---------------- 资讯审核 ----------------

/** 审核队列分页，同时返回待审和已拒绝的总数 */
export const getReviewPageApi = (query: NewsReviewQuery, basePageInfo: NewsPageParam) =>
  newsRequestData(post<NewsReviewPage>(`${REVIEW_PREFIX}/page`, { query, basePageInfo }))

/** 审核筛选栏的渠道、订阅源和标签字段 */
export const getReviewFilterOptionsApi = () =>
  newsRequestData(get<NewsReviewFilterOptions>(`${REVIEW_PREFIX}/filter-options`, {}))

/** 拒绝弹窗可选的拒绝理由 */
export const getRejectReasonsApi = () =>
  newsRequestData(get<NewsRejectReasonOption[]>(`${REVIEW_PREFIX}/reject-reasons`, {}))

/** 待审数量，供导航角标 */
export const getPendingCountApi = () =>
  newsRequestData(get<number>(`${REVIEW_PREFIX}/pending-count`, {}))

/** 审核详情 */
export const getReviewDetailApi = (id: number) =>
  newsRequestData(get<NewsItemDetail>(`${REVIEW_PREFIX}/items/${id}`, {}))

/** 保存人工修改的字段，返回保存后的详情 */
export const saveReviewFieldsApi = (id: number, fields: NewsFieldValueInput[]) =>
  newsRequestData(post<NewsItemDetail>(`${REVIEW_PREFIX}/items/${id}/fields`, { fields }))

/** 通过一条待审资讯 */
export const approveItemApi = (id: number) =>
  newsRequestData(post<null>(`${REVIEW_PREFIX}/items/${id}/approve`, {}))

/** 拒绝一条待审资讯 */
export const rejectItemApi = (id: number, reasonId: number, note: string | null) =>
  newsRequestData(post<null>(`${REVIEW_PREFIX}/items/${id}/reject`, { reasonId, note }))

/** 重新加工，返回加工后的详情 */
export const reprocessItemApi = (id: number) =>
  newsRequestData(post<NewsItemDetail>(`${REVIEW_PREFIX}/items/${id}/reprocess`, {}))

/** 单独生成一个字段，返回生成后的详情 */
export const generateFieldApi = (id: number, fieldKey: string) =>
  newsRequestData(
    post<NewsItemDetail>(`${REVIEW_PREFIX}/items/${id}/fields/${encodeURIComponent(fieldKey)}/generate`, {})
  )

/** 批量通过 */
export const batchApproveApi = (ids: number[]) =>
  newsRequestData(post<NewsBatchResult>(`${REVIEW_PREFIX}/batch-approve`, { ids }))

/** 以同一个理由批量拒绝 */
export const batchRejectApi = (ids: number[], reasonId: number, note: string | null) =>
  newsRequestData(post<NewsBatchResult>(`${REVIEW_PREFIX}/batch-reject`, { ids, reasonId, note }))

/** 撤销刚做出的通过或拒绝 */
export const undoReviewApi = (ids: number[]) =>
  newsRequestData(post<NewsBatchResult>(`${REVIEW_PREFIX}/undo`, { ids }))

/** 把已拒绝的资讯恢复到待审 */
export const restoreItemsApi = (ids: number[]) =>
  newsRequestData(post<NewsBatchResult>(`${REVIEW_PREFIX}/restore`, { ids }))

// ---------------- 资讯库 ----------------

/** 分类树：类型 → 渠道 → 订阅源，每级带数量 */
export const getLibraryTreeApi = () =>
  newsRequestData(get<NewsLibraryTree>(`${LIBRARY_PREFIX}/tree`, {}))

/** 资讯库筛选栏的标签字段 */
export const getLibraryFilterOptionsApi = () =>
  newsRequestData(get<NewsLibraryFilterOptions>(`${LIBRARY_PREFIX}/filter-options`, {}))

/** 资讯库分页 */
export const getLibraryPageApi = (query: NewsLibraryQuery, basePageInfo: NewsPageParam) =>
  newsRequestData(post<NewsLibraryPage>(`${LIBRARY_PREFIX}/page`, { query, basePageInfo }))

/** 资讯库详情 */
export const getLibraryDetailApi = (id: number) =>
  newsRequestData(get<NewsItemDetail>(`${LIBRARY_PREFIX}/items/${id}`, {}))

/** 修改已通过资讯的标签字段，返回修改后的详情 */
export const updateLibraryTagsApi = (id: number, fields: NewsFieldValueInput[]) =>
  newsRequestData(post<NewsItemDetail>(`${LIBRARY_PREFIX}/items/${id}/tags`, { fields }))
