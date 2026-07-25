<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts/core'
import { MapChart, ScatterChart, EffectScatterChart } from 'echarts/charts'
import { GeoComponent, TooltipComponent, VisualMapComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { MapIncident } from '@/types/dashboard'
import { DisasterTypeLabel } from '@/types/enums'
import { IncidentLevelLabel } from '@/types/enums'
import type { DisasterTypeValue, IncidentLevelValue } from '@/types/enums'
import yunnanGeoJSON from '@/assets/map/yunnan.json'
import { DisasterTypeColorMap, IncidentLevelSizeMap } from '@/constants/disaster'

echarts.use([MapChart, ScatterChart, EffectScatterChart, GeoComponent, TooltipComponent, VisualMapComponent, CanvasRenderer])

const props = defineProps<{
  incidents: MapIncident[]
  loading?: boolean
  error?: boolean
}>()

const emit = defineEmits<{
  (e: 'retry'): void
}>()

const chartRef = ref<HTMLDivElement>()
let chartInstance: echarts.ECharts | null = null
let disposed = false
let resizeTimer: ReturnType<typeof setTimeout> | null = null

function isValidCoord(lat: number, lng: number): boolean {
  return lat >= 21 && lat <= 29 && lng >= 97 && lng <= 106
}

function buildOption(incidents: MapIncident[]): Record<string, unknown> {
  const scatterData = incidents
    .filter((i) => i.latitude != null && i.longitude != null && isValidCoord(i.latitude, i.longitude))
    .map((i) => ({
      name: i.incidentName,
      value: [i.longitude, i.latitude],
      symbolSize: IncidentLevelSizeMap[i.incidentLevel as IncidentLevelValue] ?? 10,
      itemStyle: { color: DisasterTypeColorMap[i.disasterType as DisasterTypeValue] ?? '#FFFFFF' },
      _raw: i,
    }))

  return {
    tooltip: {
      trigger: 'item',
      formatter(params: any) {
        const raw = params?.data?._raw as MapIncident | undefined
        if (!raw) return ''
        const levelLabel = IncidentLevelLabel[raw.incidentLevel as IncidentLevelValue] ?? raw.incidentLevel
        return [
          `事件名称：${raw.incidentName}`,
          `发生时间：${raw.occurTime?.replace('T', ' ') ?? '-'}`,
          `事件等级：${levelLabel}`,
          `受灾人数：${raw.affectedCount ?? '-'}`,
        ].join('<br/>')
      },
    },
    geo: {
      map: 'yunnan',
      roam: true,
      itemStyle: {
        areaColor: '#1a2a4a',
        borderColor: '#3a5a8a',
      },
      emphasis: {
        itemStyle: { areaColor: '#2a3a5a' },
      },
      label: { show: false },
    },
    series: [
      {
        type: 'scatter',
        coordinateSystem: 'geo',
        data: scatterData,
        borderWidth: 2,
        borderColor: '#fff',
      },
    ],
  }
}

function initChart(): void {
  if (!chartRef.value || disposed || chartInstance) return
  echarts.registerMap('yunnan', yunnanGeoJSON as any)
  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption(buildOption(props.incidents))
}

function handleResize(): void {
  if (resizeTimer) clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => {
    if (!disposed && chartInstance) chartInstance.resize()
  }, 200)
}

watch(
  () => props.incidents,
  (newIncidents) => {
    if (!disposed && chartInstance) {
      chartInstance.setOption(buildOption(newIncidents), true)
    }
  }
)

watch(
  [() => props.loading, () => props.error],
  ([loading, error]) => {
    if (!loading && !error && !chartInstance && !disposed) {
      setTimeout(() => initChart(), 50)
    }
  }
)

onMounted(() => {
  if (!props.loading && !props.error) {
    initChart()
  }
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  disposed = true
  window.removeEventListener('resize', handleResize)
  if (resizeTimer) clearTimeout(resizeTimer)
  if (chartInstance) {
    try { chartInstance.dispose() } catch {}
    chartInstance = null
  }
})
</script>

<template>
  <div class="disaster-map">
    <div class="disaster-map__chart-wrapper">
      <div v-show="!loading && !error" ref="chartRef" class="disaster-map__chart" />
      <div class="disaster-map__legend" v-show="!loading && !error">
        <div class="disaster-map__legend-title">灾害类型</div>
        <div v-for="(color, type) in DisasterTypeColorMap" :key="type" class="disaster-map__legend-item">
          <span class="disaster-map__legend-dot" :style="{ backgroundColor: color, border: type === 'other' ? '1px solid #999' : 'none' }" />
          <span class="disaster-map__legend-label">{{ DisasterTypeLabel[type as DisasterTypeValue] }}</span>
        </div>
      </div>
      <div v-if="loading" class="disaster-map__overlay">
        <el-skeleton :rows="10" animated />
      </div>
      <div v-if="error" class="disaster-map__overlay disaster-map__error-overlay">
        <span>数据加载失败</span>
        <el-button size="small" link type="primary" @click="emit('retry')">重试</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.disaster-map {
  height: 100%;
}
.disaster-map__chart-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 400px;
}
.disaster-map__chart {
  width: 100%;
  height: 100%;
}
.disaster-map__overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  z-index: 20;
  padding: 16px;
}
.disaster-map__error-overlay {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--color-danger);
}
.disaster-map__legend {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 12px;
  line-height: 1.6;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.15);
  z-index: 10;
}
.disaster-map__legend-title {
  font-weight: 600;
  margin-bottom: 4px;
  color: #333;
}
.disaster-map__legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.disaster-map__legend-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.disaster-map__legend-label {
  color: #333;
  white-space: nowrap;
}
</style>
