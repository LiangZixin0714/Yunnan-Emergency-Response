<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, toRaw } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart, PieChart, MapChart, ScatterChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GeoComponent,
  VisualMapComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { EChartsOption } from 'echarts'

echarts.use([
  LineChart,
  PieChart,
  MapChart,
  ScatterChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GeoComponent,
  VisualMapComponent,
  CanvasRenderer,
])

const props = defineProps<{
  option: EChartsOption | Record<string, any>
  loading?: boolean
}>()

const chartRef = ref<HTMLDivElement>()
let chartInstance: echarts.ECharts | null = null
let disposed = false

function initChart(): void {
  if (!chartRef.value || disposed) return
  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption(toRaw(props.option))
}

function handleResize(): void {
  if (!disposed && chartInstance) {
    chartInstance.resize()
  }
}

watch(
  () => props.option,
  (newOption) => {
    if (!disposed && chartInstance) {
      chartInstance.setOption(toRaw(newOption), true)
    }
  }
)

watch(
  () => props.loading,
  (val) => {
    if (!disposed && chartInstance) {
      val ? chartInstance.showLoading() : chartInstance.hideLoading()
    }
  }
)

onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  disposed = true
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    try {
      chartInstance.dispose()
    } catch {
      // ignore dispose errors
    }
    chartInstance = null
  }
})
</script>

<template>
  <div ref="chartRef" class="chart-container"></div>
</template>

<style scoped>
.chart-container {
  width: 100%;
  height: 100%;
  min-height: 300px;
}
</style>
