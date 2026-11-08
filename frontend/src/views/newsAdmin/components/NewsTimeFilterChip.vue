<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import type { DatePickerInstance } from 'element-plus'
import { getOffsetDate } from './newsFormat'

/** 发布日期区间 [起始日, 结束日]，yyyy-MM-dd，两端都包含 */
export type NewsDatePeriod = [string, string]

/**
 * 时间筛选 chip（审核和资讯库共用），外观同 NewsFilterChip：
 * 下拉为「全部时间 / 今天 / 近 7 天 / 近 30 天 / 自定义」，选「自定义」弹出日期范围选择器。
 * 选中后 chip 显示所选值，如「近 7 天」或「11-01 至 11-04」。
 */
const period = defineModel<NewsDatePeriod | null>({ required: true })

interface PresetOption {
  value: string
  label: string
  /** 含今天在内往前的天数 */
  days: number
}

const PRESETS: PresetOption[] = [
  { value: 'today', label: '今天', days: 1 },
  { value: '7', label: '近 7 天', days: 7 },
  { value: '30', label: '近 30 天', days: 30 }
]
const ALL_VALUE = 'all'
const CUSTOM_VALUE = 'custom'

const pickerRef = ref<DatePickerInstance>()
/** 日期范围选择器的值，选「自定义」时以当前区间打开 */
const pickerValue = ref<NewsDatePeriod>()

const toPresetPeriod = (days: number): NewsDatePeriod => [getOffsetDate(1 - days), getOffsetDate(0)]

/** 当前区间对应的预设；不是预设时为 undefined */
const activePreset = computed(() => {
  const current = period.value
  if (current === null) return undefined
  return PRESETS.find((preset) => {
    const [start, end] = toPresetPeriod(preset.days)
    return current[0] === start && current[1] === end
  })
})

const selectedValue = computed(() => {
  if (period.value === null) return ALL_VALUE
  return activePreset.value?.value ?? CUSTOM_VALUE
})

const chipText = computed(() => {
  const current = period.value
  if (current === null) return '全部时间'
  return activePreset.value?.label ?? `${current[0].slice(5)} 至 ${current[1].slice(5)}`
})

const onCommand = (value: string) => {
  if (value === ALL_VALUE) {
    period.value = null
    return
  }
  if (value === CUSTOM_VALUE) {
    pickerValue.value = period.value ?? undefined
    // 等下拉菜单收起后再打开日期面板
    void nextTick(() => pickerRef.value!.handleOpen())
    return
  }
  period.value = toPresetPeriod(PRESETS.find((preset) => preset.value === value)!.days)
}

const onPickerChange = (value: NewsDatePeriod | null) => {
  if (value !== null) period.value = value
}
</script>

<template>
  <span class="time-filter">
    <el-dropdown trigger="click" placement="bottom-start" popper-class="news-admin-popper" @command="onCommand">
      <button type="button" class="filter-chip" :class="{ 'is-active': period !== null }">
        <SvgIcon v-if="period !== null" class="chip-check" icon-class="newsAdmin-check" size="16" />
        <span class="chip-text">{{ chipText }}</span>
        <SvgIcon class="chip-arrow" icon-class="newsAdmin-arrow-drop-down" size="18" />
      </button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item :command="ALL_VALUE">
            <span class="chip-option" :class="{ 'is-selected': selectedValue === ALL_VALUE }">
              <SvgIcon class="option-check" icon-class="newsAdmin-check" size="16" />
              全部时间
            </span>
          </el-dropdown-item>
          <el-dropdown-item v-for="preset in PRESETS" :key="preset.value" :command="preset.value">
            <span class="chip-option" :class="{ 'is-selected': selectedValue === preset.value }">
              <SvgIcon class="option-check" icon-class="newsAdmin-check" size="16" />
              {{ preset.label }}
            </span>
          </el-dropdown-item>
          <el-dropdown-item :command="CUSTOM_VALUE">
            <span class="chip-option" :class="{ 'is-selected': selectedValue === CUSTOM_VALUE }">
              <SvgIcon class="option-check" icon-class="newsAdmin-check" size="16" />
              自定义
            </span>
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
    <!-- 日期范围选择器只用它的弹出面板：输入框叠在 chip 下方且不可见、不可聚焦，面板贴着 chip 弹出 -->
    <span class="picker-anchor" inert>
      <el-date-picker
        ref="pickerRef"
        v-model="pickerValue"
        type="daterange"
        value-format="YYYY-MM-DD"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        placement="bottom-start"
        popper-class="news-admin-popper"
        :clearable="false"
        @change="onPickerChange"
      />
    </span>
  </span>
</template>

<style scoped lang="scss">
@use './newsMixins' as *;

@include filter-chip-styles;

.time-filter {
  position: relative;
  display: inline-flex;
}

.picker-anchor {
  position: absolute;
  inset: 0;
  overflow: hidden;
  opacity: 0;
  pointer-events: none;
}
</style>
