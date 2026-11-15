<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ChatDotRound } from '@element-plus/icons-vue'
import { NewsApiError } from '@/api/newsAdminRequest'
import { getFeishuBotProfileApi, type NewsFeishuAppDraft } from '@/api/newsSetting'
import { confirmAction, notifyError } from '../../newsNotify'
import FeishuProfileRow from './FeishuProfileRow.vue'

const props = defineProps<{
  apps: NewsFeishuAppDraft[]
  savedAppIds: string[]
  recipientCounts: Record<string, number>
  disabled: boolean
}>()
const emit = defineEmits<{ change: [app: NewsFeishuAppDraft]; remove: [appId: string] }>()

const visible = ref(false)
const querying = ref(false)
const draft = reactive({ appId: '', appSecret: '' })
const editing = ref(false)
const retainsSecret = computed(() => props.savedAppIds.includes(draft.appId.trim()))
const canConfirm = computed(
  () => !!draft.appId.trim() && (!!draft.appSecret.trim() || retainsSecret.value) && !querying.value
)

const open = (app?: NewsFeishuAppDraft) => {
  editing.value = !!app
  draft.appId = app?.appId ?? ''
  draft.appSecret = app?.appSecret ?? ''
  visible.value = true
}

const confirm = async () => {
  if (!canConfirm.value) return
  const app = { appId: draft.appId.trim(), appSecret: draft.appSecret.trim() || null }
  if (!editing.value && props.apps.some((item) => item.appId === app.appId)) {
    notifyError('该飞书应用已添加')
    return
  }
  querying.value = true
  try {
    const profile = await getFeishuBotProfileApi(app)
    emit('change', { ...app, ...profile, avatarUrl: profile.avatarUrl?.startsWith('https://') ? profile.avatarUrl : null })
    visible.value = false
  } catch (error) {
    if (!(error instanceof NewsApiError)) throw error
  } finally {
    querying.value = false
  }
}

const remove = async (app: NewsFeishuAppDraft) => {
  const count = props.recipientCounts[app.appId] ?? 0
  if (await confirmAction({
    title: '移除飞书应用？',
    message: count ? `保存后将移除这个应用及其 ${count} 位推送人员。` : '保存后将移除这个飞书应用。',
    confirmText: '移除',
    danger: true
  })) emit('remove', app.appId)
}
</script>

<template>
  <section class="feishu-list" aria-labelledby="feishu-apps-title">
    <header class="feishu-list-heading">
      <h3 id="feishu-apps-title">飞书应用 <small>{{ apps.length }}</small></h3>
      <el-button type="primary" :disabled="disabled" @click="open()">
        <SvgIcon icon-class="newsAdmin-add" size="18" /> 添加应用
      </el-button>
    </header>
    <div v-if="apps.length" class="feishu-list-rows">
      <FeishuProfileRow
        v-for="(app, index) in apps"
        :key="app.appId"
        :name="app.name || `飞书应用 ${index + 1}`"
        :detail="`${recipientCounts[app.appId] ?? 0} 位推送人员`"
        :avatar-url="app.avatarUrl"
        :icon="ChatDotRound"
        square
      >
        <el-button text :disabled="disabled" @click="open(app)">编辑</el-button>
        <el-button text type="danger" :disabled="disabled" @click="remove(app)">移除</el-button>
      </FeishuProfileRow>
    </div>
    <p v-else class="feishu-empty">添加飞书应用后，即可为它设置推送人员。</p>
  </section>
  <el-dialog
    v-model="visible"
    class="dark-dialog news-admin-dialog"
    :title="editing ? '编辑飞书应用' : '添加飞书应用'"
    width="min(480px, calc(100vw - 24px))"
    :close-on-click-modal="!querying"
    :close-on-press-escape="!querying"
    :show-close="!querying"
    @closed="draft.appSecret = ''"
  >
    <el-form label-position="top" require-asterisk-position="right" :disabled="querying" @submit.prevent="confirm">
      <el-form-item label="App ID" required>
        <el-input v-model="draft.appId" placeholder="cli_ 开头的应用 ID" :maxlength="128" :disabled="editing" />
      </el-form-item>
      <el-form-item label="App Secret" :required="!retainsSecret">
        <el-input
          v-model="draft.appSecret"
          type="password"
          autocomplete="new-password"
          :maxlength="512"
          :placeholder="retainsSecret ? '已设置，留空保留' : '填写飞书应用密钥'"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button plain :disabled="querying" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="querying" :disabled="!canConfirm" @click="confirm">确认应用</el-button>
    </template>
  </el-dialog>
</template>
