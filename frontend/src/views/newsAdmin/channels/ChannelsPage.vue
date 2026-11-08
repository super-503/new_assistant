<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getChannelTodoApi,
  listChannelsApi,
  type NewsChannel,
  type NewsChannelTodo,
  type NewsFeed,
  type NewsTodoFeed
} from '@/api/newsChannel'
import ChannelSettingsDialog from './components/ChannelSettingsDialog.vue'
import ChannelSideNav from './components/ChannelSideNav.vue'
import ChannelView from './components/ChannelView.vue'
import FeedDialog from './components/FeedDialog.vue'
import RunsDrawer from './components/RunsDrawer.vue'
import TodoView from './components/TodoView.vue'

/**
 * 渠道配置：左侧 220px 渠道栏 + 右侧当前渠道视图或「需处理」视图。
 * 支持 ?channel=渠道标识&settings=1：选中该渠道并打开渠道设置（飞书凭证失效通知里的链接）。
 */
const route = useRoute()
const router = useRouter()

const channels = ref<NewsChannel[]>([])
const todo = ref<NewsChannelTodo | null>(null)
/** 当前选中的渠道标识；null 为「需处理」视图 */
const selectedCode = ref<string | null>(null)

const channelViewRef = ref<InstanceType<typeof ChannelView>>()
const feedDialogRef = ref<InstanceType<typeof FeedDialog>>()

const settingsVisible = ref(false)
const settingsCode = ref('')
const runsVisible = ref(false)
const runsCode = ref('')
/** 从订阅源打开运行记录时的订阅源 id */
const runsFeedId = ref<number | null>(null)

const findChannel = (code: string) => channels.value.find((channel) => channel.code === code)
const selectedChannel = computed(() => (selectedCode.value === null ? undefined : findChannel(selectedCode.value)))
const settingsChannel = computed(() => findChannel(settingsCode.value))
const runsChannel = computed(() => findChannel(runsCode.value))

/** 渠道列表（数量、状态点）与「需处理」汇总 */
const loadOverview = async () => {
  const [channelList, todoSummary] = await Promise.all([listChannelsApi(), getChannelTodoApi()])
  channels.value = channelList
  todo.value = todoSummary
}

const openSettings = (code: string) => {
  settingsCode.value = code
  settingsVisible.value = true
}

const openRuns = (feed: NewsFeed | null) => {
  if (selectedCode.value === null) return
  runsCode.value = selectedCode.value
  runsFeedId.value = feed?.id ?? null
  runsVisible.value = true
}

const openAdd = () => {
  const code = selectedCode.value ?? channels.value[0]?.code
  if (code) feedDialogRef.value?.openAdd(code)
}

const openEdit = (feed: NewsFeed) => {
  if (selectedCode.value !== null) feedDialogRef.value?.openEdit(selectedCode.value, feed)
}

const openTodoEdit = (item: NewsTodoFeed) => feedDialogRef.value?.openEdit(item.channelCode, item.feed)

/** 订阅源或渠道设置有变化：刷新概览和当前渠道的订阅源 */
const refreshAll = () => {
  void loadOverview()
  void channelViewRef.value?.reload()
}

/** 应用链接参数后从地址栏移除，避免刷新页面时再次弹出渠道设置 */
const applyRouteQuery = () => {
  const { channel, settings } = route.query
  if (typeof channel !== 'string' || !findChannel(channel)) return
  selectedCode.value = channel
  if (settings === '1') openSettings(channel)
  void router.replace({ query: {} })
}

onMounted(async () => {
  await loadOverview()
  selectedCode.value = channels.value[0]?.code ?? null
  applyRouteQuery()
})

watch(() => route.query, applyRouteQuery)
</script>

<template>
  <div class="channels-page">
    <ChannelSideNav
      :channels="channels"
      :todo-total="todo?.total ?? 0"
      :selected-code="selectedCode"
      @select="selectedCode = $event"
    />
    <main class="channels-main">
      <div class="channels-inner">
        <ChannelView
          v-if="selectedChannel"
          ref="channelViewRef"
          :channel="selectedChannel"
          @open-settings="openSettings(selectedChannel.code)"
          @open-runs="openRuns"
          @add="openAdd"
          @edit="openEdit"
          @changed="loadOverview"
        />
        <TodoView
          v-else-if="selectedCode === null && todo"
          :todo="todo"
          @select="selectedCode = $event"
          @renew="openSettings"
          @edit="openTodoEdit"
          @add="openAdd"
          @changed="loadOverview"
        />
      </div>
    </main>

    <FeedDialog ref="feedDialogRef" :channels="channels" @saved="refreshAll" />
    <ChannelSettingsDialog
      v-if="settingsChannel"
      v-model="settingsVisible"
      :channel="settingsChannel"
      @changed="refreshAll"
    />
    <RunsDrawer v-if="runsChannel" v-model="runsVisible" :channel="runsChannel" :focus-feed-id="runsFeedId" />
  </div>
</template>

<style scoped lang="scss">
.channels-page {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  overflow: hidden;
}

.channels-main {
  position: relative;
  flex: 1 1 auto;
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-content: start;
  overflow: auto;
}

.channels-inner {
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-xl);
  padding: 20px var(--zg-space-2xl) 48px;
}
</style>
