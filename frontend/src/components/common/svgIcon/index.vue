<template>
  <svg v-if="!props.svgUrl" :class="svgClass" :aria-hidden="props.ariaHidden ?? 'true'" :style="{ fontSize: fontSize, color: color }">
    <use :xlink:href="iconName"></use>
  </svg>

  <!-- 远程SVG处理 -->
  <div
    v-else
    :class="svgClass"
    :aria-hidden="props.ariaHidden ?? 'true'"
    :style="{ fontSize: fontSize, color: color }"
    v-html="svgContent"
  ></div>
</template>

<script lang="ts" setup>
import { computed, ref, onMounted, watch } from 'vue'

interface IProps {
  iconClass?: string
  svgUrl?: string
  className?: string
  size?: number | string
  color?: string
  ariaHidden?: boolean | 'true' | 'false'
}

const prefix = 'icon'

defineOptions({
  name: 'SvgIcon'
})

const props = defineProps<IProps>()
const svgContent = ref('')

// 验证props，确保至少提供了一种图标来源
watch([() => props.iconClass, () => props.svgUrl], () => {
  if (!props.iconClass && !props.svgUrl) {
    console.warn('SvgIcon组件必须提供iconClass或svgUrl属性')
  }
})

// 加载远程SVG
const loadSvgFromUrl = async (url: string) => {
  try {
    const response = await fetch(url)
    if (!response.ok) {
      throw new Error(`Failed to load SVG: ${response.statusText}`)
    }
    const svgText = await response.text()
    // 处理SVG内容，移除可能的命名空间冲突
    return svgText
      .replace(/xmlns=".*?"/, '')
      .replace(/width=".*?"/, '')
      .replace(/height=".*?"/, '')
  } catch (error) {
    console.error('加载SVG失败:', error)
    return ''
  }
}

// 当svgUrl变化时重新加载
watch(
  () => props.svgUrl,
  async (newUrl) => {
    if (newUrl) {
      svgContent.value = await loadSvgFromUrl(newUrl)
    }
  }
)

// 初始加载
onMounted(() => {
  if (props.svgUrl) {
    loadSvgFromUrl(props.svgUrl).then((content) => {
      svgContent.value = content
    })
  }
})

// 原有功能的计算属性
const iconName = computed(() => (props.iconClass ? `#${prefix}-${props.iconClass}` : ''))
const svgClass = computed(() => ['svg-icon', props.className])

// 新增计算属性：处理fontSize
const fontSize = computed(() => {
  if (!props.size) return 'inherit'

  const size = props.size.toString()
  // 检查是否已包含单位 (例如: px, pt, em, rem, %, vw, vh, vmin, vmax)
  const unitRegex = /[a-zA-Z%]+$/
  if (unitRegex.test(size)) {
    // 如果已包含单位，直接使用
    return size
  } else {
    // 如果没有单位，默认使用 px
    return `${size}px`
  }
})
</script>

<style lang="scss" scoped>
.svg-icon {
  width: 1em;
  height: 1em;
  vertical-align: -0.15em;
  fill: currentColor;
  overflow: hidden;
  // 手型
  cursor: pointer;

  // 确保远程SVG也应用相同的样式
  svg {
    width: 100%;
    height: 100%;
    fill: currentColor;
  }
}
</style>
