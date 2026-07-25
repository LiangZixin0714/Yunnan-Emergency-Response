<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { usePlanStore } from '@/stores/plan'
import { useIncidentStore } from '@/stores/incident'
import { useDisposalPlanStore } from '@/stores/disposal-plan'
import { PlanStatusLabel, PlanStatusTagType, DisposalPlanStatusLabel, DisposalPlanStatusTagType } from '@/types/enums'
import type { PlanStatusValue, DisposalPlanStatusValue } from '@/types/enums'
import { ElMessage, ElLoading } from 'element-plus'
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
const streamMode = ref(false)
const editableContent = ref('')
const editorRef = ref<HTMLDivElement | null>(null)
let vditorInstance: Vditor | null = null

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
  streamMode.value = true
  editableContent.value = ''
  if (vditorInstance) {
    vditorInstance.setValue('')
  }
  planStore.startStream(selectedIncidentId.value)
  const stopWatch = watch(() => planStore.streaming, (newVal, oldVal) => {
    if (oldVal === true && newVal === false) {
      streamMode.value = false
      if (planStore.streamingContent) {
        editableContent.value = planStore.streamingContent
        updateEditorContent(planStore.streamingContent)
        ElMessage.success('方案已生成完毕')
      } else {
        ElMessage.error('连接已断开，已保存已接收的内容')
      }
      stopWatch()
    }
  })
}

function handleStopStream(): void {
  planStore.stopStream()
  streamMode.value = false
  if (selectedIncidentId.value) {
    planStore.fetchList(selectedIncidentId.value)
  }
}

async function selectPlan(planId: string): Promise<void> {
  streamMode.value = false
  selectedPlanId.value = planId
  await planStore.fetchDetail(planId)
  editableContent.value = planStore.currentPlan?.planContent || ''
  updateEditorContent(editableContent.value)
}

async function handleSaveDraft(): Promise<void> {
  const content = vditorInstance?.getValue() || editableContent.value
  if (!content.trim()) {
    ElMessage.warning('方案内容不能为空')
    return
  }
  try {
    await disposalPlanStore.saveDisposalPlan(
      planStore.currentPlan?.id ?? 0,
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
  const content = vditorInstance?.getValue() || editableContent.value
  if (!content.trim()) {
    ElMessage.warning('方案内容不能为空，请先拟定处置方案')
    return
  }
  try {
    await disposalPlanStore.submitDisposalPlan(
      planStore.currentPlan?.id ?? 0,
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

watch(() => planStore.streamingContent, (val) => {
  if (planStore.streaming) {
    editableContent.value = val
    updateEditorContent(val)
  }
})

onMounted(() => {
  loadIncidents()
  nextTick(() => {
    initEditor()
  })
  const incidentId = route.query.incidentId as string | undefined
  if (incidentId) {
    selectedIncidentId.value = incidentId
    loadPlans()
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
              :label="item.incidentName"
              :value="item.incidentId"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="planStore.streaming"
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

        <div v-if="!planStore.currentPlan && !editableContent" class="plan-page__empty">请选择或生成方案</div>
        <div v-else>
          <template v-if="planStore.currentPlan">
            <h3>{{ planStore.currentPlan.planTitle }}</h3>
            <div class="plan-page__content-meta">
              <span>生成时间：{{ planStore.currentPlan.generateTime ? formatDate(planStore.currentPlan.generateTime) : '-' }}</span>
              <el-tag size="small" :type="PlanStatusTagType[planStore.currentPlan.status as PlanStatusValue] ?? 'info'">
                {{ PlanStatusLabel[planStore.currentPlan.status as PlanStatusValue] }}
              </el-tag>
            </div>
            <el-divider />
          </template>
          <div ref="editorRef" class="plan-page__editor"></div>
          <div class="plan-page__draft-submit-area" v-if="editableContent">
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