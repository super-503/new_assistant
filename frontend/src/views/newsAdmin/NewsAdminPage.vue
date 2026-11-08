<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useNewsAdminStore } from '@/stores/newsAdmin/newsAdminStore'
import NewsNavRail from './components/NewsNavRail.vue'
import './newsAdminElement.scss'

const newsAdminStore = useNewsAdminStore()
const { pendingCount } = storeToRefs(newsAdminStore)

const ready = ref(false)

onMounted(async () => {
  await newsAdminStore.loadPendingCount()
  ready.value = true
})
</script>

<template>
  <div class="news-admin-page">
    <header class="news-topbar">
      <h1>
        <img class="news-logo" src="/news-logo.svg" width="28" height="28" alt="" />
        资讯推荐助手
      </h1>
      <a href="/logout">退出登录</a>
    </header>
    <div v-if="ready" class="news-body">
      <NewsNavRail :pending-count="pendingCount" />
      <div class="news-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.news-admin-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: var(--second-bg-color);
  color: var(--main-color);
  font-size: var(--zg-text-base);
  line-height: 20px;
  letter-spacing: 0.25px;
  color-scheme: dark;
}

.news-admin-page > .news-topbar {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 52px;
  padding: 0 var(--zg-space-2xl);
  background: var(--third-bg-color);
}

.news-topbar h1 {
  display: flex;
  align-items: center;
  gap: var(--zg-space-md);
  margin: 0;
  font-size: var(--zg-text-lg);
}

.news-logo {
  flex: none;
}

.news-topbar a {
  color: var(--second-color);
}

.news-body {
  flex: 1;
  min-height: 0;
  display: flex;
  overflow: hidden;
}

/* 各页面自己管理滚动区域 */
.news-content {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  overflow: hidden;
}

@media (prefers-reduced-motion: reduce) {
  .news-admin-page :deep(*),
  .news-admin-page :deep(*::before),
  .news-admin-page :deep(*::after) {
    animation: none !important;
    transition: none !important;
  }
}
</style>
