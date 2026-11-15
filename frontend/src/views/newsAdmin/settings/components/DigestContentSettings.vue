<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowDown, ArrowUp, Close } from '@element-plus/icons-vue'
import { listChannelsApi, type NewsChannel } from '@/api/newsChannel'
import { NewsApiError } from '@/api/newsAdminRequest'
import type { NewsDigestContentSettings } from '@/api/newsSetting'
import { useNewsSettingStore } from '@/stores/newsAdmin/newsSettingStore'

defineProps<{ disabled: boolean }>()
const content = defineModel<NewsDigestContentSettings>({ required: true })
const settingsStore = useNewsSettingStore()
const channels = ref<NewsChannel[]>([])
const loadFailed = ref(false)
const fieldToAdd = ref('')
const fields = computed(() => [
  { key: 'author', label: '作者', type: '文本' },
  { key: 'publishedAt', label: '发布时间', type: '时间' },
  { key: 'url', label: '原文链接', type: '链接' },
  ...(settingsStore.aiFields ?? [])
    .filter((field) => field.enabled && field.fieldKey !== 'title')
    .map((field) => ({ key: `ai:${field.fieldKey}`, label: field.name, type: field.outputTypeLabel }))
])
const availableFields = computed(() => fields.value.filter((field) => !content.value.fields.includes(field.key)))
const selectedFields = computed(() => content.value.fields.map((key) =>
  fields.value.find((field) => field.key === key) ?? { key, label: key.replace(/^ai:/, ''), type: '不可用' }
))

const addField = (key: string) => {
  if (content.value.fields.length < 32 && availableFields.value.some((field) => field.key === key)) {
    content.value.fields = [...content.value.fields, key]
  }
  fieldToAdd.value = ''
}

const moveField = (index: number, offset: number) => {
  const fields = [...content.value.fields]
  const target = index + offset
  if (target < 0 || target >= fields.length) return
  const [field] = fields.splice(index, 1)
  fields.splice(target, 0, field)
  content.value.fields = fields
}

const removeField = (key: string) => {
  content.value.fields = content.value.fields.filter((field) => field !== key)
}

const load = async () => {
  loadFailed.value = false
  const results = await Promise.allSettled([
    listChannelsApi().then((data) => (channels.value = data)),
    settingsStore.ensureAiFields()
  ])
  for (const result of results) {
    if (result.status === 'fulfilled') continue
    if (!(result.reason instanceof NewsApiError)) throw result.reason
    loadFailed.value = true
  }
}

onMounted(load)
</script>

<template>
  <el-form class="digest-form" label-position="top" :disabled="disabled" @submit.prevent>
    <div v-if="loadFailed" class="retry-options">
      部分选项加载失败
      <el-button text type="primary" @click="load">重试</el-button>
    </div>
    <section class="digest-group" aria-labelledby="digest-scope-title">
      <h2 id="digest-scope-title">资讯内容</h2>
      <div class="digest-fields scope-fields">
        <el-form-item label="资讯来源">
          <el-select v-model="content.channelCodes" multiple filterable placeholder="全部渠道" popper-class="news-admin-popper">
            <el-option v-for="channel in channels" :key="channel.code" :label="channel.name" :value="channel.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容类型">
          <el-select v-model="content.contentTypes" multiple placeholder="全部类型" popper-class="news-admin-popper">
            <el-option label="文章" value="ARTICLE" />
            <el-option label="视频" value="VIDEO" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核通过时间">
          <el-input-number
            :model-value="content.reviewDays ?? undefined"
            :min="1"
            :max="365"
            :precision="0"
            :controls="false"
            placeholder="不限天数"
            @change="content.reviewDays = $event ?? null"
          />
          <span class="field-hint">按最近通过审核的天数筛选，留空为不限。</span>
        </el-form-item>
      </div>
    </section>
    <section class="digest-group" aria-labelledby="digest-fields-title">
      <h2 id="digest-fields-title">显示格式</h2>
      <div class="digest-fields">
        <el-form-item label="资讯标题">
          <el-select v-model="content.useAiTitle" popper-class="news-admin-popper">
            <el-option label="原文标题" :value="false" />
            <el-option label="优先使用 AI 标题" :value="true" />
          </el-select>
        </el-form-item>
        <el-form-item label="附加字段排版">
          <el-select v-model="content.compact" popper-class="news-admin-popper">
            <el-option label="按字段分行" :value="false" />
            <el-option label="紧凑同行" :value="true" />
          </el-select>
        </el-form-item>
      </div>
      <section class="field-list" aria-labelledby="digest-field-list-title">
        <header class="field-list-header">
          <h3 id="digest-field-list-title">显示字段</h3>
          <el-select
            v-model="fieldToAdd"
            class="add-field-select"
            aria-label="添加显示字段"
            filterable
            :disabled="selectedFields.length >= 32 || !availableFields.length"
            placeholder="添加字段"
            popper-class="news-admin-popper"
            @change="addField"
          >
            <el-option v-for="field in availableFields" :key="field.key" :label="field.label" :value="field.key" />
          </el-select>
        </header>
        <div class="field-table-frame">
          <table v-if="selectedFields.length" class="field-table" aria-labelledby="digest-field-list-title">
            <thead>
              <tr><th scope="col">字段名称</th><th scope="col">类型</th><th scope="col">操作</th></tr>
            </thead>
            <tbody>
              <tr v-for="(field, index) in selectedFields" :key="field.key">
                <th scope="row">{{ field.label }}</th>
                <td>{{ field.type }}</td>
                <td>
                  <div class="field-actions">
                    <el-button text :icon="ArrowUp" :aria-label="`上移${field.label}`" :disabled="index === 0" @click="moveField(index, -1)" />
                    <el-button text :icon="ArrowDown" :aria-label="`下移${field.label}`" :disabled="index === selectedFields.length - 1" @click="moveField(index, 1)" />
                    <el-button text type="danger" :icon="Close" :aria-label="`移除${field.label}`" @click="removeField(field.key)" />
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <p v-else class="empty-fields">当前仅显示标题。</p>
        </div>
      </section>
    </section>
  </el-form>
</template>

<style scoped lang="scss">
.digest-form {
  display: grid;
  gap: var(--zg-space-xl);
  container-type: inline-size;
}

.digest-group {
  min-width: 0;
  padding: var(--zg-space-xl);
  border: 1px solid var(--surface-border);
  border-radius: var(--zg-radius-lg);
  background: var(--third-bg-color);
}

.digest-group h2 {
  margin: 0 0 var(--zg-space-xl);
  font-size: var(--zg-text-lg);
  font-weight: var(--zg-weight-semibold);
}

.digest-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--zg-space-xl);

  :deep(.el-form-item) { margin: 0; min-width: 0; }
  :deep(.el-input-number) { width: 100%; }
  :deep(.el-input-number .el-input__inner) { text-align: left; }
}

.scope-fields { grid-template-columns: repeat(3, minmax(0, 1fr)); }


.field-list { margin-top: var(--zg-space-xl); }

.field-list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--zg-space-md);
  margin-bottom: var(--zg-space-md);

  h3 {
    margin: 0;
    font-size: var(--zg-text-base);
    font-weight: var(--zg-weight-semibold);
  }
}

.add-field-select { width: min(100%, 176px); }

.field-table-frame {
  border: 1px solid var(--surface-border);
  border-radius: var(--zg-radius-md);
  overflow: hidden;
}

.field-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: var(--zg-text-base);

  th, td {
    padding: var(--zg-space-xs) var(--zg-space-md);
    overflow-wrap: anywhere;
  }

  thead {
    background: var(--surface-container);
    color: var(--second-color);

    th { height: 36px; font-weight: var(--zg-weight-medium); }
  }

  tbody th { font-weight: var(--zg-weight-medium); }
  tbody td { color: var(--second-color); }
  tbody tr { border-top: 1px solid var(--surface-border); }
  th:last-child { width: 1%; white-space: nowrap; }
  thead th:last-child { text-align: center; }
}

.field-actions {
  display: flex;
  gap: var(--zg-space-2xs);

  :deep(.el-button) { width: 28px; padding: 0; margin: 0; }
}

.empty-fields {
  margin: 0;
  padding: var(--zg-space-lg);
  color: var(--second-color);
}

.field-hint,
.retry-options {
  color: var(--second-color);
  font-size: var(--zg-text-sm);
}

@container (max-width: 620px) {
  .scope-fields { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@container (max-width: 430px) {
  .digest-fields { grid-template-columns: minmax(0, 1fr); }
}
</style>
