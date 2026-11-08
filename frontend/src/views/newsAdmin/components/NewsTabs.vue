<script setup lang="ts">
/** 资讯中心的一级标签页：44px 高，选中项主色文字加 3px 指示条，可带数量 */
export interface NewsTab {
  key: string
  label: string
  /** 数量徽标；不传则不显示 */
  count?: number
}

const props = defineProps<{
  tabs: NewsTab[]
  /** tablist 的无障碍名称，如「审核分类」 */
  label: string
}>()

/** 当前选中的标签 key */
const activeKey = defineModel<string>({ required: true })
</script>

<template>
  <div class="news-tabs" role="tablist" :aria-label="props.label">
    <button
      v-for="tab in props.tabs"
      :key="tab.key"
      type="button"
      role="tab"
      class="news-tab"
      :class="{ 'is-active': tab.key === activeKey }"
      :aria-selected="tab.key === activeKey"
      @click="activeKey = tab.key"
    >
      <span class="tab-label">
        {{ tab.label }}
        <span v-if="tab.count !== undefined" class="tab-count">{{ tab.count }}</span>
      </span>
      <span class="tab-indicator" aria-hidden="true"></span>
    </button>
  </div>
</template>

<style scoped lang="scss">
.news-tabs {
  display: flex;
  border-bottom: 1px solid var(--surface-border);
}

.news-tab {
  position: relative;
  isolation: isolate;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 44px;
  padding: 0 20px;
  overflow: hidden;
  border: 0;
  background: transparent;
  color: var(--second-color);
  font-family: inherit;
  font-size: var(--zg-text-base);
  font-weight: var(--zg-weight-medium);
  letter-spacing: 0.1px;
  cursor: pointer;

  /* 悬停状态层 */
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    z-index: -1;
    background: currentColor;
    opacity: 0;
    pointer-events: none;
    transition: opacity var(--zg-duration-base) var(--zg-ease-m3-standard);
  }
  &:hover::before {
    opacity: 0.08;
  }
  &:focus-visible {
    outline: 2px solid var(--focus-ring);
    outline-offset: -2px;
  }

  &.is-active {
    color: var(--brand-indigo);

    .tab-indicator {
      transform: scaleX(1);
    }
    .tab-count {
      background: var(--brand-indigo);
      color: var(--on-brand);
    }
  }
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: var(--zg-space-md);
}

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 var(--zg-space-sm);
  border-radius: var(--zg-radius-pill);
  background: var(--surface-container-highest);
  color: var(--second-color);
  font-size: var(--zg-text-xs);
  font-weight: var(--zg-weight-medium);
  font-variant-numeric: tabular-nums;
  transition: background var(--zg-duration-base) var(--zg-ease-m3-standard);
}

.tab-indicator {
  position: absolute;
  right: 20px;
  bottom: 0;
  left: 20px;
  height: 3px;
  border-radius: 3px 3px 0 0;
  background: var(--brand-indigo);
  transform: scaleX(0);
  transition: transform 250ms var(--zg-ease-m3-standard);
}
</style>
