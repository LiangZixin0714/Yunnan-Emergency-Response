<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { Refresh } from '@element-plus/icons-vue'
import StatCard from '@/components/StatCard.vue'
import { getDashboardOverview, getDashboardTrend, getDashboardDistribution } from '@/api/dashboard'
import type { DashboardOverview, DashboardTrend, DashboardDistribution } from '@/types/dashboard'
import * as echarts from 'echarts'

const router = useRouter()
const authStore = useAuthStore()

const overview = ref<DashboardOverview | null>(null)
const overviewLoading = ref(true)
const chartLoading = ref(true)
const overviewError = ref(false)
const chartError = ref(false)
const chartsReady = ref(false)

const trendChartRef = ref<HTMLDivElement>()
const distChartRef = ref<HTMLDivElement>()
let trendChart: echarts.ECharts | null = null
let distChart: echarts.ECharts | null = null
let disposed = false

function handleResize(): void {
  if (!disposed) {
    trendChart?.resize()
    distChart?.resize()
  }
}

async function loadOverview(): Promise<void> {
  overviewLoading.value = true
  overviewError.value = false
  try {
    const res = await getDashboardOverview() as unknown as DashboardOverview
    overview.value = res
  } catch {
    overviewError.value = true
  } finally {
    overviewLoading.value = false
  }
}

async function loadCharts(): Promise<void> {
  chartLoading.value = true
  chartError.value = false
  try {
    const [trendRes, distRes] = await Promise.all([
      getDashboardTrend() as unknown as DashboardTrend,
      getDashboardDistribution() as unknown as DashboardDistribution,
    ])
    chartLoading.value = false
    chartsReady.value = true
    await nextTick()
    if (disposed) return
    await new Promise(resolve => setTimeout(resolve, 100))
    if (disposed) return
    if (trendChartRef.value) {
      trendChart = echarts.init(trendChartRef.value)
      trendChart.setOption({
        title: { text: '灾情趋势', left: 'center', textStyle: { fontSize: 16 } },
        tooltip: { trigger: 'axis' },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: trendRes?.dates || [], boundaryGap: false },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{ name: '事件数', type: 'line', data: trendRes?.counts || [], smooth: true, areaStyle: { opacity: 0.15 }, itemStyle: { color: '#1a73e8' } }],
      })
    }
    if (distChartRef.value) {
      distChart = echarts.init(distChartRef.value)
      distChart.setOption({
        title: { text: '灾害类型分布', left: 'center', textStyle: { fontSize: 16 } },
        tooltip: { trigger: 'item' },
        legend: { orient: 'vertical', left: 'left', top: 'middle' },
        series: [{ type: 'pie', radius: ['40%', '70%'], center: ['55%', '55%'], data: distRes?.types?.map((t, i) => ({ name: t, value: distRes.counts[i] })) || [], emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } } }],
      })
    }
  } catch (e) {
    console.error('Chart load error:', e)
    chartError.value = true
    chartLoading.value = false
  }
}

function refreshAll(): void {
  loadOverview()
  loadCharts()
}

const allQuickEntries = [
  { title: '灾情上报', icon: 'EditPen', path: '/incident/report', color: '#f56c6c', roles: ['VIEWER'] },
  { title: '事件列表', icon: 'List', path: '/incident/list', color: '#e6a23c' },
  { title: '资源调度', icon: 'Box', path: '/resource', color: '#67c23a', roles: ['ADMIN', 'RESOURCE_MANAGER'] },
  { title: 'AI方案生成', icon: 'Document', path: '/plan', color: '#409eff', roles: ['ADMIN', 'OPERATOR'] },
]

const quickEntries = computed(() => {
  const role = authStore.roleName
  return allQuickEntries.filter((e) => !e.roles || (role && e.roles.includes(role)))
})

onMounted(() => {
  refreshAll()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  disposed = true
  window.removeEventListener('resize', handleResize)
  if (trendChart) { try { trendChart.dispose() } catch {} trendChart = null }
  if (distChart) { try { distChart.dispose() } catch {} distChart = null }
})
</script>

<template>
  <div class="home-page">
    <div class="page-header">
      <h2 class="page-header__title">首页</h2>
      <el-button @click="refreshAll" :icon="Refresh">刷新</el-button>
    </div>

    <div class="home-page__stats">
      <StatCard title="今日新增事件" :value="overview?.todayCount ?? '--'" icon="Warning" :loading="overviewLoading" />
      <StatCard title="进行中事件" :value="overview?.activeCount ?? '--'" icon="Loading" :loading="overviewLoading" />
      <StatCard title="已结束事件" :value="overview?.completedCount ?? '--'" icon="CircleCheck" :loading="overviewLoading" />
    </div>

    <div v-if="overviewError" class="home-page__error">
      <span>数据加载失败</span>
      <el-button size="small" @click="loadOverview">重试</el-button>
    </div>

    <div class="home-page__charts">
      <el-card shadow="hover" class="home-page__chart-card">
        <div v-if="chartsReady" ref="trendChartRef" class="home-page__chart" style="width: 100%; height: 340px;" />
        <div v-else-if="chartError" class="home-page__empty">暂无数据</div>
        <el-skeleton v-else :rows="8" />
      </el-card>
      <el-card shadow="hover" class="home-page__chart-card">
        <div v-if="chartsReady" ref="distChartRef" class="home-page__chart" style="width: 100%; height: 340px;" />
        <div v-else-if="chartError" class="home-page__empty">暂无数据</div>
        <el-skeleton v-else :rows="8" />
      </el-card>
    </div>

    <div class="home-page__quick">
      <h3 class="home-page__section-title">快捷入口</h3>
      <div class="home-page__entries">
        <el-card v-for="entry in quickEntries" :key="entry.path" shadow="hover" class="home-page__entry" @click="router.push(entry.path)">
          <el-icon :size="36" :color="entry.color"><component :is="entry.icon" /></el-icon>
          <span class="home-page__entry-title">{{ entry.title }}</span>
        </el-card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-page__stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--spacing-lg); margin-bottom: var(--spacing-lg); }
.home-page__charts { display: grid; grid-template-columns: repeat(2, 1fr); gap: var(--spacing-lg); margin-bottom: var(--spacing-lg); }
.home-page__chart-card { min-height: 380px; }
.home-page__chart { width: 100%; height: 340px; }
.home-page__error { display: flex; align-items: center; gap: var(--spacing-sm); color: var(--color-danger); margin-bottom: var(--spacing-lg); }
.home-page__empty { display: flex; align-items: center; justify-content: center; height: 300px; color: var(--color-text-secondary); font-size: var(--font-size-lg); }
.home-page__section-title { font-size: var(--font-size-lg); margin-bottom: var(--spacing-md); }
.home-page__entries { display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--spacing-md); }
.home-page__entry { cursor: pointer; text-align: center; transition: transform var(--transition-duration); }
.home-page__entry:hover { transform: translateY(-4px); }
.home-page__entry-title { display: block; margin-top: var(--spacing-sm); font-size: var(--font-size-base); color: var(--color-text-regular); }
</style>
