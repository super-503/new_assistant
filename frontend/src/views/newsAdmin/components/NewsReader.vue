<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElImageViewer } from 'element-plus'
import hljs from 'highlight.js'
import hljsThemeCss from 'highlight.js/styles/github-dark.css?inline'
import { EMBED_FRAME_SANDBOX } from './embedFrame'
import { NEWS_READER_STYLE } from './newsReaderStyle'

/**
 * 阅读组件：审核详情的「原文」和资讯库整页详情共用。
 * 后端清洗过的正文 HTML 渲染进 Shadow DOM，保留的行内样式只在其中生效；
 * 插入前在惰性的 template 里完成标签增强，iframe 在加载前就带上 sandbox。
 */
const props = withDefaults(
  defineProps<{
    /** 后端清洗过的正文 HTML */
    html: string
    /** 正文为空时的提示 */
    emptyText?: string
  }>(),
  { emptyText: '正文暂不可获取' }
)

/** 正文里裸露的网址；遇到空白、引号、尖括号和中文标点即结束 */
const BARE_URL_PATTERN = /https?:\/\/[^\s<>"'，。；：！？、（）【】《》「」]+/g
/** 网址末尾紧跟的英文标点不算网址的一部分 */
const TRAILING_PUNCTUATION = /[.,;:!?)\]}]+$/

const isEmpty = computed(() => props.html.trim() === '')

const hostRef = ref<HTMLElement>()
/** Shadow DOM 内的正文容器，挂载后创建 */
let articleEl: HTMLElement | null = null

/** 图片查看器 */
const isViewerOpen = ref(false)
const viewerUrls = ref<string[]>([])
const viewerIndex = ref(0)

const createExternalLink = (href: string) => {
  const link = document.createElement('a')
  link.href = href
  link.textContent = href
  return link
}

/** 把一段文本拆成「文本 + 链接」片段 */
const transformTextToLinkedFragment = (text: string) => {
  const fragment = document.createDocumentFragment()
  let cursor = 0
  for (const match of text.matchAll(BARE_URL_PATTERN)) {
    const url = match[0].replace(TRAILING_PUNCTUATION, '')
    fragment.append(text.slice(cursor, match.index), createExternalLink(url))
    cursor = match.index + url.length
  }
  fragment.append(text.slice(cursor))
  return fragment
}

/** 裸露网址转链接：跳过已在链接、代码里的文本 */
const linkifyBareUrls = (root: DocumentFragment) => {
  // 01- 先收集含网址的文本节点，遍历结束后再替换，避免边遍历边改树
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT)
  const textNodes: Text[] = []
  for (let node = walker.nextNode(); node; node = walker.nextNode()) {
    const textNode = node as Text
    if (textNode.parentElement?.closest('a, pre, code')) continue
    if (textNode.data.includes('http://') || textNode.data.includes('https://')) textNodes.push(textNode)
  }
  // 02- 逐个替换成文本与链接交替的片段
  textNodes.forEach((textNode) => textNode.replaceWith(transformTextToLinkedFragment(textNode.data)))
}

/** 标签增强，在正文插入 Shadow DOM 之前执行 */
const enhanceBody = (root: DocumentFragment) => {
  // 01- 裸露网址转链接，之后统一处理所有链接
  linkifyBareUrls(root)
  // 02- 链接在新标签页打开，不离开资讯中心
  root.querySelectorAll('a[href]').forEach((link) => {
    link.setAttribute('target', '_blank')
    link.setAttribute('rel', 'noopener noreferrer')
  })
  // 03- 图片懒加载；点击放大由正文容器上的事件委托处理
  root.querySelectorAll('img').forEach((image) => {
    image.loading = 'lazy'
    image.referrerPolicy = 'no-referrer'
  })
  // 04- iframe：sandbox、懒加载、宽度 100%、默认 16:9（尺寸属性去掉，比例由阅读样式给出）
  root.querySelectorAll('iframe').forEach((frame) => {
    frame.setAttribute('sandbox', EMBED_FRAME_SANDBOX)
    frame.setAttribute('loading', 'lazy')
    frame.setAttribute('allowfullscreen', '')
    frame.removeAttribute('width')
    frame.removeAttribute('height')
  })
  // 05- 视频、音频带播放控件；视频的封面沿用正文里的 poster
  root.querySelectorAll('video, audio').forEach((media) => {
    media.setAttribute('controls', '')
    media.setAttribute('preload', 'metadata')
  })
  root.querySelectorAll('video').forEach((video) => video.setAttribute('playsinline', ''))
  // 06- 代码块：公众号正文用 <br> 分行，先还原成换行符，高亮按文本重写内容时才不会丢掉行
  root.querySelectorAll('pre br').forEach((br) => br.replaceWith('\n'))
  // 07- 代码块高亮：pre 里只有一个 code 时高亮 code，否则高亮 pre 本身
  root.querySelectorAll('pre').forEach((pre) => {
    const onlyChild = pre.children.length === 1 ? pre.firstElementChild : null
    hljs.highlightElement(onlyChild instanceof HTMLElement && onlyChild.tagName === 'CODE' ? onlyChild : pre)
  })
}

const renderBody = () => {
  const template = document.createElement('template')
  template.innerHTML = props.html
  enhanceBody(template.content)
  articleEl!.replaceChildren(template.content)
}

const onArticleClick = (event: MouseEvent) => {
  const target = event.target
  if (!(target instanceof HTMLImageElement)) return
  event.preventDefault()
  const images = Array.from(articleEl!.querySelectorAll('img'))
  viewerUrls.value = images.map((img) => img.currentSrc || img.src)
  viewerIndex.value = images.indexOf(target)
  isViewerOpen.value = true
}

onMounted(() => {
  const shadow = hostRef.value!.attachShadow({ mode: 'open' })
  const style = document.createElement('style')
  // 阅读样式排在高亮主题之后，覆盖主题的底色与内边距
  style.textContent = hljsThemeCss + NEWS_READER_STYLE
  articleEl = document.createElement('article')
  articleEl.className = 'reader'
  articleEl.addEventListener('click', onArticleClick)
  shadow.append(style, articleEl)
  renderBody()
})

watch(() => props.html, renderBody)
</script>

<template>
  <div class="news-reader">
    <div v-show="!isEmpty" ref="hostRef"></div>
    <div v-if="isEmpty" class="reader-empty" role="note">
      <SvgIcon icon-class="newsAdmin-info" size="20" />
      <span>{{ props.emptyText }}</span>
    </div>
    <ElImageViewer
      v-if="isViewerOpen"
      :url-list="viewerUrls"
      :initial-index="viewerIndex"
      teleported
      hide-on-click-modal
      @close="isViewerOpen = false"
    />
  </div>
</template>

<style scoped lang="scss">
.news-reader {
  min-width: 0;
}

.reader-empty {
  display: flex;
  gap: var(--zg-space-lg);
  padding: var(--zg-space-xl);
  border-radius: var(--zg-radius-lg);
  background: var(--status-pending-container);
  color: var(--on-status-pending-container);
  font-size: var(--zg-text-base);
  line-height: 20px;

  :deep(.svg-icon) {
    flex: none;
    cursor: default;
  }
}
</style>
