<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { setRejectReasonEnabledApi, type NewsRejectReasonSetting } from '@/api/newsSetting'
import { useNewsSettingStore } from '@/stores/newsAdmin/newsSettingStore'
import RejectReasonDialog from './RejectReasonDialog.vue'

/** 拒绝理由列表：搜索、新增、启用开关、编辑 */
const settingStore = useNewsSettingStore()
const { rejectReasons } = storeToRefs(settingStore)

const keyword = ref('')
const dialogRef = ref<InstanceType<typeof RejectReasonDialog>>()

const visibleReasons = computed(() => {
  const query = keyword.value.trim()
  const reasons = rejectReasons.value ?? []
  return query ? reasons.filter((reason) => reason.name.includes(query)) : reasons
})

const toggleEnabled = (reason: NewsRejectReasonSetting) => async () => {
  await setRejectReasonEnabledApi(reason.id, !reason.enabled)
  await settingStore.refreshRejectReasons()
  return true
}

onMounted(() => void settingStore.ensureRejectReasons())
</script>

<template>
  <div class="reason-panel">
    <div class="panel-toolbar">
      <el-input
        v-model="keyword"
        class="news-search-input toolbar-search"
        placeholder="搜索拒绝理由"
        aria-label="搜索拒绝理由"
      >
        <template #prefix>
          <SvgIcon icon-class="newsAdmin-search" size="18" />
        </template>
      </el-input>
      <el-button type="primary" class="add-button" @click="dialogRef?.open(null)">
        <SvgIcon icon-class="newsAdmin-add" size="18" />
        新增理由
      </el-button>
    </div>

    <div v-if="rejectReasons" class="reason-table" role="table" aria-label="拒绝理由">
      <div class="table-head" role="row">
        <span role="columnheader">拒绝理由</span>
        <span role="columnheader">启用</span>
        <span role="columnheader">操作</span>
      </div>
      <div v-for="reason in visibleReasons" :key="reason.id" class="reason-row" role="row">
        <span class="reason-name" role="cell">{{ reason.name }}</span>
        <span role="cell">
          <el-switch
            :model-value="reason.enabled"
            :before-change="toggleEnabled(reason)"
            :aria-label="`启用：${reason.name}`"
          />
        </span>
        <span role="cell">
          <el-button text type="primary" @click="dialogRef?.open(reason)">编辑</el-button>
        </span>
      </div>
      <p v-if="visibleReasons.length === 0" class="table-empty">
        {{ keyword.trim() ? '没有匹配的拒绝理由' : '暂无拒绝理由' }}
      </p>
    </div>

    <RejectReasonDialog ref="dialogRef" />
  </div>
</template>

<style scoped lang="scss">
.reason-panel {
  display: flex;
  flex-direction: column;
  gap: var(--zg-space-xl);
  animation: panel-enter 300ms var(--zg-ease-m3-emphasized-decelerate) both;
}

.panel-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--zg-space-lg);
}

.toolbar-search {
  flex: 0 1 240px;
  min-width: 160px;
}

.add-button {
  margin-left: auto;
}

.reason-table {
  --reason-columns: minmax(240px, 4fr) repeat(2, minmax(120px, 1fr));

  min-width: 560px;
  border-radius: var(--zg-radius-lg);
  background: var(--third-bg-color);
}

.table-head,
.reason-row {
  display: grid;
  grid-template-columns: var(--reason-columns);
  gap: var(--zg-space-lg);
  align-items: center;
  padding: 0 20px;
}

.table-head {
  height: 40px;
  color: var(--second-color);
  font-size: var(--zg-text-sm);
  font-weight: var(--zg-weight-medium);
  letter-spacing: 0.5px;
}

.reason-row {
  min-height: 48px;
  border-top: 1px solid var(--surface-border);
}

.reason-name {
  min-width: 0;
  overflow-wrap: anywhere;
}

.table-empty {
  margin: 0;
  padding: var(--zg-space-3xl) 20px;
  border-top: 1px solid var(--surface-border);
  color: var(--second-color);
  text-align: center;
}

@keyframes panel-enter {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}
</style>
