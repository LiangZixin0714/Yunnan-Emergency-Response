<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useIncidentStore } from '@/stores/incident'
import { useAuthStore } from '@/stores/auth'
import StatusTag from '@/components/StatusTag.vue'
import {
  DisasterTypeLabel,
  IncidentLevelLabel,
  DisposalPlanStatusLabel,
  DisposalPlanStatusTagType,
  ResourceDispatchStatusLabel,
  ResourceDispatchStatusTagType,
} from '@/types/enums'
import type { DisasterTypeValue, IncidentLevelValue, DisposalPlanStatusValue, ResourceDispatchStatusValue } from '@/types/enums'

const router = useRouter()
const incidentStore = useIncidentStore()
const authStore = useAuthStore()

const currentPage = ref(1)
const pageSize = ref(20)

const filters = reactive({
  disasterType: '' as DisasterTypeValue | '',
  incidentLevel: '' as IncidentLevelValue | '',
  keyword: '',
})

const disasterOptions = Object.entries(DisasterTypeLabel).map(([value, label]) => ({ value, label }))
const levelOptions = Object.entries(IncidentLevelLabel).map(([value, label]) => ({ value, label }))

const incidentStatusMap: Record<string, { label: string; type: string }> = {
  processing: { label: '处置中', type: 'warning' },
  completed: { label: '已结束', type: 'success' },
}

const disposalPlanStatusMap = Object.fromEntries(
  Object.entries(DisposalPlanStatusLabel).map(([key, label]) => [
    key,
    { label, type: DisposalPlanStatusTagType[key as DisposalPlanStatusValue] },
  ])
)

const resourceDispatchStatusMap = Object.fromEntries(
  Object.entries(ResourceDispatchStatusLabel).map(([key, label]) => [
    key,
    { label, type: ResourceDispatchStatusTagType[key as ResourceDispatchStatusValue] },
  ])
)

async function loadData(): Promise<void> {
  await incidentStore.fetchList({
    page: currentPage.value,
    size: pageSize.value,
    disasterType: filters.disasterType || undefined,
    incidentLevel: filters.incidentLevel || undefined,
    keyword: filters.keyword || undefined,
  })
}

function handlePageChange(page: number): void {
  currentPage.value = page
  loadData()
}

function handleSizeChange(size: number): void {
  pageSize.value = size
  currentPage.value = 1
  loadData()
}

function handleSearch(): void {
  currentPage.value = 1
  loadData()
}

function resetFilters(): void {
  filters.disasterType = ''
  filters.incidentLevel = ''
  filters.keyword = ''
  currentPage.value = 1
  loadData()
}


function handleAction(row: { incidentId: string; resourceDispatchStatus: string | null; disposalPlanStatus: string | null; status: string }): void {
  if (authStore.roleName === 'ADMIN' && row.resourceDispatchStatus === 'shortage') {
    router.push(`/incident/${row.incidentId}?mode=shortage`)
  } else {
    router.push(`/incident/${row.incidentId}`)
  }
}

function getActionLabel(row: { resourceDispatchStatus: string | null; disposalPlanStatus: string | null }): string {
  if (authStore.roleName === 'ADMIN' && row.resourceDispatchStatus === 'shortage') {
    return '资源调度'
  }
  return '查看'
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="incident-list page-container">
    <div class="page-header">
      <h2 class="page-header__title">事件列表</h2>
      <el-button v-if="authStore.roleName === 'VIEWER'" type="primary" @click="router.push('/incident/report')">
        <el-icon><Plus /></el-icon>
        新增上报
      </el-button>
    </div>

    <el-card shadow="hover" class="incident-list__filters">
      <el-form inline>
        <el-form-item label="灾害类型">
          <el-select v-model="filters.disasterType" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="opt in disasterOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="事件等级">
          <el-select v-model="filters.incidentLevel" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="opt in levelOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="搜索">
          <el-input v-model="filters.keyword" placeholder="事件名称" clearable @clear="handleSearch" @keyup.enter="handleSearch" style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <el-table :data="incidentStore.incidentList" v-loading="incidentStore.loading" stripe>
        <el-table-column prop="incidentName" label="事件名称" min-width="160" />
        <el-table-column prop="disasterType" label="灾害类型" width="120">
          <template #default="{ row }">
            {{ DisasterTypeLabel[row.disasterType as DisasterTypeValue] ?? row.disasterType }}
          </template>
        </el-table-column>
        <el-table-column prop="incidentLevel" label="事件等级" width="140">
          <template #default="{ row }">
            {{ IncidentLevelLabel[row.incidentLevel as IncidentLevelValue] ?? row.incidentLevel }}
          </template>
        </el-table-column>
        <el-table-column prop="occurTime" label="发生时间" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.status" :status-map="incidentStatusMap" />
          </template>
        </el-table-column>
        <el-table-column
          v-if="authStore.roleName === 'OPERATOR' || authStore.roleName === 'RESOURCE_MANAGER' || authStore.roleName === 'ADMIN'"
          prop="disposalPlanStatus"
          label="处置方案状态"
          width="130"
        >
          <template #default="{ row }">
            <StatusTag v-if="row.disposalPlanStatus && (row.disposalPlanStatus === 'submitted' || row.disposalPlanStatus === 'rejected' || row.disposalPlanStatus === 'accepted')" :status="row.disposalPlanStatus" :status-map="disposalPlanStatusMap" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column
          v-if="authStore.roleName === 'RESOURCE_MANAGER' || authStore.roleName === 'ADMIN'"
          prop="resourceDispatchStatus"
          label="资源调度状态"
          width="130"
        >
          <template #default="{ row }">
            <StatusTag v-if="row.resourceDispatchStatus && (row.resourceDispatchStatus === 'executing' || row.resourceDispatchStatus === 'shortage' || row.resourceDispatchStatus === 'completed')" :status="row.resourceDispatchStatus" :status-map="resourceDispatchStatusMap" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleAction(row)">{{ getActionLabel(row) }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="incident-list__pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="incidentStore.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.incident-list__filters {
  margin-bottom: var(--spacing-md);
}

.incident-list__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--spacing-md);
}
</style>
