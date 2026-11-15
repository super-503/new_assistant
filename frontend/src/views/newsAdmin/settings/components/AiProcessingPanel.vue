<script setup lang="ts">
import AiFieldPanel from './AiFieldPanel.vue'
import AiModelPanel from './AiModelPanel.vue'
</script>

<template>
  <div class="ai-processing-panel">
    <AiModelPanel />
    <AiFieldPanel />
  </div>
</template>

<style scoped lang="scss">
.ai-processing-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--zg-space-2xl);
}
</style>
