<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import type { FormInstance, FormItemRule, FormRules, InputInstance } from 'element-plus'
import { NewsApiError } from '@/api/newsAdminRequest'
import type { NewsAiFieldOutputType } from '@/api/newsItem'
import {
  createAiFieldApi,
  updateAiFieldApi,
  type NewsAiFieldInput,
  type NewsAiFieldSetting,
  type NewsAiFieldSource
} from '@/api/newsSetting'
import { useNewsSettingStore } from '@/stores/newsAdmin/newsSettingStore'
import NewsTagChip from '../../components/NewsTagChip.vue'
import { confirmDiscard, notifySuccess } from '../../newsNotify'

/**
 * AI 字段编辑弹窗（560px）：字段名称与字段标识并排、来源字段与输出类型并排、选项、提示词、开关两列。
 * 锁定项按字段返回的 fieldKeyLocked / outputTypeLocked / promptOnly 变淡且不可改。
 */
const settingStore = useNewsSettingStore()

const SOURCE_OPTIONS: { value: NewsAiFieldSource; label: string }[] = [
  { value: 'BODY', label: '正文' },
  { value: 'TITLE', label: '标题' },
  { value: 'TITLE_BODY', label: '标题 + 正文' }
]

const OUTPUT_TYPE_OPTIONS: { value: NewsAiFieldOutputType; label: string }[] = [
  { value: 'TEXT', label: '文本' },
  { value: 'NUMBER', label: '数字' },
  { value: 'SINGLE', label: '单选' },
  { value: 'MULTI', label: '多选' }
]

/** 字段标识格式，与后端校验一致 */
const FIELD_KEY_PATTERN = /^[a-z][a-z0-9_]*$/

/** 两列排布的开关 */
type SwitchKey = 'multiline' | 'required' | 'editable' | 'enabled' | 'showAsTag'

const visible = ref(false)
/** 编辑中的字段；新增时为 null */
const editingField = ref<NewsAiFieldSetting | null>(null)
const isSubmitting = ref(false)
/** 保存失败的原因 */
const submitError = ref('')
const optionDraft = ref('')
const optionError = ref('')
const optionInputRef = ref<InputInstance>()

const createEmptyForm = () => ({
  name: '',
  fieldKey: '',
  source: 'BODY' as NewsAiFieldSource,
  outputType: 'TEXT' as NewsAiFieldOutputType,
  multiline: false,
  options: [] as string[],
  showAsTag: false,
  prompt: '',
  required: false,
  editable: true,
  enabled: true
})

const form = reactive(createEmptyForm())
/** 打开时的表单值，用于判断是否有未保存的修改 */
let initialSnapshot = JSON.stringify(form)

const formRef = ref<FormInstance>()

const isChoice = computed(() => form.outputType === 'SINGLE' || form.outputType === 'MULTI')
const promptOnly = computed(() => editingField.value?.promptOnly ?? false)
const fieldKeyLocked = computed(() => promptOnly.value || (editingField.value?.fieldKeyLocked ?? false))
const outputTypeLocked = computed(() => promptOnly.value || (editingField.value?.outputTypeLocked ?? false))
const isDirty = computed(() => optionDraft.value.trim() !== '' || JSON.stringify(form) !== initialSnapshot)

const switches = computed(() => {
  const items: { key: SwitchKey; label: string }[] = []
  if (form.outputType === 'TEXT') items.push({ key: 'multiline', label: '多行' })
  items.push(
    { key: 'required', label: '必填' },
    { key: 'editable', label: '允许人工修改' },
    { key: 'enabled', label: '启用' }
  )
  if (isChoice.value) items.push({ key: 'showAsTag', label: '显示为标签' })
  return items
})

/** 字段标识唯一：与其他字段比对 */
const validateFieldKeyUnique: FormItemRule['validator'] = (_rule, value: string, callback) => {
  const duplicated = (settingStore.aiFields ?? []).some(
    (field) => field.fieldKey === value.trim() && field.id !== editingField.value?.id
  )
  callback(duplicated ? new Error('字段标识已存在') : undefined)
}

const rules: FormRules = {
  name: [
    { required: true, whitespace: true, message: '请填写字段名称', trigger: 'blur' },
    { max: 32, message: '字段名称不能超过 32 个字符', trigger: 'blur' }
  ],
  fieldKey: [
    { required: true, whitespace: true, message: '请填写字段标识', trigger: 'blur' },
    { pattern: FIELD_KEY_PATTERN, message: '小写字母开头，仅含小写字母、数字和下划线', trigger: 'blur' },
    { max: 64, message: '字段标识不能超过 64 个字符', trigger: 'blur' },
    { validator: validateFieldKeyUnique, trigger: 'blur' }
  ],
  prompt: [
    { required: true, whitespace: true, message: '请填写提示词', trigger: 'blur' },
    { max: 2000, message: '提示词不能超过 2000 个字符', trigger: 'blur' }
  ]
}

const addOption = () => {
  const option = optionDraft.value.trim()
  if (!option) return false
  if (form.options.includes(option)) {
    optionError.value = '该选项已存在'
    return false
  }
  form.options.push(option)
  optionDraft.value = ''
  optionError.value = ''
  optionInputRef.value?.focus()
  return true
}

const onOptionEnter = (event: Event) => {
  if (event instanceof KeyboardEvent && event.isComposing) return
  event.preventDefault()
  addOption()
}

const removeOption = (index: number) => {
  form.options.splice(index, 1)
  optionError.value = ''
  optionInputRef.value?.focus()
}

/** 输出类型变化时，清掉新类型不支持的配置 */
const onOutputTypeChange = () => {
  if (form.outputType !== 'TEXT') form.multiline = false
  if (!isChoice.value) {
    form.showAsTag = false
    form.options = []
    optionDraft.value = ''
    optionError.value = ''
  }
}

const open = (field: NewsAiFieldSetting | null) => {
  editingField.value = field
  submitError.value = ''
  optionDraft.value = ''
  optionError.value = ''
  Object.assign(
    form,
    field
      ? {
          name: field.name,
          fieldKey: field.fieldKey,
          source: field.source,
          outputType: field.outputType,
          multiline: field.multiline,
          options: [...field.options],
          showAsTag: field.showAsTag,
          prompt: field.prompt,
          required: field.required,
          editable: field.editable,
          enabled: field.enabled
        }
      : createEmptyForm()
  )
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
  if (optionDraft.value.trim() && !addOption()) return
  const input: NewsAiFieldInput = {
    name: form.name.trim(),
    fieldKey: form.fieldKey.trim(),
    source: form.source,
    outputType: form.outputType,
    multiline: form.multiline,
    options: [...form.options],
    showAsTag: form.showAsTag,
    prompt: form.prompt.trim(),
    required: form.required,
    editable: form.editable,
    enabled: form.enabled
  }
  submitError.value = ''
  isSubmitting.value = true
  try {
    if (editingField.value) await updateAiFieldApi(editingField.value.id, input)
    else await createAiFieldApi(input)
  } catch (error) {
    if (!(error instanceof NewsApiError)) throw error
    submitError.value = error.message
    return
  } finally {
    isSubmitting.value = false
  }
  visible.value = false
  notifySuccess('已保存，仅影响后续加工')
  await settingStore.refreshAiFields()
}

defineExpose({ open })
</script>

<template>
  <el-dialog
    v-model="visible"
    class="dark-dialog news-admin-dialog ai-field-dialog"
    :title="editingField ? '编辑字段' : '添加字段'"
    width="560px"
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
      <div class="form-grid">
        <el-form-item label="字段名称" prop="name" :class="{ 'is-locked': promptOnly }">
          <el-input v-model="form.name" :disabled="promptOnly" maxlength="32" />
        </el-form-item>
        <el-form-item label="字段标识" prop="fieldKey" :class="{ 'is-locked': fieldKeyLocked }">
          <el-input v-model="form.fieldKey" :disabled="fieldKeyLocked" maxlength="64" />
        </el-form-item>
      </div>

      <div class="form-grid">
        <el-form-item label="来源字段" :class="{ 'is-locked': promptOnly }">
          <el-select v-model="form.source" :disabled="promptOnly" popper-class="news-admin-popper">
            <el-option v-for="item in SOURCE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="输出类型" :class="{ 'is-locked': outputTypeLocked }">
          <el-select
            v-model="form.outputType"
            :disabled="outputTypeLocked"
            popper-class="news-admin-popper"
            @change="onOutputTypeChange"
          >
            <el-option v-for="item in OUTPUT_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </div>

      <el-form-item
        v-if="isChoice"
        label="选项"
        :for="promptOnly ? undefined : 'ai-field-option'"
        :error="optionError"
        :class="{ 'is-locked': promptOnly }"
      >
        <div class="option-editor">
          <div v-if="!promptOnly" class="option-entry">
            <el-input
              id="ai-field-option"
              ref="optionInputRef"
              v-model="optionDraft"
              placeholder="输入一个选项"
              @input="optionError = ''"
              @keydown.enter="onOptionEnter"
            />
            <el-button plain :disabled="!optionDraft.trim()" @click="addOption">添加</el-button>
          </div>
          <div v-if="form.options.length > 0" class="option-tags">
            <NewsTagChip
              v-for="(option, index) in form.options"
              :key="option"
              class="option-tag"
              :label="option"
              :color-index="form.showAsTag && !promptOnly ? index : undefined"
            >
              <button
                v-if="!promptOnly"
                type="button"
                class="remove-option"
                :aria-label="`删除选项：${option}`"
                @click="removeOption(index)"
              >
                <SvgIcon icon-class="newsAdmin-close" size="14" />
              </button>
            </NewsTagChip>
          </div>
        </div>
      </el-form-item>

      <el-form-item label="提示词" prop="prompt">
        <el-input v-model="form.prompt" type="textarea" :rows="5" resize="vertical" maxlength="2000" />
      </el-form-item>

      <div class="switch-grid">
        <div v-for="item in switches" :key="item.key" class="switch-row" :class="{ 'is-locked': promptOnly }">
          <span :id="`ai-field-${item.key}`" class="switch-label">{{ item.label }}</span>
          <el-switch v-model="form[item.key]" :disabled="promptOnly" :aria-labelledby="`ai-field-${item.key}`" />
        </div>
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
@use '../../components/newsMixins' as *;

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: start;
  gap: 0 var(--zg-space-xl);
}

.is-locked {
  opacity: 0.6;

  :deep(.el-switch.is-disabled) {
    opacity: 1;
  }
}

.option-editor {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  width: 100%;
  gap: var(--zg-space-md);
}

.option-entry,
.option-tags {
  display: flex;
  align-items: center;
  gap: var(--zg-space-md);
}

.option-tags {
  flex-wrap: wrap;
}

.option-tags .option-tag {
  max-width: 100%;
  height: auto;
  min-height: 28px;
  gap: var(--zg-space-xs);
  white-space: normal;
  overflow-wrap: anywhere;
  line-height: 16px;
}

.remove-option {
  @include icon-button(20px);
  color: inherit;
}

@media (hover: hover) {
  .option-tag:not(:hover):not(:focus-within) .remove-option {
    opacity: 0;
  }
}

.switch-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 var(--zg-space-2xl);
}

.switch-row {
  display: flex;
  align-items: center;
  gap: var(--zg-space-lg);
  height: 40px;
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
