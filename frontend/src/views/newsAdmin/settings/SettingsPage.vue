<script setup lang="ts">
import { ref } from 'vue'
import { useNewsSettingStore } from '@/stores/newsAdmin/newsSettingStore'
import NewsTabs, { type NewsTab } from '../components/NewsTabs.vue'
import AiFieldPanel from './components/AiFieldPanel.vue'
import AiModelPanel from './components/AiModelPanel.vue'
import FeishuNotificationPanel from './components/FeishuNotificationPanel.vue'
import RejectReasonPanel from './components/RejectReasonPanel.vue'

const SETTINGS_TABS: NewsTab[] = [
  { key: 'aiModel', label: 'AI 模型' },
  { key: 'aiFields', label: 'AI 字段' },
  { key: 'rejectReasons', label: '拒绝理由' },
  { key: 'notification', label: '通知发送渠道' }
]

const activeTab = ref('aiModel')
const settingStore = useNewsSettingStore()
</script>

<template>
  <main class="settings-page">
    <div class="settings-content">
      <h1 class="page-title">基础配置</h1>
      <NewsTabs v-model="activeTab" class="settings-tabs" :tabs="SETTINGS_TABS" label="基础配置分类" />
      <section v-show="activeTab === 'aiModel'" role="tabpanel" aria-label="AI 模型">
        <AiModelPanel />
      </section>
      <section v-if="activeTab === 'aiFields'" class="settings-table" role="tabpanel" aria-label="AI 字段">
        <AiFieldPanel v-if="settingStore.aiModel?.configured" />
        <div v-else class="model-required">
          <p>{{ settingStore.aiModel ? '请先保存模型配置，再使用 AI 字段。' : '请先加载模型配置。' }}</p>
          <el-button type="primary" @click="activeTab = 'aiModel'">前往 AI 模型</el-button>
        </div>
      </section>
      <section v-else-if="activeTab === 'rejectReasons'" class="settings-table" role="tabpanel" aria-label="拒绝理由">
        <RejectReasonPanel />
      </section>
      <section v-else-if="activeTab === 'notification'" role="tabpanel" aria-label="通知发送渠道">
        <FeishuNotificationPanel />
      </section>
    </div>
  </main>
</template>

<style scoped lang="scss">
.settings-page {
  flex: 1 1 auto;
  min-width: 0;
  padding: var(--zg-space-2xl) clamp(12px, 2.4vw, 24px) var(--zg-space-3xl);
  overflow-y: auto;
}

.settings-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--zg-space-xl);
  max-width: 960px;
  margin-inline: auto;
}

.page-title {
  margin: 0;
  font-size: var(--zg-text-2xl);
  line-height: 28px;
  font-weight: var(--zg-weight-semibold);
}

.settings-tabs {
  :deep(.news-tab) {
    flex: none;
    white-space: nowrap;
  }
}

@media (max-width: 520px) {
  .settings-tabs {
    flex-wrap: wrap;

    :deep(.news-tab) {
      flex: 0 0 50%;
      padding-inline: var(--zg-space-sm);
    }
  }
}

.settings-table {
  overflow-x: auto;
}

.model-required {
  color: var(--second-color);
  font-size: var(--zg-text-base);

  p {
    margin: 0 0 var(--zg-space-lg);
  }
}
</style>
