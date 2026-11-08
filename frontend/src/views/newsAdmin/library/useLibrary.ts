import { computed, inject, provide, reactive, ref, type InjectionKey } from 'vue'
import { uniqBy } from 'lodash-es'
import {
  getLibraryDetailApi,
  getLibraryFilterOptionsApi,
  getLibraryPageApi,
  getLibraryTreeApi,
  type NewsContentType,
  type NewsItemCard,
  type NewsItemDetail,
  type NewsLibraryQuery,
  type NewsLibraryTree,
  type NewsTagField
} from '@/api/newsItem'
import type { NewsDatePeriod } from '../components/NewsTimeFilterChip.vue'

/** 每页条数，滚动到底部加载下一页 */
const PAGE_SIZE = 30

/** 分类树里选中的节点：全部、整个渠道、单个订阅源 */
export type LibraryNode =
  | { kind: 'all' }
  | { kind: 'channel'; channelCode: string }
  | { kind: 'feed'; channelCode: string; feedId: number }

/** 筛选栏的取值；与筛选 chip 一致，空数组表示不限 */
export interface LibraryFilters {
  keyword: string
  /** 发布日期区间，两端都包含；null 表示全部时间 */
  publishPeriod: NewsDatePeriod | null
  /** 标签字段标识 → 选中值 */
  tags: Record<string, string[]>
}

/**
 * 资讯库：分类树（类型 → 渠道 → 订阅源）、筛选、分页列表与整页详情。
 * 详情覆盖在列表之上，列表保持挂载，关闭后滚动位置和选中项不变；在分类树里切换节点时自动关闭详情。
 */
export function useLibrary() {
  const tree = ref<NewsLibraryTree | null>(null)
  const tagFields = ref<NewsTagField[]>([])
  const contentType = ref<NewsContentType>('ARTICLE')
  const node = ref<LibraryNode>({ kind: 'all' })
  const filters = reactive<LibraryFilters>({ keyword: '', publishPeriod: null, tags: {} })

  const items = ref<NewsItemCard[]>([])
  const total = ref(0)
  const isLoading = ref(false)
  const hasLoaded = ref(false)

  const selectedId = ref<number | null>(null)
  const isDetailOpen = ref(false)
  const detail = ref<NewsItemDetail | null>(null)
  const isDetailLoading = ref(false)
  let detailSeq = 0

  const typeNode = computed(() => tree.value?.types.find((type) => type.contentType === contentType.value) ?? null)
  const channelNode = computed(() => {
    const current = node.value
    if (current.kind === 'all') return null
    return typeNode.value?.channels.find((channel) => channel.channelCode === current.channelCode) ?? null
  })
  const feedNode = computed(() => {
    const current = node.value
    return current.kind === 'feed' ? (channelNode.value?.feeds.find((feed) => feed.feedId === current.feedId) ?? null) : null
  })
  /** 当前节点在分类树里的条数（不受筛选影响） */
  const nodeCount = computed(() => {
    if (node.value.kind === 'feed') return feedNode.value?.count ?? 0
    if (node.value.kind === 'channel') return channelNode.value?.count ?? 0
    return typeNode.value?.count ?? 0
  })
  const hasMore = computed(() => items.value.length < total.value)
  const hasActiveFilters = computed(
    () =>
      filters.keyword.trim() !== '' ||
      filters.publishPeriod !== null ||
      Object.values(filters.tags).some((values) => values.length > 0)
  )

  const buildQuery = (): NewsLibraryQuery => {
    const current = node.value
    return {
      contentType: contentType.value,
      channelCode: current.kind === 'all' ? null : current.channelCode,
      feedId: current.kind === 'feed' ? current.feedId : null,
      keyword: filters.keyword.trim() === '' ? null : filters.keyword.trim(),
      publishPeriod: filters.publishPeriod,
      tagFilters: Object.entries(filters.tags)
        .filter(([, values]) => values.length > 0)
        .map(([fieldKey, values]) => ({ fieldKey, values }))
    }
  }

  /** 只采用最后一次重新加载的结果 */
  let loadSeq = 0

  /** 加载列表：reset 为 true 时从第一页重新加载，否则加载下一页 */
  const loadItems = async (reset: boolean) => {
    if (!reset && (isLoading.value || !hasMore.value)) return
    const seq = ++loadSeq
    isLoading.value = true
    try {
      const pageNo = reset ? 1 : Math.floor(items.value.length / PAGE_SIZE) + 1
      const page = await getLibraryPageApi(buildQuery(), { pageNo, pageSize: PAGE_SIZE })
      if (seq !== loadSeq) return
      items.value = reset ? page.list : uniqBy([...items.value, ...page.list], 'id')
      total.value = page.pagination.total
      hasLoaded.value = true
    } catch (e) {
      console.error(e)
    } finally {
      if (seq === loadSeq) isLoading.value = false
    }
  }

  const loadTree = async () => {
    try {
      tree.value = await getLibraryTreeApi()
    } catch (e) {
      console.error(e)
    }
  }

  const loadFilterOptions = async () => {
    try {
      tagFields.value = (await getLibraryFilterOptionsApi()).tagFields
    } catch (e) {
      console.error(e)
    }
  }

  const closeDetail = () => {
    detailSeq++
    isDetailOpen.value = false
    isDetailLoading.value = false
  }

  /** 切换节点：关闭详情，从第一页重新加载 */
  const selectNode = (next: LibraryNode) => {
    node.value = next
    closeDetail()
    void loadItems(true)
  }

  /** 切换类型：回到「全部」并清空筛选 */
  const setContentType = (next: NewsContentType) => {
    if (next === contentType.value) return
    contentType.value = next
    filters.keyword = ''
    filters.tags = {}
    selectedId.value = null
    selectNode({ kind: 'all' })
  }

  const onFiltersChanged = () => {
    void loadItems(true)
  }

  const clearFilters = () => {
    filters.keyword = ''
    filters.publishPeriod = null
    filters.tags = {}
    onFiltersChanged()
  }

  const openItem = async (id: number) => {
    const seq = ++detailSeq
    selectedId.value = id
    isDetailOpen.value = true
    detail.value = null
    isDetailLoading.value = true
    try {
      const next = await getLibraryDetailApi(id)
      if (seq === detailSeq) detail.value = next
    } catch (e) {
      console.error(e)
    } finally {
      if (seq === detailSeq) isDetailLoading.value = false
    }
  }

  /** 详情里修改标签后同步列表卡片上的标签 */
  const onDetailUpdated = (next: NewsItemDetail) => {
    detail.value = next
    items.value = items.value.map((item) => (item.id === next.id ? { ...item, tags: next.tags } : item))
  }

  return {
    tree,
    tagFields,
    contentType,
    node,
    filters,
    items,
    total,
    isLoading,
    hasLoaded,
    hasMore,
    hasActiveFilters,
    typeNode,
    channelNode,
    feedNode,
    nodeCount,
    selectedId,
    isDetailOpen,
    detail,
    isDetailLoading,
    loadItems,
    loadTree,
    loadFilterOptions,
    selectNode,
    setContentType,
    onFiltersChanged,
    clearFilters,
    openItem,
    closeDetail,
    onDetailUpdated
  }
}

export type LibraryState = ReturnType<typeof useLibrary>

const LIBRARY_KEY: InjectionKey<LibraryState> = Symbol('library')

/** 在资讯库根组件创建并向子组件提供资讯库状态 */
export const provideLibrary = () => {
  const library = useLibrary()
  provide(LIBRARY_KEY, library)
  return library
}

/** 资讯库子组件取用资讯库状态 */
export const useLibraryContext = () => {
  const library = inject(LIBRARY_KEY)
  if (!library) throw new Error('useLibraryContext 必须在资讯库页内使用')
  return library
}
