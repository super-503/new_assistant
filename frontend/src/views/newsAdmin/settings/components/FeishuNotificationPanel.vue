<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { NewsApiError } from '@/api/newsAdminRequest'
import {
  getFeishuBotProfileApi,
  saveFeishuChannelApi,
  type NewsFeishuAppDraft,
  type NewsFeishuChannel,
  type NewsFeishuChannelInput
} from '@/api/newsSetting'
import { useNewsSettingStore } from '@/stores/newsAdmin/newsSettingStore'
import type { FeishuSection } from '../settingsSection'
import { notifyError, notifySuccess } from '../../newsNotify'
import { savedDigestContent } from './digestContentDraft'
import FeishuAppSettings from './FeishuAppSettings.vue'
import FeishuCardSettings from './FeishuCardSettings.vue'
import FeishuRecipientSettings from './FeishuRecipientSettings.vue'
import { hydrateCardDraft, serializeCardDraft, type FeishuCardDraft } from './feishuCardForm'

const props = defineProps<{ section: FeishuSection }>()

const settingStore = useNewsSettingStore()
const form = ref<Omit<NewsFeishuChannelInput, 'card'> & { card: FeishuCardDraft }>()
const loading = ref(false)
const saving = ref(false)
const savedApps = computed(() => settingStore.feishuChannel?.apps.map((app) => ({ ...app, appSecret: null })) ?? [])
const savedAppIds = computed(() => savedApps.value.map((app) => app.appId))
const savedRecipients = computed(() => settingStore.feishuChannel?.recipients ?? [])
const saveLabels = { apps: '保存应用', recipients: '保存人员', card: '保存卡片' }
const recipientCounts = computed(() => {
  const counts: Record<string, number> = {}
  for (const recipient of form.value?.recipients ?? []) {
    counts[recipient.appId] = (counts[recipient.appId] ?? 0) + 1
  }
  return counts
})
const copyChannel = (saved: NewsFeishuChannel): NewsFeishuChannelInput => ({
  ...saved,
  apps: saved.apps.map((app) => ({ ...app, appSecret: null })),
  recipients: saved.recipients.map((recipient) => ({ ...recipient })),
  content: savedDigestContent(saved)
})

const load = async () => {
  loading.value = true
  try {
    const saved = await settingStore.ensureFeishuChannel()
    form.value = { ...copyChannel(saved), card: hydrateCardDraft(saved.card) }
    if (saved.revision === 0 && form.value) {
      for (const app of form.value.apps) void loadLegacyProfile(app)
    }
  } catch (error) {
    if (!(error instanceof NewsApiError)) throw error
  } finally {
    loading.value = false
  }
}

const loadLegacyProfile = async (app: NewsFeishuAppDraft) => {
  try {
    const profile = await getFeishuBotProfileApi({ appId: app.appId, appSecret: null }, false)
    app.name = profile.name
    app.avatarUrl = profile.avatarUrl?.startsWith('https://') ? profile.avatarUrl : null
  } catch (error) {
    if (!(error instanceof NewsApiError)) throw error
  }
}

const changeApp = (app: NewsFeishuAppDraft) => {
  if (!form.value) return
  const index = form.value.apps.findIndex((item) => item.appId === app.appId)
  if (index < 0) form.value.apps.push(app)
  else form.value.apps[index] = app
}

const removeApp = (appId: string) => {
  if (!form.value) return
  form.value.apps = form.value.apps.filter((app) => app.appId !== appId)
}

const save = async () => {
  const draft = form.value
  const current = settingStore.feishuChannel
  const section = props.section
  if (!draft || !current || saving.value) return
  const input = copyChannel(current)

  if (section === 'apps') {
    const appIds = new Set(draft.apps.map((app) => app.appId))
    input.apps = draft.apps
    input.recipients = input.recipients.filter((recipient) => appIds.has(recipient.appId))
    input.enabled = draft.enabled && input.recipients.length > 0
  } else if (section === 'recipients') {
    input.recipients = draft.recipients
    input.enabled = input.enabled && input.recipients.length > 0
  } else {
    const payload = serializeCardDraft(draft.card)
    if ('error' in payload) {
      notifyError(payload.error)
      return
    }
    input.card = payload.card
  }

  saving.value = true
  try {
    const saved = await saveFeishuChannelApi(input)
    const next = copyChannel(saved)
    settingStore.feishuChannel = saved
    if (section === 'apps') {
      draft.apps = next.apps
      draft.enabled = next.enabled
      draft.recipients = draft.recipients.filter((recipient) => savedAppIds.value.includes(recipient.appId))
    } else if (section === 'recipients') {
      draft.recipients = next.recipients
      if (!next.recipients.length) draft.enabled = false
    } else {
      draft.card = hydrateCardDraft(saved.card)
    }
    draft.revision = next.revision
    notifySuccess({ apps: '应用已保存', recipients: '人员已保存', card: '卡片已保存' }[section])
  } catch (error) {
    if (!(error instanceof NewsApiError)) throw error
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="feishu-settings" aria-label="飞书推送渠道配置">
    <p v-if="loading" class="feishu-feedback" role="status">正在加载飞书配置</p>
    <div v-else-if="!form" class="feishu-feedback">
      <el-button plain @click="load">重新加载</el-button>
    </div>
    <template v-else>
      <template v-if="section === 'apps'">
        <div class="digest-toggle">
          <label for="feishu-digest-enabled">启用飞书渠道</label>
          <el-switch id="feishu-digest-enabled" v-model="form.enabled" :disabled="saving || !savedRecipients.length" />
        </div>
        <FeishuAppSettings
          :apps="form.apps"
          :saved-app-ids="savedAppIds"
          :recipient-counts="recipientCounts"
          :disabled="saving"
          @change="changeApp"
          @remove="removeApp"
        />
      </template>
      <FeishuRecipientSettings
        v-else-if="section === 'recipients'"
        v-model="form.recipients"
        :apps="savedApps"
        :saved-app-ids="savedAppIds"
        :disabled="saving"
      />
      <FeishuCardSettings
        v-else
        v-model="form.card"
        :apps="savedApps"
        :recipients="savedRecipients"
        :content="form.content"
        :disabled="saving"
      />
      <footer class="feishu-footer">
        <el-button type="primary" :loading="saving" @click="save">{{ saveLabels[section] }}</el-button>
      </footer>
    </template>
  </section>
</template>

<style scoped lang="scss">
.feishu-settings {
  min-width: 0;
  width: 100%;
  max-width: 960px;
}

.digest-toggle {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--zg-space-lg);
  margin-bottom: var(--zg-space-xl);
  padding-bottom: var(--zg-space-lg);
  border-bottom: 1px solid var(--surface-border);
}

.digest-toggle,
.feishu-feedback {
  color: var(--second-color);
}

.feishu-settings {
  :deep(.feishu-list) {
    display: grid;
    gap: var(--zg-space-lg);
  }

  :deep(.feishu-list-heading) {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: var(--zg-space-md);

    h3 {
      margin: 0;
      font-size: var(--zg-text-lg);
      font-weight: var(--zg-weight-semibold);
    }

    small {
      margin-left: var(--zg-space-xs);
      color: var(--second-color);
      font-size: var(--zg-text-md);
      font-weight: var(--zg-weight-medium);
    }
  }

  :deep(.feishu-list-rows) {
    border-block: 1px solid var(--surface-border);
  }

  :deep(.feishu-list-rows > .profile-row) {
    padding-inline: 0;
  }

  :deep(.feishu-list-rows > .profile-row + .profile-row) {
    border-top: 1px solid var(--surface-border);
  }

  :deep(.feishu-empty) {
    margin: 0;
    padding-block: var(--zg-space-xl);
    border-block: 1px solid var(--surface-border);
    color: var(--second-color);
  }
}

.feishu-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--zg-space-2xl);
  padding-top: var(--zg-space-lg);
  border-top: 1px solid var(--surface-border);
}

</style>
