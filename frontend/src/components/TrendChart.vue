<script setup lang="ts">
import { computed } from 'vue'
import ChartContainer from '@/components/ChartContainer.vue'
import type { DashboardTrend } from '@/types/dashboard'
import type { EChartsOption } from 'echarts'

const props = defineProps<{
  trendData: DashboardTrend | null
  loading?: boolean
  error?: boolean
}>()

const emit = defineEmits<{
  (e: 'retry'): void
}>()

const chartOption = computed<EChartsOption>(() => ({
  title: { text: '最近7天灾害趋势', left: 'center', textStyle: { fontSize: 16 } },
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'category', data: props.trendData?.dates ?? [], boundaryGap: false },
  yAxis: { type: 'value', minInterval: 1 },
  series: [{
    name: '事件数',
    type: 'line',
    data: props.trendData?.counts ?? [],
    smooth: true,
    areaStyle: { opacity: 0.15 },
    itemStyle: { color: '#1a73e8' },
  }],
}))

const hasData = computed(() => {
  return props.trendData && props.trendData.dates.length > 0
})
</script>

<template>
  <div class="trend-chart">
    <el-skeleton v-if="loading" :rows="6" animated />
    <div v-else-if="error" class="trend-chart__error">
      <span>数据加载失败</span>
      <el-button size="small" link type="primary" @click="emit('retry')">重试</el-button>
    </div>
    <div v-else-if="!hasData" class="trend-chart__empty">暂无数据</div>
    <ChartContainer v-else :option="chartOption" style="height: 300px" />
  </div>
</template>

<style scoped>
.trend-chart__error {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300px;
  gap: 8px;
  color: var(--color-danger);
}
.trend-chart__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-lg);
}
</style>