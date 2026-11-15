<script setup lang="ts">
import { RouterLink } from 'vue-router'

/** 资讯中心左侧导航栏 */
const props = defineProps<{
  /** 待审数量，显示在「资讯审核」图标右上角，0 时不显示 */
  pendingCount: number
}>()

interface NavItem {
  routeName: string
  label: string
  icon: string
}

const NAV_ITEMS: NavItem[] = [
  { routeName: 'NewsAdminReview', label: '资讯审核', icon: 'newsAdmin-review' },
  { routeName: 'NewsAdminLibrary', label: '资讯库', icon: 'newsAdmin-library' },
  { routeName: 'PaperSearch', label: '论文搜索', icon: 'newsAdmin-search' },
  { routeName: 'NewsAdminChannels', label: '渠道代理配置', icon: 'newsAdmin-channels' },
  { routeName: 'NewsAdminSettings', label: '基础配置', icon: 'newsAdmin-settings' }
]
</script>

<template>
  <nav class="nav-rail" aria-label="资讯中心导航">
    <RouterLink
      v-for="item in NAV_ITEMS"
      :key="item.routeName"
      :to="{ name: item.routeName }"
      class="rail-item"
      active-class="is-active"
    >
      <span class="rail-pill">
        <SvgIcon :icon-class="item.icon" size="20" />
      </span>
      {{ item.label }}
      <span
        v-if="item.routeName === 'NewsAdminReview' && props.pendingCount > 0"
        class="rail-badge"
        :aria-label="`${props.pendingCount} 条待审`"
      >
        {{ props.pendingCount }}
      </span>
    </RouterLink>
  </nav>
</template>

<style scoped lang="scss">
.nav-rail {
  flex: 0 0 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--zg-space-lg);
  padding: var(--zg-space-xl) 0;
  background: var(--third-bg-color);
}

.rail-item {
  position: relative;
  flex: none;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--zg-space-xs);
  width: 100%;
  border-radius: var(--zg-radius-md);
  color: var(--second-color);
  font-size: var(--zg-text-sm);
  line-height: 16px;
  font-weight: var(--zg-weight-medium);
  text-decoration: none;
  white-space: nowrap;

  &:focus-visible {
    outline: 2px solid var(--focus-ring);
    outline-offset: 1px;
  }

  &.is-active {
    color: var(--main-color);
    font-weight: var(--zg-weight-semibold);

    .rail-pill {
      background: var(--selected-bg);
      color: var(--selected-text);
    }
  }

  &:hover .rail-pill::before {
    opacity: 0.08;
  }
}

/* 56 × 32 胶囊：选中时带底色，悬停时叠一层 currentColor 状态层 */
.rail-pill {
  position: relative;
  isolation: isolate;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 32px;
  overflow: hidden;
  border-radius: var(--zg-radius-pill);
  transition: background var(--zg-duration-base) var(--zg-ease-m3-standard);

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    z-index: -1;
    border-radius: inherit;
    background: currentColor;
    opacity: 0;
    pointer-events: none;
    transition: opacity var(--zg-duration-base) var(--zg-ease-m3-standard);
  }

  :deep(.svg-icon) {
    cursor: inherit;
  }
}

.rail-badge {
  position: absolute;
  top: -4px;
  left: 42px;
  min-width: 16px;
  height: 16px;
  padding: 0 var(--zg-space-xs);
  border-radius: var(--zg-radius-md);
  background: var(--badge-bg);
  color: var(--badge-text);
  font-size: var(--zg-text-xs);
  line-height: 16px;
  font-weight: var(--zg-weight-medium);
  text-align: center;
  font-variant-numeric: tabular-nums;
}

@media (prefers-reduced-motion: reduce) {
  .rail-pill,
  .rail-pill::before {
    transition: none;
  }
}
</style>
