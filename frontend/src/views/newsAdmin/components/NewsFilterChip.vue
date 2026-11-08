<script setup lang="ts">
import { computed } from 'vue'

/** 筛选 chip 的一个选项 */
export interface NewsFilterOption {
  value: string
  label: string
}

/**
 * 筛选 chip：宽度跟随文字，点击展开下拉。
 * 未筛选时显示维度名；选中后显示值并弹出对勾。
 * multiple 为多选，下拉不随点击关闭；neutral 用于排序这类始终有值的选择，显示当前值、不显示选中态。
 */
const props = withDefaults(
  defineProps<{
    /** 维度名，未筛选时显示 */
    label: string
    options: NewsFilterOption[]
    multiple?: boolean
    neutral?: boolean
  }>(),
  { multiple: false, neutral: false }
)

/** 选中的值；单选最多一个，未筛选为空数组 */
const selected = defineModel<string[]>({ required: true })

/** 「全部」项的值，选中时清空筛选 */
const ALL_VALUE = '__all__'

const isActive = computed(() => !props.neutral && selected.value.length > 0)

const chipText = computed(() => {
  if (selected.value.length === 0) return props.label
  return props.options
    .filter((option) => selected.value.includes(option.value))
    .map((option) => option.label)
    .join('、')
})

const onCommand = (value: string) => {
  if (value === ALL_VALUE) {
    selected.value = []
    return
  }
  if (!props.multiple) {
    selected.value = [value]
    return
  }
  selected.value = selected.value.includes(value)
    ? selected.value.filter((item) => item !== value)
    : props.options.map((option) => option.value).filter((item) => item === value || selected.value.includes(item))
}
</script>

<template>
  <el-dropdown
    trigger="click"
    placement="bottom-start"
    popper-class="news-admin-popper"
    :hide-on-click="!props.multiple"
    :max-height="320"
    @command="onCommand"
  >
    <button type="button" class="filter-chip" :class="{ 'is-active': isActive }">
      <SvgIcon v-if="isActive" class="chip-check" icon-class="newsAdmin-check" size="16" />
      <span class="chip-text">{{ chipText }}</span>
      <SvgIcon class="chip-arrow" icon-class="newsAdmin-arrow-drop-down" size="18" />
    </button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item v-if="!props.neutral" :command="ALL_VALUE">
          <span class="chip-option" :class="{ 'is-selected': selected.length === 0 }">
            <SvgIcon class="option-check" icon-class="newsAdmin-check" size="16" />
            全部
          </span>
        </el-dropdown-item>
        <el-dropdown-item v-for="option in props.options" :key="option.value" :command="option.value">
          <span class="chip-option" :class="{ 'is-selected': selected.includes(option.value) }">
            <SvgIcon class="option-check" icon-class="newsAdmin-check" size="16" />
            {{ option.label }}
          </span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped lang="scss">
@use './newsMixins' as *;

@include filter-chip-styles;
</style>
