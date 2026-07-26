<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useIncidentStore } from '@/stores/incident'
import { useResourceStore } from '@/stores/resource'
import { submitResourceRequest } from '@/api/resource-request'
import { DisasterTypeLabel, IncidentLevelLabel } from '@/types/enums'
import type { DisasterTypeValue, IncidentLevelValue } from '@/types/enums'
import { ElMessage } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'

interface ResourceFormItem {
  id: number
  resourceId: string
  quantity: number
}

const route = useRoute()
const router = useRouter()
const incidentStore = useIncidentStore()
const resourceStore = useResourceStore()

const incidentId = route.params.incidentId as string
const submitting = ref(false)

const resourceItems = ref<ResourceFormItem[]>([
  { id: 1, resourceId: '', quantity: 1 }
])
let itemIdCounter = 1

function addResourceItem(): void {
  itemIdCounter++
  resourceItems.value.push({ id: itemIdCounter, resourceId: '', quantity: 1 })
}

function removeResourceItem(id: number): void {
  if (resourceItems.value.length <= 1) {
    ElMessage.warning('至少保留一行资源选择条目')
    return
  }
  const index = resourceItems.value.findIndex((item) => item.id === id)
  if (index !== -1) {
    resourceItems.value.splice(index, 1)
  }
}

function getSelectedResourceIds(excludeId?: number): string[] {
  return resourceItems.value
    .filter((item) => item.id !== excludeId && item.resourceId)
    .map((item) => item.resourceId)
}

function getResourceName(resourceId: string): string {
  const res = resourceStore.resourceList.find((r) => r.resourceId === resourceId)
  return res?.resourceName || ''
}

async function handleSubmitRequest(): Promise<void> {
  const validItems = resourceItems.value.filter((item) => item.resourceId)

  if (validItems.length === 0) {
    ElMessage.warning('请至少选择一种申请资源')
    return
  }

  const resourceIds = validItems.map((item) => item.resourceId)
  if (new Set(resourceIds).size !== resourceIds.length) {
    ElMessage.warning('不允许重复选择同一种资源')
    return
  }

  for (const item of validItems) {
    if (!item.quantity || item.quantity <= 0 || !Number.isInteger(item.quantity)) {
      ElMessage.warning('申请数量必须为正整数')
      return
    }
  }

  submitting.value = true
  try {
    const items = validItems.map((item) => ({
      resourceId: item.resourceId,
      resourceName: getResourceName(item.resourceId),
      quantity: item.quantity,
    }))
    await submitResourceRequest({ incidentId, items })
    ElMessage.success('资源申请已提交，等待系统管理员处理')
    router.replace(`/incident/${incidentId}`)
  } catch {
    ElMessage.error('资源申请提交失败，请重试')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  incidentStore.fetchDetail(incidentId)
  resourceStore.fetchList()
})
</script>

<template>
  <div class="resource-request page-container">
    <div class="page-header">
      <h2 class="page-header__title">资源申请</h2>
      <el-button @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
    </div>

    <el-card v-if="incidentStore.currentIncident" shadow="hover" v-loading="incidentStore.loading">
      <template #header>
        <span>灾情信息</span>
      </template>
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
        <el-descriptions-item label="发生地点">
          {{ incidentStore.currentIncident.location }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="hover" class="resource-request__form-card">
      <template #header>
        <span>资源申请表单</span>
      </template>
      <div class="resource-request__items">
        <div
          v-for="item in resourceItems"
          :key="item.id"
          class="resource-request__item-row"
        >
          <el-select
            v-model="item.resourceId"
            placeholder="请选择资源"
            filterable
            class="resource-request__select"
          >
            <el-option
              v-for="res in resourceStore.resourceList"
              :key="res.resourceId"
              :label="`${res.resourceName}（总数${res.totalStock}，已调度${res.lockedStock ?? 0}）`"
              :value="res.resourceId"
              :disabled="getSelectedResourceIds(item.id).includes(res.resourceId)"
            />
          </el-select>
          <el-input-number
            v-model="item.quantity"
            :min="1"
            :step="1"
            :precision="0"
            class="resource-request__quantity"
          />
          <el-button
            type="danger"
            :icon="Delete"
            circle
            :disabled="resourceItems.length <= 1"
            @click="removeResourceItem(item.id)"
          />
        </div>
      </div>
      <div class="resource-request__actions">
        <el-button type="primary" plain @click="addResourceItem">添加资源</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitRequest">提交申请</el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.resource-request__form-card {
  margin-top: var(--spacing-md);
}
.resource-request__items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.resource-request__item-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.resource-request__select {
  flex: 1;
}
.resource-request__quantity {
  width: 160px;
}
.resource-request__actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}
</style>
