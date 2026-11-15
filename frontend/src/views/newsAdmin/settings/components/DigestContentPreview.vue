<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { NewsApiError } from '@/api/newsAdminRequest'
import { previewDigestContentApi, type NewsDigestPreviewItem } from '@/api/newsPushPlan'
import type { NewsDigestContentSettings } from '@/api/newsSetting'

const props = defineProps<{
  content: NewsDigestContentSettings
  cardTitle: string
  usesTemplate: boolean
}>()
const items = ref<NewsDigestPreviewItem[]>([])
const loading = ref(false)
const errorMessage = ref('')
let requestVersion = 0
let timer: ReturnType<typeof setTimeout> | undefined

const load = async () => {
  clearTimeout(timer)
  const version = ++requestVersion
  loading.value = true
  errorMessage.value = ''
  try {
    const preview = await previewDigestContentApi(props.content)
    if (version === requestVersion) items.value = preview
  } catch (error) {
    if (!(error instanceof NewsApiError)) throw error
    if (version === requestVersion) errorMessage.value = error.message
  } finally {
    if (version === requestVersion) loading.value = false
  }
}

watch(() => JSON.stringify(props.content), () => {
  ++requestVersion
  loading.value = true
  errorMessage.value = ''
  clearTimeout(timer)
  timer = setTimeout(load, 250)
}, { immediate: true })

onBeforeUnmount(() => {
  ++requestVersion
  clearTimeout(timer)
})
</script>

<template>
  <section class="digest-preview" aria-labelledby="digest-preview-title" :aria-busy="loading">
    <header class="preview-heading">
      <h2 id="digest-preview-title">内容布局预览</h2>
      <span v-if="loading" role="status">正在更新</span>
      <span v-else-if="items.length && !errorMessage">{{ items.length }} 条已审核资讯</span>
    </header>
    <el-alert
      v-if="errorMessage"
      title="内容预览加载失败"
      type="error"
      show-icon
      :closable="false"
    >
      <span>{{ errorMessage }}</span>
      <el-button text type="primary" @click="load">重新加载</el-button>
    </el-alert>
    <p v-else-if="!items.length" class="preview-empty" role="status">
      {{ loading ? '正在读取当前范围内的资讯' : '当前范围内暂无已审核通过的资讯' }}
    </p>
    <template v-else>
      <p v-if="usesTemplate" class="template-note">以下展示传入卡片的资讯内容，模板样式以飞书客户端为准。</p>
      <div class="preview-content" :class="{ 'is-loading': loading }">
        <h3>{{ cardTitle }}</h3>
        <article v-for="(item, index) in items" :key="index" class="preview-item">
          <h4>{{ item.title }}</h4>
          <p v-if="item.fields.length" class="preview-fields" :class="{ compact: content.compact }">
            <span v-for="field in item.fields" :key="field.key">{{ field.label }}：{{ field.display }}</span>
          </p>
          <el-link v-if="item.url" :href="item.url" target="_blank" rel="noopener noreferrer" :underline="false">
            查看原文
          </el-link>
        </article>
      </div>
    </template>
  </section>
</template>

<style scoped lang="scss">
.digest-preview {
  margin-top: var(--zg-space-xl);
  padding: var(--zg-space-xl);
  border: 1px solid var(--surface-border);
  border-radius: var(--zg-radius-lg);
  background: var(--third-bg-color);
}
.preview-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--zg-space-sm);
  margin-bottom: var(--zg-space-lg);
  h2 { margin: 0; font-size: var(--zg-text-lg); font-weight: var(--zg-weight-semibold); }
  span { color: var(--second-color); font-size: var(--zg-text-sm); }
}
.preview-content {
  max-width: 640px;
  padding: var(--zg-space-xl);
  border-radius: var(--zg-radius-md);
  background: var(--surface-container);
  overflow-wrap: anywhere;
  h3 { margin: 0 0 var(--zg-space-lg); font-size: var(--zg-text-lg); }
  h4 { margin: 0; font-size: var(--zg-text-base); font-weight: var(--zg-weight-semibold); line-height: 1.6; }
  &.is-loading { opacity: 0.5; }
}
.preview-item + .preview-item {
  border-top: 1px solid var(--surface-border);
  margin-top: var(--zg-space-lg);
  padding-top: var(--zg-space-lg);
}
.preview-fields {
  display: grid;
  gap: var(--zg-space-xs);
  margin: var(--zg-space-sm) 0;
  color: var(--second-color);
  white-space: pre-wrap;
  line-height: 1.6;
  &.compact { display: block; white-space: normal; }
  &.compact span + span::before { content: ' · '; }
}
.preview-empty, .template-note { margin: 0; color: var(--second-color); }
.template-note { margin-bottom: var(--zg-space-lg); font-size: var(--zg-text-sm); }
</style>
