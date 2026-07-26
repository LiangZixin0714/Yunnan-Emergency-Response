<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useResourceStore } from '@/stores/resource'
import { useAuthStore } from '@/stores/auth'
import { getPendingResourceRequests, approveResourceRequest, rejectResourceRequest } from '@/api/resource-request'
import StatusTag from '@/components/StatusTag.vue'
import {
  ResourceTypeLabel,
  DispatchOrderStatusLabel,
  DispatchOrderStatusTagType,
} from '@/types/enums'
import type { ResourceTypeValue, DispatchOrderStatusValue } from '@/types/enums'
import type { ResourceRequest } from '@/types/resource'
import { ElMessage, ElMessageBox } from 'element-plus'

const resourceStore = useResourceStore()
const authStore = useAuthStore()

const activeTab = ref('resources')
const filterType = ref<ResourceTypeValue | ''>('')
const pendingRequests = ref<ResourceRequest[]>([])
const loadingRequests = ref(false)

const resourceTypeOptions = Object.entries(ResourceTypeLabel).map(([value, label]) => ({ value, label }))

const dispatchStatusMap = Object.fromEntries(
  Object.entries(DispatchOrderStatusLabel).map(([key, label]) => [
    key,
    { label, type: DispatchOrderStatusTagType[key as DispatchOrderStatusValue] },
  ])
)

const requestStatusMap: Record<string, { label: string; type: string }> = {
  pending: { label: '待审批', type: 'warning' },
  approved: { label: '已批准', type: 'success' },
  rejected: { label: '已驳回', type: 'danger' },
}

const addDialogVisible = ref(false)
const addForm = reactive({
  resourceName: '',
  resourceType: '' as ResourceTypeValue | '',
  unit: '个',
  totalStock: 0,
  availableStock: 0,
  location: '',
  description: '',
})

async function loadResources(): Promise<void> {
  await resourceStore.fetchList(
    filterType.value ? { resourceType: filterType.value } : undefined
  )
}

async function loadPendingRequests(): Promise<void> {
  if (authStore.roleName !== 'ADMIN') return
  loadingRequests.value = true
  try {
    const res = await getPendingResourceRequests() as unknown as ResourceRequest[]
    pendingRequests.value = res
  } catch {
    pendingRequests.value = []
  } finally {
    loadingRequests.value = false
  }
}

async function loadAll(): Promise<void> {
  await Promise.all([
    loadResources(),
    loadPendingRequests(),
  ])
}

function openAddDialog(): void {
  addForm.resourceName = ''
  addForm.resourceType = ''
  addForm.unit = '个'
  addForm.totalStock = 0
  addForm.availableStock = 0
  addForm.location = ''
  addForm.description = ''
  addDialogVisible.value = true
}

async function handleAddResource(): Promise<void> {
  if (!addForm.resourceName.trim()) {
    ElMessage.warning('请输入资源名称')
    return
  }
  try {
    await resourceStore.createNewResource({
      resourceName: addForm.resourceName,
      resourceType: addForm.resourceType || undefined,
      unit: addForm.unit || undefined,
      totalStock: addForm.totalStock,
      availableStock: addForm.availableStock,
      location: addForm.location || undefined,
      description: addForm.description || undefined,
    })
    ElMessage.success('资源添加成功')
    addDialogVisible.value = false
  } catch {
    ElMessage.error('添加资源失败')
  }
}

async function handleIncrease(row: { id: number; resourceName: string; totalStock: number; availableStock: number; lockedStock: number }): Promise<void> {
  const newTotal = row.totalStock + 1
  const newAvailable = (row.availableStock ?? 0) + 1
  try {
    await resourceStore.updateExistingResource(row.id, {
      totalStock: newTotal,
      availableStock: newAvailable,
    })
    ElMessage.success(`已增加 ${row.resourceName} 数量，当前总数：${newTotal}`)
  } catch {
    ElMessage.error('更新库存失败')
  }
}

async function handleDecrease(row: { id: number; resourceName: string; totalStock: number; availableStock: number; lockedStock: number }): Promise<void> {
  if (row.totalStock <= (row.lockedStock || 0)) {
    ElMessage.warning('数量不能小于已调度数')
    return
  }
  const newTotal = row.totalStock - 1
  const newAvailable = Math.max(0, (row.availableStock ?? 0) - 1)
  try {
    await resourceStore.updateExistingResource(row.id, {
      totalStock: newTotal,
      availableStock: newAvailable,
    })
    ElMessage.success(`已减少 ${row.resourceName} 数量，当前总数：${newTotal}`)
  } catch {
    ElMessage.error('更新库存失败')
  }
}

async function handleDelete(row: { id: number; resourceName: string }): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除资源「${row.resourceName}」？此操作不可恢复。`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await resourceStore.removeResource(row.id)
    ElMessage.success('资源已删除')
  } catch {
    return
  }
}

async function handleApproveRequest(row: ResourceRequest): Promise<void> {
  try {
    await ElMessageBox.confirm(`批准资源申请？资源：${row.resourceName}，数量：${row.quantity}`, '批准确认', {
      confirmButtonText: '确认批准',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await approveResourceRequest(row.id)
    ElMessage.success('已批准资源申请，资源已补充到库存')
    await loadPendingRequests()
    await loadResources()
  } catch {
    return
  }
}

async function handleRejectRequest(row: ResourceRequest): Promise<void> {
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回资源申请', {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      inputPattern: /.+/,
      inputErrorMessage: '驳回原因不能为空',
    })
    await rejectResourceRequest(row.id, value)
    ElMessage.success('已驳回资源申请')
    await loadPendingRequests()
  } catch {
    return
  }
}

const replenishDialogVisible = ref(false)
const replenishRow = ref<{ id: number; warningId: string; resourceType: string; requiredQuantity: number; availableQuantity: number; shortageQuantity: number } | null>(null)
const replenishNewQuantity = ref(0)

function handleReplenish(row: { id: number; warningId: string; resourceType: string; requiredQuantity: number; availableQuantity: number; shortageQuantity: number }): void {
  replenishRow.value = row
  replenishNewQuantity.value = row.requiredQuantity
  replenishDialogVisible.value = true
}

function confirmReplenish(): void {
  if (!replenishRow.value) return
  ElMessage.success('资源已补充，已通知资源管理员')
  replenishDialogVisible.value = false
  replenishRow.value = null
}

async function handleRejectShortage(_row: { warningId: string }): Promise<void> {
  try {
    await ElMessageBox.confirm('确认驳回该资源不足申请？', '驳回申请', {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      type: 'warning',
    })
    ElMessage.success('已驳回申请，已通知资源管理员')
  } catch {
    return
  }
}

onMounted(() => {
  loadAll()
})
</script>

<template>
  <div class="resource-page page-container">
    <div class="page-header">
      <h2 class="page-header__title">资源调度</h2>
      <el-button v-if="authStore.roleName === 'ADMIN'" type="primary" @click="openAddDialog">
        <el-icon><Plus /></el-icon>
        添加资源
      </el-button>
    </div>

    <el-card shadow="hover">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="资源列表" name="resources">
          <el-form inline class="resource-page__filter">
            <el-form-item label="资源类型">
              <el-select v-model="filterType" placeholder="全部" clearable style="width: 160px" @change="loadResources">
                <el-option v-for="opt in resourceTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-form>
          <el-table :data="resourceStore.resourceList" v-loading="resourceStore.loading" stripe>
            <el-table-column prop="resourceName" label="资源名称" min-width="140" />
            <el-table-column prop="resourceType" label="资源类型" width="120">
              <template #default="{ row }">
                {{ ResourceTypeLabel[row.resourceType as ResourceTypeValue] ?? row.resourceType }}
              </template>
            </el-table-column>
            <el-table-column prop="totalStock" label="总库存" width="100" />
            <el-table-column prop="lockedStock" label="已调度数" width="100">
              <template #default="{ row }">
                {{ row.lockedStock ?? 0 }}
              </template>
            </el-table-column>
            <el-table-column label="剩余库存" width="100">
              <template #default="{ row }">
                <span :style="{ color: (row.availableStock ?? 0) < 10 ? '#F56C6C' : '' }">
                  {{ row.availableStock ?? 0 }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="unit" label="单位" width="80" />
            <el-table-column prop="location" label="存放地址" min-width="160" />
            <el-table-column v-if="authStore.roleName === 'ADMIN'" label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleIncrease(row)">增加</el-button>
                <el-button link type="warning" @click="handleDecrease(row)">删减</el-button>
                <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane v-if="authStore.roleName === 'ADMIN'" label="调度指令" name="dispatch">
          <div class="dispatch-header">
            <h4 style="margin: 0">待审批资源申请</h4>
            <el-button size="small" @click="loadPendingRequests" :loading="loadingRequests">刷新</el-button>
          </div>
          <el-table :data="pendingRequests" v-loading="loadingRequests" stripe>
            <el-table-column prop="requestId" label="申请ID" width="180" />
            <el-table-column prop="incidentId" label="关联灾情" width="140" />
            <el-table-column prop="resourceName" label="资源名称" min-width="140" />
            <el-table-column prop="resourceType" label="资源类型" width="120">
              <template #default="{ row }">
                {{ ResourceTypeLabel[row.resourceType as ResourceTypeValue] ?? row.resourceType }}
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="申请数量" width="100" />
            <el-table-column prop="unit" label="单位" width="80" />
            <el-table-column prop="priority" label="优先级" width="80" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <StatusTag :status="row.status" :status-map="requestStatusMap" />
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="申请时间" width="170">
              <template #default="{ row }">{{ row.createdAt?.replace('T', ' ') }}</template>
            </el-table-column>
            <el-table-column v-if="authStore.roleName === 'ADMIN'" label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 'pending'">
                  <el-button link type="success" @click="handleApproveRequest(row)">批准</el-button>
                  <el-button link type="danger" @click="handleRejectRequest(row)">驳回</el-button>
                </template>
                <span v-else style="color: #999">-</span>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无待审批的资源申请" />
            </template>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="addDialogVisible" title="添加资源" width="520px">
      <el-form :model="addForm" label-width="100px">
        <el-form-item label="资源名称" required>
          <el-input v-model="addForm.resourceName" placeholder="请输入资源名称" />
        </el-form-item>
        <el-form-item label="资源类型">
          <el-select v-model="addForm.resourceType" placeholder="请选择类型" clearable style="width: 100%">
            <el-option v-for="opt in resourceTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="addForm.unit" placeholder="如：个、辆、套" />
        </el-form-item>
        <el-form-item label="总库存">
          <el-input-number v-model="addForm.totalStock" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="可用库存">
          <el-input-number v-model="addForm.availableStock" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="存放地址">
          <el-input v-model="addForm.location" placeholder="请输入存放地址" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="addForm.description" type="textarea" :rows="2" placeholder="资源描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddResource">确认添加</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="replenishDialogVisible" title="补充资源" width="480px">
      <template v-if="replenishRow">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="资源类型">{{ replenishRow.resourceType }}</el-descriptions-item>
          <el-descriptions-item label="需求数量">{{ replenishRow.requiredQuantity }}</el-descriptions-item>
          <el-descriptions-item label="当前可用">{{ replenishRow.availableQuantity }}</el-descriptions-item>
          <el-descriptions-item label="缺口数量">{{ replenishRow.shortageQuantity }}</el-descriptions-item>
        </el-descriptions>
        <el-form label-width="100px" style="margin-top: 16px">
          <el-form-item label="新数量">
            <el-input-number v-model="replenishNewQuantity" :min="replenishRow.availableQuantity" :step="10" />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="replenishDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReplenish">确认补充</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.resource-page__filter {
  margin-bottom: var(--spacing-sm);
}

.dispatch-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
</style>
