<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { NewsApiError } from '@/api/newsAdminRequest'
import { saveAiModelApi, verifyAiModelApi, type NewsAiModelInput, type NewsAiModelSetting } from '@/api/newsSetting'
import { useNewsSettingStore } from '@/stores/newsAdmin/newsSettingStore'
import { notifySuccess } from '../../newsNotify'
import SettingsFormCard from './SettingsFormCard.vue'

const settingStore = useNewsSettingStore()
const form = reactive({ modelUrl: '', model: '', apiKey: '', clearApiKey: false })
const action = ref<'load' | 'save' | 'verify' | null>(null)
const status = computed(() => {
  const saved = settingStore.aiModel
  if (saved) return saved.configured ? '已配置' : '未配置'
})
const hasSavedKey = computed(
  () => settingStore.aiModel?.apiKeyConfigured && form.modelUrl.trim() === settingStore.aiModel.modelUrl
)
const canSubmit = computed(() => !!form.modelUrl.trim() && !!form.model.trim() && action.value === null)

const fillForm = (setting: NewsAiModelSetting) => {
  form.modelUrl = setting.modelUrl
  form.model = setting.model
  form.apiKey = ''
  form.clearApiKey = false
}

const load = async () => {
  action.value = 'load'
  try {
    fillForm(await settingStore.ensureAiModel())
  } catch (error) {
    if (!(error instanceof NewsApiError)) throw error
  } finally {
    action.value = null
  }
}

const submit = async (mode: 'save' | 'verify') => {
  if (!canSubmit.value) return
  const input: NewsAiModelInput = {
    modelUrl: form.modelUrl.trim(),
    model: form.model.trim(),
    apiKey: form.clearApiKey ? '' : form.apiKey.trim() || null
  }
  action.value = mode
  try {
    if (mode === 'verify') {
      await verifyAiModelApi(input)
      notifySuccess('模型连接成功')
    } else {
      const saved = await saveAiModelApi(input)
      settingStore.aiModel = saved
      fillForm(saved)
      notifySuccess('模型已保存，后续资讯将使用此配置')
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
    title="OpenAI 兼容接口"
    icon="newsAdmin-spark"
    :status="status"
    :active="settingStore.aiModel?.configured"
  >
    <p v-if="action === 'load'" class="settings-feedback" role="status">正在加载模型配置</p>
    <div v-else-if="!settingStore.aiModel" class="settings-feedback">
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
        <el-form-item class="settings-field-wide" label="Chat Completions 接口地址" required>
          <el-input
            v-model="form.modelUrl"
            placeholder="http://127.0.0.1:11434/v1/chat/completions"
            :maxlength="2048"
            @input="form.clearApiKey = false"
          />
        </el-form-item>
        <el-form-item label="模型名称" required>
          <el-input v-model="form.model" placeholder="填写服务提供的模型 ID" :maxlength="255" />
        </el-form-item>
        <el-form-item label="API 密钥">
          <el-input
            v-model="form.apiKey"
            type="password"
            autocomplete="new-password"
            :maxlength="2048"
            :disabled="form.clearApiKey"
            :placeholder="hasSavedKey ? '已设置，留空保留' : '可选，按服务要求填写'"
          />
          <el-checkbox v-if="hasSavedKey && !form.apiKey" v-model="form.clearApiKey">清除已设置的密钥</el-checkbox>
        </el-form-item>
      </div>
      <div class="settings-actions">
        <el-button plain :disabled="!canSubmit" :loading="action === 'verify'" @click="submit('verify')">
          测试连接
        </el-button>
        <el-button type="primary" native-type="submit" :disabled="!canSubmit" :loading="action === 'save'">
          保存配置
        </el-button>
      </div>
    </el-form>
  </SettingsFormCard>
</template>
