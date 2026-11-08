<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { NewsApiError } from '@/api/newsAdminRequest'
import { searchSourcesApi, type NewsChannel, type NewsSourceCandidate } from '@/api/newsChannel'
import { formatStats } from '../channelFormat'
import SourceSkeleton from './SourceSkeleton.vue'

/**
 * 添加订阅源的来源选择：输入关键词搜索，结果按统一结构展示，点击选中。
 * 不支持搜索的渠道不显示搜索框，打开即展示唯一的来源并自动选中。
 * 由父组件按渠道 key 重建，换渠道即重置。
 */
const props = defineProps<{
  channel: NewsChannel
  /** 当前选中来源的 sourceKey；未选中为 null */
  selectedKey: string | null
}>()

const emit = defineEmits<{
  pick: [candidate: NewsSourceCandidate]
  /** 发起新的搜索，原选中项作废 */
  search: []
}>()

/** 搜索关键词，父组件据此判断弹窗是否有未保存的输入 */
const keyword = defineModel<string>('keyword', { required: true })

/** 搜索框占位提示：支持搜索的渠道必须提供，缺失即后端契约错误，直接报错 */
const searchHint = computed(() => {
  if (props.channel.searchHint === null) throw new Error(`渠道「${props.channel.name}」支持搜索但缺少 searchHint`)
  return props.channel.searchHint
})

const isSearching = ref(false)
/** 搜索结果；还没搜索过为 null */
const results = ref<NewsSourceCandidate[] | null>(null)
/** 搜索失败的原因 */
const searchError = ref('')
/** 关键词为空时的就地提示 */
const keywordError = ref('')

const runSearch = async () => {
  const keywordText = keyword.value.trim()
  if (props.channel.searchable && keywordText === '') {
    keywordError.value = '请输入搜索关键词'
    return
  }
  keywordError.value = ''
  searchError.value = ''
  results.value = null
  isSearching.value = true
  emit('search')
  try {
    results.value = await searchSourcesApi(props.channel.code, props.channel.searchable ? keywordText : undefined)
  } catch (error) {
    if (!(error instanceof NewsApiError)) throw error
    searchError.value = error.message
    return
  } finally {
    isSearching.value = false
  }
  if (!props.channel.searchable && results.value.length === 1) emit('pick', results.value[0])
}

onMounted(() => {
  if (!props.channel.searchable) void runSearch()
})
</script>

<template>
  <div class="source-picker">
    <div v-if="props.channel.searchable" class="search-field">
      <label class="field-label" for="feed-source-keyword">
        来源<span class="required-mark" aria-hidden="true"> *</span>
      </label>
      <div class="search-row">
        <el-input
          id="feed-source-keyword"
          v-model="keyword"
          class="search-input"
          :class="{ 'is-invalid': keywordError || searchError }"
          :placeholder="searchHint"
          @keyup.enter="runSearch"
        />
        <el-button :loading="isSearching" @click="runSearch">{{ isSearching ? '搜索中' : '搜索' }}</el-button>
      </div>
      <span v-if="keywordError" class="field-error" role="alert">{{ keywordError }}</span>
    </div>

    <span v-if="searchError" class="field-error" role="alert">
      {{ searchError }}
      <button type="button" class="inline-retry" @click="runSearch">重试</button>
    </span>

    <SourceSkeleton v-if="isSearching" :rows="1" />

    <p v-else-if="results && results.length === 0" class="not-found">未找到</p>

    <ul v-else-if="results" class="result-list" role="listbox" aria-label="搜索结果">
      <li
        v-for="candidate in results"
        :key="candidate.sourceKey"
        role="option"
        class="result-item"
        :class="{ 'is-selected': candidate.sourceKey === props.selectedKey }"
        :aria-selected="candidate.sourceKey === props.selectedKey"
        tabindex="0"
        @click="emit('pick', candidate)"
        @keydown.enter.prevent="emit('pick', candidate)"
        @keydown.space.prevent="emit('pick', candidate)"
      >
        <span class="result-avatar" aria-hidden="true">
          <img
            v-if="candidate.avatarUrl"
            :src="candidate.avatarUrl"
            alt=""
            loading="lazy"
            referrerpolicy="no-referrer"
          />
          <template v-else>{{ Array.from(candidate.name)[0] }}</template>
        </span>
        <span class="result-text">
          <span class="result-title">
            <span class="result-name">{{ candidate.name }}</span>
            <span v-if="candidate.handle" class="result-handle">{{ candidate.handle }}</span>
          </span>
          <span v-if="candidate.description" class="result-desc">{{ candidate.description }}</span>
          <span v-if="candidate.stats.length > 0" class="result-stats">{{ formatStats(candidate.stats) }}</span>
        </span>
        <SvgIcon
          v-if="candidate.sourceKey === props.selectedKey"
          class="result-check"
          icon-class="newsAdmin-check"
          size="20"
        />
      </li>
    </ul>
  </div>
</template>

<style scoped lang="scss">
@use '../../components/newsMixins' as *;

.source-picker {
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-lg);
}

.search-field {
  display: grid;
  gap: var(--zg-space-sm);
}

.field-label {
  color: var(--second-color);
  font-size: var(--zg-text-md);
  line-height: 20px;
  font-weight: var(--zg-weight-medium);
}

.required-mark {
  color: var(--status-rejected);
}

.search-row {
  display: flex;
  align-items: center;
  gap: var(--zg-space-md);

  .el-button {
    flex: none;
    height: 40px;
  }
}

.search-input {
  flex: 1;
  min-width: 0;

  &.is-invalid :deep(.el-input__wrapper) {
    box-shadow: 0 0 0 1px var(--control-error-border) inset;
  }
}

.field-error {
  display: flex;
  align-items: center;
  gap: var(--zg-space-xs);
  color: var(--status-rejected);
  font-size: var(--zg-text-sm);
  line-height: 16px;
}

.inline-retry {
  @include text-button;
  height: 28px;
  padding: 0 var(--zg-space-md);
  font-size: var(--zg-text-sm);
}

.not-found {
  margin: 0;
  padding: var(--zg-space-xl) 0;
  color: var(--second-color);
  text-align: center;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-2xs);
  max-height: 264px;
  margin: 0;
  padding: 0;
  overflow-y: auto;
  list-style: none;
  animation: picker-enter 300ms var(--zg-ease-m3-emphasized-decelerate) both;
}

.result-item {
  @include state-layer;
  @include focus-ring(-2px);
  display: flex;
  align-items: center;
  gap: var(--zg-space-lg);
  min-height: 64px;
  padding: var(--zg-space-md) var(--zg-space-lg);
  border-radius: var(--zg-radius-lg);
  cursor: pointer;
  transition: background var(--zg-duration-base) var(--zg-ease-m3-standard);

  &.is-selected {
    background: var(--selected-bg);
    color: var(--selected-text);
  }
}

.result-avatar {
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  overflow: hidden;
  border-radius: 50%;
  background: var(--surface-container-highest);
  color: var(--second-color);
  font-size: var(--zg-text-lg);
  font-weight: var(--zg-weight-medium);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.result-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-2xs);
}

.result-title {
  display: flex;
  align-items: baseline;
  gap: var(--zg-space-md);
  min-width: 0;
}

.result-name {
  min-width: 0;
  overflow: hidden;
  font-weight: var(--zg-weight-medium);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-handle,
.result-desc,
.result-stats {
  color: var(--second-color);
  font-size: var(--zg-text-sm);
  line-height: 16px;
}

.is-selected .result-handle,
.is-selected .result-desc,
.is-selected .result-stats {
  color: inherit;
}

.result-handle {
  flex: none;
}

.result-desc {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-stats {
  font-variant-numeric: tabular-nums;
}

.result-check {
  flex: none;
  animation: picker-pop var(--zg-duration-base) var(--zg-ease-m3-standard);
}

@keyframes picker-enter {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

@keyframes picker-pop {
  from {
    opacity: 0;
    transform: scale(0.4);
  }
  to {
    opacity: 1;
    transform: none;
  }
}
</style>
