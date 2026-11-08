<script setup lang="ts">
import { computed, ref } from 'vue'
import type { NewsPlayback } from '@/api/newsItem'
import { EMBED_FRAME_SANDBOX } from './embedFrame'

/**
 * 视频播放区（16:9 黑底）：
 * EMBED 内嵌播放器；FILE 用 <video> 播放，海报优先取 playback 自带的封面；
 * 没有播放地址或视频文件加载失败时显示资讯封面，点击在新标签页打开原文。
 */
const props = defineProps<{
  playback: NewsPlayback | null
  /** 资讯封面地址；没有时为 null */
  cover: string | null
  /** 原文链接 */
  link: string
  /** 资讯标题，用于 iframe 标题和链接的无障碍名称 */
  title: string
}>()

const failedUrl = ref<string>()
const poster = computed(() => props.playback?.coverUrl ?? props.cover ?? undefined)
</script>

<template>
  <div class="news-playback">
    <iframe
      v-if="props.playback?.kind === 'EMBED'"
      :src="props.playback.url"
      :title="props.title"
      :sandbox="EMBED_FRAME_SANDBOX"
      loading="lazy"
      allow="fullscreen; picture-in-picture"
      allowfullscreen
    ></iframe>
    <video
      v-else-if="props.playback?.kind === 'FILE' && props.playback.url !== failedUrl"
      :src="props.playback.url"
      :poster="poster"
      @error="failedUrl = props.playback.url"
      controls
      preload="metadata"
      playsinline
    ></video>
    <a
      v-else
      class="playback-cover"
      :href="props.link"
      target="_blank"
      rel="noopener noreferrer"
      :aria-label="`在来源平台观看：${props.title}`"
      title="在来源平台观看"
    >
      <img v-if="props.cover" :src="props.cover" alt="" referrerpolicy="no-referrer" />
      <span class="play-mark" aria-hidden="true">
        <SvgIcon icon-class="newsAdmin-play" size="32" />
      </span>
    </a>
  </div>
</template>

<style scoped lang="scss">
.news-playback {
  position: relative;
  aspect-ratio: 16 / 9;
  /* clip 不形成滚动容器，作为 grid 子项时最小高度仍按 16:9 计算 */
  overflow: clip;
  border-radius: var(--zg-radius-lg);
  background: var(--media-backdrop);

  iframe,
  video,
  .playback-cover {
    display: block;
    width: 100%;
    height: 100%;
    border: 0;
  }
}

.playback-cover {
  position: relative;
  color: var(--media-overlay-text);

  img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: contain;
    transition: transform 400ms var(--zg-ease-m3-standard);
  }
  &:hover img {
    transform: scale(1.03);
  }
  &:focus-visible {
    outline: 2px solid var(--focus-ring);
    outline-offset: -2px;
  }
}

.play-mark {
  position: absolute;
  top: 50%;
  left: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--media-overlay-scrim);
  transform: translate(-50%, -50%);
}
</style>
