<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import type { FormInstance, FormItemRule, FormRules } from 'element-plus'
import { NewsApiError } from '@/api/newsAdminRequest'
import { createRejectReasonApi, updateRejectReasonApi, type NewsRejectReasonSetting } from '@/api/newsSetting'
import { useNewsSettingStore } from '@/stores/newsAdmin/newsSettingStore'
import { confirmDiscard, notifySuccess } from '../../newsNotify'

/** 拒绝理由弹窗：理由名称（必填）和启用 */
const settingStore = useNewsSettingStore()

const visible = ref(false)
/** 编辑中的理由；新增时为 null */
const editingReason = ref<NewsRejectReasonSetting | null>(null)
const isSubmitting = ref(false)
/** 保存失败的原因 */
const submitError = ref('')

const form = reactive({ name: '', enabled: true })
let initialSnapshot = JSON.stringify(form)
const formRef = ref<FormInstance>()

const isDirty = computed(() => JSON.stringify(form) !== initialSnapshot)

/** 名称不能与其他理由重复 */
const validateNameUnique: FormItemRule['validator'] = (_rule, value: string, callback) => {
  const duplicated = (settingStore.rejectReasons ?? []).some(
    (reason) => reason.name === value.trim() && reason.id !== editingReason.value?.id
  )
  callback(duplicated ? new Error('名称已存在') : undefined)
}

const rules: FormRules = {
  name: [
    { required: true, whitespace: true, message: '请填写理由名称', trigger: 'blur' },
    { max: 64, message: '理由名称不能超过 64 个字符', trigger: 'blur' },
    { validator: validateNameUnique, trigger: 'blur' }
  ]
}

const open = (reason: NewsRejectReasonSetting | null) => {
  editingReason.value = reason
  submitError.value = ''
  Object.assign(form, reason ? { name: reason.name, enabled: reason.enabled } : { name: '', enabled: true })
  initialSnapshot = JSON.stringify(form)
  formRef.value?.clearValidate()
  visible.value = true
}

const onBeforeClose = async (done: () => void) => {
  if (isDirty.value && !(await confirmDiscard())) return
  done()
}

const close = () => onBeforeClose(() => (visible.value = false))

const submit = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().then(
    () => true,
    () => false
  )
  if (!valid) return
  const input = { name: form.name.trim(), enabled: form.enabled }
  submitError.value = ''
  isSubmitting.value = true
  try {
    if (editingReason.value) await updateRejectReasonApi(editingReason.value.id, input)
    else await createRejectReasonApi(input)
  } catch (error) {
    if (!(error instanceof NewsApiError)) throw error
    submitError.value = error.message
    return
  } finally {
    isSubmitting.value = false
  }
  visible.value = false
  notifySuccess(editingReason.value ? `已保存「${input.name}」` : `已新增「${input.name}」`)
  await settingStore.refreshRejectReasons()
}

defineExpose({ open })
</script>

<template>
  <el-dialog
    v-model="visible"
    class="dark-dialog news-admin-dialog reject-reason-dialog"
    :title="editingReason ? '编辑拒绝理由' : '新增拒绝理由'"
    width="440px"
    append-to-body
    :show-close="false"
    :before-close="onBeforeClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      require-asterisk-position="right"
      @submit.prevent
    >
      <el-form-item label="理由名称" prop="name">
        <el-input v-model="form.name" maxlength="64" @keyup.enter="submit" />
      </el-form-item>
      <div class="switch-row">
        <span id="reject-reason-enabled" class="switch-label">启用</span>
        <el-switch v-model="form.enabled" aria-labelledby="reject-reason-enabled" />
      </div>
      <p v-if="submitError" class="submit-error" role="alert">{{ submitError }}</p>
    </el-form>

    <template #footer>
      <el-button text type="primary" @click="close">取消</el-button>
      <el-button text type="primary" :loading="isSubmitting" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.switch-row {
  display: flex;
  align-items: center;
  gap: var(--zg-space-xl);
  min-height: 40px;
}

.switch-label {
  flex: 1;
  min-width: 0;
}

.submit-error {
  margin: var(--zg-space-md) 0 0;
  color: var(--status-rejected);
  font-size: var(--zg-text-sm);
  line-height: 16px;
}
</style>
