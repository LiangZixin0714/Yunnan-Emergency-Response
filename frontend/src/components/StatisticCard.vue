<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'

const props = defineProps<{
  title: string
  value: number
  unit?: string
  icon?: string
  loading?: boolean
  error?: boolean
}>()

const emit = defineEmits<{
  (e: 'retry'): void
}>()

const displayValue = ref(0)
let animationFrameId: number | null = null

function easeOutCubic(t: number): number {
  return 1 - Math.pow(1 - t, 3)
}

function animateTo(target: number): void {
  if (animationFrameId !== null) {
    cancelAnimationFrame(animationFrameId)
    animationFrameId = null
  }
  if (target === 0) {
    displayValue.value = 0
    return
  }
  const start = displayValue.value
  const diff = target - start
  if (diff === 0) return
  const duration = 1500
  const startTime = performance.now()
  function step(now: number): void {
    const elapsed = now - startTime
    const progress = Math.min(elapsed / duration, 1)
    displayValue.value = Math.round(start + diff * easeOutCubic(progress))
    if (progress < 1) {
      animationFrameId = requestAnimationFrame(step)
    } else {
      displayValue.value = target
      animationFrameId = null
    }
  }
  animationFrameId = requestAnimationFrame(step)
}

watch(() => props.value, (newVal) => {
  if (!props.loading && !props.error) {
    animateTo(newVal)
  }
}, { immediate: true })

onBeforeUnmount(() => {
  if (animationFrameId !== null) {
    cancelAnimationFrame(animationFrameId)
  }
})
</script>

<template>
  <div class="statistic-card">
    <el-card shadow="hover">
      <div class="statistic-card__content">
        <div class="statistic-card__info">
          <div class="statistic-card__title">{{ title }}</div>
          <el-skeleton v-if="loading" :rows="1" animated />
          <div v-else-if="error" class="statistic-card__error">
            <span>数据加载失败</span>
            <el-button size="small" link type="primary" @click="emit('retry')">重试</el-button>
          </div>
          <div v-else class="statistic-card__value">
            {{ displayValue }}<span v-if="unit" class="statistic-card__unit">{{ unit }}</span>
          </div>
        </div>
        <div v-if="icon" class="statistic-card__icon">
          <el-icon :size="40" color="var(--color-primary)"><component :is="icon" /></el-icon>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.statistic-card__content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.statistic-card__title {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-sm);
}
.statistic-card__value {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.statistic-card__unit {
  font-size: 14px;
  font-weight: 400;
  color: var(--color-text-secondary);
  margin-left: 4px;
}
.statistic-card__icon {
  opacity: 0.8;
}
.statistic-card__error {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-danger);
  font-size: 14px;
}
</style>