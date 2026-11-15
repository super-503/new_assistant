<script setup lang="ts">
import { VARIABLE_SOURCES, VARIABLE_TYPES, newVariable, type FeishuVariableDraft } from './feishuCardForm'

defineProps<{ disabled: boolean }>()
const variables = defineModel<FeishuVariableDraft[] | null>({ required: true })

const sourcesFor = (row: FeishuVariableDraft) => VARIABLE_SOURCES.filter((source) => source.type === row.type)
const typeLabel = (type: FeishuVariableDraft['type']) => VARIABLE_TYPES.find((entry) => entry.key === type)?.label
const updateType = (row: FeishuVariableDraft) => {
  if (row.source !== 'fixed' && !sourcesFor(row).some((source) => source.key === row.source)) row.source = 'fixed'
}
const remove = (id: number) => {
  if (variables.value) variables.value = variables.value.filter((row) => row.rowId !== id)
}
const add = () => {
  if (variables.value && variables.value.length < 100) variables.value = [...variables.value, newVariable()]
}
</script>

<template>
  <section class="variables public-variables" aria-labelledby="public-variables-title">
    <h4 id="public-variables-title">公共变量</h4>
    <div class="variable-grid variable-header" aria-hidden="true">
      <span>变量名</span><span>类型</span><span>取值</span>
    </div>
    <div v-for="source in VARIABLE_SOURCES" :key="source.key" class="variable-grid variable-row public-variable-row">
      <el-input :model-value="source.key" :aria-label="`公共变量名 ${source.key}`" readonly />
      <el-input :model-value="typeLabel(source.type)" :aria-label="`${source.key} 类型`" readonly />
      <div class="variable-value">
        <el-input :model-value="source.label" :aria-label="`${source.key} 取值`" readonly />
      </div>
    </div>
    <div class="variable-notes">
      <p>资讯列表包含标题和推送计划中选中且有值的字段。</p>
      <a href="https://open.feishu.cn/document/feishu-cards/feishu-card-overview" target="_blank" rel="noopener noreferrer">
        飞书模板发布与应用授权说明
      </a>
    </div>
  </section>

  <section v-if="variables" class="variables" aria-labelledby="template-variables-title">
    <h4 id="template-variables-title">卡片变量</h4>
    <div v-if="variables.length" class="variable-grid variable-header" aria-hidden="true">
      <span>变量名</span><span>类型</span><span>取值</span><span />
    </div>
    <div v-for="(row, index) in variables" :key="row.rowId" class="variable-grid variable-row">
      <el-input
        v-model="row.name"
        maxlength="128"
        placeholder="填写模板变量名"
        :aria-label="`变量 ${index + 1} 名称`"
        :disabled="disabled"
      />
      <el-select v-model="row.type" :aria-label="`变量 ${index + 1} 类型`" :disabled="disabled" popper-class="news-admin-popper" @change="updateType(row)">
        <el-option v-for="type in VARIABLE_TYPES" :key="type.key" :label="type.label" :value="type.key" />
      </el-select>
      <div class="variable-value">
        <el-select
          v-if="sourcesFor(row).length"
          v-model="row.source"
          :aria-label="`变量 ${index + 1} 取值来源`"
          :disabled="disabled"
          popper-class="news-admin-popper"
        >
          <el-option v-for="source in sourcesFor(row)" :key="source.key" :label="source.label" :value="source.key" />
          <el-option label="固定值" value="fixed" />
        </el-select>
        <template v-if="row.source === 'fixed'">
          <el-select v-if="row.type === 'boolean'" v-model="row.value" :aria-label="`变量 ${index + 1} 固定值`" :disabled="disabled" popper-class="news-admin-popper">
            <el-option label="true" value="true" />
            <el-option label="false" value="false" />
          </el-select>
          <el-input
            v-else
            v-model="row.value"
            :type="['text', 'array', 'object', 'json'].includes(row.type) ? 'textarea' : 'text'"
            :placeholder="row.type === 'url' ? '网址或 URL 对象' : '填写固定值'"
            :aria-label="`变量 ${index + 1} 固定值`"
            :disabled="disabled"
            :autosize="{ minRows: 1, maxRows: 4 }"
            maxlength="8000"
          />
        </template>
      </div>
      <el-button class="variable-remove" text type="danger" :disabled="disabled" :aria-label="`移除变量 ${index + 1}`" @click="remove(row.rowId)">
        <SvgIcon icon-class="newsAdmin-close" size="18" />
      </el-button>
    </div>
    <el-button plain :disabled="disabled || variables.length >= 100" @click="add">
      添加卡片变量
    </el-button>
  </section>
</template>

<style scoped lang="scss">
.variables {
  min-width: 0;
  container-type: inline-size;
}

.variables h4 {
  margin: 0 0 var(--zg-space-lg);
  font-size: var(--zg-text-base);
  font-weight: var(--zg-weight-semibold);
}

.variable-grid {
  display: grid;
  grid-template-columns: minmax(110px, 1fr) minmax(125px, 0.8fr) minmax(160px, 1.4fr) 40px;
  align-items: start;
  gap: var(--zg-space-md);
}

.variable-header,
.variable-row {
  margin-bottom: var(--zg-space-md);
}

.variable-header {
  color: var(--second-color);
  font-size: var(--zg-text-sm);
}

.variable-value {
  display: grid;
  min-width: 0;
  gap: var(--zg-space-xs);
}

.variable-remove {
  width: 40px;
  padding-inline: 0;
}

.variable-notes {
  color: var(--second-color);
  font-size: var(--zg-text-sm);

  p { margin: 0 0 var(--zg-space-xs); }
  a { color: var(--brand-indigo); }
}

@container (max-width: 500px) {
  .variable-header { display: none; }
  .variable-row {
    grid-template-columns: minmax(0, 1fr) auto;
    border-bottom: 1px solid var(--surface-border);
    padding-bottom: var(--zg-space-md);

    > :first-child { grid-column: 1; }
    > :nth-child(2), .variable-value { grid-column: 1 / -1; }
    > .variable-remove { grid-column: 2; grid-row: 1; }
  }

  .public-variable-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));

    > :nth-child(2) { grid-column: 2; grid-row: 1; }
  }
}
</style>
