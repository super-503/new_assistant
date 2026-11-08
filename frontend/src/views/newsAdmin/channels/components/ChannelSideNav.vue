<script setup lang="ts">
import { computed, ref } from 'vue'
import type { NewsChannel } from '@/api/newsChannel'
import NewsSourceAvatar from '../../components/NewsSourceAvatar.vue'

/** 左侧 220px 渠道栏：渠道搜索、「需处理」入口、渠道列表 */
const props = defineProps<{
  channels: NewsChannel[]
  /** 待处理事项数；为 0 且不在「需处理」视图时不显示入口 */
  todoTotal: number
  /** 当前选中的渠道标识；null 为「需处理」视图 */
  selectedCode: string | null
}>()

const emit = defineEmits<{
  /** 切换视图：渠道标识，或 null 表示「需处理」 */
  select: [code: string | null]
}>()

const keyword = ref('')

const showTodo = computed(() => props.todoTotal > 0 || props.selectedCode === null)

const visibleChannels = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return query ? props.channels.filter((channel) => channel.name.toLowerCase().includes(query)) : props.channels
})
</script>

<template>
  <nav class="channel-side" aria-label="渠道">
    <el-input v-model="keyword" class="news-search-input side-search" placeholder="搜索渠道" aria-label="搜索渠道">
      <template #prefix>
        <SvgIcon icon-class="newsAdmin-search" size="18" />
      </template>
    </el-input>

    <template v-if="showTodo">
      <button
        type="button"
        class="side-item"
        :class="{ 'is-active': props.selectedCode === null }"
        :aria-current="props.selectedCode === null ? 'page' : undefined"
        @click="emit('select', null)"
      >
        <SvgIcon class="side-icon" icon-class="newsAdmin-warning" size="20" />
        <span class="side-name">需处理</span>
        <span v-if="props.todoTotal > 0" class="todo-badge">{{ props.todoTotal }}</span>
      </button>

      <div class="side-divider" aria-hidden="true"></div>
    </template>

    <button
      v-for="channel in visibleChannels"
      :key="channel.code"
      type="button"
      class="side-item"
      :class="{ 'is-active': props.selectedCode === channel.code, 'is-disabled': !channel.enabled }"
      :aria-current="props.selectedCode === channel.code ? 'page' : undefined"
      @click="emit('select', channel.code)"
    >
      <NewsSourceAvatar :name="channel.name" />
      <span class="side-name">{{ channel.name }}</span>
      <span v-if="channel.needsAttention" class="attention-dot" role="img" aria-label="需要处理"></span>
      <span class="side-count">{{ channel.feedCount }}</span>
    </button>
  </nav>
</template>

<style scoped lang="scss">
@use '../../components/newsMixins' as *;

.channel-side {
  flex: 0 0 220px;
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-2xs);
  padding: var(--zg-space-lg) var(--zg-space-md);
  border-right: 1px solid var(--surface-border);
  overflow-y: auto;
}

.side-search {
  flex: none;
  margin-bottom: var(--zg-space-md);
}

.side-item {
  @include reset-button;
  @include state-layer;
  @include focus-ring(-2px);
  flex: none;
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  height: 40px;
  padding: 0 var(--zg-space-lg);
  border-radius: var(--zg-radius-pill);
  color: var(--main-color);
  font-size: var(--zg-text-base);
  font-weight: var(--zg-weight-medium);
  transition: background var(--zg-duration-base) var(--zg-ease-m3-standard);

  &.is-active {
    background: var(--selected-bg);
    color: var(--selected-text);
    font-weight: var(--zg-weight-semibold);
  }

  &.is-disabled:not(.is-active) {
    color: var(--second-color);
    opacity: 0.6;
  }

  :deep(.svg-icon) {
    cursor: inherit;
  }
}

.side-icon {
  flex: none;
}

.side-name {
  flex: 0 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attention-dot {
  flex: none;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--status-rejected);
}

.side-count {
  flex: none;
  margin-left: auto;
  font-size: var(--zg-text-sm);
  font-variant-numeric: tabular-nums;
}

.todo-badge {
  flex: none;
  min-width: 20px;
  height: 20px;
  margin-left: auto;
  padding: 0 var(--zg-space-sm);
  border-radius: 10px;
  background: var(--status-rejected-container);
  color: var(--on-status-rejected-container);
  font-size: var(--zg-text-sm);
  line-height: 20px;
  font-weight: var(--zg-weight-medium);
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.side-divider {
  flex: none;
  height: 1px;
  margin: var(--zg-space-md) var(--zg-space-lg);
  background: var(--surface-border);
}
</style>
