import type { NewsFeishuCardSettings, NewsFeishuCardTemplate } from '@/api/newsSetting'

export const VARIABLE_TYPES = [
  { key: 'text', label: '文本 · string' },
  { key: 'number', label: '数字 · number' },
  { key: 'boolean', label: '布尔 · boolean' },
  { key: 'url', label: '链接 · URL' },
  { key: 'object', label: '对象 · object' },
  { key: 'array', label: '列表 · array' },
  { key: 'json', label: 'JSON 值' }
] as const

type VariableType = (typeof VARIABLE_TYPES)[number]['key']

export const VARIABLE_SOURCES = [
  { key: '${title}', type: 'text', label: '卡片标题' },
  { key: '${content}', type: 'text', label: '日报正文' },
  { key: '${created_at}', type: 'text', label: '生成时间（ISO 8601）' },
  { key: '${count}', type: 'number', label: '资讯条数' },
  { key: '${items}', type: 'array', label: '资讯列表' }
] as const

type VariableSource = (typeof VARIABLE_SOURCES)[number]['key']

export interface FeishuVariableDraft {
  rowId: number
  name: string
  type: VariableType
  source: 'fixed' | VariableSource
  value: string
}

export interface FeishuTemplateDraft {
  id: string
  version: string
  variables: FeishuVariableDraft[]
}

export type FeishuCardDraft = Omit<NewsFeishuCardSettings, 'template'> & { template: FeishuTemplateDraft | null }

let nextRowId = 0

export const newVariable = (): FeishuVariableDraft => ({
  rowId: ++nextRowId, name: '', type: 'text', source: 'fixed', value: ''
})

export const newTemplate = (): FeishuTemplateDraft => ({
  id: '', version: '', variables: [{ ...newVariable(), name: 'content', source: '${content}' }]
})

const isObject = (value: unknown): value is Record<string, unknown> =>
  value !== null && typeof value === 'object' && !Array.isArray(value)

const inferType = (value: unknown): VariableType => {
  if (typeof value === 'string') return 'text'
  if (typeof value === 'number') return 'number'
  if (typeof value === 'boolean') return 'boolean'
  if (Array.isArray(value)) return 'array'
  return isObject(value) ? 'object' : 'json'
}

const hydrateTemplate = (template: NewsFeishuCardTemplate): FeishuTemplateDraft => {
  const values: unknown = JSON.parse(template.variablesJson)
  const variables = Object.entries(isObject(values) ? values : {}).map(([name, value]) => {
    const source = VARIABLE_SOURCES.find((entry) => entry.key === value)
    const type = VARIABLE_TYPES.find((entry) => entry.key === template.variableTypes?.[name])?.key
    return {
      ...newVariable(), name, type: source?.type ?? type ?? inferType(value), source: source?.key ?? 'fixed',
      value: typeof value === 'string' && type !== 'json' ? value : JSON.stringify(value)
    } satisfies FeishuVariableDraft
  })
  return { id: template.id, version: template.version ?? '', variables }
}

export const hydrateCardDraft = (card: NewsFeishuCardSettings): FeishuCardDraft => ({
  ...card, template: card.template ? hydrateTemplate(card.template) : null
})

const variableValue = (row: FeishuVariableDraft): unknown => {
  if (row.source !== 'fixed') return row.source
  if (row.type === 'text') return row.value
  if (row.type === 'url' && !row.value.trim().startsWith('{')) {
    const url = new URL(row.value.trim())
    if (['https:', 'http:', 'lark:'].includes(url.protocol)) return row.value.trim()
    throw new Error()
  }
  const value: unknown = JSON.parse(row.value)
  if (typeof value === 'number' && !Number.isFinite(value)) throw new Error()
  if (row.type === 'json' || row.type === inferType(value) || (row.type === 'url' && isObject(value))) return value
  throw new Error()
}

export const serializeCardDraft = (draft: FeishuCardDraft): { card: NewsFeishuCardSettings } | { error: string } => {
  const title = draft.title.trim()
  if (!title) return { error: '请填写卡片标题' }
  const template = draft.template
  if (!template) return { card: { ...draft, title, template: null } }
  if (!/^[a-zA-Z0-9_-]{1,128}$/.test(template.id)) return { error: '请填写有效的飞书卡片模板 ID' }
  if (template.version && !/^\d+\.\d+\.\d+$/.test(template.version)) {
    return { error: '请填写形如 1.0.0 的模板版本' }
  }

  const values = new Map<string, unknown>()
  const types = new Map<string, VariableType>()
  for (const row of template.variables) {
    const name = row.name.trim()
    if (!name) return { error: '请填写卡片变量名' }
    if (values.has(name)) return { error: `变量名「${name}」重复` }
    try {
      values.set(name, variableValue(row))
    } catch {
      return { error: `变量「${name}」的取值与类型不匹配` }
    }
    types.set(name, row.type)
  }
  const variablesJson = JSON.stringify(Object.fromEntries(values))
  if (variablesJson.length > 8000) return { error: '卡片变量的内容不能超过 8000 个字符' }
  return {
    card: {
      ...draft, title,
      template: { id: template.id, version: template.version, variablesJson, variableTypes: Object.fromEntries(types) }
    }
  }
}
