<script setup lang="ts">
import { computed, ref } from 'vue'
import { NewsApiError } from '@/api/newsAdminRequest'
import {
  testFeishuCardApi,
  type NewsDigestContentSettings,
  type NewsFeishuAppDraft,
  type NewsFeishuRecipient
} from '@/api/newsSetting'
import { notifyError, notifySuccess } from '../../newsNotify'
import FeishuTemplateVariables from './FeishuTemplateVariables.vue'
import { newTemplate, serializeCardDraft, type FeishuCardDraft, type FeishuTemplateDraft, type FeishuVariableDraft } from './feishuCardForm'

const props = defineProps<{
  apps: NewsFeishuAppDraft[]
  recipients: NewsFeishuRecipient[]
  content: NewsDigestContentSettings
  disabled: boolean
}>()
const card = defineModel<FeishuCardDraft>({ required: true })
const setVariables = (variables: FeishuVariableDraft[] | null) => {
  if (card.value.template && variables) card.value.template.variables = variables
}
const previousTemplate = ref<FeishuTemplateDraft>()
const cardMode = computed({
  get: () => (card.value.template ? 'template' : 'builtin'),
  set: (mode: 'template' | 'builtin') => {
    if (mode === 'template') {
      card.value.template = previousTemplate.value ?? newTemplate()
    } else {
      previousTemplate.value = card.value.template ?? undefined
      card.value.template = null
    }
  }
})
const colors = [
  { value: 'blue', label: '蓝色' },
  { value: 'green', label: '绿色' },
  { value: 'orange', label: '橙色' },
  { value: 'purple', label: '紫色' }
]
const dialog = ref(false)
const sending = ref(false)
const selected = ref('')
const recipientKey = (recipient: NewsFeishuRecipient) => `${recipient.appId}:${recipient.openId}`
const target = computed(() => props.recipients.find((recipient) => recipientKey(recipient) === selected.value))
const recipientLabel = (recipient: NewsFeishuRecipient, index: number) => {
  const appIndex = props.apps.findIndex((app) => app.appId === recipient.appId)
  const appName = props.apps[appIndex]?.name || `飞书应用 ${appIndex + 1}`
  return `${recipient.name || `飞书接收人 ${index + 1}`} · ${appName}`
}

const open = () => {
  const [first] = props.recipients
  if (!first) return
  const payload = serializeCardDraft(card.value)
  if ('error' in payload) {
    notifyError(payload.error)
    return
  }
  selected.value = recipientKey(first)
  dialog.value = true
}

const send = async () => {
  const recipient = target.value
  const app = props.apps.find((entry) => entry.appId === recipient?.appId)
  if (!app || !recipient || sending.value) return
  const payload = serializeCardDraft(card.value)
  if ('error' in payload) {
    notifyError(payload.error)
    return
  }
  sending.value = true
  try {
    await testFeishuCardApi({
      appId: app.appId,
      appSecret: app.appSecret,
      openId: recipient.openId,
      card: payload.card,
      content: props.content
    })
    dialog.value = false
    notifySuccess('日报预览已发送，请在飞书单聊查看')
  } catch (error) {
    if (!(error instanceof NewsApiError)) throw error
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <section class="feishu-list" aria-labelledby="feishu-card-title">
    <header class="feishu-list-heading">
      <h3 id="feishu-card-title">日报卡片</h3>
      <el-button
        plain
        :disabled="disabled || !recipients.length"
        :title="recipients.length ? undefined : '添加推送人员后可发送预览'"
        @click="open"
      >
        发送到飞书预览
      </el-button>
    </header>
    <div class="card-layout">
      <el-form label-position="top" require-asterisk-position="right" :disabled="disabled" @submit.prevent>
        <el-form-item label="卡片类型">
          <el-radio-group v-model="cardMode">
            <el-radio-button value="builtin">内置日报</el-radio-button>
            <el-radio-button value="template">飞书模板</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="卡片标题" required>
          <el-input v-model="card.title" maxlength="80" />
        </el-form-item>
        <el-form-item v-if="!card.template" label="标题配色">
          <el-select v-model="card.color" popper-class="news-admin-popper">
            <el-option v-for="color in colors" :key="color.value" :label="color.label" :value="color.value" />
          </el-select>
        </el-form-item>
        <template v-else>
          <el-form-item label="卡片模板 ID" required>
            <el-input v-model.trim="card.template.id" maxlength="128" placeholder="已发布模板的 ID" />
          </el-form-item>
          <el-form-item label="模板版本">
            <el-input v-model.trim="card.template.version" maxlength="32" placeholder="留空使用最新版本" />
          </el-form-item>
        </template>
      </el-form>
      <FeishuTemplateVariables
        :model-value="card.template?.variables ?? null"
        :disabled="disabled"
        @update:model-value="setVariables"
      />
    </div>
  </section>

  <el-dialog
    v-model="dialog"
    class="dark-dialog news-admin-dialog"
    title="发送日报预览"
    width="min(480px, calc(100vw - 24px))"
    :show-close="!sending"
    :close-on-click-modal="!sending"
    :close-on-press-escape="!sending"
  >
    <el-form label-position="top" :disabled="sending" @submit.prevent="send">
      <p class="preview-description">从当前推送范围取最多 3 条已审核通过的资讯，发送到所选接收人的飞书单聊。</p>
      <el-form-item label="接收人">
        <el-select v-model="selected" popper-class="news-admin-popper">
          <el-option
            v-for="(recipient, index) in recipients"
            :key="recipientKey(recipient)"
            :label="recipientLabel(recipient, index)"
            :value="recipientKey(recipient)"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button plain :disabled="sending" @click="dialog = false">取消</el-button>
      <el-button type="primary" :loading="sending" :disabled="!target || sending" @click="send">发送</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.card-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--zg-space-xl);

  :deep(.el-form-item:last-child) { margin-bottom: 0; }
}

.preview-description {
  margin: 0 0 var(--zg-space-lg);
  color: var(--second-color);
}
</style>
