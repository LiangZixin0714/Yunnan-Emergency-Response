<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useIncidentStore } from '@/stores/incident'
import { useAuthStore } from '@/stores/auth'
import { useDisposalPlanStore } from '@/stores/disposal-plan'
import { useResourceStore } from '@/stores/resource'
import StatusTag from '@/components/StatusTag.vue'
import ResourceSearchItem from '@/components/ResourceSearchItem.vue'
import {
  DisasterTypeLabel,
  IncidentLevelLabel,
  DisposalPlanStatusLabel,
  DisposalPlanStatusTagType,
} from '@/types/enums'
import type { DisasterTypeValue, IncidentLevelValue, DisposalPlanStatusValue } from '@/types/enums'
import { formatDate } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'

interface SearchItem {
  id: number
  selectedResourceId: string
  increaseQuantity: number
}

const route = useRoute()
const router = useRouter()
const incidentStore = useIncidentStore()
const authStore = useAuthStore()
const disposalPlanStore = useDisposalPlanStore()
const resourceStore = useResourceStore()

const dispatchQuantity = ref<number>(0)
const selectedResourceId = ref('')

const statusMap: Record<string, { label: string; type: string }> = {
  processing: { label: '处置中', type: 'warning' },
  completed: { label: '已结束', type: 'success' },
}

const disposalPlanStatusMap = Object.fromEntries(
  Object.entries(DisposalPlanStatusLabel).map(([key, label]) => [
    key,
    { label, type: DisposalPlanStatusTagType[key as DisposalPlanStatusValue] },
  ])
)

const incidentId = route.params.id as string
const isShortageMode = computed(() => {
  const inc = incidentStore.currentIncident
  return route.query.mode === 'shortage' && authStore.roleName === 'ADMIN' && inc?.status === 'processing' && (inc?.disposalPlanStatus === 'submitted' || inc?.disposalPlanStatus === 'resubmitted') && inc?.resourceDispatchStatus === 'shortage'
})

const searchItems = ref<SearchItem[]>([{ id: 1, selectedResourceId: '', increaseQuantity: 0 }])
let searchItemIdCounter = 1

const canCreateDisposalPlan = computed(() => {
  const role = authStore.roleName
  const dpStatus = incidentStore.currentIncident?.disposalPlanStatus
  return role === 'OPERATOR' && !dpStatus
})

const canRecreateDisposalPlan = computed(() => {
  const role = authStore.roleName
  const dpStatus = incidentStore.currentIncident?.disposalPlanStatus
  return role === 'OPERATOR' && dpStatus === 'rejected'
})

const canDispatchResource = computed(() => {
  const role = authStore.roleName
  const status = incidentStore.currentIncident?.status
  const dpStatus = incidentStore.currentIncident?.disposalPlanStatus
  return role === 'RESOURCE_MANAGER' && status !== 'completed' && (dpStatus === 'submitted' || dpStatus === 'resubmitted')
})

const canRejectPlan = computed(() => {
  const role = authStore.roleName
  const dpStatus = incidentStore.currentIncident?.disposalPlanStatus
  return role === 'RESOURCE_MANAGER' && (dpStatus === 'submitted' || dpStatus === 'resubmitted')
})

const canShowRejectedTip = computed(() => {
  const role = authStore.roleName
  const dpStatus = incidentStore.currentIncident?.disposalPlanStatus
  return role === 'RESOURCE_MANAGER' && dpStatus === 'rejected'
})

const canShowResourceRequestTip = computed(() => {
  return incidentStore.currentIncident?.resourceDispatchStatus === 'shortage'
})

const imageList = computed<string[]>(() => {
  const raw = incidentStore.currentIncident?.imageUrls
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
})

function addSearchItem(): void {
  searchItemIdCounter++
  searchItems.value.push({ id: searchItemIdCounter, selectedResourceId: '', increaseQuantity: 0 })
}

function removeSearchItem(id: number): void {
  if (searchItems.value.length <= 1) return
  searchItems.value = searchItems.value.filter((item) => item.id !== id)
}

function handleIncreaseConfirm(resourceId: string, quantity: number): void {
  const res = resourceStore.resourceList.find((r) => r.resourceId === resourceId)
  if (!res) return
  const currentDispatched = res.dispatchedCount ?? 0
  if (currentDispatched + quantity > res.quantity) {
    ElMessage.warning('请去总仓库增添资源')
    return
  }
  res.dispatchedCount = currentDispatched + quantity
  ElMessage.success(`已增加 ${res.name} 调度数${quantity}，当前已调度：${res.dispatchedCount}`)
}

async function handleRejectShortage(): Promise<void> {
  try {
    await ElMessageBox.confirm('确认驳回该资源不足申请？', '驳回申请', {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await fetch('/api/resource-shortage/reject', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${sessionStorage.getItem('token') || ''}` },
      body: JSON.stringify({ incidentId }),
    })
    await incidentStore.fetchDetail(incidentId)
    ElMessage.success('已驳回资源不足申请')
  } catch {
    return
  }
}

async function handleResolveShortage(): Promise<void> {
  try {
    await fetch('/api/resource-shortage/resolve', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${sessionStorage.getItem('token') || ''}` },
      body: JSON.stringify({ incidentId }),
    })
    await incidentStore.fetchDetail(incidentId)
    ElMessage.success('资源不足处理完成')
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleRejectPlan(): Promise<void> {
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回处置方案', {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      inputPattern: /.+/,
      inputErrorMessage: '驳回原因不能为空',
    })
    let planId = disposalPlanStore.currentDisposalPlan?.id
    if (!planId && disposalPlanStore.disposalPlanList.length > 0) {
      planId = disposalPlanStore.disposalPlanList[0].id
      disposalPlanStore.currentDisposalPlan = disposalPlanStore.disposalPlanList[0]
    }
    if (planId) {
      await disposalPlanStore.rejectDisposalPlan(planId, value, incidentId)
      ElMessage.success('方案已驳回')
      await incidentStore.fetchDetail(incidentId)
      await disposalPlanStore.fetchList(incidentId)
      if (disposalPlanStore.disposalPlanList.length > 0) {
        disposalPlanStore.currentDisposalPlan = disposalPlanStore.disposalPlanList[0]
      }
    } else {
      ElMessage.error('未找到关联的处置方案')
    }
  } catch {
    return
  }
}

function handleRequestResource(): void {
  router.push(`/resource-request/${incidentId}`)
}

async function handleDispatchResource(): Promise<void> {
  if (!selectedResourceId.value || dispatchQuantity.value <= 0) {
    ElMessage.warning('请选择资源并输入分配数量')
    return
  }
  const res = resourceStore.resourceList.find((r) => r.resourceId === selectedResourceId.value)
  if (!res) return
  const currentDispatched = res.dispatchedCount ?? 0
  if (currentDispatched + dispatchQuantity.value > res.quantity) {
    ElMessage.warning('该资源不足')
    return
  }
  try {
    await fetch('/api/resource-dispatch/submit', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${sessionStorage.getItem('token') || ''}` },
      body: JSON.stringify({ incidentId, resourceId: selectedResourceId.value, quantity: dispatchQuantity.value }),
    })
    await incidentStore.fetchDetail(incidentId)
    await disposalPlanStore.fetchList(incidentId)
    if (disposalPlanStore.disposalPlanList.length > 0) {
      disposalPlanStore.currentDisposalPlan = disposalPlanStore.disposalPlanList[0]
    }
    ElMessage.success('资源调度指令已提交')
    selectedResourceId.value = ''
    dispatchQuantity.value = 0
  } catch {
    ElMessage.error('提交调度失败')
  }
}

onMounted(async () => {
  await incidentStore.fetchDetail(incidentId)
  await disposalPlanStore.fetchList(incidentId)
  if (disposalPlanStore.disposalPlanList.length > 0) {
    disposalPlanStore.currentDisposalPlan = disposalPlanStore.disposalPlanList[0]
  } else {
    disposalPlanStore.currentDisposalPlan = null
  }
  resourceStore.fetchList()
})
</script>

<template>
  <div class="incident-detail page-container">
    <div class="page-header">
      <h2 class="page-header__title">事件详情</h2>
      <el-button @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
    </div>

    <el-card shadow="hover" v-loading="incidentStore.loading">
      <template v-if="incidentStore.currentIncident">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="事件名称">
            {{ incidentStore.currentIncident.incidentName }}
          </el-descriptions-item>
          <el-descriptions-item label="灾害类型">
            {{ DisasterTypeLabel[incidentStore.currentIncident.disasterType as DisasterTypeValue] }}
          </el-descriptions-item>
          <el-descriptions-item label="事件等级">
            {{ IncidentLevelLabel[incidentStore.currentIncident.incidentLevel as IncidentLevelValue] }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :status="incidentStore.currentIncident.status" :status-map="statusMap" />
          </el-descriptions-item>
          <el-descriptions-item label="发生时间">
            {{ incidentStore.currentIncident.occurTime ? formatDate(incidentStore.currentIncident.occurTime) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="发生地点">
            {{ incidentStore.currentIncident.location }}
          </el-descriptions-item>
          <el-descriptions-item label="死亡人数">
            {{ incidentStore.currentIncident.deathCount ?? '未填写' }}
          </el-descriptions-item>
          <el-descriptions-item label="财产损失（万元）">
            {{ incidentStore.currentIncident.propertyLoss ?? '未填写' }}
          </el-descriptions-item>
          <el-descriptions-item label="上报人">
            {{ incidentStore.currentIncident.reporterName || incidentStore.currentIncident.reporterId || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="上报时间">
            {{ incidentStore.currentIncident.reportTime ? formatDate(incidentStore.currentIncident.reportTime) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="事件描述" :span="2">
            {{ incidentStore.currentIncident.description }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="incident-detail__actions" v-if="canCreateDisposalPlan">
          <el-button type="primary" @click="router.push(`/plan?incidentId=${incidentId}`)">拟定处置方案</el-button>
        </div>

        <el-card
          v-if="incidentStore.currentIncident.disposalPlanStatus !== null"
          shadow="hover"
          class="incident-detail__disposal-plan"
        >
          <template #header>
            <div class="incident-detail__disposal-plan-header">
              <span>处置方案</span>
              <StatusTag
                :status="incidentStore.currentIncident.disposalPlanStatus!"
                :status-map="disposalPlanStatusMap"
              />
            </div>
          </template>
          <p v-if="disposalPlanStore.currentDisposalPlan?.planContent">
            {{ disposalPlanStore.currentDisposalPlan.planContent.substring(0, 200) }}{{ disposalPlanStore.currentDisposalPlan.planContent.length > 200 ? '...' : '' }}
          </p>
          <p v-else>暂无方案内容</p>
          <div v-if="incidentStore.currentIncident.disposalPlanStatus === 'rejected'" class="incident-detail__reject-info">
            <p>驳回原因：{{ disposalPlanStore.currentDisposalPlan?.rejectReason ?? '未填写' }}</p>
            <el-button v-if="canRecreateDisposalPlan" type="primary" size="small" @click="router.push(`/plan?incidentId=${incidentId}`)">重新拟定</el-button>
          </div>
        </el-card>

        <el-card
          v-if="isShortageMode"
          shadow="hover"
          class="incident-detail__shortage"
        >
          <template #header>
            <div class="incident-detail__shortage-header">
              <span>资源不足处理</span>
              <el-button type="danger" size="small" @click="handleRejectShortage">驳回申请</el-button>
            </div>
          </template>
          <div class="incident-detail__search-list">
            <div
              v-for="item in searchItems"
              :key="item.id"
              class="incident-detail__search-row"
            >
              <ResourceSearchItem
                :resource-list="resourceStore.resourceList"
                v-model="item.selectedResourceId"
                v-model:increase-quantity="item.increaseQuantity"
                @confirm="handleIncreaseConfirm"
              />
              <el-button
                v-if="searchItems.length > 1"
                type="danger"
                link
                style="margin-left: 8px"
                @click="removeSearchItem(item.id)"
              >删除</el-button>
            </div>
          </div>
          <el-button type="primary" plain @click="addSearchItem" style="margin-top: 8px">增加资源</el-button>
          <div style="margin-top: 16px; display: flex; gap: 8px">
            <el-button type="success" @click="handleResolveShortage">完成处理</el-button>
          </div>
        </el-card>

        <el-card
          v-if="canDispatchResource"
          shadow="hover"
          class="incident-detail__dispatch"
        >
          <template #header>
            <div style="display:flex;align-items:center;justify-content:space-between;width:100%">
              <span>资源调度分配</span>
              <span v-if="canShowResourceRequestTip" style="color:#F56C6C;font-size:13px">已提交资源申请，等待审批</span>
            </div>
          </template>
          <el-form inline>
            <el-form-item label="选择资源">
              <el-select v-model="selectedResourceId" placeholder="请选择资源" style="width: 280px">
                <el-option
                  v-for="res in resourceStore.resourceList"
                  :key="res.resourceId"
                  :label="`${res.name}（总数${res.quantity}，已调度${res.dispatchedCount ?? 0}，可分配${res.quantity - (res.dispatchedCount ?? 0)}）`"
                  :value="res.resourceId"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="分配数量">
              <el-input-number v-model="dispatchQuantity" :min="1" :step="1" :precision="0" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleDispatchResource">提交调度</el-button>
              <el-button type="primary" plain @click="handleRequestResource">请求资源</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <div class="incident-detail__actions" v-if="canRejectPlan">
          <el-button type="danger" @click="handleRejectPlan">驳回方案</el-button>
        </div>

        <el-alert
          v-if="canShowRejectedTip"
          type="warning"
          :closable="false"
          show-icon
          class="incident-detail__rejected-tip"
        >
          <template #title>
            已驳回方案，等待再次提交
          </template>
        </el-alert>

        <div class="incident-detail__images">
          <h3>现场图片</h3>
          <div v-if="imageList.length" class="incident-detail__image-list">
            <el-image
              v-for="(url, index) in imageList"
              :key="index"
              :src="url"
              fit="cover"
              class="incident-detail__image"
              :preview-src-list="imageList"
              :initial-index="index"
            >
              <template #error>
                <div class="incident-detail__image-error">
                  <el-icon><PictureFilled /></el-icon>
                  <span>图片加载失败</span>
                </div>
              </template>
            </el-image>
          </div>
          <p v-else class="incident-detail__no-images">未上传图片</p>
        </div>
      </template>
    </el-card>
  </div>
</template>

<style scoped>
.incident-detail__actions {
  margin-top: var(--spacing-md);
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--color-border-light);
  display: flex;
  gap: var(--spacing-sm);
}

.incident-detail__images {
  margin-top: var(--spacing-lg);
}

.incident-detail__images h3 {
  font-size: var(--font-size-lg);
  margin-bottom: var(--spacing-md);
}

.incident-detail__image-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
}

.incident-detail__image {
  width: 160px;
  height: 120px;
  border-radius: var(--border-radius-sm);
  border: 1px solid var(--color-border-light);
}

.incident-detail__image-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: var(--color-bg-page);
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  gap: var(--spacing-xs);
}

.incident-detail__disposal-plan {
  margin-top: var(--spacing-md);
}

.incident-detail__disposal-plan-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.incident-detail__reject-info {
  margin-top: var(--spacing-sm);
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--color-border-light);
  color: var(--color-text-secondary);
}

.incident-detail__shortage {
  margin-top: var(--spacing-md);
}

.incident-detail__shortage-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.incident-detail__search-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.incident-detail__search-row {
  display: flex;
  align-items: center;
}

.incident-detail__dispatch {
  margin-top: var(--spacing-md);
}

.incident-detail__rejected-tip {
  margin-top: var(--spacing-md);
}

.incident-detail__no-images {
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
  padding: var(--spacing-md) 0;
}
</style>
