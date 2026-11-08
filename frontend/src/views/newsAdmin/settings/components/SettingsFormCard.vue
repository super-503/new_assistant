<script setup lang="ts">
const props = defineProps<{
  title: string
  icon: string
  status?: string
  active?: boolean
}>()
</script>

<template>
  <div class="settings-form-card">
    <header class="card-header">
      <div class="card-heading">
        <span class="card-icon"><SvgIcon :icon-class="props.icon" size="20" /></span>
        <h2>{{ props.title }}</h2>
      </div>
      <span v-if="props.status" class="card-status" :class="{ 'is-active': props.active }">
        {{ props.status }}
      </span>
    </header>
    <slot />
  </div>
</template>

<style scoped lang="scss">
.settings-form-card {
  --form-padding: clamp(12px, 2.4vw, 24px);

  width: 100%;
  max-width: 760px;
  overflow: hidden;
  border: 1px solid var(--surface-border);
  border-radius: var(--zg-radius-lg);
  background: var(--third-bg-color);
}

.card-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--zg-space-md);
  padding: var(--zg-space-xl) var(--form-padding);
  border-bottom: 1px solid var(--surface-border);
}

.card-heading {
  display: flex;
  align-items: center;
  gap: var(--zg-space-lg);

  h2 {
    margin: 0;
    font-size: var(--zg-text-lg);
    line-height: 22px;
    font-weight: var(--zg-weight-semibold);
  }
}

.card-icon {
  display: grid;
  flex: none;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: var(--zg-radius-md);
  background: var(--surface-container-high);
  color: var(--second-color);

  :deep(.svg-icon) {
    cursor: inherit;
  }
}

.card-status {
  display: inline-flex;
  flex: none;
  align-items: center;
  gap: var(--zg-space-sm);
  padding: 2px var(--zg-space-md);
  border-radius: var(--zg-radius-pill);
  background: var(--surface-container-high);
  color: var(--second-color);
  font-size: var(--zg-text-sm);
  white-space: nowrap;

  &::before {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: currentColor;
    content: '';
  }

  &.is-active {
    background: var(--status-approved-container);
    color: var(--on-status-approved-container);
  }
}

.settings-form-card :deep(.settings-fields) {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 240px), 1fr));
  gap: var(--zg-space-xl);
  padding: var(--zg-space-xl) var(--form-padding);

  > .el-form-item {
    min-width: 0;
    margin: 0;
  }

  > .settings-field-wide {
    grid-column: 1 / -1;
  }
}

.settings-form-card :deep(.settings-option) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--zg-space-lg);
  min-height: 48px;
  margin-inline: var(--form-padding);
  border-top: 1px solid var(--surface-border);
  color: var(--second-color);
  font-size: var(--zg-text-md);
}

.settings-form-card :deep(.settings-feedback) {
  margin: 0;
  padding: var(--zg-space-xl) var(--form-padding);
  color: var(--second-color);
}

.settings-form-card :deep(.settings-actions) {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--zg-space-md);
  padding: var(--zg-space-lg) var(--form-padding);
  border-top: 1px solid var(--surface-border);
  background: var(--surface-container);

  .el-button + .el-button {
    margin-left: 0;
  }
}
</style>
