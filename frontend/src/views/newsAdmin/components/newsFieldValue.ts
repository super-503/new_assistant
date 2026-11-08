import type { NewsFieldValue, NewsItemField } from '@/api/newsItem'

/**
 * AI 字段值的判空、比较与提交前规整，审核详情和资讯库标签修改共用。
 */

/** 空值：null、空白文本、空数组 */
export const isEmptyFieldValue = (value: NewsFieldValue) => {
  if (value === null) return true
  if (Array.isArray(value)) return value.length === 0
  return typeof value === 'string' && value.trim() === ''
}

/** 两个值是否等价：空值互相等价，多选不计顺序，文本忽略首尾空白 */
export const isSameFieldValue = (a: NewsFieldValue, b: NewsFieldValue) => {
  if (isEmptyFieldValue(a) || isEmptyFieldValue(b)) return isEmptyFieldValue(a) && isEmptyFieldValue(b)
  if (Array.isArray(a) && Array.isArray(b)) return a.length === b.length && a.every((item) => b.includes(item))
  if (typeof a === 'string' && typeof b === 'string') return a.trim() === b.trim()
  return a === b
}

/** 提交前规整：空值为 null，文本去首尾空白，多选按选项顺序排列 */
export const normalizeFieldValue = (field: NewsItemField, value: NewsFieldValue): NewsFieldValue => {
  if (isEmptyFieldValue(value)) return null
  if (Array.isArray(value)) return field.options.map((option) => option.value).filter((item) => value.includes(item))
  return typeof value === 'string' ? value.trim() : value
}

/** 单选、多选的字段 */
export const isChoiceField = (field: NewsItemField) => field.outputType === 'SINGLE' || field.outputType === 'MULTI'

/** 值的展示文本：多选用顿号连接 */
export const formatFieldValue = (value: NewsFieldValue) => {
  if (value === null) return ''
  return Array.isArray(value) ? value.join('、') : String(value)
}

/** 值里选中的选项（多选为数组，单选为单个值） */
export const getSelectedOptions = (value: NewsFieldValue): string[] => {
  if (Array.isArray(value)) return value
  return typeof value === 'string' && value !== '' ? [value] : []
}

/** 选项的标签配色序号：只有显示为标签的字段用配色 */
export const getOptionColorIndex = (field: NewsItemField, optionValue: string) => {
  if (!field.showAsTag) return null
  return field.options.find((option) => option.value === optionValue)?.colorIndex ?? null
}

/** 点选一个选项后的新值：多选切换，单选再点一次取消 */
export const toggleFieldOption = (field: NewsItemField, value: NewsFieldValue, optionValue: string): NewsFieldValue => {
  const selected = getSelectedOptions(value)
  if (field.outputType === 'MULTI') {
    return selected.includes(optionValue) ? selected.filter((item) => item !== optionValue) : [...selected, optionValue]
  }
  return selected.includes(optionValue) ? null : optionValue
}

/** 必填未填的提示：选择类为「请选择 X」，其余为「请填写 X」 */
export const getRequiredMessage = (field: NewsItemField) => `${isChoiceField(field) ? '请选择' : '请填写'}${field.name}`
