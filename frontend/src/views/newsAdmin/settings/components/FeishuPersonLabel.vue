<script setup lang="ts">
defineProps<{ name: string; avatarUrl?: string | null }>()
</script>

<template>
  <span class="person-label">
    <el-avatar :size="22" :src="avatarUrl?.startsWith('https://') ? avatarUrl : undefined" aria-hidden="true">
      {{ Array.from(name)[0] }}
    </el-avatar>
    <span class="person-name">{{ name }}</span>
  </span>
</template>

<style scoped lang="scss">
.person-label {
  display: inline-flex;
  align-items: center;
  gap: var(--zg-space-sm);
  min-width: 0;
  max-width: 100%;
  vertical-align: middle;
  line-height: 22px;
}
.person-label :deep(.el-avatar) {
  flex-shrink: 0;
  background: var(--surface-container-highest);
  color: var(--second-color);
  font-size: var(--zg-text-sm);
}
.person-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
