<script setup lang="ts">
import { computed, ref } from 'vue'
import AiProcessingPanel from './components/AiProcessingPanel.vue'
import FeishuNotificationPanel from './components/FeishuNotificationPanel.vue'
import PushPlanPanel from './components/PushPlanPanel.vue'
import RejectReasonPanel from './components/RejectReasonPanel.vue'
import { FEISHU_SECTIONS, type FeishuSection, type SettingsSection } from './settingsSection'

const section = ref<SettingsSection>('ai')
const pushExpanded = ref(false)
const feishuExpanded = ref(false)
const feishuSection = computed(() => FEISHU_SECTIONS.find((entry) => entry.key === section.value))
const title = computed(() => {
  if (section.value === 'ai') return '模型与 AI 字段'
  if (section.value === 'plans') return '推送计划'
  if (section.value === 'rejectReasons') return '拒绝理由'
  return feishuSection.value?.label
})

const openFeishu = (entry: FeishuSection) => {
  pushExpanded.value = true
  feishuExpanded.value = true
  section.value = entry
}
</script>

<template>
  <div class="settings-page">
    <nav class="settings-nav" aria-label="基础配置分类">
      <button
        type="button"
        class="settings-nav-item"
        :class="{ 'is-active': section === 'ai' }"
        :aria-current="section === 'ai' ? 'page' : undefined"
        @click="section = 'ai'"
      >
        <SvgIcon icon-class="newsAdmin-spark" size="20" /> 模型与 AI 字段
      </button>
      <button
        type="button"
        class="settings-nav-item branch"
        :class="{ 'branch-current': feishuSection }"
        :aria-expanded="pushExpanded"
        aria-controls="settings-push-channels"
        @click="pushExpanded = !pushExpanded"
      >
        <SvgIcon icon-class="newsAdmin-rss" size="20" /> 推送渠道配置
        <SvgIcon class="branch-arrow" icon-class="newsAdmin-chevron-right" size="16" />
      </button>
      <div v-show="pushExpanded" id="settings-push-channels" class="nav-children" role="group" aria-label="推送渠道配置">
        <button
          type="button"
          class="settings-nav-item branch nested-item"
          :class="{ 'branch-current': feishuSection }"
          :aria-expanded="feishuExpanded"
          aria-controls="settings-feishu-sections"
          @click="feishuExpanded = !feishuExpanded"
        >
          飞书
          <SvgIcon class="branch-arrow" icon-class="newsAdmin-chevron-right" size="16" />
        </button>
        <div v-show="feishuExpanded" id="settings-feishu-sections" class="nav-children" role="group" aria-label="飞书">
          <button
            v-for="entry in FEISHU_SECTIONS"
            :key="entry.key"
            type="button"
            class="settings-nav-item nested-item"
            :class="{ 'is-active': section === entry.key }"
            :aria-current="section === entry.key ? 'page' : undefined"
            @click="section = entry.key"
          >
            {{ entry.label }}
          </button>
        </div>
      </div>
      <button
        type="button"
        class="settings-nav-item"
        :class="{ 'is-active': section === 'plans' }"
        :aria-current="section === 'plans' ? 'page' : undefined"
        @click="section = 'plans'"
      >
        <SvgIcon icon-class="newsAdmin-article" size="20" /> 推送计划
      </button>
      <button
        type="button"
        class="settings-nav-item"
        :class="{ 'is-active': section === 'rejectReasons' }"
        :aria-current="section === 'rejectReasons' ? 'page' : undefined"
        @click="section = 'rejectReasons'"
      >
        <SvgIcon icon-class="newsAdmin-review" size="20" /> 拒绝理由
      </button>
    </nav>
    <main class="settings-main">
      <div class="settings-content">
        <header class="page-heading">
          <p v-if="feishuSection" class="settings-path">推送渠道配置 / 飞书</p>
          <h1 class="page-title">{{ title }}</h1>
        </header>
        <KeepAlive>
          <AiProcessingPanel v-if="section === 'ai'" />
          <RejectReasonPanel v-else-if="section === 'rejectReasons'" />
          <PushPlanPanel v-else-if="section === 'plans'" @open-feishu="openFeishu" />
          <FeishuNotificationPanel v-else :section="section" />
        </KeepAlive>
      </div>
    </main>
  </div>
</template>

<style scoped lang="scss">
@use '../components/newsConfigLayout' as *;

.settings-page {
  @include config-page;
}

.settings-nav {
  @include config-nav;
}

.settings-nav-item {
  @include config-nav-item;
}

.nav-children {
  display: grid;
  gap: var(--zg-space-2xs);
  margin-left: var(--zg-space-xl);
  padding-left: var(--zg-space-sm);
  border-left: 1px solid var(--surface-border);
}

.nested-item { height: 36px; }
.branch-current { font-weight: var(--zg-weight-semibold); }
.branch-arrow { margin-left: auto; }
.branch[aria-expanded='true'] .branch-arrow { transform: rotate(90deg); }

.settings-path {
  margin: 0 0 var(--zg-space-sm);
  color: var(--second-color);
  font-size: var(--zg-text-sm);
}

.settings-main {
  @include config-main;
}

.settings-content {
  @include config-inner;
}

.page-title {
  margin: 0;
  font-size: var(--zg-text-2xl);
  line-height: 28px;
  font-weight: var(--zg-weight-semibold);
}

@media (max-width: 760px) {
  .settings-page {
    flex-direction: column;
  }

  .settings-nav {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    flex: none;
    border-right: 0;
    border-bottom: 1px solid var(--surface-border);
  }

  .settings-nav-item {
    height: auto;
    min-height: 36px;
    padding: var(--zg-space-xs) var(--zg-space-sm);
  }

  .nav-children {
    grid-column: 1 / -1;
    display: flex;
    flex-wrap: wrap;
    margin-left: var(--zg-space-sm);

    .settings-nav-item { width: auto; }
  }
}
</style>
