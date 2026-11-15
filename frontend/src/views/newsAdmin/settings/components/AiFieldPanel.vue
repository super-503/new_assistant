<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { deleteAiFieldApi, setAiFieldEnabledApi, sortAiFieldsApi, type NewsAiFieldSetting } from '@/api/newsSetting'
import { useNewsSettingStore } from '@/stores/newsAdmin/newsSettingStore'
import { confirmAction, notifySuccess } from '../../newsNotify'
import AiFieldDialog from './AiFieldDialog.vue'

/**
 * AI 字段列表：拖动把手排序（把手聚焦时可用上下方向键移动），系统 / 内置标签，启用开关，
 * 编辑，普通字段可删除。列表顺序就是详情页的展示顺序。
 */
const settingStore = useNewsSettingStore()
const { aiFields, aiModel } = storeToRefs(settingStore)

/** 列表行：拖动时先在本地调整顺序，保存后以接口返回为准 */
const rows = ref<NewsAiFieldSetting[]>([])
watch(aiFields, (fields) => (rows.value = fields ? [...fields] : []), { immediate: true })

const dialogRef = ref<InstanceType<typeof AiFieldDialog>>()
const panelRef = ref<HTMLElement>()

/** 拖动中的字段 id，以及当前悬停的目标行 */
const draggingId = ref<number | null>(null)
const overId = ref<number | null>(null)

/** 把字段移到目标位置并保存顺序 */
const moveField = async (fromIndex: number, toIndex: number) => {
  if (fromIndex === toIndex || toIndex < 0 || toIndex >= rows.value.length) return
  const next = [...rows.value]
  const [moved] = next.splice(fromIndex, 1)
  next.splice(toIndex, 0, moved)
  rows.value = next
  try {
    await sortAiFieldsApi(next.map((field) => field.id))
  } finally {
    await settingStore.refreshAiFields()
  }
}

const onDragStart = (event: DragEvent, field: NewsAiFieldSetting) => {
  if (!event.dataTransfer) return
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', String(field.id))
  const row = (event.currentTarget as HTMLElement).closest('.field-row')
  if (row) event.dataTransfer.setDragImage(row, 20, 24)
  draggingId.value = field.id
}

const onDragOver = (event: DragEvent, field: NewsAiFieldSetting) => {
  if (draggingId.value === null) return
  event.preventDefault()
  overId.value = field.id
}

const onDrop = (field: NewsAiFieldSetting) => {
  if (draggingId.value === null) return
  const fromIndex = rows.value.findIndex((item) => item.id === draggingId.value)
  const toIndex = rows.value.findIndex((item) => item.id === field.id)
  draggingId.value = null
  overId.value = null
  void moveField(fromIndex, toIndex)
}

const onDragEnd = () => {
  draggingId.value = null
  overId.value = null
}

/** 方向键移动：行节点被移动后焦点会丢失，移动后把焦点放回该字段的把手 */
const onHandleKeydown = async (event: KeyboardEvent, field: NewsAiFieldSetting, index: number) => {
  if (event.key !== 'ArrowUp' && event.key !== 'ArrowDown') return
  event.preventDefault()
  const saving = moveField(index, event.key === 'ArrowUp' ? index - 1 : index + 1)
  await nextTick()
  panelRef.value?.querySelector<HTMLElement>(`[data-handle-id="${field.id}"]`)?.focus()
  await saving
}

/** 拖到目标行时的落点指示：从下往上拖显示在目标行上沿，反之下沿 */
const dropIndicatorClass = (field: NewsAiFieldSetting) => {
  if (draggingId.value === null || overId.value !== field.id || draggingId.value === field.id) return ''
  const fromIndex = rows.value.findIndex((item) => item.id === draggingId.value)
  const toIndex = rows.value.findIndex((item) => item.id === field.id)
  return fromIndex > toIndex ? 'is-drop-before' : 'is-drop-after'
}

const toggleEnabled = (field: NewsAiFieldSetting) => async () => {
  await setAiFieldEnabledApi(field.id, !field.enabled)
  await settingStore.refreshAiFields()
  return true
}

const remove = async (field: NewsAiFieldSetting) => {
  const confirmed = await confirmAction({
    title: `删除字段「${field.name}」？`,
    message: '删除后不再生成和展示该字段。',
    confirmText: '删除',
    danger: true
  })
  if (!confirmed) return
  await deleteAiFieldApi(field.id)
  notifySuccess(`已删除「${field.name}」`)
  await settingStore.refreshAiFields()
}

watch(
  () => aiModel.value?.configured,
  (configured) => {
    if (configured) void settingStore.ensureAiFields()
  },
  { immediate: true }
)
</script>

<template>
  <section ref="panelRef" class="ai-field-panel" aria-labelledby="ai-fields-title">
    <header class="panel-toolbar">
      <h2 id="ai-fields-title">AI 字段</h2>
      <el-button v-if="aiModel?.configured" type="primary" @click="dialogRef?.open(null)">
        <SvgIcon icon-class="newsAdmin-add" size="18" />
        添加字段
      </el-button>
    </header>

    <p v-if="aiModel && !aiModel.configured" class="panel-feedback">保存模型配置后即可管理 AI 字段。</p>
    <div v-else-if="aiModel?.configured && aiFields" class="field-table-scroll">
      <div class="field-table" role="table" aria-label="AI 字段">
        <div class="table-head" role="row">
          <span role="columnheader"><span class="sr-only">排序</span></span>
          <span role="columnheader">字段名称</span>
          <span role="columnheader">输出类型</span>
          <span role="columnheader">提示词</span>
          <span role="columnheader">启用</span>
          <span role="columnheader">操作</span>
        </div>
        <div
          v-for="(field, index) in rows"
          :key="field.id"
          class="field-row"
          :class="[dropIndicatorClass(field), { 'is-dragging': draggingId === field.id }]"
          role="row"
          @dragover="onDragOver($event, field)"
          @drop="onDrop(field)"
        >
          <span
            class="drag-handle"
            role="button"
            tabindex="0"
            draggable="true"
            title="拖动排序"
            :aria-label="`拖动排序：${field.name}，可用上下方向键移动`"
            :data-handle-id="field.id"
            @dragstart="onDragStart($event, field)"
            @dragend="onDragEnd"
            @keydown="onHandleKeydown($event, field, index)"
          >
            <SvgIcon icon-class="newsAdmin-drag-indicator" size="20" />
          </span>
          <span class="field-name" role="cell">
            <span class="name-text">{{ field.name }}</span>
            <span v-if="field.kind !== 'CUSTOM'" class="kind-tag">{{ field.kindDesc }}</span>
          </span>
          <span class="field-type" role="cell">{{ field.outputTypeLabel }}</span>
          <span class="field-prompt" role="cell">
            <el-tooltip
              :content="field.prompt"
              :trigger="['hover', 'focus']"
              placement="top-start"
              popper-class="news-admin-popper ai-field-prompt-popper"
            >
              <span class="prompt-preview" tabindex="0">{{ field.prompt }}</span>
            </el-tooltip>
          </span>
          <span role="cell">
            <el-switch
              :model-value="field.enabled"
              :disabled="field.promptOnly"
              :before-change="toggleEnabled(field)"
              :aria-label="`启用：${field.name}`"
            />
          </span>
          <span class="field-actions" role="cell">
            <el-button text type="primary" @click="dialogRef?.open(field)">编辑</el-button>
            <el-button v-if="field.deletable" text type="primary" @click="remove(field)">删除</el-button>
          </span>
        </div>
      </div>
    </div>

    <AiFieldDialog v-if="aiModel?.configured" ref="dialogRef" />
  </section>
</template>

<style scoped lang="scss">
@use '../../components/newsMixins' as *;

.ai-field-panel {
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-xl);
  animation: panel-enter 300ms var(--zg-ease-m3-emphasized-decelerate) both;
}

.panel-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--zg-space-lg);
  min-height: 32px;

  h2 {
    margin: 0;
    font-size: var(--zg-text-lg);
    font-weight: var(--zg-weight-semibold);
  }
}

.panel-feedback {
  margin: 0;
  padding: var(--zg-space-xl);
  border: 1px solid var(--surface-border);
  border-radius: var(--zg-radius-lg);
  background: var(--third-bg-color);
  color: var(--second-color);
}

.field-table-scroll {
  overflow-x: auto;
  border-radius: var(--zg-radius-lg);
}

.field-table {
  --field-columns: 32px minmax(120px, 1.5fr) minmax(120px, 1fr) minmax(160px, 3fr) repeat(2, minmax(120px, 1fr));

  min-width: 760px;
  border-radius: var(--zg-radius-lg);
  background: var(--third-bg-color);
}

.table-head,
.field-row {
  display: grid;
  grid-template-columns: var(--field-columns);
  gap: var(--zg-space-lg);
  align-items: center;
  padding: 0 20px 0 var(--zg-space-md);
}

.table-head {
  height: 40px;
  color: var(--second-color);
  font-size: var(--zg-text-sm);
  font-weight: var(--zg-weight-medium);
  letter-spacing: 0.5px;
}

.field-row {
  min-height: 48px;
  border-top: 1px solid var(--surface-border);
  transition: opacity var(--zg-duration-fast) var(--zg-ease-m3-standard);

  &.is-dragging {
    opacity: 0.4;
  }
  &.is-drop-before {
    box-shadow: inset 0 2px 0 var(--brand-indigo);
  }
  &.is-drop-after {
    box-shadow: inset 0 -2px 0 var(--brand-indigo);
  }
}

.drag-handle {
  @include icon-button;
  cursor: grab;

  &:active {
    cursor: grabbing;
  }
}

.field-name {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: var(--zg-space-md);
}

.name-text,
.prompt-preview {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kind-tag {
  flex: none;
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 var(--zg-space-sm);
  border-radius: var(--zg-radius-xs);
  background: var(--surface-container-highest);
  color: var(--second-color);
  font-size: var(--zg-text-xs);
  font-weight: var(--zg-weight-medium);
}

.field-type,
.field-prompt {
  color: var(--second-color);
}

.field-prompt {
  min-width: 0;
}

.prompt-preview {
  @include focus-ring;
  display: block;
}

:global(.news-admin-popper.ai-field-prompt-popper) {
  max-width: min(480px, calc(100vw - var(--zg-space-3xl)));
  max-height: 50vh;
  overflow: auto;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.field-actions {
  display: flex;
  gap: var(--zg-space-xs);

  .el-button + .el-button {
    margin-left: 0;
  }
}

@keyframes panel-enter {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}
</style>
