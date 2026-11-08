import { computed, inject, provide, reactive, ref, type InjectionKey } from 'vue'
import { uniqBy } from 'lodash-es'
import {
  approveItemApi,
  batchApproveApi,
  getReviewFilterOptionsApi,
  getReviewPageApi,
  restoreItemsApi,
  undoReviewApi,
  type NewsBatchFailure,
  type NewsBatchResult,
  type NewsContentType,
  type NewsItemCard,
  type NewsItemDetail,
  type NewsReviewFilterOptions,
  type NewsReviewQuery,
  type NewsReviewSort,
  type NewsReviewStatus
} from '@/api/newsItem'
import { useNewsAdminStore } from '@/stores/newsAdmin/newsAdminStore'
import type { NewsRejectOutcome } from '../components/NewsRejectDialog.vue'
import type { NewsDatePeriod } from '../components/NewsTimeFilterChip.vue'
import { useReviewDetail } from './useReviewDetail'

/** 每页条数，滚动到底部加载下一页 */
const PAGE_SIZE = 30
/** 卡片处理后向右滑出的时长 */
const LEAVE_MS = 220

const CONTENT_TYPES: NewsContentType[] = ['ARTICLE', 'VIDEO']
const SORTS: NewsReviewSort[] = ['NEWEST', 'OLDEST']

/** 筛选栏的取值；与筛选 chip 一致，选项类维度为选中值数组，空数组表示不限 */
export interface ReviewFilters {
  keyword: string
  contentType: string[]
  channelCode: string[]
  feedId: string[]
  /** 发布日期区间，两端都包含；null 表示全部时间 */
  publishPeriod: NewsDatePeriod | null
  /** 标签字段标识 → 选中值 */
  tags: Record<string, string[]>
  /** 只有一个值 */
  sort: string[]
}

/** 卡片显示密度 */
export type ReviewDensity = 'card' | 'compact'

/** 可撤销的一次通过 / 拒绝：移出队列的卡片及其原位置 */
interface UndoableAction {
  action: 'APPROVED' | 'REJECTED'
  removed: { item: NewsItemCard; index: number }[]
}

/** 底部提示 */
interface ReviewSnackbar {
  /** 每条提示一个新 key，重新计时 */
  key: number
  message: string
  undo: UndoableAction | null
}

const toOutcome = (ids: number[], result: NewsBatchResult): NewsRejectOutcome => {
  const failedIds = result.failures.map((failure) => failure.id)
  return { succeededIds: ids.filter((id) => !failedIds.includes(id)), failures: result.failures }
}

/**
 * 审核队列：分类与筛选、分页加载、多选、当前详情，以及通过 / 拒绝 / 恢复 / 撤销。
 * 通过、拒绝、恢复成功的卡片先向右滑出再离开队列，详情自动打开下一条。
 */
export function useReviewQueue() {
  const newsAdminStore = useNewsAdminStore()

  const status = ref<NewsReviewStatus>('PENDING')
  const filters = reactive<ReviewFilters>({
    keyword: '',
    contentType: [],
    channelCode: [],
    feedId: [],
    publishPeriod: null,
    tags: {},
    sort: ['NEWEST']
  })
  const filterOptions = ref<NewsReviewFilterOptions>({ channels: [], tagFields: [] })

  const items = ref<NewsItemCard[]>([])
  /** 当前分类在筛选后的条数 */
  const total = ref(0)
  const pendingCount = ref(0)
  const rejectedCount = ref(0)
  const isLoading = ref(false)
  const hasLoaded = ref(false)

  const checkedIds = ref<number[]>([])
  const selectedId = ref<number | null>(null)
  /** 正在滑出的卡片 */
  const leavingIds = ref<number[]>([])
  const density = ref<ReviewDensity>('card')

  const batchFailures = ref<NewsBatchFailure[]>([])
  const snackbar = ref<ReviewSnackbar | null>(null)
  let snackbarSeq = 0

  const rejectDialogVisible = ref(false)
  const rejectIds = ref<number[]>([])

  const hasMore = computed(() => items.value.length < total.value)
  const hasActiveFilters = computed(
    () =>
      filters.keyword.trim() !== '' ||
      filters.contentType.length > 0 ||
      filters.channelCode.length > 0 ||
      filters.feedId.length > 0 ||
      filters.publishPeriod !== null ||
      Object.values(filters.tags).some((values) => values.length > 0)
  )
  /** 全选框状态：只针对已加载的条目 */
  const checkState = computed<'none' | 'all' | 'mixed'>(() => {
    if (checkedIds.value.length === 0) return 'none'
    return checkedIds.value.length === items.value.length ? 'all' : 'mixed'
  })

  const showSnackbar = (message: string, undo: UndoableAction | null) => {
    snackbar.value = { key: ++snackbarSeq, message, undo }
  }

  const setCounts = (pending: number, rejected: number) => {
    pendingCount.value = pending
    rejectedCount.value = rejected
    newsAdminStore.pendingCount = pending
  }

  /** 详情刷新后同步卡片上的标题、摘要、标签和结构化失败提示 */
  const syncCard = (next: NewsItemDetail) => {
    items.value = items.value.map((item) =>
      item.id === next.id
        ? { ...item, title: next.title, summary: next.summary, tags: next.tags, missingFields: next.missingFields }
        : item
    )
  }

  const detail = useReviewDetail({ onRefreshed: syncCard, onNotify: (message) => showSnackbar(message, null) })

  const buildQuery = (): NewsReviewQuery => ({
    status: status.value,
    contentType: CONTENT_TYPES.find((type) => type === filters.contentType[0]) ?? null,
    channelCode: filters.channelCode[0] ?? null,
    feedId: filters.feedId.length > 0 ? Number(filters.feedId[0]) : null,
    keyword: filters.keyword.trim() === '' ? null : filters.keyword.trim(),
    publishPeriod: filters.publishPeriod,
    tagFilters: Object.entries(filters.tags)
      .filter(([, values]) => values.length > 0)
      .map(([fieldKey, values]) => ({ fieldKey, values })),
    sort: SORTS.find((sort) => sort === filters.sort[0])!
  })

  /** 只采用最后一次重新加载的结果 */
  let loadSeq = 0

  /**
   * 加载列表。reset 为 true 时从第一页重新加载；否则加载下一页，
   * 已离开队列的卡片会让后续条目前移，按已加载条数推算页码并按主键去重。
   */
  const loadItems = async (reset: boolean) => {
    if (!reset && (isLoading.value || !hasMore.value)) return
    const seq = ++loadSeq
    isLoading.value = true
    try {
      const pageNo = reset ? 1 : Math.floor(items.value.length / PAGE_SIZE) + 1
      const page = await getReviewPageApi(buildQuery(), { pageNo, pageSize: PAGE_SIZE })
      if (seq !== loadSeq) return
      items.value = reset ? page.list : uniqBy([...items.value, ...page.list], 'id')
      total.value = page.pagination.total
      setCounts(page.pendingCount, page.rejectedCount)
      hasLoaded.value = true
      if (reset && selectedId.value !== null && !items.value.some((item) => item.id === selectedId.value)) {
        selectedId.value = null
        detail.closeDetail()
      }
    } catch (e) {
      console.error(e)
    } finally {
      if (seq === loadSeq) isLoading.value = false
    }
  }

  const loadFilterOptions = async () => {
    try {
      filterOptions.value = await getReviewFilterOptionsApi()
    } catch (e) {
      console.error(e)
    }
  }

  /** 筛选变化：清空勾选，从第一页重新加载 */
  const onFiltersChanged = () => {
    checkedIds.value = []
    batchFailures.value = []
    void loadItems(true)
  }

  const clearFilters = () => {
    filters.keyword = ''
    filters.contentType = []
    filters.channelCode = []
    filters.feedId = []
    filters.publishPeriod = null
    filters.tags = {}
    onFiltersChanged()
  }

  /** 有未保存的修改时先确认放弃，再执行 */
  const runGuarded = async (action: () => void) => {
    if (await detail.confirmDiscard()) action()
  }

  const setStatus = (next: NewsReviewStatus) =>
    runGuarded(() => {
      status.value = next
      checkedIds.value = []
      batchFailures.value = []
      selectedId.value = null
      detail.closeDetail()
      void loadItems(true)
    })

  const openItem = (id: number) => {
    if (id === selectedId.value) return
    void runGuarded(() => {
      selectedId.value = id
      void detail.openDetail(id)
    })
  }

  const closeItem = () =>
    runGuarded(() => {
      selectedId.value = null
      detail.closeDetail()
    })

  /** J / K：上下切换并打开详情，没有当前条目时打开第一条 */
  const moveSelection = (step: 1 | -1) => {
    if (items.value.length === 0) return null
    const index = items.value.findIndex((item) => item.id === selectedId.value)
    const nextIndex = index < 0 ? 0 : Math.min(Math.max(index + step, 0), items.value.length - 1)
    const nextId = items.value[nextIndex].id
    openItem(nextId)
    return nextId
  }

  const toggleChecked = (id: number) => {
    checkedIds.value = checkedIds.value.includes(id)
      ? checkedIds.value.filter((item) => item !== id)
      : [...checkedIds.value, id]
  }

  const toggleAllChecked = () => {
    checkedIds.value = checkState.value === 'all' ? [] : items.value.map((item) => item.id)
  }

  const clearChecked = () => {
    checkedIds.value = []
  }

  /** 已在滑出的卡片不再重复处理 */
  const isLeaving = (ids: number[]) => ids.some((id) => leavingIds.value.includes(id))

  /** 移出队列后详情要打开的下一条：优先往后找，没有再往前找 */
  const findNextId = (removedIds: number[]) => {
    const index = items.value.findIndex((item) => item.id === selectedId.value)
    const after = items.value.slice(index + 1).find((item) => !removedIds.includes(item.id))
    const before = items.value
      .slice(0, Math.max(index, 0))
      .reverse()
      .find((item) => !removedIds.includes(item.id))
    return (after ?? before)?.id ?? null
  }

  /**
   * 卡片向右滑出后离开队列；当前详情被移出时自动打开下一条，没有下一条时关闭。
   * @returns 移出的卡片及其原位置
   */
  const removeItems = async (ids: number[]) => {
    leavingIds.value = ids
    await new Promise((resolve) => setTimeout(resolve, LEAVE_MS))
    const isSelectedRemoved = selectedId.value !== null && ids.includes(selectedId.value)
    const nextId = isSelectedRemoved ? findNextId(ids) : selectedId.value
    const removed = items.value.flatMap((item, index) => (ids.includes(item.id) ? [{ item, index }] : []))
    items.value = items.value.filter((item) => !ids.includes(item.id))
    checkedIds.value = checkedIds.value.filter((id) => !ids.includes(id))
    leavingIds.value = []
    total.value -= removed.length
    if (isSelectedRemoved) {
      selectedId.value = nextId
      if (nextId === null) detail.closeDetail()
      else void detail.openDetail(nextId)
    }
    return removed
  }

  /** 通过：通过当前详情里的条目时先校验必填，有未保存的修改先保存 */
  const approve = async (ids: number[]) => {
    if (isLeaving(ids)) return
    if (selectedId.value !== null && ids.includes(selectedId.value) && detail.detail.value) {
      if (!detail.validateRequired()) return
      if (!(await detail.saveDraft(false))) return
    }
    let outcome: NewsRejectOutcome
    try {
      if (ids.length === 1) {
        await approveItemApi(ids[0])
        outcome = { succeededIds: ids, failures: [] }
      } else {
        outcome = toOutcome(ids, await batchApproveApi(ids))
      }
    } catch (e) {
      console.error(e)
      return
    }
    batchFailures.value = outcome.failures
    if (outcome.succeededIds.length === 0) return
    setCounts(pendingCount.value - outcome.succeededIds.length, rejectedCount.value)
    const removed = await removeItems(outcome.succeededIds)
    showSnackbar(`已通过 ${removed.length} 条资讯，已进入资讯库`, { action: 'APPROVED', removed })
  }

  const openReject = (ids: number[]) => {
    if (isLeaving(ids)) return
    rejectIds.value = ids
    rejectDialogVisible.value = true
  }

  /** 拒绝弹窗提交后：成功的离开队列，失败的保留在队列里并保持勾选 */
  const onRejected = async (outcome: NewsRejectOutcome) => {
    batchFailures.value = outcome.failures
    if (outcome.succeededIds.length === 0) return
    const count = outcome.succeededIds.length
    setCounts(pendingCount.value - count, rejectedCount.value + count)
    const removed = await removeItems(outcome.succeededIds)
    showSnackbar(`已拒绝 ${removed.length} 条资讯`, { action: 'REJECTED', removed })
  }

  /** 已拒绝的资讯恢复到待审 */
  const restore = async (ids: number[]) => {
    if (isLeaving(ids)) return
    let outcome: NewsRejectOutcome
    try {
      outcome = toOutcome(ids, await restoreItemsApi(ids))
    } catch (e) {
      console.error(e)
      return
    }
    batchFailures.value = outcome.failures
    if (outcome.succeededIds.length === 0) return
    const count = outcome.succeededIds.length
    setCounts(pendingCount.value + count, rejectedCount.value - count)
    const removed = await removeItems(outcome.succeededIds)
    showSnackbar(`已恢复 ${removed.length} 条资讯到待审`, null)
  }

  /** 撤销刚做出的通过或拒绝：撤销成功的卡片回到原位置 */
  const undo = async () => {
    const undoable = snackbar.value?.undo
    if (!undoable) return
    snackbar.value = null
    const ids = undoable.removed.map(({ item }) => item.id)
    let outcome: NewsRejectOutcome
    try {
      outcome = toOutcome(ids, await undoReviewApi(ids))
    } catch (e) {
      console.error(e)
      return
    }
    batchFailures.value = outcome.failures
    const count = outcome.succeededIds.length
    if (count === 0) return
    setCounts(pendingCount.value + count, rejectedCount.value - (undoable.action === 'REJECTED' ? count : 0))
    if (status.value !== 'PENDING') {
      void loadItems(true)
      return
    }
    const next = [...items.value]
    undoable.removed
      .filter(({ item }) => outcome.succeededIds.includes(item.id))
      .sort((a, b) => a.index - b.index)
      .forEach(({ item, index }) => next.splice(Math.min(index, next.length), 0, item))
    items.value = next
    total.value += count
  }

  const closeSnackbar = () => {
    snackbar.value = null
  }

  return {
    status,
    filters,
    filterOptions,
    items,
    total,
    pendingCount,
    rejectedCount,
    isLoading,
    hasLoaded,
    hasMore,
    hasActiveFilters,
    checkedIds,
    checkState,
    selectedId,
    leavingIds,
    density,
    batchFailures,
    snackbar,
    rejectDialogVisible,
    rejectIds,
    detail,
    loadItems,
    loadFilterOptions,
    onFiltersChanged,
    clearFilters,
    setStatus,
    openItem,
    closeItem,
    moveSelection,
    toggleChecked,
    toggleAllChecked,
    clearChecked,
    approve,
    openReject,
    onRejected,
    restore,
    undo,
    closeSnackbar
  }
}

export type ReviewQueue = ReturnType<typeof useReviewQueue>

const REVIEW_QUEUE_KEY: InjectionKey<ReviewQueue> = Symbol('reviewQueue')

/** 在审核页根组件创建并向子组件提供审核队列 */
export const provideReviewQueue = () => {
  const queue = useReviewQueue()
  provide(REVIEW_QUEUE_KEY, queue)
  return queue
}

/** 审核页子组件取用审核队列 */
export const useReviewQueueContext = () => {
  const queue = inject(REVIEW_QUEUE_KEY)
  if (!queue) throw new Error('useReviewQueueContext 必须在审核页内使用')
  return queue
}
