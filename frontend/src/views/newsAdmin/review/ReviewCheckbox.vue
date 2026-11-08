<script setup lang="ts">
/** 审核队列的复选框：32px 点击区，勾选时描画勾号；mixed 为部分选中（全选框） */
const props = defineProps<{
  state: 'none' | 'all' | 'mixed'
  /** 无障碍名称，如「全选」「选择：标题」 */
  label: string
}>()

const emit = defineEmits<{ toggle: [] }>()
</script>

<template>
  <button
    type="button"
    role="checkbox"
    class="review-checkbox"
    :aria-checked="props.state === 'all' ? 'true' : props.state === 'mixed' ? 'mixed' : 'false'"
    :aria-label="props.label"
    :title="props.label"
    @click="emit('toggle')"
  >
    <span class="checkbox-box" :class="{ 'is-on': props.state !== 'none' }" aria-hidden="true">
      <svg v-if="props.state === 'all'" class="checkbox-mark" width="14" height="14" viewBox="0 0 24 24">
        <path d="M5 12.5l4.5 4.5L19 7.5" />
      </svg>
      <svg v-else-if="props.state === 'mixed'" width="14" height="14" viewBox="0 0 24 24">
        <path d="M6 12h12" />
      </svg>
    </span>
  </button>
</template>

<style scoped lang="scss">
@use '../components/newsMixins' as *;

.review-checkbox {
  @include icon-button;
  color: var(--main-color);
}

.checkbox-box {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border: 2px solid var(--second-color);
  border-radius: var(--zg-radius-xs);
  transition:
    background var(--zg-duration-fast) var(--zg-ease-m3-standard),
    border-color var(--zg-duration-fast) var(--zg-ease-m3-standard);

  &.is-on {
    border-color: var(--brand-indigo);
    background: var(--brand-indigo);
  }

  svg {
    fill: none;
    stroke: var(--on-brand);
    stroke-width: 3.2;
    stroke-linecap: round;
    stroke-linejoin: round;
  }
}

/* 勾选时描画勾号 */
.checkbox-mark path {
  stroke-dasharray: 24;
  animation: news-draw var(--zg-duration-base) var(--zg-ease-m3-standard);
}

@keyframes news-draw {
  from {
    stroke-dashoffset: 24;
  }
  to {
    stroke-dashoffset: 0;
  }
}
</style>
