<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { NewsApiError } from '@/api/newsAdminRequest'
import { listPushPlansApi, savePushPlanApi, type NewsPushPlan, type NewsPushPlanDefinition } from '@/api/newsPushPlan'
import { useNewsSettingStore } from '@/stores/newsAdmin/newsSettingStore'
import { notifySuccess } from '../../newsNotify'
import type { FeishuSection } from '../settingsSection'
import DigestContentSettings from './DigestContentSettings.vue'
import PushPlanPreviewButton from './PushPlanPreviewButton.vue'
import DigestContentPreview from './DigestContentPreview.vue'
import FeishuPersonLabel from './FeishuPersonLabel.vue'
import { copyDigestContent, savedDigestContent } from './digestContentDraft'

const emit = defineEmits<{ openFeishu: [section: FeishuSection] }>()
const settingsStore = useNewsSettingStore()
type PlanDraft = {
  id?: number
  revision?: number
  enabled: boolean
  definition: Omit<NewsPushPlanDefinition, 'card'> & { card: NewsPushPlanDefinition['card'] | '' }
}

const plans = ref<NewsPushPlan[]>([])
const timeZone = ref('')
const draft = ref<PlanDraft>()
const loading = ref(false)
const loadFailed = ref(false)
const saving = ref(false)
const channel = computed(() => settingsStore.feishuChannel)
const recipients = computed(() => channel.value?.recipients.filter((person) => person.appId === draft.value?.definition.appId) ?? [])
const savedCardLabel = computed(() => `已保存卡片 · ${channel.value?.card.title ?? '资讯日报'}`)
const labelForApp = (appId: string) => {
  const apps = channel.value?.apps ?? []
  const index = apps.findIndex((app) => app.appId === appId)
  return apps[index]?.name || (index < 0 ? '原应用已移除' : `飞书应用 ${index + 1}`)
}

const canSave = computed(() => {
  const plan = draft.value
  if (!plan || saving.value) return false
  const { name, sendTime, appId, card, recipientOpenIds } = plan.definition
  if (!name.trim() || !/^([01]\d|2[0-3]):[0-5]\d$/.test(sendTime) || !appId || !card || !recipientOpenIds.length) return false
  return (plan.id !== undefined && !plan.enabled) || recipientOpenIds.every((id) => recipients.value.some((person) => person.openId === id))
})

const previewDefinition = computed<NewsPushPlanDefinition | undefined>(() => {
  const definition = draft.value?.definition
  if (!definition?.card || !definition.appId || !definition.recipientOpenIds.length) return undefined
  if (!definition.recipientOpenIds.every((id) => recipients.value.some((person) => person.openId === id))) return undefined
  return { ...definition, name: definition.name.trim() || '测试发送', card: definition.card }
})

const load = async () => {
  loading.value = true
  loadFailed.value = false
  try {
    const [data] = await Promise.all([listPushPlansApi(), settingsStore.ensureFeishuChannel()])
    plans.value = data.plans
    timeZone.value = data.timeZone
  } catch (error) {
    if (!(error instanceof NewsApiError)) throw error
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

const open = (plan?: NewsPushPlan) => {
  draft.value = plan ? {
    id: plan.id, revision: plan.revision, enabled: plan.enabled,
    definition: {
      ...plan.definition,
      recipientOpenIds: [...plan.definition.recipientOpenIds],
      content: copyDigestContent(plan.definition.content)
    }
  } : {
    enabled: true,
    definition: {
      name: '', sendTime: '21:05', channel: 'FEISHU', appId: '', recipientOpenIds: [], card: '',
      content: savedDigestContent(channel.value)
    }
  }
}

const changeApp = () => {
  if (draft.value) draft.value.definition.recipientOpenIds = []
}

const save = async () => {
  const plan = draft.value
  const card = plan?.definition.card
  if (!plan || !card || !canSave.value) return
  saving.value = true
  try {
    const saved = await savePushPlanApi(plan.id, {
      revision: plan.revision,
      enabled: plan.enabled,
      definition: { ...plan.definition, name: plan.definition.name.trim(), card }
    })
    plans.value = plan.id === undefined
      ? [saved, ...plans.value]
      : plans.value.map((item) => item.id === saved.id ? saved : item)
    draft.value = undefined
    notifySuccess('推送计划已保存')
  } catch (error) {
    if (!(error instanceof NewsApiError)) throw error
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="plan-panel" aria-label="推送计划设置">
    <header class="plan-toolbar">
      <h2>{{ draft ? (draft.id === undefined ? '新建推送计划' : '编辑推送计划') : '计划列表' }}</h2>
      <el-button v-if="draft" plain :disabled="saving" @click="draft = undefined">返回计划列表</el-button>
      <el-button v-else type="primary" :disabled="loading || loadFailed" @click="open()">去配置</el-button>
    </header>

    <p v-if="loading" role="status" class="plan-feedback">正在加载推送计划</p>
    <div v-else-if="loadFailed" class="plan-feedback">
      <el-button plain @click="load">重新加载</el-button>
    </div>
    <template v-else-if="draft">
      <el-form label-position="top" :disabled="saving" @submit.prevent="save">
        <section class="plan-group" aria-labelledby="plan-schedule-title">
          <h3 id="plan-schedule-title">计划时间</h3>
          <div class="plan-fields">
            <el-form-item label="计划名称" required>
              <el-input v-model="draft.definition.name" maxlength="80" placeholder="例如：每日技术资讯" />
            </el-form-item>
            <el-form-item :label="`每日发送时间（${timeZone}）`" required>
              <el-time-picker v-model="draft.definition.sendTime" format="HH:mm" value-format="HH:mm" :clearable="false" />
            </el-form-item>
          </div>
          <div class="plan-option">
            <label for="push-plan-enabled">启用计划</label>
            <el-switch id="push-plan-enabled" v-model="draft.enabled" />
          </div>
        </section>
        <section class="plan-group" aria-labelledby="plan-destination-title">
          <h3 id="plan-destination-title">推送渠道</h3>
          <div class="plan-fields">
            <el-form-item label="渠道" required>
              <el-select v-model="draft.definition.channel" popper-class="news-admin-popper">
                <el-option label="飞书" value="FEISHU" />
              </el-select>
            </el-form-item>
            <el-form-item label="飞书应用" required>
              <el-select v-model="draft.definition.appId" placeholder="选择应用" popper-class="news-admin-popper" @change="changeApp">
                <el-option v-for="app in channel?.apps ?? []" :key="app.appId" :label="labelForApp(app.appId)" :value="app.appId" />
              </el-select>
              <el-button v-if="!channel?.apps.length" text type="primary" @click="emit('openFeishu', 'apps')">去配置应用</el-button>
            </el-form-item>
            <el-form-item label="推送人员" required>
              <el-select
                v-model="draft.definition.recipientOpenIds"
                multiple filterable :multiple-limit="100" :disabled="!draft.definition.appId"
                placeholder="选择该应用的接收人" popper-class="news-admin-popper"
              >
                <template #label="{ label, value }">
                  <FeishuPersonLabel
                    :name="recipients.some((person) => person.openId === value) ? label : '接收人已移除'"
                    :avatar-url="recipients.find((person) => person.openId === value)?.avatarUrl"
                  />
                </template>
                <el-option v-for="(person, index) in recipients" :key="person.openId" :label="person.name || `接收人 ${index + 1}`" :value="person.openId">
                  <FeishuPersonLabel :name="person.name || `接收人 ${index + 1}`" :avatar-url="person.avatarUrl" />
                </el-option>
              </el-select>
              <el-button v-if="draft.definition.appId && !recipients.length" text type="primary" @click="emit('openFeishu', 'recipients')">去配置人员</el-button>
            </el-form-item>
            <el-form-item label="飞书卡片" required>
              <el-select v-model="draft.definition.card" placeholder="选择卡片" popper-class="news-admin-popper">
                <el-option :label="savedCardLabel" value="SAVED" />
                <el-option label="内置日报卡片" value="BUILTIN" />
              </el-select>
            </el-form-item>
          </div>
          <div v-if="!channel?.enabled" class="plan-feedback channel-feedback">
            飞书渠道停用中
            <el-button text type="primary" @click="emit('openFeishu', 'apps')">去启用</el-button>
          </div>
        </section>
      </el-form>
      <DigestContentSettings v-model="draft.definition.content" :disabled="saving" />
      <DigestContentPreview
        :content="draft.definition.content"
        :card-title="draft.definition.card === 'SAVED' ? channel?.card.title ?? '资讯日报' : '资讯日报'"
        :uses-template="draft.definition.card === 'SAVED' && !!channel?.card.template"
      />
      <p v-if="draft.id === undefined" class="plan-feedback">
        首次发送会包含所选范围内的历史已审核资讯。每条计划独立记录发送结果。
      </p>
      <footer class="plan-footer">
        <PushPlanPreviewButton :definition="previewDefinition" :recipients="recipients" :disabled="saving">
          <el-button type="primary" :loading="saving" :disabled="!canSave" @click="save">保存推送计划</el-button>
        </PushPlanPreviewButton>
      </footer>
    </template>
    <ul v-else-if="plans.length" class="plan-list" aria-label="推送计划">
      <li v-for="plan in plans" :key="plan.id" class="plan-row">
        <div class="plan-summary">
          <strong>{{ plan.definition.name }}</strong>
          <span>飞书 · {{ labelForApp(plan.definition.appId) }} · {{ plan.definition.recipientOpenIds.length }} 位接收人</span>
        </div>
        <span>每日 {{ plan.definition.sendTime }}</span>
        <span class="plan-state">{{ plan.enabled ? '已启用' : '已停用' }}</span>
        <el-button text @click="open(plan)">编辑</el-button>
      </li>
    </ul>
    <p v-else class="plan-feedback">暂无推送计划。</p>
  </section>
</template>

<style scoped lang="scss">
.plan-panel { min-width: 0; max-width: 960px; }
.plan-toolbar, .plan-footer, .plan-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--zg-space-md);
}
.plan-toolbar { margin-bottom: var(--zg-space-xl); }
.plan-toolbar h2, .plan-group h3 { margin: 0; font-size: var(--zg-text-lg); font-weight: var(--zg-weight-semibold); }
.plan-group {
  margin-bottom: var(--zg-space-xl);
  padding: var(--zg-space-xl);
  border: 1px solid var(--surface-border);
  border-radius: var(--zg-radius-lg);
  background: var(--third-bg-color);
}
.plan-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--zg-space-xl);
  margin-top: var(--zg-space-xl);
  :deep(.el-form-item) { margin: 0; min-width: 0; }
  :deep(.el-date-editor) { width: 100%; }
}
.plan-option, .channel-feedback { margin-top: var(--zg-space-md); }
.plan-footer { justify-content: flex-end; margin-top: var(--zg-space-xl); }
.plan-feedback, .plan-state, .plan-summary span { color: var(--second-color); }
.plan-list { list-style: none; margin: 0; padding: 0; border-block: 1px solid var(--surface-border); }
.plan-row, .plan-summary { display: grid; gap: var(--zg-space-sm); }
.plan-row {
  align-items: center;
  grid-template-columns: minmax(0, 1fr) auto auto auto;
  column-gap: var(--zg-space-xl);
  padding-block: var(--zg-space-lg);
}
.plan-row + .plan-row { border-top: 1px solid var(--surface-border); }
.plan-summary { overflow-wrap: anywhere; }

@media (max-width: 640px) {
  .plan-fields { grid-template-columns: minmax(0, 1fr); }
  .plan-row { grid-template-columns: minmax(0, 1fr) auto auto; column-gap: var(--zg-space-md); }
  .plan-summary { grid-column: 1 / -1; }
}
</style>
