<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { NewsApiError } from '@/api/newsAdminRequest'
import { previewPushPlanApi, type NewsPushPlanDefinition } from '@/api/newsPushPlan'
import type { NewsFeishuRecipient } from '@/api/newsSetting'
import { notifyError, notifySuccess } from '../../newsNotify'
import FeishuPersonLabel from './FeishuPersonLabel.vue'

const props = defineProps<{
  definition?: NewsPushPlanDefinition
  recipients: NewsFeishuRecipient[]
  disabled: boolean
}>()
const dialog = ref(false)
const sending = ref(false)
const selected = ref('')
const result = ref<{ type: 'success' | 'error'; message: string }>()
const targets = computed(() => props.recipients.filter((person) =>
  props.definition?.appId === person.appId && props.definition.recipientOpenIds.includes(person.openId)
))
const canSend = computed(() => !!props.definition && targets.value.some((person) => person.openId === selected.value))

const open = () => {
  selected.value = targets.value[0]?.openId ?? ''
  dialog.value = true
}

const send = async () => {
  if (!props.definition || !canSend.value || sending.value || props.disabled) return
  sending.value = true
  result.value = undefined
  try {
    await previewPushPlanApi(props.definition, selected.value)
    const person = targets.value.find((person) => person.openId === selected.value)
    result.value = { type: 'success', message: `已发送到${person?.name || '所选接收人'}的飞书单聊` }
    dialog.value = false
    notifySuccess('测试消息已发送')
  } catch (error) {
    if (!(error instanceof NewsApiError)) throw error
    result.value = { type: 'error', message: error.message }
    notifyError('测试发送失败，请查看错误详情')
  } finally {
    sending.value = false
  }
}

watch(() => props.definition, () => { result.value = undefined })
</script>

<template>
  <section class="plan-test" aria-label="计划测试发送">
    <div class="plan-test-actions">
      <el-button
        plain
        :disabled="disabled || !definition || !targets.length"
        :title="definition ? undefined : '选择飞书应用、推送人员和卡片后可测试发送'"
        @click="open"
      >测试发送</el-button>
      <slot />
    </div>
    <el-alert
      v-if="result && !dialog"
      class="plan-result"
      :title="result.type === 'success' ? '测试发送成功' : '测试发送失败'"
      :description="result.message"
      :type="result.type"
      show-icon
      :closable="false"
      role="status"
    />
    <el-dialog
      v-model="dialog"
      class="dark-dialog news-admin-dialog"
      title="测试发送"
      width="min(480px, calc(100vw - 24px))"
      :show-close="!sending"
      :close-on-click-modal="!sending"
      :close-on-press-escape="!sending"
    >
      <el-form label-position="top" :disabled="sending" @submit.prevent="send">
        <p>使用本计划的卡片和内容范围，向所选人员发送最多 3 条已审核资讯。正式推送记录单独保存。</p>
        <el-form-item label="接收人" required>
          <el-select v-model="selected" popper-class="news-admin-popper">
            <template #label="{ label, value }">
              <FeishuPersonLabel
                :name="label"
                :avatar-url="targets.find((person) => person.openId === value)?.avatarUrl"
              />
            </template>
            <el-option
              v-for="(person, index) in targets"
              :key="person.openId"
              :label="person.name || `接收人 ${index + 1}`"
              :value="person.openId"
            >
              <FeishuPersonLabel :name="person.name || `接收人 ${index + 1}`" :avatar-url="person.avatarUrl" />
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <el-alert
        v-if="result?.type === 'error'"
        class="plan-result"
        title="测试发送失败"
        :description="result.message"
        type="error"
        show-icon
        :closable="false"
        role="alert"
      />
      <template #footer>
        <el-button plain :disabled="sending" @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="sending" :disabled="disabled || !canSend || sending" @click="send">
          发送测试消息
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped lang="scss">
.plan-test {
  display: grid;
  gap: var(--zg-space-lg);
  width: 100%;
  min-width: 0;
}
.plan-test-actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: var(--zg-space-sm);
}
.plan-result :deep(.el-alert__description) {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
</style>
