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
  pending: { label: '处置中', type: 'warning' },
  processing: { label: '处置中', type: 'warning' },
  completed: { label: '已结束', type: 'success' },
}

const unifiedStatusMap: Record<string, { label: string; type: string }> = {
  pending_review: { label: '待审核', type: 'warning' },
  requesting_resource: { label: '申请资源中', type: 'info' },
  rejected: { label: '已驳回', type: 'danger' },
  completed: { label: '已完成', type: 'success' },
  no_plan: { label: '待拟方案', type: 'info' },
  dispatch_done: { label: '调度完成', type: 'success' },
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
  if (authStore.roleName === 'RESOURCE_MANAGER' && row.status === 'processing' && (row.disposalPlanStatus === 'submitted' || row.disposalPlanStatus === 'resubmitted') && row.resourceDispatchStatus === 'shortage') {
    router.push(`/incident/${row.incidentId}?mode=shortage`)
  } else {
    router.push(`/incident/${row.incidentId}`)
  }
}

function getUnifiedStatus(row: { incidentId: string; resourceDispatchStatus: string | null; disposalPlanStatus: string | null; status: string }): string {
  if (row.status === 'completed') return 'completed'
  if (row.disposalPlanStatus === 'rejected') return 'rejected'
  if (!row.disposalPlanStatus || row.disposalPlanStatus === 'draft') return 'no_plan'
  if (row.disposalPlanStatus === 'submitted' || row.disposalPlanStatus === 'resubmitted') return 'pending_review'
  if (row.disposalPlanStatus === 'accepted' && row.resourceDispatchStatus === 'shortage') return 'requesting_resource'
  if (row.resourceDispatchStatus === 'executing' || row.resourceDispatchStatus === 'completed') return 'dispatch_done'
  if (row.disposalPlanStatus === 'accepted') return 'pending_review'
  return 'no_plan'
}

function getActionLabel(row: { resourceDispatchStatus: string | null; disposalPlanStatus: string | null; status: string }): string {
  if (authStore.roleName === 'RESOURCE_MANAGER' && row.status === 'processing' && (row.disposalPlanStatus === 'submitted' || row.disposalPlanStatus === 'resubmitted') && row.resourceDispatchStatus === 'shortage') {
    return '资源调度'
  }
  return '查看'
}

function getImageList(imageUrls: string | null): string[] {
  if (!imageUrls) return []
  try {
    const parsed = JSON.parse(imageUrls)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function getFirstImage(imageUrls: string | null): string | null {
  const list = getImageList(imageUrls)
  return list.length > 0 ? list[0] : null
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
        <el-table-column label="现场图片" width="80">
          <template #default="{ row }">
            <div v-if="getFirstImage(row.imageUrls)" class="incident-list__image-wrapper">
              <el-image
                :src="getFirstImage(row.imageUrls)!"
                fit="cover"
                class="incident-list__image"
                :preview-src-list="getImageList(row.imageUrls)"
              />
            </div>
            <span v-else class="incident-list__no-image">-</span>
          </template>
        </el-table-column>
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
          label="处置进度"
          width="120"
        >
          <template #default="{ row }">
            <StatusTag
              :status="getUnifiedStatus(row)"
              :status-map="unifiedStatusMap"
            />
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

.incident-list__image-wrapper {
  width: 60px;
  height: 60px;
  border-radius: var(--border-radius-sm);
  overflow: hidden;
  border: 1px solid var(--color-border-light);
}

.incident-list__image {
  width: 100%;
  height: 100%;
}

.incident-list__no-image {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}
</style>
