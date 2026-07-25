<script setup lang="ts">
import { computed } from 'vue'
import ChartContainer from '@/components/ChartContainer.vue'
import { DisasterTypeColorMap } from '@/constants/disaster'
import { DisasterTypeLabel } from '@/types/enums'
import type { DisasterTypeValue } from '@/types/enums'

interface TypeDistributionItem {
  name: string
  value: number
}

const props = defineProps<{
  typeDistributionData: TypeDistributionItem[]
  loading?: boolean
  error?: boolean
}>()

const emit = defineEmits<{
  (e: 'retry'): void
}>()

const labelToTypeKey: Record<string, DisasterTypeValue> = Object.fromEntries(
  (Object.entries(DisasterTypeLabel) as [DisasterTypeValue, string][]).map(
    ([key, label]) => [label, key]
  )
) as Record<string, DisasterTypeValue>

const chartOption = computed(() => ({
  title: { text: '灾害类型分布', left: 'center', textStyle: { fontSize: 16 } },
  tooltip: {
    trigger: 'item',
    formatter(params: any) {
      const p = params as { name?: string; value?: number; percent?: number }
      if (!p?.name) return ''
      return `${p.name}<br/>数量：${p.value ?? 0}<br/>占比：${p.percent?.toFixed(1) ?? 0}%`
    },
  },
  legend: {
    orient: 'vertical',
    right: '5%',
    top: 'center',
  },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    center: ['40%', '50%'],
    data: props.typeDistributionData.map((item) => {
      const typeKey = labelToTypeKey[item.name]
      return {
        name: item.name,
        value: item.value,
        itemStyle: {
          color: typeKey ? DisasterTypeColorMap[typeKey] : '#999999',
        },
      }
    }),
    emphasis: {
      itemStyle: {
        shadowBlur: 10,
        shadowOffsetX: 0,
        shadowColor: 'rgba(0, 0, 0, 0.5)',
      },
    },
  }],
}))

const hasData = computed(() => props.typeDistributionData.length > 0)
</script>

<template>
  <div class="disaster-type-chart">
    <el-skeleton v-if="loading" :rows="6" animated />
    <div v-else-if="error" class="disaster-type-chart__error">
      <span>数据加载失败</span>
      <el-button size="small" link type="primary" @click="emit('retry')">重试</el-button>
    </div>
    <div v-else-if="!hasData" class="disaster-type-chart__empty">暂无数据</div>
    <ChartContainer v-else :option="chartOption" style="height: 300px" />
  </div>
</template>

<style scoped>
.disaster-type-chart__error {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300px;
  gap: 8px;
  color: var(--color-danger);
}
.disaster-type-chart__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-lg);
}
</style>