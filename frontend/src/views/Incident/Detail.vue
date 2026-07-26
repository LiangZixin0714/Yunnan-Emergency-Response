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
} from '@/types/enums'
import type { DisasterTypeValue, IncidentLevelValue } from '@/types/enums'
import { formatDate } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'

interface SearchItem {
  id: number
  selectedResourceId: string
  increaseQuantity: number
}

interface DispatchItem {
  id: number
  resourceId: string
  quantity: number
}

const route = useRoute()
const router = useRouter()
const incidentStore = useIncidentStore()
const authStore = useAuthStore()
const disposalPlanStore = useDisposalPlanStore()
const resourceStore = useResourceStore()

const dispatchItems = ref<DispatchItem[]>([{ id: 1, resourceId: '', quantity: 1 }])
let dispatchItemIdCounter = 1

const statusMap: Record<string, { label: string; type: string }> = {
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

function getUnifiedStatus(): string {
  const inc = incidentStore.currentIncident
  if (!inc) return 'no_plan'
  if (inc.status === 'completed') return 'completed'
  if (inc.disposalPlanStatus === 'rejected') return 'rejected'
  if (!inc.disposalPlanStatus || inc.disposalPlanStatus === 'draft') return 'no_plan'
  if (inc.disposalPlanStatus === 'submitted' || inc.disposalPlanStatus === 'resubmitted') return 'pending_review'
  if (inc.disposalPlanStatus === 'accepted' && inc.resourceDispatchStatus === 'shortage') return 'requesting_resource'
  if (inc.resourceDispatchStatus === 'executing' || inc.resourceDispatchStatus === 'completed') return 'dispatch_done'
  if (inc.disposalPlanStatus === 'accepted') return 'pending_review'
  return 'no_plan'
}

const incidentId = route.params.id as string
const isShortageMode = computed(() => {
  const inc = incidentStore.currentIncident
  return route.query.mode === 'shortage' && authStore.roleName === 'RESOURCE_MANAGER' && inc?.status === 'processing' && (inc?.disposalPlanStatus === 'submitted' || inc?.disposalPlanStatus === 'resubmitted') && inc?.resourceDispatchStatus === 'shortage'
})

const searchItems = ref<SearchItem[]>([{ id: 1, selectedResourceId: '', increaseQuantity: 0 }])
let searchItemIdCounter = 1

const canCreateDisposalPlan = computed(() => {
  const role = authStore.roleName
  const dpStatus = incidentStore.currentIncident?.disposalPlanStatus
  const status = incidentStore.currentIncident?.status
  return role === 'OPERATOR' && !dpStatus && status !== 'completed'
})

const canRecreateDisposalPlan = computed(() => {
  const role = authStore.roleName
  const dpStatus = incidentStore.currentIncident?.disposalPlanStatus
  const status = incidentStore.currentIncident?.status
  return role === 'OPERATOR' && dpStatus === 'rejected' && status !== 'completed'
})

const canSubmitPlan = computed(() => {
  const role = authStore.roleName
  const dpStatus = incidentStore.currentIncident?.disposalPlanStatus
  const status = incidentStore.currentIncident?.status
  return role === 'OPERATOR' && dpStatus === 'draft' && status !== 'completed'
})

const canDispatchResource = computed(() => {
  const role = authStore.roleName
  const status = incidentStore.currentIncident?.status
  const dpStatus = incidentStore.currentIncident?.disposalPlanStatus
  return (role === 'RESOURCE_MANAGER' || role === 'ADMIN') && status !== 'completed' && (dpStatus === 'submitted' || dpStatus === 'resubmitted' || dpStatus === 'accepted' || dpStatus === 'draft' || !dpStatus)
})

const canRejectPlan = computed(() => {
  const role = authStore.roleName
  const dpStatus = incidentStore.currentIncident?.disposalPlanStatus
  return (role === 'RESOURCE_MANAGER' || role === 'ADMIN') && dpStatus === 'submitted'
})

const canCompleteIncident = computed(() => {
  const role = authStore.roleName
  const status = incidentStore.currentIncident?.status
  const dpStatus = incidentStore.currentIncident?.disposalPlanStatus
  const rdStatus = incidentStore.currentIncident?.resourceDispatchStatus
  return (role === 'RESOURCE_MANAGER' || role === 'ADMIN') && status === 'processing' && dpStatus === 'accepted' && (rdStatus === 'executing' || rdStatus === 'completed')
})

const canShowRejectedTip = computed(() => {
  const dpStatus = incidentStore.currentIncident?.disposalPlanStatus
  return dpStatus === 'rejected'
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

function renderMarkdown(content: string): string {
  if (!content) return ''

  let text = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  text = text.replace(/\*\*(.*?)\*\*/g, '<strong style="color:#333">$1</strong>')

  const lines = text.split('\n')
  const blocks: string[] = []
  let paraBuffer: string[] = []
  let listBuffer: string[] = []

  function flushPara(): void {
    if (paraBuffer.length > 0) {
      const paraHtml = paraBuffer.join('<br/>')
      blocks.push(`<p style="margin:6px 0;line-height:1.9;">${paraHtml}</p>`)
      paraBuffer = []
    }
  }

  function flushList(): void {
    if (listBuffer.length > 0) {
      const lis = listBuffer.map(line => {
        const t = line.replace(/^\s*[-*]\s*/, '')
        return `<li style="margin-left:18px;list-style-type:disc;line-height:1.9;">${t}</li>`
      }).join('')
      blocks.push(`<ul style="margin:6px 0;padding-left:0;">${lis}</ul>`)
      listBuffer = []
    }
  }

  for (const line of lines) {
    const headingMatch = line.match(/^(#{1,6})\s*(.+)$/)
    if (headingMatch) {
      flushPara()
      flushList()
      const level = headingMatch[1].length
      const fontSize = Math.max(13, 24 - level * 2)
      blocks.push(`<div style="margin:14px 0 8px;font-weight:bold;font-size:${fontSize}px;color:#1a73e8;border-left:3px solid #1a73e8;padding-left:8px;">${headingMatch[2].trim()}</div>`)
      continue
    }

    const listMatch = line.match(/^\s*[-*]\s+/)
    if (listMatch) {
      flushPara()
      listBuffer.push(line)
      continue
    } else if (listBuffer.length > 0) {
      flushList()
    }

    if (line.trim() === '') {
      flushPara()
      flushList()
      continue
    }

    flushList()
    paraBuffer.push(line)
  }

  flushPara()
  flushList()

  return blocks.join('')
}

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
  const currentAvailable = res.availableStock ?? 0
  res.availableStock = currentAvailable + quantity
  res.lockedStock = (res.lockedStock ?? 0) + quantity
  ElMessage.success(`已增加 ${res.resourceName} 调度数${quantity}，当前已调度：${res.lockedStock}`)
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

async function handleSubmitPlan(): Promise<void> {
  try {
    const planId = disposalPlanStore.currentDisposalPlan?.id
    if (!planId) {
      ElMessage.error('未找到处置方案')
      return
    }
    await disposalPlanStore.submitDisposalPlan(planId, disposalPlanStore.currentDisposalPlan?.planContent || '', incidentId)
    ElMessage.success('方案已提交，等待资源管理员审核')
    await incidentStore.fetchDetail(incidentId)
    await disposalPlanStore.fetchList(incidentId)
  } catch {
    ElMessage.error('提交方案失败')
  }
}

function handleRequestResource(): void {
  router.push(`/resource-request/${incidentId}`)
}

function addDispatchItem(): void {
  dispatchItemIdCounter++
  dispatchItems.value.push({ id: dispatchItemIdCounter, resourceId: '', quantity: 1 })
}

function removeDispatchItem(id: number): void {
  if (dispatchItems.value.length <= 1) return
  dispatchItems.value = dispatchItems.value.filter((item) => item.id !== id)
}

async function handleDispatchResource(): Promise<void> {
  const validItems = dispatchItems.value.filter((item) => item.resourceId && item.quantity > 0)
  if (validItems.length === 0) {
    ElMessage.warning('请至少选择一个资源并输入分配数量')
    return
  }

  for (const item of validItems) {
    const res = resourceStore.resourceList.find((r) => r.resourceId === item.resourceId)
    if (!res) {
      ElMessage.error(`资源 ${item.resourceId} 不存在`)
      return
    }
    const currentAvailable = res.availableStock ?? 0
    if (currentAvailable < item.quantity) {
      ElMessage.warning(`资源「${res.resourceName}」可用库存不足，当前剩余: ${currentAvailable}`)
      return
    }
  }

  let successCount = 0
  for (const item of validItems) {
    try {
      const result = await resourceStore.lockResource({
        resourceId: item.resourceId,
        quantity: item.quantity,
        incidentId: incidentId as string,
        remark: '资源调度分配',
      })
      if (result) successCount++
    } catch {
      ElMessage.error(`资源调度失败: ${item.resourceId}`)
      break
    }
  }

  if (successCount > 0) {
    ElMessage.success(`资源调度成功！共调度 ${successCount} 个资源。`)
    dispatchItems.value = [{ id: 1, resourceId: '', quantity: 1 }]
    dispatchItemIdCounter = 1
    await incidentStore.fetchDetail(incidentId)
    await resourceStore.fetchList()
  }
}

async function handleCompleteIncident(): Promise<void> {
  try {
    await ElMessageBox.confirm('确认结束本次处置？结束后事件状态将变为"已结束"。', '结束处置', {
      confirmButtonText: '确认结束',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await incidentStore.completeIncidentAction(incidentId)
    ElMessage.success('处置已结束')
  } catch {
    return
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
          <el-descriptions-item label="处置进度">
            <StatusTag :status="getUnifiedStatus()" :status-map="unifiedStatusMap" />
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
                :status="getUnifiedStatus()"
                :status-map="unifiedStatusMap"
              />
            </div>
          </template>
          
          <div v-if="disposalPlanStore.currentDisposalPlan?.planContent" class="disposal-plan-content-scroll">
            <div class="markdown-body-preview" v-html="renderMarkdown(disposalPlanStore.currentDisposalPlan.planContent)"></div>
          </div>
          <p v-else class="incident-detail__no-images">暂无方案内容</p>

          <div v-if="incidentStore.currentIncident.disposalPlanStatus === 'rejected'" class="incident-detail__reject-info">
            <p>驳回原因：{{ disposalPlanStore.currentDisposalPlan?.rejectReason ?? '未填写' }}</p>
            <el-button v-if="canRecreateDisposalPlan" type="primary" size="small" @click="router.push(`/plan?incidentId=${incidentId}`)">重新拟定</el-button>
          </div>

          <div v-if="canSubmitPlan" style="margin-top: 12px; display: flex; gap: 8px">
            <el-button type="primary" @click="handleSubmitPlan">提交方案</el-button>
            <el-button plain @click="router.push(`/plan?incidentId=${incidentId}`)">编辑方案</el-button>
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
          <div class="incident-detail__dispatch-list">
            <div
              v-for="item in dispatchItems"
              :key="item.id"
              class="incident-detail__dispatch-row"
            >
              <el-select v-model="item.resourceId" placeholder="选择资源" style="width: 320px">
                <el-option
                  v-for="res in resourceStore.resourceList"
                  :key="res.resourceId"
                  :label="`${res.resourceName}（总${res.totalStock}，已调度${res.lockedStock ?? 0}，剩${res.availableStock ?? 0}）`"
                  :value="res.resourceId"
                />
              </el-select>
              <el-input-number v-model="item.quantity" :min="1" :step="1" :precision="0" style="margin-left: 12px" />
              <el-button
                v-if="dispatchItems.length > 1"
                type="danger"
                link
                style="margin-left: 12px"
                @click="removeDispatchItem(item.id)"
              >删除</el-button>
            </div>
          </div>
          <div style="margin-top: 8px">
            <el-button type="primary" plain @click="addDispatchItem">+ 添加资源</el-button>
          </div>
          <div style="margin-top: 16px; display: flex; gap: 8px">
            <el-button type="primary" @click="handleDispatchResource">提交调度</el-button>
            <el-button type="primary" plain @click="handleRequestResource">请求资源</el-button>
          </div>
        </el-card>

        <div class="incident-detail__actions" v-if="canRejectPlan">
          <el-button type="danger" @click="handleRejectPlan">驳回方案</el-button>
        </div>

        <div class="incident-detail__actions" v-if="canCompleteIncident">
          <el-button type="success" @click="handleCompleteIncident">处置结束</el-button>
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

.disposal-plan-content-scroll {
  max-height: 400px;
  overflow-y: auto;
  padding: var(--spacing-sm) var(--spacing-md);
  background-color: var(--color-bg-page);
  border-radius: var(--border-radius-sm);
  border: 1px solid var(--color-border-light);
}

.markdown-body-preview {
  font-size: var(--font-size-sm);
  line-height: 1.8;
  word-break: break-all;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  color: var(--color-text-regular);
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

.incident-detail__dispatch-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.incident-detail__dispatch-row {
  display: flex;
  align-items: center;
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
