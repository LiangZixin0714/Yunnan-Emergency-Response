<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import IncidentMap from '@/components/IncidentMap.vue'
import { getScreenData } from '@/api/dashboard'
import type { ScreenData, MapIncident } from '@/types/dashboard'
import { DisasterTypeLabel, IncidentLevelLabel, IncidentStatusLabel, DisposalPlanStatusLabel, ResourceDispatchStatusLabel } from '@/types/enums'
import type { DisasterTypeValue, IncidentLevelValue } from '@/types/enums'
import * as echarts from 'echarts/core'
import { PieChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([PieChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

const router = useRouter()

const screenData = ref<ScreenData | null>(null)
const loading = ref(true)
const dataReady = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let disposed = false

const trendChartRef = ref<HTMLDivElement>()
const resourceChartRef = ref<HTMLDivElement>()
let trendChart: echarts.ECharts | null = null
let resourceChart: echarts.ECharts | null = null

const filterDisasterType = ref<DisasterTypeValue | ''>('')
const filterIncidentLevel = ref<IncidentLevelValue | ''>('')
const selectedIncident = ref<MapIncident | null>(null)

const disasterTypeOptions = Object.entries(DisasterTypeLabel).map(([value, label]) => ({ value, label }))
const levelOptions = Object.entries(IncidentLevelLabel).map(([value, label]) => ({ value, label }))

const filteredMapIncidents = computed<MapIncident[]>(() => {
  const list = screenData.value?.mapIncidents ?? []
  return list.filter((inc) => {
    if (filterDisasterType.value && inc.disasterType !== filterDisasterType.value) return false
    if (filterIncidentLevel.value && inc.incidentLevel !== filterIncidentLevel.value) return false
    return true
  })
})

const recentIncidents = computed<MapIncident[]>(() => {
  return (screenData.value?.incidents?.slice(0, 8) ?? []) as MapIncident[]
})

function getStatusColor(status: string): string {
  const colors: Record<string, string> = {
    pending: '#909399',
    confirmed: '#409eff',
    processing: '#f56c6c',
    completed: '#67c23a',
  }
  return colors[status] || '#909399'
}

function getDisasterColor(disasterType: string): string {
  const colors: Record<string, string> = {
    earthquake: '#ff4d4f',
    mudslide: '#fa8c16',
    flood: '#1890ff',
    drought: '#faad14',
    landslide: '#722ed1',
    fire: '#f5222d',
    other: '#13c2c2',
  }
  return colors[disasterType] || '#409eff'
}

function computeDisposalProgress(inc: MapIncident): number {
  if (inc.status === 'completed') return 100
  let steps = 0
  if (inc.disposalPlanStatus && inc.disposalPlanStatus !== '') steps++
  if (inc.disposalPlanStatus === 'accepted') steps++
  if (inc.resourceDispatchStatus && inc.resourceDispatchStatus !== '') steps++
  if (inc.resourceDispatchStatus === 'completed') steps++
  if (inc.status === 'processing') steps++
  return Math.round((steps / 5) * 100)
}

function getProgressColor(progress: number): string {
  if (progress >= 100) return '#67c23a'
  if (progress >= 60) return '#409eff'
  if (progress >= 30) return '#e6a23c'
  return '#f56c6c'
}

function handleIncidentSelect(inc: MapIncident): void {
  selectedIncident.value = inc
}

function handleResize(): void {
  if (!disposed) {
    trendChart?.resize()
    resourceChart?.resize()
  }
}

async function loadData(): Promise<void> {
  try {
    const res = await getScreenData() as unknown as ScreenData
    screenData.value = res
    if (!dataReady.value) {
      dataReady.value = true
      await nextTick()
    }
    if (disposed) return
    if (trendChartRef.value) {
      const incidentsByType: Record<string, number> = {}
      res.incidents.forEach((inc) => { incidentsByType[inc.disasterType] = (incidentsByType[inc.disasterType] || 0) + 1 })
      if (!trendChart) trendChart = echarts.init(trendChartRef.value)
      trendChart.setOption({
        title: { text: '灾情类型统计', textStyle: { color: '#fff', fontSize: 14 } },
        tooltip: { trigger: 'item' },
        series: [{ type: 'pie', radius: ['35%', '65%'], center: ['50%', '55%'], data: Object.entries(incidentsByType).map(([name, value]) => ({ name, value })), label: { color: '#fff' } }],
      })
    }
    if (resourceChartRef.value) {
      const resourcesByType: Record<string, number> = {}
      res.resources.forEach((r) => { resourcesByType[r.resourceType] = (resourcesByType[r.resourceType] || 0) + r.totalStock })
      if (!resourceChart) resourceChart = echarts.init(resourceChartRef.value)
      resourceChart.setOption({
        title: { text: '资源分布', textStyle: { color: '#fff', fontSize: 14 } },
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: Object.keys(resourcesByType), axisLabel: { color: '#fff', fontSize: 10 } },
        yAxis: { type: 'value', axisLabel: { color: '#fff' } },
        series: [{ type: 'bar', data: Object.values(resourcesByType), itemStyle: { color: '#409eff' } }],
      })
    }
  } catch {
    // Keep previous data
  } finally {
    loading.value = false
  }
}

function enterFullscreen(): void {
  document.documentElement.requestFullscreen().catch(() => {})
}

function exitFullscreen(): void {
  if (document.fullscreenElement) {
    document.exitFullscreen().catch(() => {})
  }
}

function handleKeydown(e: KeyboardEvent): void {
  if (e.key === 'Escape') {
    if (selectedIncident.value) {
      selectedIncident.value = null
    } else {
      router.push('/home')
    }
  }
}

onMounted(() => {
  enterFullscreen()
  loadData()
  refreshTimer = setInterval(loadData, 60000)
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  disposed = true
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  window.removeEventListener('resize', handleResize)
  if (trendChart) { try { trendChart.dispose() } catch {} trendChart = null }
  if (resourceChart) { try { resourceChart.dispose() } catch {} resourceChart = null }
  exitFullscreen()
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div class="screen-page">
    <div class="screen-page__header">
      <h1>云南自然灾害应急协同决策平台</h1>
      <div class="screen-page__header-actions">
        <el-button text @click="router.push('/home')" style="color: #fff">
          退出大屏
        </el-button>
      </div>
    </div>

    <div class="screen-page__stats" v-if="screenData">
      <div class="screen-page__stat">
        <div class="screen-page__stat-value">{{ screenData.statistics.todayCount }}</div>
        <div class="screen-page__stat-label">今日事件数</div>
      </div>
      <div class="screen-page__stat">
        <div class="screen-page__stat-value">{{ screenData.statistics.activeCount }}</div>
        <div class="screen-page__stat-label">活跃事件数</div>
      </div>
      <div class="screen-page__stat">
        <div class="screen-page__stat-value">{{ screenData.statistics.completedCount }}</div>
        <div class="screen-page__stat-label">已结束事件</div>
      </div>
      <div class="screen-page__stat">
        <div class="screen-page__stat-value">{{ filteredMapIncidents.length }}</div>
        <div class="screen-page__stat-label">地图点位</div>
      </div>
    </div>

    <div class="screen-page__filter" v-if="screenData">
      <el-select v-model="filterDisasterType" placeholder="灾害类型" clearable size="small" style="width: 140px; margin-right: 8px">
        <el-option v-for="opt in disasterTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-select v-model="filterIncidentLevel" placeholder="事件等级" clearable size="small" style="width: 140px">
        <el-option v-for="opt in levelOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
    </div>

    <div class="screen-page__body" v-if="screenData">
      <div class="screen-page__map-area">
        <IncidentMap :incidents="filteredMapIncidents" @select="handleIncidentSelect" />
      </div>

      <div class="screen-page__side">
        <div class="screen-page__chart-area">
          <div v-if="dataReady" ref="trendChartRef" style="width: 100%; height: 100%" />
        </div>
        <div class="screen-page__chart-area">
          <div v-if="dataReady" ref="resourceChartRef" style="width: 100%; height: 100%" />
        </div>
        <div class="screen-page__list-area">
          <div class="screen-page__list-title">最近灾情</div>
          <div class="screen-page__list">
            <div
              v-for="inc in recentIncidents"
              :key="inc.incidentId"
              class="screen-page__list-item"
              :style="{ borderLeftColor: getStatusColor(inc.status) }"
              @click="selectedIncident = inc"
            >
              <div class="screen-page__list-item-header">
                <span class="screen-page__list-item-name">{{ inc.incidentName }}</span>
                <span class="screen-page__list-item-status" :style="{ color: getStatusColor(inc.status) }">
                  {{ IncidentStatusLabel[inc.status as keyof typeof IncidentStatusLabel] }}
                </span>
              </div>
              <div class="screen-page__list-item-meta">
                <span :style="{ color: getDisasterColor(inc.disasterType) }">
                  {{ DisasterTypeLabel[inc.disasterType as keyof typeof DisasterTypeLabel] }}
                </span>
                <span>·</span>
                <span>{{ IncidentLevelLabel[inc.incidentLevel as keyof typeof IncidentLevelLabel] }}</span>
                <span v-if="inc.location">·</span>
                <span v-if="inc.location" class="screen-page__list-item-loc">{{ inc.location }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <Teleport to="body">
      <Transition name="detail-fade">
        <div v-if="selectedIncident" class="incident-detail-overlay" @click.self="selectedIncident = null">
          <div class="incident-detail-panel">
            <div class="incident-detail-header" :style="{ background: `linear-gradient(135deg, ${getDisasterColor(selectedIncident.disasterType)}dd, ${getStatusColor(selectedIncident.status)}dd)` }">
              <div>
                <div class="incident-detail-title">{{ selectedIncident.incidentName }}</div>
                <div class="incident-detail-sub">
                  {{ DisasterTypeLabel[selectedIncident.disasterType as keyof typeof DisasterTypeLabel] }}
                  · {{ IncidentLevelLabel[selectedIncident.incidentLevel as keyof typeof IncidentLevelLabel] }}
                  · {{ IncidentStatusLabel[selectedIncident.status as keyof typeof IncidentStatusLabel] }}
                </div>
              </div>
              <el-icon class="incident-detail-close" @click="selectedIncident = null"><Close /></el-icon>
            </div>
            <div class="incident-detail-body">
              <div class="incident-detail-section">
                <div class="incident-detail-section-title">基本信息</div>
                <div class="incident-detail-info">
                  <div class="incident-detail-row">
                    <span class="incident-detail-label">事件编号</span>
                    <span class="incident-detail-value">{{ selectedIncident.incidentId }}</span>
                  </div>
                  <div class="incident-detail-row">
                    <span class="incident-detail-label">发生地点</span>
                    <span class="incident-detail-value">{{ selectedIncident.location || '-' }}</span>
                  </div>
                  <div class="incident-detail-row" v-if="selectedIncident.occurTime">
                    <span class="incident-detail-label">发生时间</span>
                    <span class="incident-detail-value">{{ selectedIncident.occurTime }}</span>
                  </div>
                  <div class="incident-detail-row" v-if="selectedIncident.deathCount">
                    <span class="incident-detail-label">死亡人数</span>
                    <span class="incident-detail-value incident-detail-danger">{{ selectedIncident.deathCount }} 人</span>
                  </div>
                  <div class="incident-detail-row" v-if="selectedIncident.propertyLoss">
                    <span class="incident-detail-label">财产损失</span>
                    <span class="incident-detail-value">{{ selectedIncident.propertyLoss }} 万元</span>
                  </div>
                </div>
              </div>
              <div class="incident-detail-section">
                <div class="incident-detail-section-title">处置进度</div>
                <div class="incident-progress">
                  <div class="incident-progress-bar">
                    <div
                      class="incident-progress-fill"
                      :style="{ width: computeDisposalProgress(selectedIncident) + '%', background: getProgressColor(computeDisposalProgress(selectedIncident)) }"
                    ></div>
                  </div>
                  <div class="incident-progress-text">{{ computeDisposalProgress(selectedIncident) }}%</div>
                </div>
                <div class="incident-progress-steps">
                  <div class="incident-step" :class="{ active: selectedIncident.disposalPlanStatus }">
                    <div class="incident-step-dot" :style="{ background: selectedIncident.disposalPlanStatus ? getProgressColor(30) : 'transparent', border: selectedIncident.disposalPlanStatus ? 'none' : '2px solid #ccc' }"></div>
                    <div class="incident-step-label">方案拟定</div>
                    <div class="incident-step-status" v-if="selectedIncident.disposalPlanStatus">
                      {{ DisposalPlanStatusLabel[selectedIncident.disposalPlanStatus] || selectedIncident.disposalPlanStatus }}
                    </div>
                  </div>
                  <div class="incident-step" :class="{ active: selectedIncident.disposalPlanStatus === 'accepted' }">
                    <div class="incident-step-dot" :style="{ background: selectedIncident.disposalPlanStatus === 'accepted' ? getProgressColor(60) : 'transparent', border: selectedIncident.disposalPlanStatus === 'accepted' ? 'none' : '2px solid #ccc' }"></div>
                    <div class="incident-step-label">方案审批</div>
                    <div class="incident-step-status" v-if="selectedIncident.disposalPlanStatus === 'accepted'">已通过</div>
                  </div>
                  <div class="incident-step" :class="{ active: selectedIncident.resourceDispatchStatus }">
                    <div class="incident-step-dot" :style="{ background: selectedIncident.resourceDispatchStatus ? getProgressColor(60) : 'transparent', border: selectedIncident.resourceDispatchStatus ? 'none' : '2px solid #ccc' }"></div>
                    <div class="incident-step-label">资源调度</div>
                    <div class="incident-step-status" v-if="selectedIncident.resourceDispatchStatus">
                      {{ ResourceDispatchStatusLabel[selectedIncident.resourceDispatchStatus] || selectedIncident.resourceDispatchStatus }}
                    </div>
                  </div>
                  <div class="incident-step" :class="{ active: selectedIncident.status === 'processing' || selectedIncident.status === 'completed' }">
                    <div class="incident-step-dot" :style="{ background: selectedIncident.status === 'completed' ? getProgressColor(100) : selectedIncident.status === 'processing' ? getProgressColor(60) : 'transparent', border: (selectedIncident.status === 'processing' || selectedIncident.status === 'completed') ? 'none' : '2px solid #ccc' }"></div>
                    <div class="incident-step-label">现场处置</div>
                    <div class="incident-step-status">
                      {{ IncidentStatusLabel[selectedIncident.status as keyof typeof IncidentStatusLabel] }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="incident-detail-footer">
              <el-button type="primary" @click="router.push('/incident/detail?id=' + selectedIncident.incidentId)">查看详情</el-button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.screen-page {
  width: 100vw;
  height: 100vh;
  background: linear-gradient(135deg, #0d1b3e 0%, #0a1628 50%, #060e1a 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.screen-page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 32px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.screen-page__header h1 {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 4px;
  margin: 0;
}

.screen-page__header-actions {
  display: flex;
  gap: 12px;
}

.screen-page__stats {
  display: flex;
  justify-content: center;
  gap: 60px;
  padding: 16px 32px;
}

.screen-page__stat {
  text-align: center;
}

.screen-page__stat-value {
  font-size: 36px;
  font-weight: 700;
  color: #409eff;
}

.screen-page__stat-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  margin-top: 4px;
}

.screen-page__filter {
  display: flex;
  justify-content: center;
  padding: 0 32px 8px;
}

.screen-page__body {
  flex: 1;
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 16px;
  padding: 0 32px 16px;
  min-height: 0;
}

.screen-page__map-area {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  overflow: hidden;
}

.screen-page__side {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}

.screen-page__chart-area {
  flex: 1;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  min-height: 0;
}

.screen-page__list-area {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  flex: 1.2;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.screen-page__list-title {
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.screen-page__list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px;
}

.screen-page__list-item {
  padding: 8px 12px;
  margin-bottom: 6px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 6px;
  border-left: 3px solid;
  cursor: pointer;
  transition: background 0.2s;
}

.screen-page__list-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.screen-page__list-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.screen-page__list-item-name {
  font-size: 13px;
  font-weight: 500;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 70%;
}

.screen-page__list-item-status {
  font-size: 12px;
  font-weight: 500;
}

.screen-page__list-item-meta {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  display: flex;
  gap: 6px;
  align-items: center;
}

.screen-page__list-item-loc {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100px;
}

.incident-detail-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  backdrop-filter: blur(4px);
}

.incident-detail-panel {
  width: 480px;
  max-height: 80vh;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  color: #333;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.incident-detail-header {
  padding: 16px 20px;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.incident-detail-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 4px;
}

.incident-detail-sub {
  font-size: 13px;
  opacity: 0.9;
}

.incident-detail-close {
  font-size: 18px;
  cursor: pointer;
  opacity: 0.8;
}

.incident-detail-close:hover {
  opacity: 1;
}

.incident-detail-body {
  padding: 16px 20px;
  overflow-y: auto;
  flex: 1;
}

.incident-detail-section {
  margin-bottom: 20px;
}

.incident-detail-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #666;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid #f0f0f0;
}

.incident-detail-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.incident-detail-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.incident-detail-label {
  color: #999;
}

.incident-detail-value {
  color: #333;
  font-weight: 500;
}

.incident-detail-danger {
  color: #f56c6c;
}

.incident-progress {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.incident-progress-bar {
  flex: 1;
  height: 8px;
  background: #f0f2f5;
  border-radius: 4px;
  overflow: hidden;
}

.incident-progress-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s;
}

.incident-progress-text {
  font-size: 14px;
  font-weight: 700;
  min-width: 45px;
  text-align: right;
}

.incident-progress-steps {
  display: flex;
  justify-content: space-between;
}

.incident-step {
  flex: 1;
  text-align: center;
  position: relative;
}

.incident-step-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  margin: 0 auto 6px;
  transition: all 0.3s;
}

.incident-step-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 2px;
}

.incident-step.active .incident-step-label {
  color: #333;
  font-weight: 600;
}

.incident-step-status {
  font-size: 11px;
  color: #409eff;
  font-weight: 500;
}

.incident-detail-footer {
  padding: 12px 20px;
  border-top: 1px solid #f0f0f0;
  text-align: right;
}

.detail-fade-enter-active,
.detail-fade-leave-active {
  transition: opacity 0.3s ease;
}

.detail-fade-enter-from,
.detail-fade-leave-to {
  opacity: 0;
}
</style>
