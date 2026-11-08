<script setup lang="ts">
import { onMounted, ref } from 'vue'
import NewsSnackbar from '../components/NewsSnackbar.vue'
import LibraryDetail from './LibraryDetail.vue'
import LibraryList from './LibraryList.vue'
import LibrarySideTree from './LibrarySideTree.vue'
import { provideLibrary } from './useLibrary'

/** 资讯库：左侧 220px 分类栏 + 右侧列表；整页详情覆盖在右侧区域之上 */
const library = provideLibrary()

/** 底部提示；每条一个新 key，重新计时 */
const snackbar = ref<{ key: number; message: string } | null>(null)
let snackbarSeq = 0

const onNotify = (message: string) => {
  snackbar.value = { key: ++snackbarSeq, message }
}

onMounted(() => {
  void library.loadTree()
  void library.loadFilterOptions()
  void library.loadItems(true)
})
</script>

<template>
  <div class="library-page">
    <LibrarySideTree />
    <div class="library-main">
      <LibraryList />
      <LibraryDetail v-if="library.isDetailOpen.value" @notify="onNotify" />
    </div>
    <NewsSnackbar
      v-if="snackbar"
      :key="snackbar.key"
      :message="snackbar.message"
      :action-text="null"
      @close="snackbar = null"
    />
  </div>
</template>

<style scoped lang="scss">
.library-page {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  overflow: hidden;
}

.library-main {
  position: relative;
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  overflow: hidden;
}
</style>
