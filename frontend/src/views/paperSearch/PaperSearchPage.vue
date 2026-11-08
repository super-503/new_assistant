<script setup lang="ts">
import { computed, ref } from 'vue'
import { searchPapersApi, type PaperHit, type PaperQuery, type PaperSource } from '@/api/papers'

const SOURCES: { value: PaperSource; label: string }[] = [
  { value: 'CNKI', label: '中国知网' },
  { value: 'ARXIV', label: 'arXiv' },
  { value: 'CROSSREF', label: 'Crossref' },
  { value: 'EUROPE_PMC', label: 'Europe PMC' }
]

const keyword = ref('')
const source = ref<PaperSource>('CNKI')
const activeQuery = ref<PaperQuery>()
const activeSourceLabel = computed(() => SOURCES.find((entry) => entry.value === activeQuery.value?.source)?.label)
const items = ref<PaperHit[]>([])
const nextCursor = ref<string | null>(null)
const loading = ref(false)
const error = ref('')

const cnkiSearchUrl = computed(() => {
  if (activeQuery.value?.source !== 'CNKI') return null
  const url = new URL('https://search.cnki.com.cn/Search/Result')
  url.searchParams.set('content', activeQuery.value.keyword)
  return url.toString()
})

async function load(query: PaperQuery, cursor?: string) {
  loading.value = true
  error.value = ''
  try {
    const page = await searchPapersApi(query, cursor)
    items.value = [...items.value, ...page.items]
    nextCursor.value = page.nextCursor
  } catch (e) {
    error.value = e instanceof Error ? e.message : '论文检索失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function search() {
  const term = keyword.value.trim()
  if (!term || loading.value) return
  const query: PaperQuery = { keyword: term, source: source.value }
  activeQuery.value = query
  items.value = []
  nextCursor.value = null
  void load(query)
}

function loadMore() {
  if (!activeQuery.value || !nextCursor.value || loading.value) return
  void load(activeQuery.value, nextCursor.value)
}
</script>

<template>
  <section class="paper-search-page" aria-labelledby="paper-search-title" :aria-busy="loading">
    <div class="paper-content">
      <header class="paper-header">
        <h2 id="paper-search-title">论文搜索</h2>
        <p>按关键词检索学术论文，查看题录并打开来源页面。</p>
      </header>

      <form class="paper-form" @submit.prevent="search">
        <el-select
          v-model="source"
          aria-label="论文来源"
          popper-class="news-admin-popper"
          :disabled="loading"
        >
          <el-option v-for="entry in SOURCES" :key="entry.value" :label="entry.label" :value="entry.value" />
        </el-select>
        <el-input
          v-model="keyword"
          aria-label="论文关键词"
          placeholder="输入论文题名或关键词"
          :maxlength="100"
          :disabled="loading"
          clearable
        />
        <el-button type="primary" native-type="submit" :loading="loading" :disabled="!keyword.trim()">
          搜索
        </el-button>
      </form>

      <div v-if="activeQuery" class="paper-results-header">
        <h3>{{ activeSourceLabel }} · {{ activeQuery.keyword }}</h3>
        <a v-if="cnkiSearchUrl" :href="cnkiSearchUrl" target="_blank" rel="noopener noreferrer">在知网检索</a>
      </div>
      <p v-if="error" class="paper-error" role="alert">{{ error }}</p>
      <p v-else-if="loading && items.length === 0" class="paper-status" role="status">正在检索论文…</p>
      <p v-else-if="activeQuery && items.length === 0 && !nextCursor" class="paper-status" role="status">
        未找到匹配的论文。
      </p>

      <ol v-if="items.length" class="paper-results" aria-label="论文搜索结果">
        <li v-for="(paper, index) in items" :key="`${paper.url}-${index}`" class="paper-card">
          <a :href="paper.url" target="_blank" rel="noopener noreferrer" class="paper-title">{{ paper.title }}</a>
          <p v-if="paper.authors" class="paper-meta">{{ paper.authors }}</p>
          <p v-if="paper.publication || paper.publicationDate" class="paper-meta">
            {{ [paper.publication, paper.publicationDate].filter(Boolean).join(' · ') }}
          </p>
          <p v-if="paper.doi" class="paper-meta">DOI {{ paper.doi }}</p>
          <details v-if="paper.summary">
            <summary>摘要</summary>
            <p class="paper-summary">{{ paper.summary }}</p>
          </details>
        </li>
      </ol>

      <el-button v-if="nextCursor" :loading="loading" @click="loadMore">加载更多</el-button>
    </div>
  </section>
</template>

<style scoped lang="scss">
.paper-search-page {
  flex: 1;
  min-width: 0;
  overflow: auto;
  padding: var(--zg-space-3xl) var(--zg-space-2xl);
}

.paper-content {
  display: grid;
  gap: var(--zg-space-xl);
  max-width: 960px;
  margin: 0 auto;
}

.paper-header h2,
.paper-results-header h3,
.paper-header p,
.paper-meta,
.paper-summary,
.paper-status,
.paper-error {
  margin: 0;
}

.paper-header h2 {
  font-size: var(--zg-text-3xl);
  line-height: var(--zg-leading-tight);
}

.paper-header p {
  margin-top: var(--zg-space-sm);
}

.paper-header p,
.paper-meta,
.paper-status {
  color: var(--second-color);
}

.paper-form {
  display: grid;
  grid-template-columns: minmax(130px, 180px) minmax(0, 1fr) auto;
  gap: var(--zg-space-lg);
}

.paper-results-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--zg-space-md);
}

.paper-results-header h3 {
  font-size: var(--zg-text-base);
  overflow-wrap: anywhere;
}

.paper-error {
  color: var(--control-error-border);
}

.paper-results {
  display: grid;
  gap: var(--zg-space-md);
  margin: 0;
  padding: 0;
  list-style: none;
}

.paper-card {
  display: grid;
  gap: var(--zg-space-xs);
  padding: var(--zg-space-xl);
  border: 1px solid var(--surface-border);
  border-radius: var(--zg-radius-md);
  background: var(--surface-container);
  overflow-wrap: anywhere;
}

.paper-title,
.paper-results-header a {
  color: var(--brand-indigo);
}

.paper-title {
  justify-self: start;
  font-size: var(--zg-text-lg);
  font-weight: var(--zg-weight-semibold);
  line-height: var(--zg-leading-normal);
}

.paper-card summary {
  width: fit-content;
  cursor: pointer;
  color: var(--second-color);
}

.paper-summary {
  padding-top: var(--zg-space-xs);
  line-height: var(--zg-leading-relaxed);
}

.paper-content > .el-button {
  justify-self: center;
}

@media (max-width: 620px) {
  .paper-form {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .paper-form > .el-select {
    grid-column: 1 / -1;
  }
}
</style>
