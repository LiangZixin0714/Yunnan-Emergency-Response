<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useResourceStore } from '@/stores/resource'
import { useAuthStore } from '@/stores/auth'
import StatusTag from '@/components/StatusTag.vue'
import {
  ResourceTypeLabel,
  DispatchOrderStatusLabel,
  DispatchOrderStatusTagType,
} from '@/types/enums'
import type { ResourceTypeValue, DispatchOrderStatusValue } from '@/types/enums'
import { ElMessage, ElMessageBox } from 'element-plus'

const resourceStore = useResourceStore()
const authStore = useAuthStore()

const activeTab = ref('resources')
const filterType = ref<ResourceTypeValue | ''>('')

const resourceTypeOptions = Object.entries(ResourceTypeLabel).map(([value, label]) => ({ value, label }))

const dispatchStatusMap = Object.fromEntries(
  Object.entries(DispatchOrderStatusLabel).map(([key, label]) => [
    key,
    { label, type: DispatchOrderStatusTagType[key as DispatchOrderStatusValue] },
  ])
)

const replenishDialogVisible = ref(false)
const replenishRow = ref<{ id: number; warningId: string; resourceType: string; requiredQuantity: number; availableQuantity: number; shortageQuantity: number } | null>(null)
const replenishNewQuantity = ref(0)

async function loadResources(): Promise<void> {
  await resourceStore.fetchList(
    filterType.value ? { resourceType: filterType.value } : undefined
  )
}

async function loadAll(): Promise<void> {
  await Promise.all([
    loadResources(),
    resourceStore.fetchDispatchOrders(),
  ])
}

async function handleIncrease(row: { id: number; name: string; quantity: number; dispatchedCount: number }): Promise<void> {
  const newQuantity = row.quantity + 1
  const res = resourceStore.resourceList.find((r) => r.id === row.id)
  if (res) {
    res.quantity = newQuantity
    ElMessage.success(`已增加 ${row.name} 数量，当前总数：${newQuantity}`)
  }
}

async function handleDecrease(row: { id: number; name: string; quantity: number; dispatchedCount: number }): Promise<void> {
  if (row.quantity <= (row.dispatchedCount || 0)) {
    ElMessage.warning('数量不能小于已调度数')
    return
  }
  const newQuantity = row.quantity - 1
  const res = resourceStore.resourceList.find((r) => r.id === row.id)
  if (res) {
    res.quantity = newQuantity
    ElMessage.success(`已减少 ${row.name} 数量，当前总数：${newQuantity}`)
  }
}

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
            <el-table-column prop="name" label="资源名称" min-width="140" />
            <el-table-column prop="resourceType" label="资源类型" width="120">
              <template #default="{ row }">
                {{ ResourceTypeLabel[row.resourceType as ResourceTypeValue] ?? row.resourceType }}
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="100" />
            <el-table-column prop="unit" label="单位" width="80" />
            <el-table-column prop="dispatchedCount" label="已调度数" width="100">
              <template #default="{ row }">
                {{ row.dispatchedCount ?? 0 }}
              </template>
            </el-table-column>
            <el-table-column prop="storageAddress" label="存放地址" min-width="160" />
            <el-table-column v-if="authStore.roleName === 'ADMIN'" label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleIncrease(row)">增加</el-button>
                <el-button link type="danger" @click="handleDecrease(row)">删减</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="调度指令" name="dispatch">
          <el-table :data="resourceStore.dispatchOrders" stripe>
            <el-table-column prop="orderId" label="指令ID" width="160" />
            <el-table-column prop="incidentId" label="关联灾情" width="140" />
            <el-table-column prop="content" label="调度内容" min-width="200" />
            <el-table-column prop="priority" label="优先级" width="80" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <StatusTag :status="row.status" :status-map="dispatchStatusMap" />
              </template>
            </el-table-column>
            <el-table-column prop="dispatchTime" label="调度时间" width="180" />
            <el-table-column v-if="authStore.roleName === 'ADMIN'" label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 'shortage'">
                  <el-button link type="primary" @click="handleReplenish(row)">补充资源</el-button>
                  <el-button link type="danger" @click="handleRejectShortage(row)">驳回申请</el-button>
                </template>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

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
</style>
