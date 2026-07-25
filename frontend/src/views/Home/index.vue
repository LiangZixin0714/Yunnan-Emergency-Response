<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import StatisticCard from '@/components/StatisticCard.vue'
import DisasterMap from '@/components/DisasterMap.vue'
import TrendChart from '@/components/TrendChart.vue'
import DisasterTypeChart from '@/components/DisasterTypeChart.vue'
import { getIncidentList } from '@/api/incident'
import type { Incident, IncidentListResult } from '@/types/incident'
import type { DashboardTrend, MapIncident } from '@/types/dashboard'
import { DisasterTypeLabel } from '@/types/enums'
import type { DisasterTypeValue } from '@/types/enums'

const incidentList = ref<Incident[]>([])
const loading = ref(true)
const error = ref(false)

const todayCount = computed(() => {
  const todayStr = new Date().toISOString().slice(0, 10)
  return incidentList.value.filter(
    (i) => i.occurTime != null && i.occurTime.slice(0, 10) === todayStr
  ).length
})

const activeCount = computed(() => {
  return incidentList.value.filter((i) => i.status === 'processing').length
})

const trendData = computed<DashboardTrend>(() => {
  const dates: string[] = []
  const counts: number[] = []
  for (let offset = 6; offset >= 0; offset--) {
    const d = new Date()
    d.setDate(d.getDate() - offset)
    const ds = d.toISOString().slice(0, 10)
    dates.push(`${d.getMonth() + 1}/${d.getDate()}`)
    counts.push(
      incidentList.value.filter((i) => i.occurTime != null && i.occurTime.slice(0, 10) === ds).length
    )
  }
  return { dates, counts }
})

const typeDistributionData = computed(() => {
  const typeKeys: DisasterTypeValue[] = ['earthquake', 'mudslide', 'flood', 'drought', 'landslide', 'fire', 'other']
  return typeKeys
    .map((key) => ({
      name: DisasterTypeLabel[key],
      value: incidentList.value.filter((i) => i.disasterType === key).length,
    }))
    .filter((item) => item.value > 0)
})

const mapIncidents = computed<MapIncident[]>(() => {
  return incidentList.value
    .filter((i) => {
      if (i.latitude == null || i.longitude == null) return false
      return i.latitude >= 21 && i.latitude <= 29 && i.longitude >= 97 && i.longitude <= 106
    })
    .map((i) => ({
      incidentId: i.incidentId,
      incidentName: i.incidentName,
      disasterType: i.disasterType,
      incidentLevel: i.incidentLevel,
      status: i.status,
      latitude: i.latitude!,
      longitude: i.longitude!,
      occurTime: i.occurTime ?? '',
      affectedCount: i.affectedCount ?? i.deathCount ?? 0,
    }))
})

async function loadIncidentList(): Promise<void> {
  loading.value = true
  error.value = false
  try {
    const res = await getIncidentList({ page: 1, size: 999 }) as unknown as IncidentListResult
    incidentList.value = res.list ?? []
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

function refreshAll(): void {
  loadIncidentList()
}

onMounted(() => {
  refreshAll()
})
</script>

<template>
  <div class="home-page">
    <div class="page-header">
      <h2 class="page-header__title">首页</h2>
      <el-button @click="refreshAll" :icon="Refresh">刷新</el-button>
    </div>

    <div class="home-page__stats">
      <StatisticCard title="今日灾害数量" :value="todayCount" unit="起" icon="Warning" :loading="loading" :error="error" @retry="loadIncidentList" />
      <StatisticCard title="待处理事件" :value="activeCount" unit="起" icon="Loading" :loading="loading" :error="error" @retry="loadIncidentList" />
    </div>

    <el-card shadow="hover" class="home-page__map-card">
      <DisasterMap :incidents="mapIncidents" :loading="loading" :error="error" @retry="loadIncidentList" />
    </el-card>

    <el-row :gutter="20" class="home-page__bottom-row">
      <el-col :span="12">
        <el-card shadow="hover" class="home-page__chart-card">
          <TrendChart :trend-data="trendData" :loading="loading" :error="error" @retry="loadIncidentList" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="home-page__chart-card">
          <DisasterTypeChart :type-distribution-data="typeDistributionData" :loading="loading" :error="error" @retry="loadIncidentList" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.home-page__stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
}
.home-page__map-card {
  margin-bottom: var(--spacing-lg);
  height: 600px;
}
.home-page__map-card :deep(.el-card__body) {
  height: 100%;
  padding: 8px;
}
.home-page__bottom-row {
  margin-bottom: var(--spacing-lg);
}
.home-page__chart-card {
  height: 380px;
}
.home-page__chart-card :deep(.el-card__body) {
  height: 100%;
  padding: 12px;
}
@media (max-width: 768px) {
  .home-page__stats {
    grid-template-columns: 1fr;
  }
  .home-page__bottom-row .el-col {
    max-width: 100%;
    flex: 0 0 100%;
  }
}
</style>