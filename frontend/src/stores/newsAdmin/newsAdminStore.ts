import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getPendingCountApi } from '@/api/newsItem'

/** 资讯中心跨页面共享的状态 */
export const useNewsAdminStore = defineStore('newsAdmin', () => {
  /** 待审数量：显示为导航栏「资讯审核」的角标，0 时不显示；审核页加载和处理后同步 */
  const pendingCount = ref(0)

  /** 进入资讯中心时加载待审数量 */
  const loadPendingCount = async () => {
    try {
      pendingCount.value = await getPendingCountApi()
    } catch (e) {
      console.error(e)
    }
  }

  return { pendingCount, loadPendingCount }
})
