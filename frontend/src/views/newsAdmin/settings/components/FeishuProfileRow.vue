<script setup lang="ts">
import type { Component } from 'vue'

defineProps<{
  name: string
  detail: string
  avatarUrl?: string | null
  icon?: Component
  square?: boolean
}>()
</script>

<template>
  <div class="profile-row">
    <el-avatar
      class="profile-avatar"
      :shape="square ? 'square' : 'circle'"
      :size="36"
      :src="avatarUrl?.startsWith('https://') ? avatarUrl : undefined"
      aria-hidden="true"
    >
      <el-icon v-if="icon" :size="22"><component :is="icon" /></el-icon>
      <span v-else>{{ Array.from(name)[0] }}</span>
    </el-avatar>
    <div class="profile-copy">
      <strong>{{ name }}</strong>
      <span>{{ detail }}</span>
    </div>
    <div v-if="$slots.default" class="profile-actions">
      <slot />
    </div>
  </div>
</template>

<style scoped lang="scss">
.profile-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--zg-space-lg);
  min-height: 64px;
  padding: var(--zg-space-lg) var(--zg-space-xl);
}

.profile-avatar {
  flex: none;
  background: var(--surface-container-highest);
  color: var(--second-color);
  font-size: var(--zg-text-xl);
}

.profile-copy {
  display: grid;
  flex: 1 1 112px;
  gap: var(--zg-space-xs);
  min-width: 0;
  overflow-wrap: anywhere;

  strong {
    font-size: var(--zg-text-base);
    font-weight: var(--zg-weight-semibold);
  }

  span {
    color: var(--second-color);
    font-size: var(--zg-text-md);
  }
}

.profile-actions {
  display: flex;
  flex-wrap: wrap;
  margin-left: auto;

  :deep(.el-button + .el-button) {
    margin-left: 0;
  }
}
</style>
