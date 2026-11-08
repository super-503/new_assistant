<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { NewsApiError } from '@/api/newsAdminRequest'
import {
  saveNotificationChannelApi,
  verifyNotificationChannelApi,
  type NewsFeishuNotificationInput,
  type NewsFeishuNotificationSetting
} from '@/api/newsSetting'
import { useNewsSettingStore } from '@/stores/newsAdmin/newsSettingStore'
import { notifySuccess } from '../../newsNotify'
import SettingsFormCard from './SettingsFormCard.vue'

const settingStore = useNewsSettingStore()
const form = reactive({ appId: '', appSecret: '', openId: '', enabled: true })
const action = ref<'load' | 'save' | 'verify' | null>(null)
const status = computed(() => {
  const saved = settingStore.notificationChannel
  if (!saved) return
  if (!saved.configured) return '未配置'
  return saved.enabled ? '已启用' : '已停用'
})
const hasSavedSecret = computed(
  () => settingStore.notificationChannel?.configured && form.appId.trim() === settingStore.notificationChannel.appId
)
const canSubmit = computed(
  () => !!form.appId.trim() && !!form.openId.trim() && (!!form.appSecret.trim() || hasSavedSecret.value) && !action.value
)

const fillForm = (setting: NewsFeishuNotificationSetting) => {
  form.appId = setting.appId
  form.appSecret = ''
  form.openId = setting.openId
  form.enabled = !setting.configured || setting.enabled
}

const load = async () => {
  action.value = 'load'
  try {
    fillForm(await settingStore.ensureNotificationChannel())
  } catch (error) {
    if (!(error instanceof NewsApiError)) throw error
  } finally {
    action.value = null
  }
}

const submit = async (mode: 'save' | 'verify') => {
  if (!canSubmit.value) return
  const input: NewsFeishuNotificationInput = {
    appId: form.appId.trim(),
    appSecret: form.appSecret.trim() || null,
    openId: form.openId.trim(),
    enabled: form.enabled
  }
  action.value = mode
  try {
    if (mode === 'verify') {
      await verifyNotificationChannelApi(input)
      notifySuccess('已发送测试消息，请查看飞书单聊')
    } else {
      const saved = await saveNotificationChannelApi(input)
      settingStore.notificationChannel = saved
      fillForm(saved)
      notifySuccess('通知渠道已保存')
    }
  } catch (error) {
    if (!(error instanceof NewsApiError)) throw error
  } finally {
    action.value = null
  }
}

onMounted(load)
</script>

<template>
  <SettingsFormCard
    title="飞书应用机器人"
    icon="newsAdmin-channels"
    :status="status"
    :active="settingStore.notificationChannel?.enabled"
  >
    <p v-if="action === 'load'" class="settings-feedback" role="status">正在加载通知渠道</p>
    <div v-else-if="!settingStore.notificationChannel" class="settings-feedback">
      <el-button plain @click="load">重新加载</el-button>
    </div>
    <el-form
      v-else
      label-position="top"
      require-asterisk-position="right"
      :disabled="action !== null"
      @submit.prevent="submit('save')"
    >
      <div class="settings-fields">
        <el-form-item label="App ID" required>
          <el-input v-model="form.appId" placeholder="cli_ 开头的应用 ID" :maxlength="128" />
        </el-form-item>
        <el-form-item label="App Secret" required>
          <el-input
            v-model="form.appSecret"
            type="password"
            autocomplete="new-password"
            :maxlength="512"
            :placeholder="hasSavedSecret ? '已设置，留空保留' : '填写飞书应用密钥'"
          />
        </el-form-item>
        <el-form-item class="settings-field-wide" label="接收人 Open ID" required>
          <el-input v-model="form.openId" placeholder="ou_ 开头的用户 ID" :maxlength="128" />
        </el-form-item>
      </div>
      <div class="settings-option">
        <span id="digest-enabled-label">启用日报推送</span>
        <el-switch v-model="form.enabled" aria-labelledby="digest-enabled-label" />
      </div>
      <div class="settings-actions">
        <el-button plain :disabled="!canSubmit" :loading="action === 'verify'" @click="submit('verify')">
          发送测试消息
        </el-button>
        <el-button type="primary" native-type="submit" :disabled="!canSubmit" :loading="action === 'save'">
          保存配置
        </el-button>
      </div>
    </el-form>
  </SettingsFormCard>
</template>
