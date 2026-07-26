<script setup lang="ts">
import { ref, onMounted, watch, nextTick, computed } from 'vue'
import { useRoute } from 'vue-router'
import { usePlanStore } from '@/stores/plan'
import { useIncidentStore } from '@/stores/incident'
import { useDisposalPlanStore } from '@/stores/disposal-plan'
import { PlanStatusLabel, PlanStatusTagType, DisposalPlanStatusLabel, DisposalPlanStatusTagType } from '@/types/enums'
import type { PlanStatusValue, DisposalPlanStatusValue } from '@/types/enums'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import { formatDate } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import { exportPdf, exportWord } from '@/api/disposal-plan'

const route = useRoute()
const planStore = usePlanStore()
const incidentStore = useIncidentStore()
const disposalPlanStore = useDisposalPlanStore()

const selectedIncidentId = ref('')
const selectedPlanId = ref('')
const editableContent = ref('')
const editorRef = ref<HTMLDivElement | null>(null)
let vditorInstance: Vditor | null = null

const selectedIncident = computed(() => {
  return incidentStore.incidentList.find((i) => i.incidentId === selectedIncidentId.value)
})

const isIncidentCompleted = computed(() => {
  return selectedIncident.value?.status === 'completed'
})

const disposalPlanStatusMap = Object.fromEntries(
  Object.entries(DisposalPlanStatusLabel).map(([key, label]) => [
    key,
    { label, type: DisposalPlanStatusTagType[key as DisposalPlanStatusValue] },
  ])
)
function initEditor(): void {
  if (!editorRef.value) return
  vditorInstance = new Vditor(editorRef.value, {
    height: 500,
    mode: 'sv',
    theme: 'classic',
    // 【必须加上 cache.id，防止 Vditor 内部报错崩溃】
    cache: {
      enable: false, // 或者给个唯一字符串 id，比如 id: 'disposal-plan-editor'
    },
    toolbar: ['bold', 'italic', 'heading', 'list', 'quote', 'link', 'hr'],
    preview: {
      theme: {
        current: 'classic',
      },
    },
    input: (value) => {
      editableContent.value = value
    },
  })
}
// function initEditor(): void {
//   if (!editorRef.value) return
//   vditorInstance = new Vditor(editorRef.value, {
//     height: 500,
//     mode: 'sv',
//     theme: 'classic',
//     toolbar: ['bold', 'italic', 'heading', 'list', 'quote', 'link', 'hr'],
//     preview: {
//       theme: {
//         current: 'classic',
//       },
//     },
//     input: (value) => {
//       editableContent.value = value
//     },
//   })
// }

function updateEditorContent(content: string): void {
  if (vditorInstance) {
    vditorInstance.setValue(content)
  }
}

async function loadIncidents(): Promise<void> {
  await incidentStore.fetchList({ page: 1, size: 100 })
}

async function loadPlans(): Promise<void> {
  if (!selectedIncidentId.value) return
  selectedPlanId.value = ''
  planStore.currentPlan = null
  editableContent.value = ''
  if (vditorInstance) {
    vditorInstance.setValue('')
  }
  await planStore.fetchList(selectedIncidentId.value)
}

async function handleGenerate(): Promise<void> {
  if (!selectedIncidentId.value) {
    ElMessage.warning('请先选择灾情事件')
    return
  }
  if (isIncidentCompleted.value) {
    ElMessage.warning('已结束的事件不可再生成处置方案')
    return
  }
  editableContent.value = ''
  if (vditorInstance) {
    vditorInstance.setValue('')
  }
  planStore.startStream(selectedIncidentId.value)
}

function handleStopStream(): void {
  planStore.stopStream()
  if (selectedIncidentId.value) {
    planStore.fetchList(selectedIncidentId.value)
  }
}

async function selectPlan(planId: string): Promise<void> {
  selectedPlanId.value = planId
  await planStore.fetchDetail(planId)
  editableContent.value = planStore.currentPlan?.planContent || ''
  if (vditorInstance) {
    updateEditorContent(editableContent.value)
  }
}

// async function handleSaveDraft(): Promise<void> {
//   const content = vditorInstance?.getValue() || editableContent.value
//   if (!content.trim()) {
//     ElMessage.warning('方案内容不能为空')
//     return
//   }
//   try {
//     await disposalPlanStore.saveDisposalPlan(
//       planStore.currentPlan?.id ?? 0,
//       content,
//       selectedIncidentId.value
//     )
//     ElMessage.success('草稿已保存')
//     if (selectedIncidentId.value) await planStore.fetchList(selectedIncidentId.value)
//   } catch {
//     ElMessage.error('草稿保存失败，请重试')
//   }
// }

// async function handleSubmitDisposalPlan(): Promise<void> {
//   const content = vditorInstance?.getValue() || editableContent.value
//   if (!content.trim()) {
//     ElMessage.warning('方案内容不能为空，请先拟定处置方案')
//     return
//   }
//   try {
//     await disposalPlanStore.submitDisposalPlan(
//       planStore.currentPlan?.id ?? 0,
//       content,
//       selectedIncidentId.value
//     )
//     ElMessage.success('处置方案已提交给资源管理员')
//     if (selectedIncidentId.value) await planStore.fetchList(selectedIncidentId.value)
//   } catch {
//     ElMessage.error('提交失败，请稍后重试')
//   }
// }
// async function handleSaveDraft(): Promise<void> {
//   const content = vditorInstance?.getValue() || editableContent.value
//   if (!content.trim()) {
//     ElMessage.warning('方案内容不能为空')
//     return
//   }
//   // 【修复】：优先取 planId，如果没有则提示或兼容
//   const targetPlanId = planStore.currentPlan?.planId
//   if (!targetPlanId) {
//     ElMessage.warning('请先选择或生成一个有效的方案')
//     return
//   }

//   try {
//     await disposalPlanStore.saveDisposalPlan(
//       targetPlanId as any, // 如果后端要求 String 就传 string，要求 Long 根据实际后端调整
//       content,
//       selectedIncidentId.value
//     )
//     ElMessage.success('草稿已保存')
//     if (selectedIncidentId.value) await planStore.fetchList(selectedIncidentId.value)
//   } catch {
//     ElMessage.error('草稿保存失败，请重试')
//   }
// }

// async function handleSubmitDisposalPlan(): Promise<void> {
//   const content = vditorInstance?.getValue() || editableContent.value
//   if (!content.trim()) {
//     ElMessage.warning('方案内容不能为空，请先拟定处置方案')
//     return
//   }
  
//   const targetPlanId = planStore.currentPlan?.planId
//   if (!targetPlanId) {
//     ElMessage.warning('请先选择或生成一个有效的方案')
//     return
//   }

//   try {
//     await disposalPlanStore.submitDisposalPlan(
//       targetPlanId as any,
//       content,
//       selectedIncidentId.value
//     )
//     ElMessage.success('处置方案已提交给资源管理员')
//     if (selectedIncidentId.value) await planStore.fetchList(selectedIncidentId.value)
//   } catch {
//     ElMessage.error('提交失败，请稍后重试')
//   }
// }
async function handleSaveDraft(): Promise<void> {
  const content = vditorInstance?.getValue() || editableContent.value
  if (!content.trim()) {
    ElMessage.warning('方案内容不能为空')
    return
  }
  
  // 【关键修复】：必须取数字自增主键 id，而不是字符串 planId
  const targetId = planStore.currentPlan?.id
  if (!targetId) {
    ElMessage.warning('请先选择或生成一个有效的方案')
    return
  }

  try {
    await disposalPlanStore.saveDisposalPlan(
      targetId, // 传数字 Long id
      content,
      selectedIncidentId.value
    )
    ElMessage.success('草稿已保存')
    if (selectedIncidentId.value) await planStore.fetchList(selectedIncidentId.value)
  } catch {
    ElMessage.error('草稿保存失败，请重试')
  }
}

async function handleSubmitDisposalPlan(): Promise<void> {
  if (isIncidentCompleted.value) {
    ElMessage.warning('已结束的事件不可再提交处置方案')
    return
  }
  const content = vditorInstance?.getValue() || editableContent.value
  if (!content.trim()) {
    ElMessage.warning('方案内容不能为空，请先拟定处置方案')
    return
  }
  
  const targetId = planStore.currentPlan?.id
  if (!targetId) {
    ElMessage.warning('请先选择或生成一个有效的方案')
    return
  }

  try {
    await disposalPlanStore.submitDisposalPlan(
      targetId,
      content,
      selectedIncidentId.value
    )
    ElMessage.success('处置方案已提交给资源管理员')
    if (selectedIncidentId.value) await planStore.fetchList(selectedIncidentId.value)
  } catch {
    ElMessage.error('提交失败，请稍后重试')
  }
}

async function handleExportPdf(planId: string): Promise<void> {
  const loading = ElLoading.service({ text: '正在生成PDF...' })
  try {
    const response = await exportPdf(planId)
    const blob = new Blob([response], { type: 'application/pdf' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `处置方案_${planId}.pdf`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('PDF导出成功')
  } catch {
    ElMessage.error('PDF导出失败，请重试')
  } finally {
    loading.close()
  }
}

async function handleExportWord(planId: string): Promise<void> {
  const loading = ElLoading.service({ text: '正在生成Word...' })
  try {
    const response = await exportWord(planId)
    const blob = new Blob([response], { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `处置方案_${planId}.docx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('Word导出成功')
  } catch {
    ElMessage.error('Word导出失败，请重试')
  } finally {
    loading.close()
  }
}

async function handleDeletePlan(planId: string): Promise<void> {
  try {
    await ElMessageBox.confirm('确认删除该方案？删除后无法恢复。', '确认删除', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await planStore.removePlan(planId)
    ElMessage.success('方案已删除')
    editableContent.value = ''
    if (vditorInstance) {
      vditorInstance.setValue('')
    }
  } catch {
    ElMessage.info('已取消删除')
  }
}

watch(() => planStore.streamingContent, (val) => {
  if (planStore.streaming) {
    editableContent.value = val
  }
})

watch(() => planStore.streaming, (newVal, oldVal) => {
  if (oldVal === true && newVal === false) {
    if (planStore.streamingContent) {
      editableContent.value = planStore.streamingContent
      if (vditorInstance) {
        updateEditorContent(planStore.streamingContent)
      }
      ElMessage.success('方案已生成完毕')
    } else {
      ElMessage.error('连接已断开，已保存已接收的内容')
    }
    if (selectedIncidentId.value) {
      planStore.fetchList(selectedIncidentId.value)
    }
  }
})

onMounted(() => {
  loadIncidents()
  const incidentId = route.query.incidentId as string | undefined
  if (incidentId) {
    selectedIncidentId.value = incidentId
    loadPlans()
  }
})

watch(selectedIncidentId, (newVal) => {
  if (newVal && !vditorInstance) {
    nextTick(() => {
      initEditor()
    })
  }
})
</script>

<template>
  <div class="plan-page page-container">
    <div class="page-header">
      <h2 class="page-header__title">AI方案生成</h2>
    </div>

    <el-card shadow="hover" class="plan-page__selector">
      <el-form inline>
        <el-form-item label="选择灾情事件">
          <el-select
            v-model="selectedIncidentId"
            placeholder="请选择灾情事件"
            style="width: 360px"
            @change="loadPlans"
          >
            <el-option
              v-for="item in incidentStore.incidentList"
              :key="item.incidentId"
              :label="item.incidentName + (item.status === 'completed' ? ' (已结束)' : '')"
              :value="item.incidentId"
              :disabled="item.status === 'completed'"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="planStore.streaming"
            :disabled="isIncidentCompleted"
            @click="handleGenerate"
          >
            {{ planStore.streaming ? '生成中...' : '生成' }}
          </el-button>
          <el-button
            v-if="planStore.streaming"
            type="danger"
            @click="handleStopStream"
          >
            停止
          </el-button>
        </el-form-item>
      </el-form>
      <el-alert
        v-if="isIncidentCompleted"
        type="warning"
        :closable="false"
        show-icon
        style="margin-top: 12px"
      >
        该事件已结束，不可再生成或提交处置方案
      </el-alert>
    </el-card>

    <div class="plan-page__body" v-if="selectedIncidentId">
      <el-card shadow="hover" class="plan-page__list">
        <template #header>
          <span>历史方案</span>
        </template>
        <div v-if="planStore.planList.length === 0" class="plan-page__empty">暂无方案</div>
        <div
          v-for="plan in planStore.planList"
          :key="plan.planId"
          class="plan-page__item"
          :class="{ 'plan-page__item--active': selectedPlanId === plan.planId }"
          @click="selectPlan(plan.planId)"
        >
          <div class="plan-page__item-title">{{ plan.planTitle }}</div>
          <div class="plan-page__item-meta">
            <span>灾情：{{ plan.incidentId }}</span>
            <span>{{ plan.generateTime ? formatDate(plan.generateTime, 'YYYY-MM-DD HH:mm') : '-' }}</span>
            <el-tag size="small" :type="PlanStatusTagType[plan.status as PlanStatusValue] ?? 'info'">
              {{ PlanStatusLabel[plan.status as PlanStatusValue] ?? plan.status }}
            </el-tag>
          </div>
          <div class="plan-page__item-actions">
              <el-button
                size="small"
                type="primary"
                plain
                @click.stop="handleExportPdf(plan.planId)"
              >
                PDF
              </el-button>
              <el-button
                size="small"
                type="success"
                plain
                @click.stop="handleExportWord(plan.planId)"
              >
                Word
              </el-button>
              <el-button
                size="small"
                type="danger"
                plain
                @click.stop="handleDeletePlan(plan.planId)"
              >
                删除
              </el-button>
            </div>
        </div>
      </el-card>

      <el-card shadow="hover" class="plan-page__detail">
        <template #header>
          <div class="plan-page__detail-header">
            <div class="plan-page__detail-header-left">
              <span>方案详情</span>
              <StatusTag
                v-if="planStore.currentPlan"
                :status="planStore.currentPlan.status"
                :status-map="disposalPlanStatusMap"
              />
            </div>
          </div>
        </template>

        <div v-show="!planStore.currentPlan && !editableContent && !planStore.streaming" class="plan-page__empty">请选择或生成方案</div>
        
        <div v-show="planStore.currentPlan || editableContent || planStore.streaming">
          <template v-if="planStore.currentPlan">
            <h3>{{ planStore.currentPlan.planTitle }}</h3>
            <div class="plan-page__content-meta">
              <span>灾情ID：{{ planStore.currentPlan.incidentId }}</span>
              <span>生成时间：{{ planStore.currentPlan.generateTime ? formatDate(planStore.currentPlan.generateTime) : '-' }}</span>
              <el-tag size="small" :type="PlanStatusTagType[planStore.currentPlan.status as PlanStatusValue] ?? 'info'">
                {{ PlanStatusLabel[planStore.currentPlan.status as PlanStatusValue] }}
              </el-tag>
            </div>
            <el-divider />
          </template>

          <div v-if="planStore.streaming" class="plan-page__streaming-content">
            <div class="plan-page__streaming-content-inner">{{ planStore.streamingContent }}</div>
          </div>

          <div v-show="!planStore.streaming" ref="editorRef" class="plan-page__editor"></div>

          <div class="plan-page__draft-submit-area" v-if="editableContent && !planStore.streaming">
            <el-button type="primary" plain @click="handleSaveDraft">保存草稿</el-button>
            <el-button type="primary" @click="handleSubmitDisposalPlan">确认提交</el-button>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.plan-page__selector {
  margin-bottom: var(--spacing-md);
}

.plan-page__body {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: var(--spacing-md);
  min-height: 500px;
}

.plan-page__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: var(--color-text-secondary);
}

.plan-page__item {
  padding: var(--spacing-md);
  border-radius: var(--border-radius-sm);
  cursor: pointer;
  transition: background-color var(--transition-duration);
  border-bottom: 1px solid var(--color-border-light);
}

.plan-page__item:hover {
  background-color: var(--color-bg-page);
}

.plan-page__item--active {
  background-color: rgba(26, 115, 232, 0.08);
  border-left: 3px solid var(--color-primary);
}

.plan-page__item-title {
  font-weight: 500;
  margin-bottom: var(--spacing-xs);
}

.plan-page__item-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-xs);
}

.plan-page__item-actions {
  display: flex;
  gap: var(--spacing-xs);
}

.plan-page__detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.plan-page__detail-header-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.plan-page__editor {
  min-height: 500px;
}

.plan-page__streaming-content {
  min-height: 500px;
  padding: var(--spacing-md);
  background-color: var(--color-bg-page);
  border-radius: var(--border-radius-sm);
  font-size: var(--font-size-sm);
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-all;
  overflow-y: auto;
}

.plan-page__streaming-content-inner {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.plan-page__detail h3 {
  font-size: var(--font-size-xl);
  margin-bottom: var(--spacing-sm);
}

.plan-page__content-meta {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.plan-page__draft-submit-area {
  margin-top: var(--spacing-md);
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--color-border-light);
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-sm);
}
</style>